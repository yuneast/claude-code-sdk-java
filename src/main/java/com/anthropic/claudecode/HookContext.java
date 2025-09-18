package com.anthropic.claudecode;

/**
 * Context information passed to hook callbacks.
 */
public class HookContext {
    private Object signal;

    public Object getSignal() {
        return signal;
    }

    public HookContext setSignal(Object signal) {
        this.signal = signal;
        return this;
    }
}
