package com.actvn.enotary.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email phải có định dạng hợp lệ")
    private String email;
    // Allow common separators (space, dash, parentheses) and require the number start with 0 or +84.
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(?:\\+84|0)[0-9\\s\\-()]+$", message = "Số điện thoại phải bắt đầu bằng 0 hoặc +84 và chỉ chứa chữ số/ký tự phân tách")
    private String phoneNumber;
    @NotBlank(message = "Mật khẩu không được để trống") 
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự") 
    private String password;
}