package com.anthropic.claudecode.messages;

import java.util.Map;

/**
 * Represents a system message providing metadata about the session.
 */
public class SystemMessage implements Message {
    private final String subtype;
    private final Map<String, Object> data;

    public SystemMessage(String subtype, Map<String, Object> data) {
        this.subtype = subtype;
        this.data = data;
    }

    public String getSubtype() {
        return subtype;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SYSTEM;
    }
}
