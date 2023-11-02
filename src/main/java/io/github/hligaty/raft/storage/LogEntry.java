package io.github.hligaty.raft.storage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class LogEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final LogId logId;
    private final Object object;

    public LogEntry(
            LogId logId,
            Object object
    ) {
        this.logId = logId;
        this.object = object;
    }

    public LogId logId() {
        return logId;
    }

    public Object object() {
        return object;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LogEntry) obj;
        return Objects.equals(this.logId, that.logId) &&
               Objects.equals(this.object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId, object);
    }

    @Override
    public String toString() {
        return "LogEntry[" +
               "logId=" + logId + ", " +
               "object=" + object + ']';
    }
    
    public LogEntry setLogIndex(long logIndex) {
        return new LogEntry(new LogId(logIndex, logId.term()), object);
    }
}
