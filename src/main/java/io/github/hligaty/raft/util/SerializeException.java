package io.github.hligaty.raft.util;

/**
 * 序列化异常
 */
public class SerializeException extends RuntimeException {
    public SerializeException(Throwable cause) {
        super(cause);
    }
}
