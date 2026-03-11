package com.actvn.enotary.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ScheduleAppointmentRequest {

    @NotNull(message = "Thời gian hẹn không được để trống")
    @Future(message = "Thời gian hẹn phải ở tương lai")
    private OffsetDateTime scheduledTime;

    /**
     * Chỉ áp dụng cho ServiceType.OFFLINE.
     * Nếu để trống sẽ dùng địa chỉ mặc định "Văn phòng công chứng số 1".
     */
    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String physicalAddress;
}

