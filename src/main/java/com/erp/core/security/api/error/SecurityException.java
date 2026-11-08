package com.erp.core.security.api.error;

public class SecurityException extends RuntimeException {
    private final SecurityErrorCode errorCode;

    public SecurityException(SecurityErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SecurityException(SecurityErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public SecurityErrorCode getErrorCode() {
        return errorCode;
    }
}