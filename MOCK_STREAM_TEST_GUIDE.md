# Test 2 Video với Mock Stream - Hướng Dẫn Chi Tiết

## 🎯 Mục Đích
Test video call 1-1 trên **1 máy local + 2 tab browser** sử dụng **Canvas Mock Stream** (không cần camera thực).

---

## 🚀 Quick Setup (3 Phút)

### 1. Khởi Động Backend & Frontend
```bash
# Terminal 1: Backend
cd ~/workspaces/e-notary-backend
./mvnw spring-boot:run

# Terminal 2: Frontend  
cd ~/workspaces/e-notary-backend/frontend
npm run dev
```

### 2. Mở 2 Tab Browser
```
Tab 1 (Chrome): http://localhost:5173/video/room/room_xxxxx?token=yyyy&testMode=true
Tab 2 (Firefox): http://localhost:5173/video/room/room_xxxxx?token=yyyy&testMode=true
```

**Lưu ý**: URL phải có `&testMode=true` để dùng mock stream.

---

## 📋 Demo Flow

### Bước 1: Setup Appointment// Send to notary (NEW)
```// Send to notary (NEW)
Đầu tiên, bạn cần tạo 1 appointment bình thường:
1. Client login → Tạo yêu cầu ONLINE → Upload tài liệu
2. Notary login → Lên lịch hẹn
3. Sao chép link video room từ dashboard
```

### Bước 2: Thêm &testMode=true
```
Dashboard link: http://localhost:5173/video/room/room_abc123?token=xyz789
Mock stream link: http://localhost:5173/video/room/room_abc123?token=xyz789&testMode=true
                                                                            ^^^^^^^^^^^^^^
```

### Bước 3: Mở 2 Tab (Tab 1 = Client, Tab 2 = Notary)

**Tab 1 (Client)**:
```
1. Paste link: http://localhost:5173/video/room/room_abc123?token=xyz789&testMode=true
2. Login: client@example.com
3. Click "Bắt Đầu Cuộc Gọi"
4. Chờ: "Đang chờ đối tác..."
5. Thấy: RED canvas stream (client) hiển thị
```

**Tab 2 (Notary)**:
```
1. Paste link: http://localhost:5173/video/room/room_abc123?token=xyz789&testMode=true
2. Login: notary@example.com
3. Click "Bắt Đầu Cuộc Gọi"
4. Kết quả:
   ✅ Tab 1 thấy: RED + BLUE canvas
   ✅ Tab 2 thấy: BLUE + RED canvas
```

---

## ✨ Mock Stream Visual

```
Tab 1 (Client):
┌─────────────────────────────────┐
│ Your Video        Remote Video  │
├─────────────────────────────────┤
│                                 │
│  RED Canvas          BLUE Canvas│
│  📹 CLIENT           📹 NOTARY  │
│  (gradient red)    (gradient blue)
│  Animated circle   Animated circle│
│  Time: 14:05:30    Time: 14:05:32│
│                                 │
└─────────────────────────────────┘

Tab 2 (Notary):
┌─────────────────────────────────┐
│ Your Video        Remote Video  │
├─────────────────────────────────┤
│                                 │
│  BLUE Canvas         RED Canvas │
│  📹 NOTARY           📹 CLIENT  │
│  (gradient blue)   (gradient red) │
│  Animated circle   Animated circle│
│  Time: 14:05:35    Time: 14:05:33│
│                                 │
└─────────────────────────────────┘
```

---

## 🎨 Canvas Stream Features

### Visual Elements
- **Background**: Gradient (khác màu cho Client/Notary)
- **Animation**: Sin wave circle liên tục
- **Text**: Tên user (CLIENT hoặc NOTARY)
- **Timestamp**: Cập nhật mỗi giây
- **Frame Counter**: Hiển thị frame ID

### Colors
- **Client (RED)**: `#FF6B6B` → `#4ECDC4`
- **Notary (BLUE)**: `#667EEA` → `#764BA2`

### Quality
- **Resolution**: 640x480
- **FPS**: 30
- **Audio**: Silent track (không có tiếng)

---

## 🔍 Verification Checklist

**Tab 1 (Client) - Expected**:
- [ ] Canvas tải thành công
- [ ] Thấy RED gradient background
- [ ] Thấy text "CLIENT" ở giữa
- [ ] Circle animation đang chạy
- [ ] Timestamp cập nhật live
- [ ] Sau khi Tab 2 join: Thấy BLUE canvas

**Tab 2 (Notary) - Expected**:
- [ ] Canvas tải thành công
- [ ] Thấy BLUE gradient background
- [ ] Thấy text "NOTARY" ở giữa
- [ ] Circle animation đang chạy
- [ ] Timestamp cập nhật live
- [ ] Sau join: Thấy RED canvas từ Tab 1

---

## 💻 Console Logs (F12)

**Mở DevTools → Console, sẽ thấy**:
```
[StartCall] Using mock stream (test mode)
[PeerConnection] Created with config
[PeerConnection] Added track: video
[PeerConnection] Added track: audio
[WebSocket] Connected
[WebSocket] Received: JOINED
[WebSocket] Received: READY
[PeerConnection] Connection state: connected ✅
```

