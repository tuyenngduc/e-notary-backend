package com.actvn.enotary.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Auth Errors
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện hành động này."),
    INVALID_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "Xác thực không hợp lệ hoặc đã hết hạn."),
    INVALID_AUTHORIZATION(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này."),
    
    // Business Errors (Dành cho nghiệp vụ công chứng)
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu công chứng."),
    REQUEST_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "Hồ sơ này đã được tiếp nhận bởi công chứng viên khác."),
    REQUEST_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "Yêu cầu đã được công chứng viên khác tiếp nhận."),
    REQUEST_MISSING_REQUIRED_DOCUMENTS(HttpStatus.BAD_REQUEST, "Hồ sơ chưa đủ để tiếp nhận."),
    REQUEST_TERMINAL_STATUS(HttpStatus.CONFLICT, "Không thể cập nhật khi yêu cầu đã kết thúc."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu."),
    DOCUMENT_REPLACE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Loại tài liệu này không được phép thay thế."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ."),
    
    // Validation Errors
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ."),
    
    // System Errors
    UNCATEGORIZED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}

