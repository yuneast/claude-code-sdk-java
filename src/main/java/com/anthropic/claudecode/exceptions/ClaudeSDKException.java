package com.anthropic.claudecode.exceptions;

/**
 * Base exception for Claude Code Java SDK errors.
 */
public class ClaudeSDKException extends Exception {
    public ClaudeSDKException(String message) {
        super(message);
    }

    public ClaudeSDKException(String message, Throwable cause) {
        super(message, cause);
    }
}
