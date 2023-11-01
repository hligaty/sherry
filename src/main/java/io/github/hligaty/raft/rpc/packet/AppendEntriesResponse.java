package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class AppendEntriesResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final long term;
    private final boolean success;
    private final long lastLogIndex;

    public AppendEntriesResponse(
            long term,
            boolean success,
            long lastLogIndex
    ) {
        this.term = term;
        this.success = success;
        this.lastLogIndex = lastLogIndex;
    }

    public long term() {
        return term;
    }

    public boolean success() {
        return success;
    }

    public long lastLogIndex() {
        return lastLogIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AppendEntriesResponse) obj;
        return this.term == that.term &&
               this.success == that.success &&
               this.lastLogIndex == that.lastLogIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, success, lastLogIndex);
    }

    @Override
    public String toString() {
        return "AppendEntriesResponse[" +
               "term=" + term + ", " +
               "success=" + success + ", " +
               "lastLogIndex=" + lastLogIndex + ']';
    }

}
