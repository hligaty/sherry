package io.github.hligaty.raft.rpc.packet;


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class LogEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final LogId logId;
    private final Serializable data;

    public LogEntry(LogId logId, Serializable data) {
        this.logId = logId;
        this.data = data;
    }

    public long term() {
        return logId.term();
    }

    public long index() {
        return logId.index();
    }

    public LogId logId() {
        return logId;
    }

    public Serializable data() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LogEntry) obj;
        return Objects.equals(this.logId, that.logId) &&
               Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId, data);
    }

    @Override
    public String toString() {
        return "LogEntry[" +
               "logId=" + logId + ", " +
               "data=" + data + ']';
    }


}
