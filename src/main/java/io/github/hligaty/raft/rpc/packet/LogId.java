package io.github.hligaty.raft.rpc.packet;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class LogId implements Serializable, Comparable<LogId> {

    private static final LogId ZERO = new LogId(0, 0);
    @Serial
    private static final long serialVersionUID = 0L;
    private final long term;
    private final long index;

    public LogId(long term, long index) {
        this.term = term;
        this.index = index;
    }

    @Override
    public int compareTo(@Nonnull LogId o) {
        final int c = Long.compare(term, o.term);
        if (c == 0) {
            return Long.compare(index, o.index);
        } else {
            return c;
        }
    }

    public static LogId zero() {
        return ZERO;
    }

    @Override
    public String toString() {
        return String.format("term=%s, index=%s", term, index);
    }

    public long term() {
        return term;
    }

    public long index() {
        return index;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LogId) obj;
        return this.term == that.term &&
               this.index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, index);
    }


}
