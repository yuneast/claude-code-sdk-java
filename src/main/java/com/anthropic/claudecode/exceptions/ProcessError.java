package com.anthropic.claudecode.exceptions;

/**
 * Indicates that the Claude Code CLI process exited with a non-zero code.
 */
public class ProcessError extends ClaudeSDKException {
    private final int exitCode;
    private final String stderr;

    public ProcessError(String message, int exitCode, String stderr) {
        super(message);
        this.exitCode = exitCode;
        this.stderr = stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStderr() {
        return stderr;
    }
}
