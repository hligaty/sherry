package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class PeerId implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private static final PeerId EMPTY_ID = new PeerId("0.0.0.0", 0);
    private final String address;
    private final int port;

    public PeerId(
            String address,
            int port
    ) {
        this.address = address;
        this.port = port;
    }
    
    public static PeerId emptyId() {
        return EMPTY_ID;
    }
    
    public boolean isEmpty() {
        return this.equals(emptyId());
    }
    
    public static PeerId parse(String s) {
        String[] params = s.split(":");
        String address = params[0];
        int port = Integer.parseInt(params[1]);
        return EMPTY_ID.address.equals(address) && EMPTY_ID.port == port ? EMPTY_ID : new PeerId(address, port);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", address, port);
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
        var that = (PeerId) obj;
        return Objects.equals(this.address, that.address) &&
               this.port == that.port;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

}
