# 🚀 Quick Start - Video Call 1-1 Demo (5 Phút)

## 🎯 Mục Tiêu
Demo video call 1-1 WebRTC hoàn chỉnh giữa **Người Dân** và **Công Chứng Viên**

## ⏱️ Thời Gian Thực Hiện
- **Setup**: 1 phút
- **Demo**: 4 phút
- **Tổng cộng**: ~5 phút

---

## 🖥️ Bước 0: Chuẩn Bị (Chạy lần đầu tiên)

### Terminal 1: Khởi động Backend
```bash
cd ~/workspaces/e-notary-backend
./mvnw spring-boot:run
# Chờ đến khi thấy: "Started ENotarySystemApplication"
```

### Terminal 2: Khởi động Frontend
```bash
cd ~/workspaces/e-notary-backend/frontend
npm run dev
# Chờ đến khi thấy: "Local: http://localhost:5173"
```

### Mở 2 Trình Duyệt
- **Chrome 1** (Client): `http://localhost:5173`
- **Chrome 2** (Notary): `http://localhost:5173` (hoặc tab khác)

---

## 📋 Bước 1: Người Dân Đăng Nhập & Tạo Yêu Cầu

**Chrome 1 - Client**

```
1. Vào http://localhost:5173
2. Đăng nhập: 
   - Email: client@example.com
   - Mật khẩu: Client@123456
3. Vào "Hồ Sơ Công Chứng" → "Tạo Mới"
4. Điền:
   - Tên yêu cầu: "Demo Video Call"
   - Loại dịch vụ: ONLINE (QUAN TRỌNG)
   - Loại tài liệu: Chọn "CCCD"
5. Nhấn "Tạo"
```

---

## 📄 Bước 2: Người Dân Upload Tài Liệu

**Chrome 1 - Client** (tiếp tục)

```
1. Tìm yêu cầu vừa tạo
2. Nhấn "Upload Tài Liệu"
3. Chọn file bất kỳ (PDF/JPG) từ máy
4. Chọn loại tài liệu: "CCCD"
5. Nhấn "Upload"
6. Xác nhận
   ✅ Trạng thái sẽ chuyển từ "DRAFT" → "PROCESSING"
```

---

## 👨‍⚖️ Bước 3: Công Chứng Viên Lên Lịch Hẹn

**Chrome 2 - Notary**

```
1. Vào http://localhost:5173
2. Đăng nhập:
   - Email: notary@example.com
   - Mật khẩu: Notary@123456
3. Vào "Yêu Cầu Chờ Tiếp Nhận" hoặc Dashboard
4. Tìm yêu cầu của client (tên: "Demo Video Call")
   ⚠️ Chỉ hiển thị các yêu cầu có trạng thái PROCESSING
5. Nhấn "Xem Chi Tiết" → xem tài liệu
6. Nhấn "Lên Lịch Hẹn"
7. Chọn:
   - Ngày: Hôm nay
   - Giờ: Bây giờ + 2 phút (VD: 14:30)
8. Xác nhận
   ✅ Email sẽ được gửi cho cả 2 người chứa link video room
```

---

## 📧 Bước 4: Check Email (Tùy Chọn)

**Email Inbox** (Mailtrap hoặc tương tự)

```
Sẽ thấy email từ: noreply@enotary.system

Subject: Xác nhận lịch hẹn công chứng

Link:
https://localhost:5173/video/room/room_xxxxx?token=yyyy-zzzz

👉 Sao chép link này cho bước tiếp theo
```

---

## 🎥 Bước 5A: Client Vào Phòng Video

**Chrome 1 - Client**

