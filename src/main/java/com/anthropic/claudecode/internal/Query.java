package com.anthropic.claudecode.internal;

import com.anthropic.claudecode.CanUseTool;
import com.anthropic.claudecode.HookCallback;
import com.anthropic.claudecode.HookContext;
import com.anthropic.claudecode.HookEvent;
import com.anthropic.claudecode.HookMatcher;
import com.anthropic.claudecode.PermissionResultAllow;
import com.anthropic.claudecode.PermissionResultDeny;
import com.anthropic.claudecode.ToolPermissionContext;
import com.anthropic.claudecode.exceptions.CLIConnectionError;
import com.anthropic.claudecode.exceptions.ClaudeSDKException;
import com.anthropic.claudecode.messages.Message;
import com.anthropic.claudecode.messages.MessageParser;
import com.anthropic.claudecode.transport.Transport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Handles the control protocol and message routing for the Java SDK.
 */
public class Query implements AutoCloseable {
    private static final Duration CONTROL_TIMEOUT = Duration.ofSeconds(60);
    private static final Logger LOGGER = Logger.getLogger(Query.class.getName());

    private final Transport transport;
    private final boolean streamingMode;
    private final CanUseTool canUseTool;
    private final Map<HookEvent, List<HookMatcher>> hookConfig;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SubmissionPublisher<Message> publisher = new SubmissionPublisher<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "claude-query");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, CompletableFuture<Map<String, Object>>> pendingControlResponses =
            new ConcurrentHashMap<>();
    private final Map<String, HookCallback> hookCallbacks = new ConcurrentHashMap<>();
    private final AtomicInteger nextCallbackId = new AtomicInteger();
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();
    private Map<String, Object> initializationResult;

    public Query(Transport transport,
                 boolean streamingMode,
                 CanUseTool canUseTool,
                 Map<HookEvent, List<HookMatcher>> hookConfig) {
        this.transport = transport;
        this.streamingMode = streamingMode;
        this.canUseTool = canUseTool;
        this.hookConfig = hookConfig != null ? hookConfig : Collections.emptyMap();
    }

    public void start() {
        transport.readMessages(new Transport.MessageHandler() {
            @Override
            public void onMessage(Map<String, Object> message) {
                handleMessage(message);
            }

            @Override
            public void onError(Throwable error) {
                publisher.closeExceptionally(error);
            }

            @Override
            public void onClosed() {
                publisher.close();
            }
        });
    }

    private void handleMessage(Map<String, Object> message) {
        String type = String.valueOf(message.get("type"));
        if ("control_response".equals(type)) {
            handleControlResponse(message);
            return;
        }
        if ("control_request".equals(type)) {
            executor.submit(() -> handleControlRequest(message));
            return;
        }
        if ("control_cancel_request".equals(type)) {
            LOGGER.fine("Received control_cancel_request which is not yet supported");
            return;
        }
        try {
            Message parsed = MessageParser.parseMessage(message);
            publisher.submit(parsed);
        } catch (ClaudeSDKException e) {
            publisher.closeExceptionally(e);
        }
    }

    private void handleControlResponse(Map<String, Object> message) {
        Object responseObj = message.get("response");
        if (!(responseObj instanceof Map<?, ?> response)) {
            return;
        }
        String requestId = String.valueOf(response.get("request_id"));
        CompletableFuture<Map<String, Object>> future = pendingControlResponses.remove(requestId);
        if (future == null) {
            return;
        }
        String subtype = String.valueOf(response.get("subtype"));
        if ("error".equals(subtype)) {
            future.completeExceptionally(new CLIConnectionError(String.valueOf(response.get("error"))));
        } else {
            Object payload = response.get("response");
            if (payload instanceof Map<?, ?> map) {
                Map<String, Object> converted = new LinkedHashMap<>();
                map.forEach((k, v) -> converted.put(String.valueOf(k), v));
                future.complete(converted);
            } else {
                future.complete(new LinkedHashMap<>());
            }
        }
    }

    private void handleControlRequest(Map<String, Object> message) {
        Map<String, Object> request = toMap(message.get("request"));
        if (request == null) {
            return;
        }
        String subtype = String.valueOf(request.get("subtype"));
        switch (subtype) {
            case "can_use_tool" -> handleCanUseTool(message, request);
            case "hook_callback" -> handleHookCallback(message, request);
            case "mcp_message" -> handleMcpMessage(message, request);
            default -> sendErrorResponse(message, "Unsupported control request subtype: " + subtype);
        }
    }

    private void handleCanUseTool(Map<String, Object> envelope, Map<String, Object> request) {
        if (canUseTool == null) {
            sendErrorResponse(envelope, "canUseTool callback is not provided");
            return;
        }
        String toolName = String.valueOf(request.get("tool_name"));
        Map<String, Object> input = toMap(request.get("input"));
        ToolPermissionContext context = new ToolPermissionContext();
        context.setSuggestions(new ArrayList<>());
        canUseTool.apply(toolName, input != null ? input : Collections.emptyMap(), context)
                .toCompletableFuture()
                .whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        sendErrorResponse(envelope, error.getMessage());
                        return;
                    }
                    if (result instanceof PermissionResultAllow allow) {
                        Map<String, Object> responseData = new LinkedHashMap<>();
                        responseData.put("allow", true);
                        if (allow.getUpdatedInput() != null) {
                            responseData.put("input", allow.getUpdatedInput());
                        }
                        sendSuccessResponse(envelope, responseData);
                    } else if (result instanceof PermissionResultDeny deny) {
                        Map<String, Object> responseData = new LinkedHashMap<>();
                        responseData.put("allow", false);
                        responseData.put("reason", deny.getMessage());
                        if (deny.isInterrupt()) {
                            responseData.put("interrupt", true);
                        }
                        sendSuccessResponse(envelope, responseData);
                    } else {
                        sendErrorResponse(envelope, "Invalid PermissionResult type");
                    }
                }, executor);
    }

    private void handleHookCallback(Map<String, Object> envelope, Map<String, Object> request) {
        String callbackId = String.valueOf(request.get("callback_id"));
        HookCallback callback = hookCallbacks.get(callbackId);
        if (callback == null) {
            sendErrorResponse(envelope, "No hook callback found for ID: " + callbackId);
            return;
        }
        Map<String, Object> input = toMap(request.get("input"));
        String toolUseId = request.get("tool_use_id") != null ? String.valueOf(request.get("tool_use_id")) : null;
        callback.apply(input != null ? input : Collections.emptyMap(), toolUseId, new HookContext())
                .toCompletableFuture()
                .whenCompleteAsync((output, error) -> {
                    if (error != null) {
                        sendErrorResponse(envelope, error.getMessage());
                        return;
                    }
                    Map<String, Object> response = new LinkedHashMap<>();
                    if (output.getDecision() != null) {
                        response.put("decision", output.getDecision());
                    }
                    if (output.getSystemMessage() != null) {
                        response.put("systemMessage", output.getSystemMessage());
                    }
                    if (output.getHookSpecificOutput() != null) {
                        response.put("hookSpecificOutput", output.getHookSpecificOutput());
                    }
                    sendSuccessResponse(envelope, response);
                }, executor);
    }

    private void handleMcpMessage(Map<String, Object> envelope, Map<String, Object> request) {
        Map<String, Object> message = toMap(request.get("message"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", message != null ? message.get("id") : null);
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", -32601);
        error.put("message", "SDK MCP servers are not supported in the Java SDK yet.");
        response.put("error", error);
        sendSuccessResponse(envelope, Map.of("mcp_response", response));
    }

    private void sendSuccessResponse(Map<String, Object> envelope, Map<String, Object> responseData) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "control_response");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subtype", "success");
        payload.put("request_id", envelope.get("request_id"));
        payload.put("response", responseData);
        response.put("response", payload);
        sendRaw(response);
    }

    private void sendErrorResponse(Map<String, Object> envelope, String error) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "control_response");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subtype", "error");
        payload.put("request_id", envelope.get("request_id"));
        payload.put("error", error);
        response.put("response", payload);
        sendRaw(response);
    }

    private void sendRaw(Map<String, Object> data) {
        try {
            transport.write(mapper.writeValueAsString(data) + "\n");
        } catch (ClaudeSDKException | JsonProcessingException e) {
            publisher.closeExceptionally(e);
        }
    }

    public Map<String, Object> initialize() throws ClaudeSDKException {
        if (!streamingMode) {
            return null;
        }
        Map<String, Object> hooksPayload = new LinkedHashMap<>();
        hookConfig.forEach((event, matchers) -> {
            List<Map<String, Object>> matcherConfigs = new ArrayList<>();
            for (HookMatcher matcher : matchers) {
                Map<String, Object> matcherConfig = new LinkedHashMap<>();
                matcherConfig.put("matcher", matcher.getMatcher());
                List<String> callbackIds = new ArrayList<>();
                if (matcher.getHooks() != null) {
                    for (HookCallback callback : matcher.getHooks()) {
                        String id = "hook_" + nextCallbackId.getAndIncrement();
                        hookCallbacks.put(id, callback);
                        callbackIds.add(id);
                    }
                }
                matcherConfig.put("hookCallbackIds", callbackIds);
                matcherConfigs.add(matcherConfig);
            }
            hooksPayload.put(event.getValue(), matcherConfigs);
        });
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("subtype", "initialize");
        if (!hooksPayload.isEmpty()) {
            request.put("hooks", hooksPayload);
        }
        Map<String, Object> response = sendControlRequest(request);
        initializationResult = response;
        return response;
    }

    public SubmissionPublisher<Message> getPublisher() {
        return publisher;
    }

    public Map<String, Object> getInitializationResult() {
        return initializationResult;
    }

    public Map<String, Object> sendControlRequest(Map<String, Object> request) throws ClaudeSDKException {
        if (!streamingMode) {
            throw new CLIConnectionError("Control requests require streaming mode");
        }
        String requestId = "req_" + requestCounter.incrementAndGet() + "_" + UUID.randomUUID();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingControlResponses.put(requestId, future);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "control_request");
        envelope.put("request_id", requestId);
        envelope.put("request", request);
        try {
            transport.write(mapper.writeValueAsString(envelope) + "\n");
        } catch (ClaudeSDKException | JsonProcessingException e) {
            pendingControlResponses.remove(requestId);
            throw new CLIConnectionError("Failed to send control request", e);
        }
        try {
            return future.get(CONTROL_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingControlResponses.remove(requestId);
            throw new CLIConnectionError(
                    "Control request timeout: " + request.getOrDefault("subtype", "unknown"), e);
        } catch (Exception e) {
            Throwable cause = e instanceof CompletionException ? e.getCause() : e;
            if (cause instanceof ClaudeSDKException ce) {
                throw ce;
            }
            throw new CLIConnectionError("Control request failed", cause);
        }
    }

    public void interrupt() throws ClaudeSDKException {
        sendControlRequest(Map.of("subtype", "interrupt"));
    }

    public void setPermissionMode(String mode) throws ClaudeSDKException {
        sendControlRequest(Map.of("subtype", "set_permission_mode", "mode", mode));
    }

    public CompletableFuture<Void> streamInput(Flow.Publisher<Map<String, Object>> stream) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        stream.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Map<String, Object> item) {
                try {
                    transport.write(mapper.writeValueAsString(item) + "\n");
                } catch (ClaudeSDKException | JsonProcessingException e) {
                    if (subscription != null) {
                        subscription.cancel();
                    }
                    result.completeExceptionally(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                try {
                    transport.endInput();
                    result.complete(null);
                } catch (ClaudeSDKException e) {
                    result.completeExceptionally(e);
                }
            }
        });
        return result;
    }

    @Override
    public void close() throws ClaudeSDKException {
        if (closed.compareAndSet(false, true)) {
            publisher.close();
            executor.shutdownNow();
            transport.close();
        }
    }

    private Map<String, Object> toMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((k, v) -> converted.put(String.valueOf(k), v));
            return converted;
        }
        return null;
    }
}
