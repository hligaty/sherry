package io.github.hligaty.raft.rpc.packet;

import io.github.hligaty.raft.storage.LogEntry;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public final class AppendEntriesRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final long term;
    private final List<LogEntry> logEntries;

    public AppendEntriesRequest(
            long term,
            List<LogEntry> logEntries
    ) {
        this.term = term;
        this.logEntries = logEntries;
    }

    public long term() {
        return term;
    }

    public List<LogEntry> logEntries() {
        return logEntries;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AppendEntriesRequest) obj;
        return this.term == that.term &&
               Objects.equals(this.logEntries, that.logEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, logEntries);
    }

    @Override
    public String toString() {
        return "AppendEntriesRequest[" +
               "term=" + term + ", " +
               "logEntries=" + logEntries + ']';
    }

}
