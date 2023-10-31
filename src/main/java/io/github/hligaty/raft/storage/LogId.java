package io.github.hligaty.raft.storage;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class LogId implements Serializable, Comparable<LogId> {
    @Serial
    private static final long serialVersionUID = 0L;
    private final long index;
    private final long term;

    public LogId(
            long index,
            long term
    ) {
        this.index = index;
        this.term = term;
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

    public long index() {
        return index;
    }

    public long term() {
        return term;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LogId) obj;
        return this.index == that.index &&
               this.term == that.term;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, term);
    }

    @Override
    public String toString() {
        return "LogId[" +
               "index=" + index + ", " +
               "term=" + term + ']';
    }

}