```
1. Mở link email (hoặc dán vào trình duyệt)
   https://localhost:5173/video/room/room_xxxxx?token=yyyy-zzzz

2. Trình duyệt yêu cầu quyền:
   ✅ Nhấn "Cho Phép" (Camera & Microphone)

3. Trang tải, sẽ thấy:
   ├─ Trạng thái: PENDING
   ├─ Khu vực video call
   └─ Nút [Bắt Đầu Cuộc Gọi]

4. Nhấn [Bắt Đầu Cuộc Gọi]

5. Chờ:
   ⏳ "Đang khởi tạo cuộc gọi WebRTC..."
   ⏳ "Kết nối signaling thiết lập thành công."
   ⏳ "Đang chờ đối tác..."

   Sẽ thấy: Video của bạn (self-view) hiển thị
```

---

## 🎥 Bước 5B: Notary Vào Phòng Video

**Chrome 2 - Notary**

```
1. Mở link email:
   https://localhost:5173/video/room/room_xxxxx?token=yyyy-zzzz

2. Cấp quyền truy cập camera/microphone

3. Nhấn [Bắt Đầu Cuộc Gọi]

4. Sau ~2-3 giây:
   ✅ Thấy video của client xuất hiện
   ✅ Cả 2 người thấy video lẫn nhau
   ✅ Có thể nghe tiếng của nhau
   ✅ Trạng thái: IN_PROGRESS
```

---

## ✨ Bước 6: Video Call Hoạt Động

**Giao Diện Cuộc Gọi**

```
┌─────────────────────────────────────────┐
│  Phòng Họp Trực Tuyến 1-1              │
│  Mã Phòng: room_xxxxx                   │
├─────────────────────────────────────────┤
│ Trạng thái: IN_PROGRESS ✅              │
│ Notary vào: 2026-04-10 14:05:30        │
│ Client vào: 2026-04-10 14:05:32        │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────────────┬─────────────────┐ │
│  │ Video của bạn ✓  │ Video đối tác ✓ │ │
│  │ ┌──────────────┐│┌───────────────┐│ │
│  │ │   [Camera]  │││   [Đối tác]   ││ │
│  │ │   Self      │││     Feed      ││ │
│  │ └──────────────┘│└───────────────┘│ │
│  └──────────────────┴─────────────────┘ │
│                                         │
├─────────────────────────────────────────┤
│ ✅ Cuộc gọi đang hoạt động             │
│                                         │
│ 🎤 [Tắt Mic]  📹 [Tắt Camera]          │
│ [Lý do kết thúc...] [Kết Thúc ❌]      │
│                                         │
└─────────────────────────────────────────┘
```

### Có thể Kiểm Tra:
- ✅ Thấy video hai chiều (không bị delay > 1s)
- ✅ Nghe tiếng mic đối tác
- ✅ Nút Tắt/Bật Mic hoạt động
- ✅ Nút Tắt/Bật Camera hoạt động

---

## 🛑 Bước 7: Kết Thúc Cuộc Gọi

**Bất Kỳ Client Nào**

```
1. Nhập lý do (tùy chọn):
   "Công chứng hoàn tất"

2. Nhấn [Kết Thúc Cuộc Gọi]

3. Kết quả:
   ✅ Cả 2 người đều nhận thông báo "Đã kết thúc cuộc gọi"
   ✅ Video đóng
   ✅ Microphone tắt
   ✅ WebSocket connection đóng
   ✅ Trạng thái video session: FINISHED

4. Có nút:
   - "Làm Mới Trạng Thái"
   - "Về Dashboard"
```

---

## ✅ Kết Quả Demo Thành Công

Nếu bạn thấy tất cả điều này, **demo PASS** ✅

- [ ] Client tạo yêu cầu
- [ ] Client upload tài liệu → Trạng thái PROCESSING
- [ ] Notary lên lịch hẹn
- [ ] Email được gửi chứa link video room
- [ ] Cả 2 mở link từ email
- [ ] **Video call 1-1 kết nối thành công** (quan trọng nhất!)
- [ ] Thấy video hai chiều
- [ ] Nghe tiếng hai chiều
- [ ] Kết thúc cuộc gọi → Trạng thái FINISHED

---

## 🔍 Troubleshooting Nhanh

