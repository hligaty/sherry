package io.github.hligaty.raft.rpc.packet;

import io.github.hligaty.raft.core.ErrorType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class ClientResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final boolean success;
    private final ErrorType errorType;
    private final Serializable data;

    private ClientResponse(boolean success, ErrorType errorType, Serializable data) {
        this.success = success;
        this.errorType = errorType;
        this.data = data;
    }
    
    public static ClientResponse fail(ErrorType errorType) {
        return new ClientResponse(false, errorType, null);
    }
    
    public static ClientResponse success(Serializable data) {
        return new ClientResponse(true, null, data);
    }

    public boolean success() {
        return success;
    }

    public ErrorType errorType() {
        return errorType;
    }

    public Serializable data() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ClientResponse) obj;
        return this.success == that.success &&
               Objects.equals(this.errorType, that.errorType) &&
               Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, errorType, data);
    }

    @Override
    public String toString() {
        return "ClientResponse[" +
               "success=" + success + ", " +
               "errorType=" + errorType + ", " +
               "data=" + data + ']';
    }

}
