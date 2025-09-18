package com.anthropic.claudecode.messages;

import java.util.List;

/**
 * Represents a message returned by Claude.
 */
public class AssistantMessage implements Message {
    private final List<ContentBlock> content;
    private final String model;

    public AssistantMessage(List<ContentBlock> content, String model) {
        this.content = content;
        this.model = model;
    }

    public List<ContentBlock> getContent() {
        return content;
    }

    public String getModel() {
        return model;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ASSISTANT;
    }
}
