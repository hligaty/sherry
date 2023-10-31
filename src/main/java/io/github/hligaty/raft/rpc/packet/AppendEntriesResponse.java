package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class AppendEntriesResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final long term;
    private final boolean success;

    public AppendEntriesResponse(
            long term,
            boolean success
    ) {
        this.term = term;
        this.success = success;
    }

    public long term() {
        return term;
    }

    public boolean success() {
        return success;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AppendEntriesResponse) obj;
        return this.term == that.term &&
               this.success == that.success;
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, success);
    }

    @Override
    public String toString() {
        return "AppendEntriesResponse[" +
               "term=" + term + ", " +
               "success=" + success + ']';
    }

}
