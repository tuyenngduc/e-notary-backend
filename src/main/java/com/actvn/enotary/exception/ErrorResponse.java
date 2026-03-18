package com.actvn.enotary.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String code;
    private Map<String, String> errors;
    private Map<String, Object> details;
}
