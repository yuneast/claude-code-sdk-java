package com.anthropic.claudecode.exceptions;

/**
 * Indicates that the Claude Code CLI could not be located on the system.
 */
public class CLINotFoundError extends ClaudeSDKException {
    public CLINotFoundError(String message) {
        super(message);
    }

    public CLINotFoundError(String message, Throwable cause) {
        super(message, cause);
    }
}
