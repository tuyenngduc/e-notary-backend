package com.actvn.enotary.video;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VideoSignalingService {

    public record JoinResult(int participantCount, String offererEmail) {}

    private static class RoomState {
        private final LinkedHashMap<String, WebSocketSession> participants = new LinkedHashMap<>();
    }

    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToEmail = new ConcurrentHashMap<>();

    public synchronized JoinResult join(String roomId, String email, WebSocketSession webSocketSession) {
        RoomState room = rooms.computeIfAbsent(roomId, ignored -> new RoomState());

        if (!room.participants.containsKey(email) && room.participants.size() >= 2) {
            throw new IllegalStateException("Phòng họp đã đủ 2 người tham gia.");
        }

        room.participants.put(email, webSocketSession);
        sessionToRoom.put(webSocketSession.getId(), roomId);
        sessionToEmail.put(webSocketSession.getId(), email);

        String offererEmail = room.participants.keySet().iterator().next();
        return new JoinResult(room.participants.size(), offererEmail);
    }

    public synchronized LeaveResult leave(WebSocketSession webSocketSession) {
        String webSocketSessionId = webSocketSession.getId();
        String roomId = sessionToRoom.remove(webSocketSessionId);
        String email = sessionToEmail.remove(webSocketSessionId);

        if (roomId == null || email == null) {
            return null;
        }

        RoomState room = rooms.get(roomId);
        if (room == null) {
            return null;
        }

        room.participants.remove(email);

        List<WebSocketSession> remainingSessions = new ArrayList<>(room.participants.values());
        if (room.participants.isEmpty()) {
            rooms.remove(roomId);
        }

        return new LeaveResult(roomId, email, remainingSessions);
    }

    public synchronized List<WebSocketSession> getOtherParticipants(String roomId, String senderEmail) {
        RoomState room = rooms.get(roomId);
        if (room == null) {
            return List.of();
        }

        return room.participants.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(senderEmail))
                .map(Map.Entry::getValue)
                .toList();
    }

    public record LeaveResult(String roomId, String email, List<WebSocketSession> remainingSessions) {}
}

