# Hướng dẫn test video call 1-1 bằng **điện thoại (iPhone)** + **laptop/PC** (local)

Mục tiêu: 2 thiết bị cùng vào 1 phòng (room) để test đầy đủ **API + WebSocket signaling + WebRTC**.

> Thông tin mạng của bạn (từ `ipconfig`): **IPv4 của laptop/PC = `192.168.1.210`**.

---

## 1) Điều kiện bắt buộc

- iPhone và laptop/PC phải **cùng Wi‑Fi/LAN**.
- Backend (Spring Boot) phải lắng nghe ra ngoài LAN (không chỉ `localhost`).
- Frontend (Vite) phải chạy với `--host 0.0.0.0` để iPhone truy cập được.
- Firewall Windows phải cho phép truy cập các cổng:
  - `5173` (frontend dev)
  - `8080` (backend)

---

## 2) Chạy backend (Spring Boot) cho phép iPhone truy cập

Mở PowerShell tại thư mục project:

```powershell
cd C:\Users\tuyen\workspaces\e-notary-backend

# chạy backend lắng nghe tất cả IP
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.address=0.0.0.0 --server.port=8080"
```

Kiểm tra nhanh trên laptop/PC:
- Mở `http://localhost:8080` (hoặc gọi một API) xem server chạy.

Kiểm tra nhanh trên iPhone:
- Mở Safari → vào thử `http://192.168.1.210:8080` (nếu không vào được thì thường do firewall).

> Nếu bạn thấy log kiểu:
> `Securing POST /api/auth/login`
> thì đây **không phải lỗi**. Đó chỉ là log DEBUG của Spring Security.
> Lỗi thật thường nằm ở response (401/403/500) hoặc **CORS bị chặn** trên iPhone.

---

## 3) Chạy frontend (Vite) cho phép iPhone truy cập

```powershell
cd C:\Users\tuyen\workspaces\e-notary-backend\frontend
npm install

# chạy Vite cho phép thiết bị khác trong LAN truy cập
npm run dev -- --host 0.0.0.0 --port 5173
```

Trên iPhone mở Safari:
- `http://192.168.1.210:5173`

> Lưu ý quan trọng (iPhone/iPad): **Camera/Micro cần HTTPS**
>
> Trên iOS Safari, `getUserMedia()` (bật camera/mic) chỉ hoạt động trong **secure context (HTTPS)**.
> Nếu bạn mở bằng `http://192.168.1.210:5173` thì khi vào video call sẽ thường lỗi ở bước GET_MEDIA (TypeError/InsecureContext).
>
> Cách đơn giản nhất để test mà không cấu hình chứng chỉ phức tạp: dùng **tunnel HTTPS** trỏ vào Vite dev server.
> Vì bạn đang dùng Vite proxy, iPhone chỉ cần truy cập **5173**, API/WS sẽ được proxy về backend.

### Tạo HTTPS URL bằng tunnel (khuyến nghị)

**Cách A: ngrok**
```powershell
ngrok http 5173
```
Sau đó dùng URL `https://....ngrok-free.app` mở trên iPhone.

Nếu gặp lỗi từ Vite:
`Blocked request. This host ("...ngrok-free.dev") is not allowed.`
→ Hãy đảm bảo trong `frontend/vite.config.ts` có cấu hình:
`server.allowedHosts: true`
và **restart** lại `npm run dev`.

**Cách B: Cloudflare Tunnel (cloudflared)**
```powershell
cloudflared tunnel --url http://localhost:5173
```
Sau đó dùng URL `https://....trycloudflare.com` mở trên iPhone.

> Khuyến nghị cho local dev: **để frontend gọi API theo same-origin** (đường dẫn `/api/...`) và dùng Vite proxy.
> Cách này giúp bạn **tránh CORS** và thường là nguyên nhân chính khiến iPhone báo “Đăng nhập thất bại”.

---

## 4) Cấu hình frontend để gọi đúng API/WS từ iPhone

### Cách 4.1 (khuyến nghị): Dùng Vite proxy (KHÔNG cần CORS)
1) **Không set** `VITE_API_BASE_URL` và `VITE_VIDEO_SIGNALING_WS_URL` (hoặc tạm thời xoá/đổi tên file `frontend/.env.local`).
2) Giữ nguyên cấu hình proxy trong `frontend/vite.config.ts`:
   - `/api` → `http://localhost:8080`
   - `/ws` → `ws://localhost:8080`

