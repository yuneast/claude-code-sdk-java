package com.anthropic.claudecode;

/**
 * Supported hook events in the Claude Code Java SDK.
 */
public enum HookEvent {
    PRE_TOOL_USE("PreToolUse"),
    POST_TOOL_USE("PostToolUse"),
    USER_PROMPT_SUBMIT("UserPromptSubmit"),
    STOP("Stop"),
    SUBAGENT_STOP("SubagentStop"),
    PRE_COMPACT("PreCompact");

    private final String value;

    HookEvent(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HookEvent fromValue(String value) {
        for (HookEvent event : values()) {
            if (event.value.equals(value)) {
                return event;
            }
        }
        throw new IllegalArgumentException("Unknown hook event: " + value);
    }
}
