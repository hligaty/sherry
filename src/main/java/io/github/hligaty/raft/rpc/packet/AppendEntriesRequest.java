package io.github.hligaty.raft.rpc.packet;

import io.github.hligaty.raft.storage.LogEntry;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public final class AppendEntriesRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final PeerId serverId;
    private final long term;
    private final List<LogEntry> logEntries;
    private final long prevLogTerm;
    private final long prevLogIndex;
    private final long committedIndex;

    public AppendEntriesRequest(PeerId serverId, long term, List<LogEntry> logEntries, long prevLogTerm, long prevLogIndex,
                                long committedIndex) {
        this.serverId = serverId;
        this.term = term;
        this.logEntries = logEntries;
        this.prevLogTerm = prevLogTerm;
        this.prevLogIndex = prevLogIndex;
        this.committedIndex = committedIndex;
    }

    public PeerId serverId() {
        return serverId;
    }

    public long term() {
        return term;
    }

    public List<LogEntry> logEntries() {
        return logEntries;
    }

    public long prevLogTerm() {
        return prevLogTerm;
    }

    public long prevLogIndex() {
        return prevLogIndex;
    }

    public long committedIndex() {
        return committedIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AppendEntriesRequest) obj;
        return Objects.equals(this.serverId, that.serverId) &&
               this.term == that.term &&
               Objects.equals(this.logEntries, that.logEntries) &&
               this.prevLogTerm == that.prevLogTerm &&
               this.prevLogIndex == that.prevLogIndex &&
               this.committedIndex == that.committedIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverId, term, logEntries, prevLogTerm, prevLogIndex, committedIndex);
    }

    @Override
    public String toString() {
        return "AppendEntriesRequest[" +
               "serverId=" + serverId + ", " +
               "term=" + term + ", " +
               "logEntries=" + logEntries + ", " +
               "prevLogTerm=" + prevLogTerm + ", " +
               "prevLogIndex=" + prevLogIndex + ", " +
               "committedIndex=" + committedIndex + ']';
    }


}
