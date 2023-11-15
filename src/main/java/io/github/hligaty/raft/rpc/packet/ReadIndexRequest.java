package io.github.hligaty.raft.rpc.packet;

import java.io.Serial;
import java.io.Serializable;

public final class ReadIndexRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;
    private static final ReadIndexRequest INSTANCE = new ReadIndexRequest();

    private ReadIndexRequest() {
    }
    
    public static ReadIndexRequest getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public String toString() {
        return "ReadIndexRequest[]";
    }

}
