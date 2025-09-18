package com.anthropic.claudecode;

import com.anthropic.claudecode.exceptions.CLIConnectionError;
import com.anthropic.claudecode.exceptions.ClaudeSDKException;
import com.anthropic.claudecode.internal.Query;
import com.anthropic.claudecode.messages.Message;
import com.anthropic.claudecode.messages.ResultMessage;
import com.anthropic.claudecode.transport.SubprocessCLITransport;
import com.anthropic.claudecode.transport.Transport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-level client providing interactive access to Claude Code.
 */
public class ClaudeSDKClient implements AutoCloseable {
    private static final Flow.Publisher<Map<String, Object>> EMPTY_STREAM = subscriber ->
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    // No-op - stream stays open without emitting values
                }

                @Override
                public void cancel() {
                    // No-op
                }
            });

    private final ObjectMapper mapper = new ObjectMapper();
    private ClaudeCodeOptions options;
    private Transport transport;
    private Query query;
    private SubmissionPublisher<Message> responsePublisher;
    private final AtomicBoolean connected = new AtomicBoolean();

    public ClaudeSDKClient() {
        this(new ClaudeCodeOptions());
    }

    public ClaudeSDKClient(ClaudeCodeOptions options) {
        this.options = options != null ? options : new ClaudeCodeOptions();
    }

    public void connect() throws ClaudeSDKException {
        connect((Flow.Publisher<Map<String, Object>>) null);
    }

    public void connect(String prompt) throws ClaudeSDKException {
        connectInternal(prompt, null);
    }

    public void connect(Flow.Publisher<Map<String, Object>> promptStream) throws ClaudeSDKException {
        connectInternal(null, promptStream);
    }

    private void connectInternal(String promptString, Flow.Publisher<Map<String, Object>> promptStream)
            throws ClaudeSDKException {
        if (connected.get()) {
            return;
        }
        Flow.Publisher<Map<String, Object>> actualStream = promptStream;
        boolean streaming = promptStream != null;
        if (promptString == null && promptStream == null) {
            streaming = true;
            actualStream = EMPTY_STREAM;
        }
        if (options.getCanUseTool() != null && !streaming) {
            throw new CLIConnectionError(
                    "canUseTool callback requires streaming mode. Provide a streaming prompt instead of a string.");
        }
        if (options.getCanUseTool() != null && options.getPermissionPromptToolName() != null) {
            throw new CLIConnectionError(
                    "canUseTool callback cannot be used with permission_prompt_tool_name simultaneously.");
        }
        ClaudeCodeOptions effectiveOptions = options.copy();
        if (options.getCanUseTool() != null) {
            effectiveOptions.setPermissionPromptToolName("stdio");
        }
        this.transport = new SubprocessCLITransport(streaming, promptString, effectiveOptions);
        transport.connect();

        this.query = new Query(
                transport,
                streaming,
                effectiveOptions.getCanUseTool(),
                effectiveOptions.getHooks());
        query.start();
        query.initialize();
        this.responsePublisher = query.getPublisher();
        connected.set(true);

        if (streaming && actualStream != null) {
            query.streamInput(actualStream);
        }
    }

    public Flow.Publisher<Message> receiveMessages() {
        ensureConnected();
        return responsePublisher;
    }

    public Flow.Publisher<Message> receiveResponse() {
        ensureConnected();
        SubmissionPublisher<Message> publisher = new SubmissionPublisher<>();
        responsePublisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            private boolean done;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Message item) {
                if (done) {
                    return;
                }
                publisher.submit(item);
                if (item instanceof ResultMessage) {
                    done = true;
                    subscription.cancel();
                    publisher.close();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (!done) {
                    publisher.closeExceptionally(throwable);
                }
            }

            @Override
            public void onComplete() {
                if (!done) {
                    publisher.close();
                }
            }
        });
        return publisher;
    }

    public void query(String prompt) throws ClaudeSDKException {
        query(prompt, "default");
    }

    public void query(String prompt, String sessionId) throws ClaudeSDKException {
        ensureConnected();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "user");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("role", "user");
        payload.put("content", prompt);
        message.put("message", payload);
        message.put("parent_tool_use_id", null);
        message.put("session_id", sessionId);
        try {
            transport.write(mapper.writeValueAsString(message) + "\n");
        } catch (JsonProcessingException e) {
            throw new CLIConnectionError("Failed to encode message", e);
        }
    }

    public void query(Flow.Publisher<Map<String, Object>> stream, String sessionId) throws ClaudeSDKException {
        ensureConnected();
        Flow.Publisher<Map<String, Object>> normalized = subscriber ->
                stream.subscribe(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscriber.onSubscribe(subscription);
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(Map<String, Object> item) {
                        Map<String, Object> copy = new LinkedHashMap<>(item);
                        copy.putIfAbsent("session_id", sessionId);
                        subscriber.onNext(copy);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
        query.streamInput(normalized);
    }

    public void interrupt() throws ClaudeSDKException {
        ensureConnected();
        query.interrupt();
    }

    public Map<String, Object> getServerInfo() throws ClaudeSDKException {
        ensureConnected();
        return query.getInitializationResult();
    }

    @Override
    public void close() throws ClaudeSDKException {
        disconnect();
    }

    public void disconnect() throws ClaudeSDKException {
        if (!connected.get()) {
            return;
        }
        query.close();
        connected.set(false);
    }

    private void ensureConnected() {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected. Call connect() first.");
        }
    }
}
