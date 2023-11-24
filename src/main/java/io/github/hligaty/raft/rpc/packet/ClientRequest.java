package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class ClientRequest implements Serializable, Traceable {

    private static final ClientRequest EMPTY = ClientRequest.write(null);
    @Serial
    private static final long serialVersionUID = 0L;
    private final String traceId;
    private final boolean readOnly;
    private final Serializable data;

    public ClientRequest(String traceId, boolean readOnly, Serializable data) {
        this.traceId = traceId;
        this.readOnly = readOnly;
        this.data = data;
    }

    public static ClientRequest read(Serializable data) {
        return new ClientRequest(UUID.randomUUID().toString(), true, data);
    }

    public static ClientRequest write(Serializable data) {
        return new ClientRequest(UUID.randomUUID().toString(), false, data);
    }

    public static ClientRequest noop() {
        return EMPTY;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public Serializable data() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ClientRequest) obj;
        return Objects.equals(this.traceId, that.traceId) &&
               this.readOnly == that.readOnly &&
               Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, readOnly, data);
    }

    @Override
    public String toString() {
        return "ClientRequest[" +
               "traceId=" + traceId + ", " +
               "readOnly=" + readOnly + ", " +
               "data=" + data + ']';
    }


}
