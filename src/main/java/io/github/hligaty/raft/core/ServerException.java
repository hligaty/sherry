package io.github.hligaty.raft.core;

/**
 * 服务端异常
 */
public class ServerException extends RuntimeException {
    
    private final ErrorType errorType;

    public ServerException(ErrorType errorType) {
        super();
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
