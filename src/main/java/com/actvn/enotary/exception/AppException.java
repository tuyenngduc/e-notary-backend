package com.actvn.enotary.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, Object> details;
    private final String code;

    // Constructor 1: message + status (most common)
    public AppException(String message, HttpStatus status) {
        this(message, status, null, null);
    }

    // Constructor 2: message + status + details
    public AppException(String message, HttpStatus status, Map<String, Object> details) {
        this(message, status, details, null);
    }

    // Constructor 3: message + status + details + code
    public AppException(String message, HttpStatus status, Map<String, Object> details, String code) {
        super(message);
        this.status = status;
        this.details = details;
        this.code = code;
    }

    // Constructor 4: ErrorCode enum (new way)
    public AppException(ErrorCode errorCode) {
        this(errorCode.getMessage(), errorCode.getHttpStatus(), null, errorCode.name());
    }

    // Constructor 5: ErrorCode + details
    public AppException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode.getMessage(), errorCode.getHttpStatus(), details, errorCode.name());
    }
}