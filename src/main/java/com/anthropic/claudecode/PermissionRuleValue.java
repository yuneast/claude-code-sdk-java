package com.anthropic.claudecode;

/**
 * Represents a permission rule value for updates.
 */
public class PermissionRuleValue {
    private String toolName;
    private String ruleContent;

    public PermissionRuleValue() {}

    public PermissionRuleValue(String toolName, String ruleContent) {
        this.toolName = toolName;
        this.ruleContent = ruleContent;
    }

    public String getToolName() {
        return toolName;
    }

    public PermissionRuleValue setToolName(String toolName) {
        this.toolName = toolName;
        return this;
    }

    public String getRuleContent() {
        return ruleContent;
    }

    public PermissionRuleValue setRuleContent(String ruleContent) {
        this.ruleContent = ruleContent;
        return this;
    }
}
