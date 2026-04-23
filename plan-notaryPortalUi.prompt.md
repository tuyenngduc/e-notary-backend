## Plan: Hoàn thiện portal công chứng viên

Chuẩn hoá UX theo đúng nghiệp vụ NOTARY: bắt buộc xác thực hồ sơ trước khi xử lý, hiển thị dữ liệu tiếng Việt (label enum), xem tài liệu trực tiếp như phía client, và siết lại hành động (từ chối có popup, lên lịch chỉ sau khi đã tiếp nhận và thuộc “Hồ sơ của tôi”). Đồng thời cập nhật backend để enforce “NOTARY phải VERIFIED” khi accept/reject/schedule, tránh chỉ chặn ở UI.

### Steps
1. Sắp xếp lại menu NOTARY trong `DashboardLayout` ở [frontend/src/components/DashboardLayout.tsx](frontend/src/components/DashboardLayout.tsx): “Trang chủ” → “Thông tin cá nhân” → “Yêu cầu công chứng” → “Lịch hẹn”.
2. Tạo bộ label tiếng Việt dùng chung (contractType/serviceType/docType/status) và áp dụng lên [NotaryDashboardPage](frontend/src/pages/notary/NotaryDashboardPage.tsx), [NotaryRequestsPage](frontend/src/pages/notary/NotaryRequestsPage.tsx), [NotaryRequestDetailPage](frontend/src/pages/notary/NotaryRequestDetailPage.tsx) để không còn hiển thị kiểu `TRANSFER_OF_PROPERTY`.
3. Bắt buộc “NOTARY cần xác thực thông tin” ở UI: hiển thị banner cảnh báo + CTA về `/profile` (ưu tiên đặt trong `DashboardLayout` để áp dụng toàn portal), đồng thời disable các action nghiệp vụ khi `verificationStatus !== VERIFIED`.
4. Nâng cấp xem tài liệu trực tiếp cho NOTARY tại [NotaryRequestDetailPage](frontend/src/pages/notary/NotaryRequestDetailPage.tsx): thêm nút “Xem” mở trình xem (iframe overlay) giống [CustomerRequestDetailPage](frontend/src/pages/customer/CustomerRequestDetailPage.tsx) với `viewingDocUrl`.
5. Đổi UX “Từ chối” tại [NotaryRequestDetailPage](frontend/src/pages/notary/NotaryRequestDetailPage.tsx): chỉ khi bấm nút “Từ chối” mới mở popup nhập lý do; popup có “Hủy/ Xác nhận” và chỉ call `rejectRequestApi` khi xác nhận.
6. Siết điều kiện “Lên lịch hẹn” theo nghiệp vụ & API: chỉ render/enable phần lên lịch khi `request.status === 'ACCEPTED'` (tức đã tiếp nhận, phù hợp backend `scheduleAppointment`), và thể hiện rõ rằng chỉ hồ sơ trong tab “Hồ sơ của tôi” mới có thể lên lịch trong [NotaryRequestsPage](frontend/src/pages/notary/NotaryRequestsPage.tsx) (tab accepted).

### Further Considerations
1. “Chỉnh API” (backend enforcement): cập nhật [NotaryRequestController](src/main/java/com/actvn/enotary/controller/NotaryRequestController.java) để chặn `accept/reject/schedule` nếu NOTARY `verificationStatus != VERIFIED` (fetch từ `UserRepository`/`UserService`).
2. Lịch hẹn NOTARY: nếu muốn trang [NotaryAppointmentsPage](frontend/src/pages/notary/NotaryAppointmentsPage.tsx) hoạt động thật, cần thêm endpoint như `GET /api/appointments/me` trả về `scheduledTime`, `meetingUrl/physicalAddress`, `status`, `requestId`.
3. Quy tắc upload/replace `SESSION_VIDEO`: NOTARY có được upload/replace không, hay chỉ hệ thống tạo? Nên thống nhất để tránh UI hiển thị sai hành vi.

Kiểm tra hệ thống của tôi, tôi cần tài khoản công chứng viên cũng cần xác thực ( api PUT /api/profile) như client, 