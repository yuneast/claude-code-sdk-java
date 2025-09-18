package com.anthropic.claudecode;

import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Functional interface for hook callbacks triggered by Claude Code events.
 */
@FunctionalInterface
public interface HookCallback {
    CompletionStage<HookJsonOutput> apply(
            Map<String, Object> input, String toolUseId, HookContext context);
}
