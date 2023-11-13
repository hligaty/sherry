package io.github.hligaty.raft.core;

public class ApplyException extends RuntimeException {
    
    private final ErrorType errorType;

    public ApplyException(ErrorType errorType) {
        super();
        this.errorType = errorType;
    }

    @Override
    public String getMessage() {
        return errorType.name();
    }
}
