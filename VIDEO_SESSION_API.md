# Video Session Microservice - E-Notary System

## Overview
Đây là một microservice đơn giản để quản lý online video meeting giữa Notary và Client trong hệ thống công chứng điện tử.

## Architecture & Flow

### 1. Khi Schedule Appointment (ONLINE)
```
Client tạo yêu cầu công chứng (ServiceType.ONLINE)
    ↓
Notary duyệt hồ sơ (status → PROCESSING)
    ↓
Notary gọi POST /api/requests/{id}/schedule
    ↓
Backend tự động tạo VideoSession + Appointment
    ↓
Return meeting URL với session token
```

### 2. Khi Tham Gia Video Call
```
Client/Notary nhận email chứa meeting link + token
    ↓
Click link → Verify token → Join room
    ↓
System cập nhật session status (PENDING → NOTARY_JOINED → IN_PROGRESS)
    ↓
Client/Notary can video call với nhau
```

### 3. Kết Thúc Call
```
Một bên kết thúc cuộc gọi
    ↓
Gọi POST /api/video/sessions/{id}/end
    ↓
System tính thời gian kết nối
    ↓
Update status → FINISHED
```

## Database Schema

```sql
CREATE TABLE video_sessions (
    session_id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments,
    
    session_token VARCHAR(255) UNIQUE,  -- Token xác minh
    meeting_url VARCHAR(500),            -- URL công khai
    room_id VARCHAR(100) UNIQUE,         -- ID phòng họp nội bộ
    
    status VARCHAR(20),  -- PENDING | NOTARY_JOINED | IN_PROGRESS | FINISHED | CANCELLED
    
    notary_joined_at TIMESTAMPTZ,
    client_joined_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    
    duration_seconds BIGINT,  -- Thời lượng cuộc gọi
    notes TEXT,
    
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);
```

## API Endpoints

### 1. Tạo Video Session (Notary/Admin)
```bash
POST /api/video/sessions
Authorization: Bearer {token}
Content-Type: application/json

{
    "appointmentId": "550e8400-e29b-41d4-a716-446655440000"
}

Response:
{
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "appointmentId": "550e8400-e29b-41d4-a716-446655440000",
    "sessionToken": "eyJhbGciOiJIUzI1NiIs...",
    "meetingUrl": "http://localhost:8080/api/video/room/room_a1b2c3d4?token=eyJhbGci...",
    "roomId": "room_a1b2c3d4",
    "status": "PENDING",
    "createdAt": "2026-03-11T10:30:00Z",
    "notaryJoinedAt": null,
    "clientJoinedAt": null,
    "endedAt": null,
    "durationSeconds": null
}
```

### 2. Verify Session Token (Public - không cần auth)
```bash
GET /api/video/verify-token?token={sessionToken}

Response:
{
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "PENDING",
    "meetingUrl": "http://localhost:8080/api/video/room/room_a1b2c3d4",
    ...
}
```

### 3. Lấy Video Session bằng Appointment ID
```bash
GET /api/video/appointments/{appointmentId}/session
Authorization: Bearer {token}

Response: (VideoSessionResponse)
```

### 4. Join Video Room
```bash
POST /api/video/room/{roomId}/join
Authorization: Bearer {token}

Response:
{
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "NOTARY_JOINED",  // hoặc "IN_PROGRESS"
    "notaryJoinedAt": "2026-03-11T10:35:00Z",
    "clientJoinedAt": null,
    ...
}
```

### 5. Kết Thúc Video Call
```bash
POST /api/video/sessions/{sessionId}/end?reason=Normal%20end
Authorization: Bearer {token}

Response:
{
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "FINISHED",
    "endedAt": "2026-03-11T10:45:00Z",
    "durationSeconds": 600,
    "notes": "Normal end"
}
```

### 6. Hủy Video Session
```bash
POST /api/video/sessions/{sessionId}/cancel?reason=No%20show
Authorization: Bearer {token}

Response:
{
    "sessionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "CANCELLED",
    "notes": "No show"
}
```

### 7. View Video Room (Simple Test Page)
```bash
GET /api/video/room/{roomId}?token={sessionToken}

Response: HTML page (placeholder for frontend)
```

## Flow Diagram

### Schedule Appointment (ONLINE Service)
```
┌─────────────────────────────────────────────────────────────┐
│ POST /api/requests/{id}/schedule                            │
│ (Notary authenticates)                                       │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ▼
        ┌─────────────────────┐
        │ Create Appointment  │
        │ status = PENDING    │
        └─────────┬───────────┘
                  │
                  ▼
        ┌──────────────────────────────┐
        │ ServiceType == ONLINE?       │
        │ YES: Create VideoSession     │
        │ NO:  Set physicalAddress     │
        └─────────┬────────────────────┘
                  │
                  ▼
        ┌─────────────────────────────────────┐
        │ Generate unique:                    │
        │ - roomId: room_{uuid}               │
        │ - sessionToken: {uuid}              │
        │ - meetingUrl: base + room + token   │
        └─────────┬───────────────────────────┘
                  │
                  ▼
        ┌───────────────────────────────────────┐
        │ Save VideoSession (status=PENDING)    │
        │ Update Appointment.meetingUrl         │
        │ Update NotaryRequest.status=SCHEDULED │
        └─────────┬─────────────────────────────┘
                  │
                  ▼
        ┌──────────────────────────┐
        │ Response AppointmentDTO  │
        │ (with meetingUrl)        │
        └──────────────────────────┘
```