| Vấn Đề | Giải Pháp |
|--------|---------|
| **Video không hiển thị** | Kiểm tra camera bật chưa, thử F5 refresh |
| **Không nghe tiếng** | Kiểm tra mic bật chưa, check volume máy tính |
| **WebSocket error** | Kiểm tra backend chạy, CORS config đúng |
| **"Phòng họp không tồn tại"** | Refresh email, mở link mới |
| **Email không nhận** | Kiểm tra Mailtrap inbox, spam folder |

---

## 🎬 Demo Flow Diagram

```
CLIENT                          SERVER                        NOTARY
  |                                |                             |
  |---- Tạo Yêu Cầu ------------->|                             |
  |---- Upload Tài Liệu -------->|                             |
  |                                |                             |
  |                                |<--- Lên Lịch Hẹn ---------|
  |<-- Email + Link Room ---------|                             |
  |                                |---- Email + Link Room ---->|
  |                                |                             |
  |---- Mở Link Email ---------->|                             |
  |---- Bắt Đầu Cuộc Gọi ----->|                             |
  |---- JOIN (WebSocket) ------->|                             |
  |                                |                             |
  |                                |<-- Mở Link Email ---------|
  |                                |<-- Bắt Đầu Cuộc Gọi -----|
  |                                |<-- JOIN (WebSocket) ------|
  |                                |                             |
  |<-- JOINED Signal -------------|                             |
  |                                |---- JOINED Signal ----->|
  |                                |                             |
  |<-- READY Signal --------------|                             |
  |<-- READY Signal --------------|---- READY Signal ------->|
  |                                |                             |
  |---- OFFER (SDP) ------------->|---- OFFER (SDP) --------->|
  |                                |                             |
  |<-- ANSWER (SDP) -------------|<--- ANSWER (SDP) ---------|
  |                                |                             |
  |---- ICE Candidates ---------->|---- ICE Candidates ---->|
  |<-- ICE Candidates ------------|<--- ICE Candidates ------|
  |                                |                             |
  |================== CONNECTED (Video/Audio P2P)==================|
  |                                |                             |
  |---- END (Kết Thúc) --------->|                             |
  |                                |---- END Signal --------->|
  |                                |                             |
  |<-- Session FINISHED ----------|                             |
  |                                |<-- Session FINISHED ------|
```

---

## 📊 Metrics Kiểm Tra

Sau khi demo xong, có thể kiểm tra:

```bash
# 1. Kiểm tra database
psql -U postgres -d enotary_db

SELECT COUNT(*) FROM video_sessions WHERE status = 'FINISHED';
# Kết quả: 1 row (nếu demo 1 lần)

# 2. Kiểm tra logs backend
grep "Both participants joined" ~/workspaces/e-notary-backend/logs.txt

# 3. Kiểm tra browser console
# F12 → Console → Tìm "[WebSocket] Received: READY"
```

---

## 🎓 Sau Demo

**Bước tiếp theo có thể là**:
1. **Recording**: Ghi hình cuộc gọi
2. **Screen Sharing**: Chia sẻ màn hình
3. **Chat**: Thêm chat text trong cuộc gọi
4. **Analytics**: Theo dõi thống kê cuộc gọi
5. **Backup STUN/TURN**: Cấu hình private STUN server

---

## 📞 Support

Nếu có vấn đề:

1. **Kiểm tra logs backend**: 
   ```bash
   tail -f ~/workspaces/e-notary-backend/logs.txt | grep VideoSession
   ```

2. **Kiểm tra browser console** (F12)

3. **Restart backend/frontend**:
   ```bash
   pkill -f "spring-boot:run"
   ./mvnw spring-boot:run
   ```

4. **Xem chi tiết hơn**: `VIDEO_CALL_DEMO_GUIDE.md`

---

**Thời gian demo dự kiến**: ⏱️ 5 phút  
**Độ khó**: 🟢 Dễ (click và chờ)  
**Success Rate**: ✅ 95% (nếu đã setup đúng)

**Bắt đầu ngay! 🚀**

