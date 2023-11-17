package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class ReadIndexRequest implements Serializable, Traceable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final String traceId;

    public ReadIndexRequest(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ReadIndexRequest) obj;
        return Objects.equals(this.traceId, that.traceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId);
    }

    @Override
    public String toString() {
        return "ReadIndexRequest[" +
               "traceId=" + traceId + ']';
    }

}