### Join Video Room
```
┌──────────────────────────────────────────┐
│ POST /api/video/room/{roomId}/join       │
│ (User authenticated)                     │
└──────────────┬───────────────────────────┘
               │
               ▼
       ┌──────────────────────────┐
       │ Find VideoSession        │
       │ Authorize user           │
       │ (notary or client check) │
       └──────────┬───────────────┘
                  │
                  ▼
       ┌─────────────────────────────────┐
       │ Update:                         │
       │ - notaryJoinedAt (if notary)    │
       │ - clientJoinedAt (if client)    │
       │ - status update                 │
       └──────────┬──────────────────────┘
                  │
                  ▼
       ┌─────────────────────────────────┐
       │ Status transitions:             │
       │ PENDING → NOTARY_JOINED         │
       │ NOTARY_JOINED → IN_PROGRESS     │
       │ (when both joined)              │
       └─────────────────────────────────┘
```

## Video Session Status Flow

```
PENDING (Session created, waiting for users)
  ├─→ NOTARY_JOINED (Notary joined, waiting for client)
  │     └─→ IN_PROGRESS (Both joined, call active)
  │           ├─→ FINISHED (Call ended normally)
  │           └─→ CANCELLED (One party disconnected)
  │
  └─→ CANCELLED (Session cancelled before anyone joined)
```

## Configuration

### application.yml
```yaml
app:
  meeting:
    base-url: ${MEETING_BASE_URL:http://localhost:8080}
```

### Environment Variables
```bash
export MEETING_BASE_URL="https://yourdomain.com"  # Production URL
```

## Frontend Integration

### 1. Nhận link từ email
```
Meeting link: https://yourdomain.com/api/video/room/room_a1b2c3d4?token=eyJhbGci...
```

### 2. Frontend xử lý
```javascript
// Verify token khi load trang
GET /api/video/verify-token?token={token}

// Nếu valid, hiển thị UI để join
POST /api/video/room/{roomId}/join

// Load WebRTC library (Jitsi, Daily.co, etc)
// Tạo video connection với roomId
// Khi disconnect
POST /api/video/sessions/{sessionId}/end
```

## Testing

### 1. Create Appointment (ONLINE)
```bash
curl -X POST http://localhost:8080/api/requests/{requestId}/schedule \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledTime": "2026-03-15T10:00:00Z"
  }'
```

### 2. Verify Token
```bash
curl "http://localhost:8080/api/video/verify-token?token={sessionToken}"
```

### 3. Join Room
```bash
curl -X POST http://localhost:8080/api/video/room/{roomId}/join \
  -H "Authorization: Bearer {token}"
```

### 4. End Session
```bash
curl -X POST http://localhost:8080/api/video/sessions/{sessionId}/end \
  -H "Authorization: Bearer {token}" \
  -d "reason=Normal%20end"
```

## Advanced Features (Future)

1. **Recording**: Lưu video recording của cuộc gọi
2. **Screen Sharing**: Chia sẻ màn hình giữa notary và client
3. **Call Timeout**: Tự động hủy session nếu client không join trong X phút
4. **Notifications**: Gửi push notification khi notary/client join/leave
5. **Analytics**: Thống kê thời gian cuộc gọi, tỷ lệ kết nối thành công
6. **WebRTC Integration**: Tích hợp Jitsi Meet hoặc Daily.co
7. **Call Quality Metrics**: Theo dõi chất lượng kết nối
8. **Participant Management**: Quản lý danh sách người tham gia

## Error Handling

| Error | HTTP Code | Message |
|-------|-----------|---------|
| Appointment not found | 404 | "Không tìm thấy lịch hẹn" |
| Video session not found | 404 | "Không tìm thấy video session" |
| Invalid token | 401 | "Token không hợp lệ" |
| Not authorized | 403 | "Bạn không có quyền truy cập phòng họp này" |
| Session already exists | 409 | "Lịch hẹn này đã có video session" |
| Wrong service type | 400 | "Video session chỉ dành cho cuộc hẹn ONLINE" |
| Not authenticated | 401 | Unauthorized |

## Dependencies

- Spring Boot 3.4.12
- Spring Data JPA
- PostgreSQL
- Spring Security
- Lombok

## Files Created

```
src/main/java/
├── entity/
│   └── VideoSession.java
├── enums/
│   └── VideoSessionStatus.java
├── repository/
│   └── VideoSessionRepository.java
├── service/
│   └── VideoSessionService.java
├── controller/
│   └── VideoSessionController.java
└── dto/
    ├── request/
    │   ├── CreateVideoSessionRequest.java
    │   └── JoinVideoSessionRequest.java
    └── response/
        └── VideoSessionResponse.java

src/main/resources/
└── db/migration/
    └── V4__video_sessions.sql

Modified:
- NotaryRequestService.java (integrate video session creation)
- application.yml (add meeting config)
```

## Notes

1. **Token-based Access**: Session token được sử dụng để verify user có quyền access phòng họp mà không cần login
2. **Automatic Session Creation**: VideoSession tự động được tạo khi Notary schedule ONLINE appointment
3. **Status Tracking**: System tự động cập nhật trạng thái dựa trên khi users join/leave
4. **Duration Calculation**: Thời lượng cuộc gọi được tính từ khi client join cho đến khi call kết thúc
5. **Database Indexed**: Có indexes trên các field thường xuyên query (appointment_id, room_id, status, created_at)

---

**Version**: 1.0.0  
**Last Updated**: March 11, 2026

