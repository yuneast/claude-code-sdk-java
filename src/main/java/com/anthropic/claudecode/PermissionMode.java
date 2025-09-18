package com.anthropic.claudecode;

/**
 * Supported permission modes for Claude Code.
 */
public enum PermissionMode {
    DEFAULT("default"),
    ACCEPT_EDITS("acceptEdits"),
    PLAN("plan"),
    BYPASS_PERMISSIONS("bypassPermissions");

    private final String value;

    PermissionMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PermissionMode fromValue(String value) {
        for (PermissionMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown permission mode: " + value);
    }
}
