package com.actvn.enotary.controller;
import com.actvn.enotary.dto.request.CreateVideoSessionRequest;
import com.actvn.enotary.dto.response.VideoSessionResponse;
import com.actvn.enotary.security.CustomUserDetails;
import com.actvn.enotary.service.VideoSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoSessionController {
    private final VideoSessionService videoSessionService;
    /**
     * Tạo video session cho appointment (Notary/Admin gọi API này)
     * POST /api/video/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<VideoSessionResponse> createVideoSession(
            Authentication authentication,
            @Valid @RequestBody CreateVideoSessionRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return ResponseEntity.status(401).build();
        }
        String role = userDetails.getRole() != null ? userDetails.getRole().name() : "";
        boolean isNotary = "NOTARY".equals(role);
        boolean isAdmin = "ADMIN".equals(role);
        if (!isNotary && !isAdmin) {
            return ResponseEntity.status(403).build();
        }
        VideoSessionResponse response = videoSessionService.createVideoSession(request);
        URI location = URI.create("/api/video/sessions/" + response.getSessionId());
        return ResponseEntity.created(location).body(response);
    }
    /**
     * Lấy thông tin video session bằng ID
     * GET /api/video/sessions/{id}
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<VideoSessionResponse> getVideoSession(
            Authentication authentication,
            @PathVariable("id") UUID sessionId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }
        VideoSessionResponse response = videoSessionService.getVideoSession(sessionId);
        return ResponseEntity.ok(response);
    }
    /**
     * Lấy video session bằng appointment ID
     * GET /api/video/appointments/{appointmentId}/session
     */
    @GetMapping("/appointments/{appointmentId}/session")
    public ResponseEntity<VideoSessionResponse> getVideoSessionByAppointment(
            Authentication authentication,
            @PathVariable("appointmentId") UUID appointmentId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }
        VideoSessionResponse response = videoSessionService.getVideoSessionByAppointmentId(appointmentId);
        return ResponseEntity.ok(response);
    }
    /**
     * Verify session token (dùng khi client truy cập từ email link)
     * GET /api/video/verify-token?token={token}
     */
    @GetMapping("/verify-token")
    public ResponseEntity<VideoSessionResponse> verifyToken(
            @RequestParam("token") String token) {
        VideoSessionResponse response = videoSessionService.verifySessionToken(token);
        return ResponseEntity.ok(response);
    }
    /**
     * Join vào video room
     * POST /api/video/room/{roomId}/join
     */
    @PostMapping("/room/{roomId}/join")
    public ResponseEntity<VideoSessionResponse> joinSession(
            Authentication authentication,
            @PathVariable("roomId") String roomId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        VideoSessionResponse response = videoSessionService.joinSession(roomId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
    /**
     * Kết thúc video session
     * POST /api/video/sessions/{id}/end
     */
    @PostMapping("/sessions/{id}/end")
    public ResponseEntity<VideoSessionResponse> endSession(
            Authentication authentication,
            @PathVariable("id") UUID sessionId,
            @RequestParam(value = "reason", defaultValue = "Normal end") String reason) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return ResponseEntity.status(401).build();
        }
        VideoSessionResponse response = videoSessionService.endSession(sessionId, reason);
        return ResponseEntity.ok(response);
    }
    /**
     * Hủy video session
     * POST /api/video/sessions/{id}/cancel
     */
    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<VideoSessionResponse> cancelSession(
            Authentication authentication,
            @PathVariable("id") UUID sessionId,
            @RequestParam(value = "reason", defaultValue = "Cancelled") String reason) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return ResponseEntity.status(401).build();
        }
        String role = userDetails.getRole() != null ? userDetails.getRole().name() : "";
        boolean isNotary = "NOTARY".equals(role);
        boolean isAdmin = "ADMIN".equals(role);
        if (!isNotary && !isAdmin) {
            return ResponseEntity.status(403).build();
        }
        VideoSessionResponse response = videoSessionService.cancelSession(sessionId, reason);
        return ResponseEntity.ok(response);
    }
    /**
     * Simple endpoint để test video room (development only)
     * GET /api/video/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<String> getVideoRoom(
            @PathVariable("roomId") String roomId,
            @RequestParam(value = "token", required = false) String token) {
        // Nếu có token, verify nó
        if (token != null && !token.isBlank()) {
            videoSessionService.verifySessionToken(token);
        }
        // Return HTML simple test page (hoặc redirect to frontend)
        String html = """
                <html>
                <head>
                    <title>Video Meet - %s</title>
                    <style>
                        body { font-family: Arial; margin: 40px; }
                        .container { max-width: 800px; margin: 0 auto; }
                        h1 { color: #333; }
                        p { color: #666; }
                        .info { background: #f0f0f0; padding: 10px; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Video Meeting Room</h1>
                        <div class="info">
                            <p><strong>Room ID:</strong> %s</p>
                            <p>Phòng họp online sẽ được tải từ frontend.</p>
                            <p>Frontend có thể sử dụng WebRTC library (Jitsi, Daily.co, etc.) để xây dựng UI video call.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(roomId, roomId);
        return ResponseEntity.ok(html);
    }
}
