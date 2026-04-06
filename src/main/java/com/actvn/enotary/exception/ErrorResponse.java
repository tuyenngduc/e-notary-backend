package com.actvn.enotary.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String code;
    private Map<String, Object> details;

    public static ErrorResponse of(int status, String errorName, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC).toString())
                .status(status)
                .error(errorName)
                .message(message)
                .path(path)
                .build();
    }

    public static ErrorResponse of(int status, String errorName, String message, String path, String code, Map<String, Object> details) {
        return ErrorResponse.builder()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC).toString())
                .status(status)
                .error(errorName)
                .message(message)
                .path(path)
                .code(code)
                .details(details)
                .build();
    }
}
