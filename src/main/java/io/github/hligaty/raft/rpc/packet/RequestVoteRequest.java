package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class RequestVoteRequest implements Serializable, Traceable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final String traceId;
    private final PeerId serverId;
    private final long term;
    private final long lastLogIndex;
    private final long lastLogTerm;
    private final boolean preVote;

    public RequestVoteRequest(String traceId, PeerId serverId, long term, long lastLogIndex, long lastLogTerm,
                              boolean preVote) {
        this.traceId = traceId;
        this.serverId = serverId;
        this.term = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
        this.preVote = preVote;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    public PeerId serverId() {
        return serverId;
    }

    public long term() {
        return term;
    }

    public long lastLogIndex() {
        return lastLogIndex;
    }

    public long lastLogTerm() {
        return lastLogTerm;
    }

    public boolean preVote() {
        return preVote;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RequestVoteRequest) obj;
        return Objects.equals(this.traceId, that.traceId) &&
               Objects.equals(this.serverId, that.serverId) &&
               this.term == that.term &&
               this.lastLogIndex == that.lastLogIndex &&
               this.lastLogTerm == that.lastLogTerm &&
               this.preVote == that.preVote;
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, serverId, term, lastLogIndex, lastLogTerm, preVote);
    }

    @Override
    public String toString() {
        return "RequestVoteRequest[" +
               "traceId=" + traceId + ", " +
               "serverId=" + serverId + ", " +
               "term=" + term + ", " +
               "lastLogIndex=" + lastLogIndex + ", " +
               "lastLogTerm=" + lastLogTerm + ", " +
               "preVote=" + preVote + ']';
    }


}
