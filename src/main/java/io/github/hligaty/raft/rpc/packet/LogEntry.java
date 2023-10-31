package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class LogEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final long term;
    private final long index;
    private final Object object;

    public LogEntry(
            long term,
            long index,
            Object object
    ) {
        this.term = term;
        this.index = index;
        this.object = object;
    }

    public long term() {
        return term;
    }

    public long index() {
        return index;
    }

    public Object object() {
        return object;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LogEntry) obj;
        return this.term == that.term &&
               this.index == that.index &&
               Objects.equals(this.object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, index, object);
    }

    @Override
    public String toString() {
        return "LogEntry[" +
               "term=" + term + ", " +
               "index=" + index + ", " +
               "object=" + object + ']';
    }

}
