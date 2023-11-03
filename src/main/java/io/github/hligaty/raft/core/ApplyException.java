package io.github.hligaty.raft.core;

public class ApplyException extends RuntimeException {
    
    private final ErrorType errorType;

    public ApplyException(ErrorType errorType) {
        super();
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
