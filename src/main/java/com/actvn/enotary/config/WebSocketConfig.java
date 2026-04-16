package com.actvn.enotary.config;

import com.actvn.enotary.video.VideoSignalingWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final VideoSignalingWebSocketHandler videoSignalingWebSocketHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsProperty;

    public WebSocketConfig(VideoSignalingWebSocketHandler videoSignalingWebSocketHandler) {
        this.videoSignalingWebSocketHandler = videoSignalingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allowedOrigins = java.util.Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);

        registry.addHandler(videoSignalingWebSocketHandler, "/ws/video-signaling")
                .setAllowedOrigins(allowedOrigins);
    }
}

