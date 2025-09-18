package com.anthropic.claudecode.messages;

/**
 * Marker interface for messages returned by the CLI.
 */
public interface Message {
    MessageType getMessageType();
}
