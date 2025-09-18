package com.anthropic.claudecode.messages;

import java.util.List;

/**
 * Represents a user message.
 */
public class UserMessage implements Message {
    private final Object content; // Either String or List<ContentBlock>

    public UserMessage(Object content) {
        this.content = content;
    }

    public Object getContent() {
        return content;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.USER;
    }
}
