package io.github.hligaty.raft.rpc.packet;

import io.github.hligaty.raft.util.PeerId;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class RequestVoteRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final PeerId peerId;
    private final long term;
    private final long lastLogIndex;
    private final long lastLogTerm;
    private final boolean preVote;

    public RequestVoteRequest(
            PeerId peerId,
            long term,
            long lastLogIndex,
            long lastLogTerm,
            boolean preVote
    ) {
        this.peerId = peerId;
        this.term = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
        this.preVote = preVote;
    }

    public PeerId peer() {
        return peerId;
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
        return Objects.equals(this.peerId, that.peerId) &&
               this.term == that.term &&
               this.lastLogIndex == that.lastLogIndex &&
               this.lastLogTerm == that.lastLogTerm &&
               this.preVote == that.preVote;
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerId, term, lastLogIndex, lastLogTerm, preVote);
    }

    @Override
    public String toString() {
        return "RequestVoteRequest[" +
               "endpoint=" + peerId + ", " +
               "term=" + term + ", " +
               "lastLogIndex=" + lastLogIndex + ", " +
               "lastLogTerm=" + lastLogTerm + ", " +
               "preVote=" + preVote + ']';
    }

}
