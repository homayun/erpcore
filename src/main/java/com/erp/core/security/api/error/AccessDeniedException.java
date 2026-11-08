package com.erp.core.security.api.error;

public class AccessDeniedException extends SecurityException {
    public AccessDeniedException() {
        super(SecurityErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedException(String reason) {
        super(SecurityErrorCode.ACCESS_DENIED);
    }
}