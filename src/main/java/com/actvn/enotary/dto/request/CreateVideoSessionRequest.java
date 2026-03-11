package com.actvn.enotary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * DTO request tạo video session khi appointment schedule
 */
@Data
public class CreateVideoSessionRequest {

    @NotNull(message = "Appointment ID không được để trống")
    private UUID appointmentId;
}

