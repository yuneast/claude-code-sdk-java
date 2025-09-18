package com.anthropic.claudecode.transport;

import com.anthropic.claudecode.exceptions.CLIConnectionError;
import com.anthropic.claudecode.exceptions.ClaudeSDKException;

import java.util.Map;

/**
 * Abstraction over the underlying mechanism used to communicate with Claude Code.
 */
public interface Transport extends AutoCloseable {
    void connect() throws ClaudeSDKException;

    void write(String data) throws ClaudeSDKException;

    void endInput() throws ClaudeSDKException;

    boolean isReady();

    void readMessages(MessageHandler handler);

    @Override
    void close() throws ClaudeSDKException;

    interface MessageHandler {
        void onMessage(Map<String, Object> message);

        void onError(Throwable error);

        void onClosed();
    }
}
