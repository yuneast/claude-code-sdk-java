package com.anthropic.claudecode;

import java.util.ArrayList;
import java.util.List;

/**
 * Context information passed to canUseTool callbacks.
 */
public class ToolPermissionContext {
    private Object signal;
    private List<PermissionUpdate> suggestions = new ArrayList<>();

    public Object getSignal() {
        return signal;
    }

    public ToolPermissionContext setSignal(Object signal) {
        this.signal = signal;
        return this;
    }

    public List<PermissionUpdate> getSuggestions() {
        return suggestions;
    }

    public ToolPermissionContext setSuggestions(List<PermissionUpdate> suggestions) {
        this.suggestions = suggestions;
        return this;
    }
}
