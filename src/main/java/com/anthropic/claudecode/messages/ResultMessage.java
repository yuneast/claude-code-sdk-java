package com.anthropic.claudecode.messages;

import java.util.Map;

/**
 * Represents the result of a Claude Code session.
 */
public class ResultMessage implements Message {
    private final String subtype;
    private final int durationMs;
    private final int durationApiMs;
    private final boolean error;
    private final int numTurns;
    private final String sessionId;
    private final Double totalCostUsd;
    private final Map<String, Object> usage;
    private final String result;

    public ResultMessage(
            String subtype,
            int durationMs,
            int durationApiMs,
            boolean error,
            int numTurns,
            String sessionId,
            Double totalCostUsd,
            Map<String, Object> usage,
            String result) {
        this.subtype = subtype;
        this.durationMs = durationMs;
        this.durationApiMs = durationApiMs;
        this.error = error;
        this.numTurns = numTurns;
        this.sessionId = sessionId;
        this.totalCostUsd = totalCostUsd;
        this.usage = usage;
        this.result = result;
    }

    public String getSubtype() {
        return subtype;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int getDurationApiMs() {
        return durationApiMs;
    }

    public boolean isError() {
        return error;
    }

    public int getNumTurns() {
        return numTurns;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Double getTotalCostUsd() {
        return totalCostUsd;
    }

    public Map<String, Object> getUsage() {
        return usage;
    }

    public String getResult() {
        return result;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.RESULT;
    }
}
