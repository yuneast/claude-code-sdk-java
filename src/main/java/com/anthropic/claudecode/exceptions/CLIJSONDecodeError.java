package com.anthropic.claudecode.exceptions;

/**
 * Indicates that the SDK failed to parse JSON output from the CLI.
 */
public class CLIJSONDecodeError extends ClaudeSDKException {
    public CLIJSONDecodeError(String message) {
        super(message);
    }

    public CLIJSONDecodeError(String message, Throwable cause) {
        super(message, cause);
    }
}
