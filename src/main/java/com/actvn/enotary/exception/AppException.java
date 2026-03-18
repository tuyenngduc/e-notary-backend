package com.actvn.enotary.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final String code;
    private final Map<String, Object> details;

    public AppException(String message, HttpStatus status) {
        this(message, status, (ErrorCode) null);
    }

    public AppException(String message, HttpStatus status, ErrorCode errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.code = null;
        this.details = null;
    }

    public AppException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.errorCode = null;
        this.code = code;
        this.details = null;
    }

    public AppException(String message, HttpStatus status, String code, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.errorCode = null;
        this.code = code;
        this.details = details;
    }
}