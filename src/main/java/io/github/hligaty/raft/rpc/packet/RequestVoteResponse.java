package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class RequestVoteResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final long term;
    private final boolean granted;

    public RequestVoteResponse(
            long term,
            boolean granted
    ) {
        this.term = term;
        this.granted = granted;
    }

    public long term() {
        return term;
    }

    public boolean granted() {
        return granted;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RequestVoteResponse) obj;
        return this.term == that.term &&
               this.granted == that.granted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, granted);
    }

    @Override
    public String toString() {
        return "RequestVoteResponse[" +
               "term=" + term + ", " +
               "granted=" + granted + ']';
    }

}
