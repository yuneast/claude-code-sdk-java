package com.anthropic.claudecode;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration describing when hook callbacks should fire.
 */
public class HookMatcher {
    private String matcher;
    private List<HookCallback> hooks = new ArrayList<>();

    public String getMatcher() {
        return matcher;
    }

    public HookMatcher setMatcher(String matcher) {
        this.matcher = matcher;
        return this;
    }

    public List<HookCallback> getHooks() {
        return hooks;
    }

    public HookMatcher setHooks(List<HookCallback> hooks) {
        this.hooks = hooks;
        return this;
    }
}