Khi đó trên iPhone:
- UI chạy tại `http://192.168.1.210:5173`
- API sẽ gọi tới `http://192.168.1.210:5173/api/...` (same-origin)
- Vite sẽ proxy sang backend `localhost:8080`

=> iPhone chỉ cần truy cập port **5173** (thường ít bị chặn hơn).

Nếu iPhone vẫn báo `axiosStatus=403` khi login qua `/api/auth/login`:
- Hãy **restart** lại `npm run dev` (vì thay đổi proxy chỉ có hiệu lực sau khi restart).
- Đảm bảo bạn đang truy cập đúng: `http://192.168.1.210:5173` (không phải `localhost`).

### Cách 4.2: Gọi trực tiếp backend (CẦN CORS + mở port 8080)
Chỉ dùng cách này khi bạn muốn test gần production hơn.

Tạo file `frontend/.env.local` với nội dung:

```env
VITE_API_BASE_URL=http://192.168.1.210:8080
VITE_VIDEO_SIGNALING_WS_URL=ws://192.168.1.210:8080/ws/video-signaling
```

Sau đó **restart** `npm run dev`.

### (Quan trọng) CORS cho iPhone/LAN
Backend đã được cấu hình để cho phép CORS local/LAN theo `app.cors.allowed-origins`.
Mặc định đã bao gồm các origin kiểu:
- `http://localhost:5173`
- `http://127.0.0.1:5173`
- `http://192.168.*.*:5173`
- `http://10.*.*.*:5173`

Nếu bạn muốn set chặt hơn, có thể override khi chạy backend:

```powershell
$env:APP_CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://192.168.1.210:5173"
cd C:\Users\tuyen\workspaces\e-notary-backend
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.address=0.0.0.0 --server.port=8080"
```

---

## 5) Cách test 2 role (NOTARY/CLIENT)

### Cách 5.1 (khuyến nghị): dùng link meetingUrl/token
1) Trên laptop/PC: đăng nhập NOTARY → vào lịch hẹn → lấy link phòng họp (meeting URL), dạng:
   - `http://<backend-host>/api/video/room/<roomId>?token=<token>`
2) Gửi link đó sang iPhone (AirDrop/iMessage/Zalo…)
3) Mở link trên iPhone (Safari) → hệ thống redirect sang frontend `/video/room/...`.

### Cách 5.2: vào từ giao diện
- Laptop/PC: đăng nhập NOTARY → vào phòng.
- iPhone: đăng nhập CLIENT → vào phòng từ lịch hẹn.

> Lưu ý: iPhone phải **Allow** camera/mic khi Safari hỏi quyền.

---

## 6) Nếu iPhone vào được UI nhưng không gọi được API/WS

### 6.1 Windows Firewall
Nếu iPhone không mở được `http://192.168.1.210:5173` hoặc `http://192.168.1.210:8080`:
- Mở **Windows Security → Firewall & network protection → Allow an app through firewall**
  - Cho phép: Java(TM) Platform / IntelliJ / Node.js (tuỳ cách bạn chạy).
- Hoặc mở port inbound 5173 và 8080.

### 6.2 iPhone dùng 4G (khác mạng)
Nếu iPhone không cùng Wi‑Fi (đang 4G), sẽ không vào được IP LAN `192.168...`.

---

## 7) Checklist WebRTC (nếu vào phòng được nhưng không thấy hình/tiếng)

- Hai thiết bị cùng Wi‑Fi thường OK với STUN.
- Nếu test qua NAT phức tạp/khác mạng, bạn có thể cần TURN server.
- Đảm bảo không bị chặn camera/micro ở iPhone (Safari → Settings → Camera/Microphone permissions).

---

## 8) Cách thu thập lỗi dễ nhất (không cần DevTools)

Trong màn hình `VideoRoomPage` mình đã thêm:
- Thông báo lỗi kèm **Bước lỗi** (Verify token / Join / Get media / WS…)
- Mục **“Chi tiết kỹ thuật”** (bấm mở ra)

Bạn chỉ cần chụp màn hình phần đó là đủ để xác định lỗi.

