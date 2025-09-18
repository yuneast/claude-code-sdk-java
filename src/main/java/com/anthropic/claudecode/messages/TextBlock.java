package com.anthropic.claudecode.messages;

/**
 * Represents a text block within a message.
 */
public class TextBlock implements ContentBlock {
    private final String text;

    public TextBlock(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String getType() {
        return "text";
    }
}
