package com.erp.core.security.api.error;

public enum SecurityErrorCode {
    UNAUTHENTICATED(401, "SEC-4010", "Authentication required"),
    ACCESS_DENIED(403, "SEC-4030", "Access denied"),
    TENANT_MISMATCH(403, "SEC-4031", "Cross-tenant access forbidden"),
    STEP_UP_REQUIRED(403, "SEC-4032", "Step-up authentication required"),
    ACCOUNT_LOCKED(423, "SEC-4230", "Account locked");

    private final int httpStatus;
    private final String code;
    private final String message;

    SecurityErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}