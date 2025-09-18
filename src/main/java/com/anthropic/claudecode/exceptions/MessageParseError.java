package com.anthropic.claudecode.exceptions;

import java.util.Map;

/**
 * Indicates that an incoming message could not be parsed into a typed object.
 */
public class MessageParseError extends ClaudeSDKException {
    private final Map<String, Object> payload;

    public MessageParseError(String message, Map<String, Object> payload) {
        super(message);
        this.payload = payload;
    }

    public MessageParseError(String message, Map<String, Object> payload, Throwable cause) {
        super(message, cause);
        this.payload = payload;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
