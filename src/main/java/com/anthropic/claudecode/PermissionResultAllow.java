package com.anthropic.claudecode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Indicates that a tool permission request was approved.
 */
public class PermissionResultAllow implements PermissionResult {
    private Map<String, Object> updatedInput;
    private List<PermissionUpdate> updatedPermissions = new ArrayList<>();

    @Override
    public String getBehavior() {
        return "allow";
    }

    public Map<String, Object> getUpdatedInput() {
        return updatedInput;
    }

    public PermissionResultAllow setUpdatedInput(Map<String, Object> updatedInput) {
        this.updatedInput = updatedInput;
        return this;
    }

    public List<PermissionUpdate> getUpdatedPermissions() {
        return updatedPermissions;
    }

    public PermissionResultAllow setUpdatedPermissions(List<PermissionUpdate> updatedPermissions) {
        this.updatedPermissions = updatedPermissions;
        return this;
    }
}
