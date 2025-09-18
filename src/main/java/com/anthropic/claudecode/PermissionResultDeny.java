package com.anthropic.claudecode;

/**
 * Indicates that a tool permission request was denied.
 */
public class PermissionResultDeny implements PermissionResult {
    private String message = "";
    private boolean interrupt;

    @Override
    public String getBehavior() {
        return "deny";
    }

    public String getMessage() {
        return message;
    }

    public PermissionResultDeny setMessage(String message) {
        this.message = message;
        return this;
    }

    public boolean isInterrupt() {
        return interrupt;
    }

    public PermissionResultDeny setInterrupt(boolean interrupt) {
        this.interrupt = interrupt;
        return this;
    }
}
