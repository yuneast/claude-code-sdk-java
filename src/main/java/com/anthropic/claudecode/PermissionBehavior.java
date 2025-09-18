package com.anthropic.claudecode;

/**
 * Supported permission behaviors for rule updates.
 */
public enum PermissionBehavior {
    ALLOW("allow"),
    DENY("deny"),
    ASK("ask");

    private final String value;

    PermissionBehavior(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
