package com.actvn.enotary.exception;

public final class ErrorCodes {
    private ErrorCodes() {
    }

    public static final String REQUEST_ALREADY_ASSIGNED = "REQUEST_ALREADY_ASSIGNED";
    public static final String REQUEST_MISSING_REQUIRED_DOCUMENTS = "REQUEST_MISSING_REQUIRED_DOCUMENTS";
    public static final String REQUEST_TERMINAL_STATUS = "REQUEST_TERMINAL_STATUS";
    public static final String DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
    public static final String DOCUMENT_REPLACE_NOT_ALLOWED = "DOCUMENT_REPLACE_NOT_ALLOWED";
}

