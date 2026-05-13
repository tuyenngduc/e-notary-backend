# Hướng dẫn test local: Video call WebRTC 1-1 (trên cùng một máy)

## Vì sao bạn gặp `NotReadableError`
Nhiều webcam/micro trên Windows chỉ cho phép **một ứng dụng/tiến trình** sử dụng tại một thời điểm.
Vì vậy khi bạn mở phòng họp trên **2 tab / 2 trình duyệt** cùng một máy, phiên vào sau thường bị lỗi:

- `NotReadableError`: thiết bị đang bận / không thể khởi động

Đây **không phải** lỗi signaling/WebRTC của hệ thống, mà là giới hạn truy cập thiết bị.

## Các cách test local khuyến nghị

### Cách A (nhanh nhất): Cho 1 trình duyệt dùng camera/mic giả (fake)
Cách này giúp bạn test đầy đủ luồng video call 1-1 trên **một máy** mà không cần thiết bị thứ hai.

**Chrome** (PowerShell):
```powershell
& "C:\Program Files\Google\Chrome\Application\chrome.exe" `
  --user-data-dir="$env:TEMP\enotary-chrome-notary" `
  --use-fake-device-for-media-stream `
  --use-fake-ui-for-media-stream
```

**Edge** (PowerShell):
```powershell
& "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe" `
  --user-data-dir="$env:TEMP\enotary-edge-client" `
  --use-fake-device-for-media-stream `
  --use-fake-ui-for-media-stream
```

Ghi chú:
- `--user-data-dir=...` giúp mỗi trình duyệt dùng một profile/session riêng (không bị dính đăng nhập).
- Với fake devices, trình duyệt sẽ luôn “có” camera/mic để chạy test.

### Cách B: Dùng 2 thiết bị thật
- PC = NOTARY
- Điện thoại/laptop khác = CLIENT

Đây là cách gần giống production nhất.

### Cách C: Dùng virtual camera
Cài OBS Studio và bật **OBS Virtual Camera**, sau đó chọn virtual camera cho một phía.

## Checklist nhanh khi lỗi camera/mic
- Đóng các app có thể đang chiếm camera: Zoom/Teams/Meet/Discord/OBS...
- Windows Settings → Privacy & security → Camera/Microphone → bật quyền truy cập.
- Site permissions trên trình duyệt: cho phép camera/mic đối với domain local của bạn.

