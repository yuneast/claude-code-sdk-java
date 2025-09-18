package com.anthropic.claudecode;

/**
 * Destinations for permission updates.
 */
public enum PermissionUpdateDestination {
    USER_SETTINGS("userSettings"),
    PROJECT_SETTINGS("projectSettings"),
    LOCAL_SETTINGS("localSettings"),
    SESSION("session");

    private final String value;

    PermissionUpdateDestination(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
