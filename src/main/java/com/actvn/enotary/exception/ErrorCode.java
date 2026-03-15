package com.actvn.enotary.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR,
    AUTHENTICATION_FAILED,
    BAD_REQUEST,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_ERROR,
    REQUEST_ALREADY_CLAIMED;

    public static ErrorCode fromHttpStatus(HttpStatus status) {
        if (status == null) {
            return INTERNAL_ERROR;
        }
        return switch (status) {
            case BAD_REQUEST -> BAD_REQUEST;
            case FORBIDDEN -> FORBIDDEN;
            case NOT_FOUND -> NOT_FOUND;
            case CONFLICT -> CONFLICT;
            default -> INTERNAL_ERROR;
        };
    }
}

