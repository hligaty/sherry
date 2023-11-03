package io.github.hligaty.raft.rpc.packet;

import io.github.hligaty.raft.util.Peer;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class RequestVoteRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final Peer peer;
    private final long term;
    private final long lastLogIndex;
    private final long lastLogTerm;
    private final boolean preVote;

    public RequestVoteRequest(
            Peer peer,
            long term,
            long lastLogIndex,
            long lastLogTerm,
            boolean preVote
    ) {
        this.peer = peer;
        this.term = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
        this.preVote = preVote;
    }

    public Peer peer() {
        return peer;
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
        return Objects.equals(this.peer, that.peer) &&
               this.term == that.term &&
               this.lastLogIndex == that.lastLogIndex &&
               this.lastLogTerm == that.lastLogTerm &&
               this.preVote == that.preVote;
    }

    @Override
    public int hashCode() {
        return Objects.hash(peer, term, lastLogIndex, lastLogTerm, preVote);
    }

    @Override
    public String toString() {
        return "RequestVoteRequest[" +
               "endpoint=" + peer + ", " +
               "term=" + term + ", " +
               "lastLogIndex=" + lastLogIndex + ", " +
               "lastLogTerm=" + lastLogTerm + ", " +
               "preVote=" + preVote + ']';
    }

}
