package com.meridian.disputes.application;

public class DisputeRuleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public DisputeRuleException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() { return code; }
}
