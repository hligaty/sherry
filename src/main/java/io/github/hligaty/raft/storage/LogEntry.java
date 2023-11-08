package io.github.hligaty.raft.storage;

import io.github.hligaty.raft.rpc.packet.Command;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class LogEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final LogId logId;
    private final Command command;

    public LogEntry(
            LogId logId,
            Command command
    ) {
        this.logId = logId;
        this.command = command;
    }

    public LogId logId() {
        return logId;
    }

    public Command command() {
        return command;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LogEntry) obj;
        return Objects.equals(this.logId, that.logId) &&
               Objects.equals(this.command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId, command);
    }

    @Override
    public String toString() {
        return "LogEntry[" +
               "logId=" + logId + ", " +
               "command=" + command + ']';
    }
}
