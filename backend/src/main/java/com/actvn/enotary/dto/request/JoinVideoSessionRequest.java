package com.actvn.enotary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO request khi user join vào video session
 */
@Data
public class JoinVideoSessionRequest {

    @NotBlank(message = "Room ID không được để trống")
    private String roomId;
}

