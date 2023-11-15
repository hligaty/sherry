package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class ReadIndexResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final boolean success;
    private final long committedIndex;

    public ReadIndexResponse(boolean success, long committedIndex) {
        this.success = success;
        this.committedIndex = committedIndex;
    }

    @Override
    public String toString() {
        return "ReadIndexResponse[" +
               "success=" + success + ", " +
               "committedIndex=" + committedIndex + ']';
    }

    public boolean success() {
        return success;
    }

    public long committedIndex() {
        return committedIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ReadIndexResponse) obj;
        return this.success == that.success &&
               this.committedIndex == that.committedIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, committedIndex);
    }


}
