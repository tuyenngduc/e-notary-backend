package com.actvn.enotary.config;

import com.actvn.enotary.video.VideoSignalingWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        if (allowedOrigins.length == 0) {
            allowedOrigins = new String[] {
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://192.168.*:5173",
                    "http://192.168.*.*:5173",
                    "http://10.*:5173",
                    "http://10.*.*.*:5173",
                    "http://172.16.*:5173",
                    "http://172.17.*:5173",
                    "http://172.18.*:5173",
                    "http://172.19.*:5173",
                    "http://172.2*.*:5173",
                    "http://172.3*.*:5173"
            };
        }

        // If config looks like local development (has localhost), also allow LAN IPv4 addresses of this machine.
        if (java.util.Arrays.stream(allowedOrigins).anyMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"))) {
            allowedOrigins = withLocalLanOrigins(allowedOrigins, 5173);
        }

        boolean hasWildcard = java.util.Arrays.stream(allowedOrigins).anyMatch(origin -> origin.contains("*"));
        if (hasWildcard) {
            registry.addHandler(videoSignalingWebSocketHandler, "/ws/video-signaling")
                    .setAllowedOriginPatterns(allowedOrigins);
        } else {
            registry.addHandler(videoSignalingWebSocketHandler, "/ws/video-signaling")
                    .setAllowedOrigins(allowedOrigins);
        }
    }

    private String[] withLocalLanOrigins(String[] base, int port) {
        Set<String> merged = new LinkedHashSet<>(List.of(base));
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address inet4) {
                        String ip = inet4.getHostAddress();
                        merged.add("http://" + ip + ":" + port);
                    }
                }
            }
        } catch (Exception ignored) {
            // Best-effort only.
        }
        return merged.toArray(new String[0]);
    }
}

