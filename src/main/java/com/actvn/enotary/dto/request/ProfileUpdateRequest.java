package com.actvn.enotary.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(regexp = "^\\d{12}$", message = "Số CCCD phải bao gồm đúng 12 chữ số")
    private String identityNumber;
    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    private LocalDate dateOfBirth;
    @NotBlank(message = "Giới tính không được để trống")
    @Pattern(regexp = "^(Nam|Nữ|Khác)$", message = "Giới tính chỉ có thể là 'Nam', 'Nữ' hoặc 'Khác'")
    private String gender;
    @NotBlank(message = "Quốc tịch không được để trống")
    private String nationality;
    @NotBlank(message = "Quê quán không được để trống")
    @Size(min = 5, max = 255, message = "Quê quán phải từ 5 đến 255 ký tự")
    @Pattern(

            regexp = "^[a-zA-Z0-9\\s,./ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵỷỹ]*$",

            message = "Địa chỉ không được chứa ký tự đặc biệt"

    )
    private String placeOfOrigin;
    @NotBlank(message = "Nơi thường trú không được để trống")
    @Size(min = 5, max = 255, message = "Nơi thường trú phải từ 5 đến 255 ký tự")
    @Pattern(

            regexp = "^[a-zA-Z0-9\\s,./ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵỷỹ]*$",

            message = "Địa chỉ không được chứa ký tự đặc biệt"

    )
    private String placeOfResidence;
    @NotNull(message = "Ngày cấp không được để trống")
    @PastOrPresent(message = "Ngày cấp không thể ở tương lai")
    private LocalDate issueDate;
    @NotBlank(message = "Nơi cấp không được để trống")
    private String issuePlace;
}

