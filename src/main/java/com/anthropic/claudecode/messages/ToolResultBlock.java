package com.anthropic.claudecode.messages;

/**
 * Represents the result of a tool execution.
 */
public class ToolResultBlock implements ContentBlock {
    private final String toolUseId;
    private final Object content;
    private final Boolean isError;

    public ToolResultBlock(String toolUseId, Object content, Boolean isError) {
        this.toolUseId = toolUseId;
        this.content = content;
        this.isError = isError;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public Object getContent() {
        return content;
    }

    public Boolean getError() {
        return isError;
    }

    @Override
    public String getType() {
        return "tool_result";
    }
}
