package io.github.hligaty.raft.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class Peer implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private final String address;
    private final int port;

    public Peer(
            String address,
            int port
    ) {
        this.address = address;
        this.port = port;
    }

    @Override
    public String toString() {
        return String.format("address=%s, port=%s", address, port);
    }

    public String address() {
        return address;
    }

    public int port() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Peer) obj;
        return Objects.equals(this.address, that.address) &&
               this.port == that.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

}
