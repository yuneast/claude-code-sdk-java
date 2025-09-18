package com.anthropic.claudecode;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Functional interface used to determine whether Claude Code can execute a tool.
 */
@FunctionalInterface
public interface CanUseTool {
    CompletionStage<? extends PermissionResult> apply(
            String toolName, Map<String, Object> input, ToolPermissionContext context);
}
