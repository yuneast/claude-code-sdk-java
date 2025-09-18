package com.anthropic.claudecode;

/**
 * Response returned by hook callbacks.
 */
public class HookJsonOutput {
    private String decision;
    private String systemMessage;
    private Object hookSpecificOutput;

    public String getDecision() {
        return decision;
    }

    public HookJsonOutput setDecision(String decision) {
        this.decision = decision;
        return this;
    }

    public String getSystemMessage() {
        return systemMessage;
    }

    public HookJsonOutput setSystemMessage(String systemMessage) {
        this.systemMessage = systemMessage;
        return this;
    }

    public Object getHookSpecificOutput() {
        return hookSpecificOutput;
    }

    public HookJsonOutput setHookSpecificOutput(Object hookSpecificOutput) {
        this.hookSpecificOutput = hookSpecificOutput;
        return this;
    }
}
