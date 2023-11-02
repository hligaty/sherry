package io.github.hligaty.raft.storage;

public class StoreException extends RuntimeException {
    public StoreException(Throwable cause) {
        super(cause);
    }

    public StoreException(String message) {
        super(message);
    }
}
