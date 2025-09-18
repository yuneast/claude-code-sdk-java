package com.anthropic.claudecode;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a permission update request.
 */
public class PermissionUpdate {
    public enum Type {
        ADD_RULES("addRules"),
        REPLACE_RULES("replaceRules"),
        REMOVE_RULES("removeRules"),
        SET_MODE("setMode"),
        ADD_DIRECTORIES("addDirectories"),
        REMOVE_DIRECTORIES("removeDirectories");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private Type type;
    private List<PermissionRuleValue> rules = new ArrayList<>();
    private PermissionBehavior behavior;
    private PermissionMode mode;
    private List<String> directories = new ArrayList<>();
    private PermissionUpdateDestination destination;

    public Type getType() {
        return type;
    }

    public PermissionUpdate setType(Type type) {
        this.type = type;
        return this;
    }

    public List<PermissionRuleValue> getRules() {
        return rules;
    }

    public PermissionUpdate setRules(List<PermissionRuleValue> rules) {
        this.rules = rules;
        return this;
    }

    public PermissionBehavior getBehavior() {
        return behavior;
    }

    public PermissionUpdate setBehavior(PermissionBehavior behavior) {
        this.behavior = behavior;
        return this;
    }

    public PermissionMode getMode() {
        return mode;
    }

    public PermissionUpdate setMode(PermissionMode mode) {
        this.mode = mode;
        return this;
    }

    public List<String> getDirectories() {
        return directories;
    }

    public PermissionUpdate setDirectories(List<String> directories) {
        this.directories = directories;
        return this;
    }

    public PermissionUpdateDestination getDestination() {
        return destination;
    }

    public PermissionUpdate setDestination(PermissionUpdateDestination destination) {
        this.destination = destination;
        return this;
    }
}
