package com.actvn.enotary.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode errorCode;

    public AppException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public AppException(String message, HttpStatus status, ErrorCode errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}