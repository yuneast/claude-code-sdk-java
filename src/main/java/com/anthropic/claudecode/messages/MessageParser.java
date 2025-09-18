package com.anthropic.claudecode.messages;

import com.anthropic.claudecode.exceptions.MessageParseError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting raw CLI JSON payloads into strongly typed messages.
 */
public final class MessageParser {
    private MessageParser() {}

    @SuppressWarnings("unchecked")
    public static Message parseMessage(Map<String, Object> data) throws MessageParseError {
        if (data == null) {
            throw new MessageParseError("Message payload was null", null);
        }
        Object typeObj = data.get("type");
        if (!(typeObj instanceof String)) {
            throw new MessageParseError("Message missing 'type' field", data);
        }
        String messageType = (String) typeObj;
        switch (messageType) {
            case "user":
                return parseUserMessage(data);
            case "assistant":
                return parseAssistantMessage(data);
            case "system":
                return parseSystemMessage(data);
            case "result":
                return parseResultMessage(data);
            default:
                throw new MessageParseError("Unknown message type: " + messageType, data);
        }
    }

    private static Message parseUserMessage(Map<String, Object> data) throws MessageParseError {
        try {
            Map<String, Object> message = getMap(data, "message");
            Object content = message.get("content");
            if (content instanceof List<?>) {
                List<ContentBlock> blocks = new ArrayList<>();
                for (Object blockObj : (List<?>) content) {
                    blocks.add(parseContentBlock(blockObj));
                }
                return new UserMessage(blocks);
            }
            return new UserMessage(content);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new MessageParseError("Invalid user message payload", data, e);
        }
    }

    private static Message parseAssistantMessage(Map<String, Object> data) throws MessageParseError {
        try {
            Map<String, Object> message = getMap(data, "message");
            Object contentObj = message.get("content");
            if (!(contentObj instanceof List<?> contentList)) {
                throw new MessageParseError("Assistant message missing content blocks", data);
            }
            List<ContentBlock> blocks = new ArrayList<>();
            for (Object item : contentList) {
                blocks.add(parseContentBlock(item));
            }
            Object model = message.get("model");
            if (!(model instanceof String modelStr)) {
                throw new MessageParseError("Assistant message missing model", data);
            }
            return new AssistantMessage(blocks, modelStr);
        } catch (ClassCastException e) {
            throw new MessageParseError("Invalid assistant message payload", data, e);
        }
    }

    private static Message parseSystemMessage(Map<String, Object> data) throws MessageParseError {
        Object subtype = data.get("subtype");
        if (!(subtype instanceof String subtypeStr)) {
            throw new MessageParseError("System message missing subtype", data);
        }
        return new SystemMessage(subtypeStr, new LinkedHashMap<>(data));
    }

    private static Message parseResultMessage(Map<String, Object> data) throws MessageParseError {
        try {
            String subtype = getString(data, "subtype");
            int durationMs = getNumber(data, "duration_ms");
            int durationApiMs = getNumber(data, "duration_api_ms");
            boolean isError = getBoolean(data, "is_error");
            int numTurns = getNumber(data, "num_turns");
            String sessionId = getString(data, "session_id");
            Double totalCost = getOptionalDouble(data.get("total_cost_usd"));
            Map<String, Object> usage = toMap(data.get("usage"));
            String result = data.get("result") instanceof String ? (String) data.get("result") : null;
            return new ResultMessage(
                    subtype,
                    durationMs,
                    durationApiMs,
                    isError,
                    numTurns,
                    sessionId,
                    totalCost,
                    usage,
                    result);
        } catch (ClassCastException | IllegalArgumentException e) {
            throw new MessageParseError("Invalid result message payload", data, e);
        }
    }

    private static ContentBlock parseContentBlock(Object blockObj) throws MessageParseError {
        if (!(blockObj instanceof Map)) {
            throw new MessageParseError("Content block was not an object", null);
        }
        Map<String, Object> block = (Map<String, Object>) blockObj;
        Object typeObj = block.get("type");
        if (!(typeObj instanceof String type)) {
            throw new MessageParseError("Content block missing type", block);
        }
        switch (type) {
            case "text":
                return new TextBlock(getString(block, "text"));
            case "thinking":
                return new ThinkingBlock(getString(block, "thinking"), getString(block, "signature"));
            case "tool_use":
                return new ToolUseBlock(
                        getString(block, "id"),
                        getString(block, "name"),
                        toMap(block.get("input")));
            case "tool_result":
                return new ToolResultBlock(
                        getString(block, "tool_use_id"),
                        block.get("content"),
                        block.containsKey("is_error") ? getBoolean(block, "is_error") : null);
            default:
                throw new MessageParseError("Unknown content block type: " + type, block);
        }
    }

    private static Map<String, Object> getMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("Expected map for key '" + key + "'");
        }
        return (Map<String, Object>) value;
    }

    private static String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Expected string for key '" + key + "'");
        }
        return (String) value;
    }

    private static int getNumber(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected numeric value for key '" + key + "'");
    }

    private static boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalArgumentException("Expected boolean for key '" + key + "'");
    }

    private static Double getOptionalDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private static Map<String, Object> toMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return null;
    }
}
