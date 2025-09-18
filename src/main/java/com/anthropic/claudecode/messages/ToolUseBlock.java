package com.anthropic.claudecode.messages;

import java.util.Map;

/**
 * Represents a tool use request from Claude.
 */
public class ToolUseBlock implements ContentBlock {
    private final String id;
    private final String name;
    private final Map<String, Object> input;

    public ToolUseBlock(String id, String name, Map<String, Object> input) {
        this.id = id;
        this.name = name;
        this.input = input;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    @Override
    public String getType() {
        return "tool_use";
    }
}
