package io.github.hligaty.raft.rpc.packet;

import java.io.Serializable;
import java.util.UUID;

public class ClientRequest implements Serializable, Traceable {

    private static final ClientRequest EMPTY = ClientRequest.write(null);

    private final Option option;
    
    private final Serializable data;
    
    private final String traceId;

    private ClientRequest(Option option, Serializable data) {
        this.option = option;
        this.data = data;
        this.traceId = UUID.randomUUID().toString();
    }
    
    public static ClientRequest read(Serializable data) {
        return new ClientRequest(Option.READ, data);
    }

    public static ClientRequest write(Serializable data) {
        return new ClientRequest(Option.WRITE, data);
    }
    
    public static ClientRequest noop() {
        return EMPTY;
    }

    public Serializable data() {
        return data;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    public enum Option {
        READ,
        WRITE,
    }
    
    public boolean readOnly() {
        return option == Option.READ;
    }

    @Override
    public String toString() {
        return "Command{" +
               "option=" + option +
               ", data=" + data +
               ", traceId=" + traceId +
               '}';
    }
}