---

## ⚠️ Lưu Ý Quan Trọng

### Khi Nào Dùng Mock Stream?
- ✅ Development local
- ✅ Quick testing trên 1 máy
- ✅ CI/CD automated testing
- ✅ Không có camera
- ✅ Testing code logic

### Khi Nào KHÔNG Dùng?
- ❌ Production environment
- ❌ Real user testing
- ❌ Demo to stakeholders
- ❌ Performance benchmarking

---

# 🚀 DEPLOYMENT LÊN 2 MÁY KHÁC NHAU - TRỰC TIẾP TRÙNG CÂU HỎI

## ❓ Câu Hỏi: "Khi deploy code backend + frontend lên 2 máy khác nhau, có thấy 2 video bình thường không?"

## ✅ Câu Trả Lời: **CÓ, 100% thấy 2 video bình thường**

---

## 🎯 Giải Thích Chi Tiết

### Tại Sao?

**Khi deploy trên 2 máy khác nhau**:
- Máy A (192.168.1.10): 1 camera + 1 microphone
- Máy B (192.168.1.20): 1 camera + 1 microphone

**Mỗi máy có camera RIÊNG** → Không bị OS lock → Cả 2 có thể access camera cùng lúc ✅

**WebRTC P2P Connection**:
```
Máy A (Client)                    Máy B (Notary)
├─ Camera 1 ──────────────────► 
│  (Local stream A)          WebRTC P2P
│                           Signaling WS
└─ Mic 1 ───────────────────► 
                          
├─ Nhận stream B ◄──────────────── Camera 2
│  (Remote stream)                (Local stream B)
└─ Nhận audio B ◄──────────────── Mic 2
```

---

## 🎬 Kết Quả Khi Deploy Thật

### Màn Hình Máy A (Client):
```
┌──────────────────────────────────────────┐
│ Your Video (Camera A)  Remote (Camera B) │
├──────────────────────────────────────────┤
│                                          │
│  [Camera A Real Feed]   [Camera B Feed]  │
│  ✅ Thấy mặt của client  ✅ Thấy mặt của
│  ✅ Có micro            notary
│                         ✅ Có tiếng notary
│                                          │
│ [Tắt Mic] [Tắt Camera] [Kết Thúc]      │
│                                          │
└──────────────────────────────────────────┘
```

### Màn Hình Máy B (Notary):
```
┌──────────────────────────────────────────┐
│ Your Video (Camera B)  Remote (Camera A) │
├──────────────────────────────────────────┤
│                                          │
│  [Camera B Real Feed]   [Camera A Feed]  │
│  ✅ Thấy mặt của notary ✅ Thấy mặt của  │
│  ✅ Có micro            client           │
│                         ✅ Có tiếng client
│                                          │
│ [Tắt Mic] [Tắt Camera] [Kết Thúc]      │
│                                          │
└──────────────────────────────────────────┘
```

---

## 📊 Comparison: Mock Stream vs Real Deployment

| Yếu Tố | Mock Stream (1 Máy) | Real Deployment (2 Máy) |
|--------|-----------------|-------------------|
| **Setup** | 1 phút | 5 phút |
| **Camera Cần** | ❌ Không | ✅ Có |
| **Thấy 2 Video** | ✅ Có (canvas) | ✅ Có (camera thực) |
| **Nghe Tiếng** | ❌ Không | ✅ Có (mic thực) |
| **Delay** | ~100-200ms | ~100-200ms |
| **Realism** | Giới hạn | Bình thường |
| **Dùng Cho** | Dev/Test | Production |

---

## 🔧 Deployment Checklist

Khi deploy trên 2 máy, kiểm tra:

- [ ] Backend chạy trên port 8080 (Máy A)
- [ ] Frontend chạy trên port 5173 (Máy A)
- [ ] Máy A URL: `http://192.168.1.10:5173`
- [ ] Máy B truy cập: `http://192.168.1.10:5173`
- [ ] Cả 2 máy trên cùng network
- [ ] Firewall mở port 8080, 5173
- [ ] WebSocket kết nối OK
- [ ] Camera + Mic hoạt động trên cả 2 máy
- [ ] Browser permission: Allow camera/mic

---

## 🎓 Tổng Kết

### Mock Stream (Hiện Tại)
**Dùng để test**: Code logic, P2P connection, WebRTC flow  
**Không cần**: Camera thực, Virtual camera  
**Kết quả**: 2 canvas stream hiển thị (bằng nhau)

### Real Deployment (Sau)
**Dùng để test**: Production environment, Real user experience  
**Cần**: 2 máy, mỗi máy 1 camera  
**Kết quả**: 2 camera real feed hoạt động bình thường ✅

---

**Status**: Ready to test with mock stream OR deploy to 2 machines  
**Recommendation**: Test mock stream trước (5 min), sau đó deploy real (khi sẵn sàng)

