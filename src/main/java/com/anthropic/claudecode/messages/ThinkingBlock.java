package com.anthropic.claudecode.messages;

/**
 * Represents a thinking block returned by Claude.
 */
public class ThinkingBlock implements ContentBlock {
    private final String thinking;
    private final String signature;

    public ThinkingBlock(String thinking, String signature) {
        this.thinking = thinking;
        this.signature = signature;
    }

    public String getThinking() {
        return thinking;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String getType() {
        return "thinking";
    }
}
