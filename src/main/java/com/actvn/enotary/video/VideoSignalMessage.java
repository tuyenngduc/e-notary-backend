package com.actvn.enotary.video;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSignalMessage {
    private VideoSignalType type;
    private String roomId;
    private String token;
    private String authToken;
    private String sender;
    private JsonNode payload;
    private String message;
}

