package com.anthropic.claudecode.exceptions;

/**
 * Indicates a failure connecting to or communicating with the Claude Code CLI.
 */
public class CLIConnectionError extends ClaudeSDKException {
    public CLIConnectionError(String message) {
        super(message);
    }

    public CLIConnectionError(String message, Throwable cause) {
        super(message, cause);
    }
}
