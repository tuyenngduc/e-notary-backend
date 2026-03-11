# Reject Notary Request API - Hướng Dẫn

## Tổng Quan

API này cho phép Notary hoặc Admin **từ chối** một yêu cầu công chứng khi sau khi review hồ sơ và phát hiện có vấn đề.

## Endpoint

```
POST /api/requests/{id}/reject
```

---

## Authentication & Authorization

| Role | Permission |
|------|-----------|
| Notary | ✅ Có thể từ chối yêu cầu được assign cho họ |
| Admin | ✅ Có thể từ chối bất kỳ yêu cầu nào |
| Client | ❌ Không được phép |

---

## Request

### URL
```
POST /api/requests/{id}/reject
```

### Headers
```
Authorization: Bearer {notary_or_admin_token}
Content-Type: application/json
```

### Body
```json
{
    "reason": "Hồ sơ không đầy đủ - thiếu chứng thực của chứng thực viên"
}
```

### Validation Rules
- `reason`: Bắt buộc, độ dài 1-1000 ký tự

---

## Response

### Status Code: 200 OK

```json
{
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "clientId": "client-uuid",
    "notaryId": "notary-uuid",
    "serviceType": "ONLINE",
    "contractType": "CONTRACT",
    "description": "Công chứng hợp đồng mua bán nhà đất",
    "status": "REJECTED",
    "rejectionReason": "Hồ sơ không đầy đủ - thiếu chứng thực của chứng thực viên",
    "documents": [...],
    "createdAt": "2026-03-11T08:00:00Z",
    "updatedAt": "2026-03-11T10:30:00Z"
}
```

---

## Error Cases

### 1. Request Không Tồn Tại
```
Status: 404 Not Found
Response: "Không tìm thấy yêu cầu"
```

### 2. Không Có Quyền Từ Chối
```
Status: 403 Forbidden
Response: "Không có quyền từ chối yêu cầu này"

Nguyên nhân: 
- Bạn là Notary nhưng yêu cầu không được assign cho bạn
- Bạn không phải Notary hay Admin
```

### 3. Không Được Phép Từ Chối (Trạng Thái Không Hợp Lệ)
```
Status: 400 Bad Request
Response: "Không thể từ chối yêu cầu ở trạng thái hiện tại"

Các trạng thái không thể từ chối:
- COMPLETED (đã hoàn thành)
- CANCELLED (đã hủy)
- REJECTED (đã từ chối rồi)
```

### 4. Không Được Xác Thực
```
Status: 401 Unauthorized
Response: (body trống)

Nguyên nhân: Không có JWT token hay token hết hạn
```

---

## Trạng Thái & State Transitions

### Trạng Thái Hiện Tại Khi Có Thể Từ Chối

```
NEW (Client vừa submit)
  └─→ REJECTED ✅

ASSIGNED (Notary được assign)
  └─→ REJECTED ✅

PROCESSING (Notary reviewing documents)
  └─→ REJECTED ✅
```

### Trạng Thái KHÔNG Thể Từ Chối

```
SCHEDULED (Đã lên lịch hẹn)
  └─→ REJECTED ❌ (BẠN KHÔNG THỂ)

COMPLETED (Đã hoàn thành)
  └─→ REJECTED ❌

CANCELLED (Đã hủy)
  └─→ REJECTED ❌

REJECTED (Đã từ chối rồi)
  └─→ REJECTED ❌
```

---

## Use Cases

### Use Case 1: Notary Review & Reject (Not Enough Documents)

```
1. Client tạo yêu cầu công chứng
   POST /api/requests
   
2. Notary được assign
   Admin gọi: POST /api/requests/{id}/assign
   status → ASSIGNED

3. Notary review documents (GET /api/requests/{id})
   Phát hiện: Thiếu ID card của one party

4. Notary từ chối
   POST /api/requests/{id}/reject
   {
     "reason": "Thiếu ID card bản gốc của one party. Vui lòng upload lại."
   }
   
5. Status → REJECTED
   rejectionReason được set
   
6. Client nhận notification
   → Có thể cancel hay resubmit với documents mới
```

### Use Case 2: Admin Reject Due to Suspicious Document

```
1. Client submit notary request
2. Notary review but submit to Admin
3. Admin detect fraud/forgery
4. Admin reject immediately
   POST /api/requests/{id}/reject
   {
     "reason": "Phát hiện giấy tờ giả mạo. Yêu cầu này bị từ chối vô thời hạn."
   }
```

---

## Document Handling After Rejection

### Current Behavior
- Tất cả documents của request vẫn được lưu lại
- Có thể view được lý do từ chối
- Client có thể gửi request mới

### Future Enhancement
- Cho phép client resubmit request với documents mới
- Ghi log audit khi reject
- Notify client via email

---

## Implementation Details

### Code Location
```java
// src/main/java/com/actvn/enotary/controller/NotaryRequestController.java

@PostMapping("/{id}/reject")
public ResponseEntity<NotaryRequestResponse> rejectRequest(
        Authentication authentication,
        @PathVariable("id") UUID id,
        @Valid @RequestBody RejectNotaryRequestRequest request) {
    // ...implementation...
}
```

### Business Logic (NotaryRequestService)
```java
@Transactional
public NotaryRequest rejectRequest(UUID requestId, String reviewerEmail, String reason) {
    NotaryRequest request = getById(requestId);

    User reviewer = userRepository.findByEmail(reviewerEmail)
            .orElseThrow(() -> new AppException("Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

    // Authorization check
    boolean isAdmin = reviewer.getRole().name().equals("ADMIN");
    boolean isAssignedNotary = reviewer.getRole().name().equals("NOTARY")
            && request.getNotary() != null
            && request.getNotary().getUserId().equals(reviewer.getUserId());

    if (!isAdmin && !isAssignedNotary) {
        throw new AppException("Không có quyền từ chối yêu cầu này", HttpStatus.FORBIDDEN);
    }

    // Status check
    if (request.getStatus() == RequestStatus.COMPLETED
            || request.getStatus() == RequestStatus.CANCELLED
            || request.getStatus() == RequestStatus.REJECTED) {
        throw new AppException("Không thể từ chối yêu cầu ở trạng thái hiện tại", HttpStatus.BAD_REQUEST);
    }

    // Update
    request.setStatus(RequestStatus.REJECTED);
    request.setRejectionReason(reason.trim());
    request.setUpdatedAt(OffsetDateTime.now());
    
    return notaryRequestRepository.save(request);
}
```

---

## Testing

### Test 1: Notary Reject Own Request
```bash
# 1. Get Notary Token
NOTARY_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "notary@example.com",
    "password": "password123"
  }' | jq -r '.token')

# 2. Reject request
curl -X POST http://localhost:8080/api/requests/{requestId}/reject \
  -H "Authorization: Bearer $NOTARY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Hồ sơ không đầy đủ"
  }'
```

### Test 2: Admin Reject Any Request
```bash
# 1. Get Admin Token
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@localhost",
    "password": "DefaultAdmin@123"
  }' | jq -r '.token')

# 2. Reject request
curl -X POST http://localhost:8080/api/requests/{requestId}/reject \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Phát hiện giấy tờ không hợp pháp"
  }'
```

### Test 3: Error - Try to Reject Already Rejected Request
```bash
# Should return 400 Bad Request
curl -X POST http://localhost:8080/api/requests/{rejectedRequestId}/reject \
  -H "Authorization: Bearer $NOTARY_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Try again"
  }'
  
# Response: "Không thể từ chối yêu cầu ở trạng thái hiện tại"
```

---

## Related APIs

### Get Request Details (with rejection reason)
```
GET /api/requests/{id}
Authorization: Bearer {token}

Response:
{
    ...
    "status": "REJECTED",
    "rejectionReason": "Hồ sơ không đầy đủ - thiếu chứng thực của chứng thực viên",
    ...
}
```

### View Request Documents (to review before reject)
```
GET /api/requests/{id}
Authorization: Bearer {token}

Response:
{
    ...
    "documents": [
        {
            "documentId": "...",
            "filePath": "...",
            "docType": "ID_CARD",
            ...
        },
        ...
    ]
}
```

### List Requests to Review
```
GET /api/requests/filter?status=PROCESSING&page=0&size=20
Authorization: Bearer {notary_token}

Response: Danh sách các request ở status PROCESSING
```

---

## Best Practices

### ✅ Do's
1. ✅ Review tất cả documents trước khi reject
2. ✅ Viết lý do từ chối rõ ràng, chi tiết
3. ✅ Reject ngay khi phát hiện vấn đề (không để dự)
4. ✅ Log lý do từ chối để audit trail
5. ✅ Notify client về rejection (future feature)

### ❌ Don'ts
1. ❌ Không reject vì lý do tùy tiện
2. ❌ Không viết lý do chỉ "Không hợp lệ"
3. ❌ Không reject request của notary khác (nếu bạn không phải admin)
4. ❌ Không reject request ở trạng thái SCHEDULED (đã lên lịch hẹn)

---

## Future Enhancements

| Feature | Status | Details |
|---------|--------|---------|
| Email Notification | ⏳ Planned | Tự động gửi email cho client khi reject |
| Audit Log | ⏳ Planned | Ghi log ai reject khi nào lý do gì |
| Resubmit | ⏳ Planned | Cho phép client resubmit sau reject |
| Appeal | ⏳ Planned | Client có thể appeal quyết định reject |
| Auto-Expiry | ⏳ Planned | Rejected request tự động expire sau X ngày |
| Statistics | ⏳ Planned | Thống kê tỷ lệ reject theo notary |

---

## Workflow Diagram

```
Client Submit Request (NEW)
    ↓
Assign to Notary (ASSIGNED)
    ↓
Notary Review Documents (PROCESSING)
    ├─→ All OK: Schedule Appointment (SCHEDULED)
    │    └─→ Complete Notary Session (COMPLETED)
    │
    └─→ Not OK: REJECT
         {
           "reason": "..."
         }
         ↓
         Status = REJECTED
         rejectionReason set
         ↓
         Client Notification
         ↓
         Client Options:
         - View rejection reason
         - (Future) Resubmit with new docs
         - (Future) Appeal decision
```

---

## Entity Fields

### NotaryRequest
```java
@Column(name = "rejection_reason", columnDefinition = "TEXT")
private String rejectionReason;  // Set when status = REJECTED
```

### RequestStatus Enum
```java
public enum RequestStatus {
    NEW,
    ASSIGNED,
    PROCESSING,
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    REJECTED  // Terminal state
}
```

---

## Database

### Table: notary_requests
```sql
ALTER TABLE notary_requests ADD COLUMN rejection_reason TEXT;

-- When reject:
UPDATE notary_requests 
SET status = 'REJECTED', 
    rejection_reason = 'Hồ sơ không đầy đủ',
    updated_at = CURRENT_TIMESTAMP
WHERE request_id = '550e8400-e29b-41d4-a716-446655440000';
```

---

## Summary

✅ **Reject API** cho phép Notary/Admin từ chối yêu cầu công chứng  
✅ **Authorization** kiểm soát ai được phép reject  
✅ **Validation** đảm bảo chỉ reject ở trạng thái hợp lệ  
✅ **Audit Trail** lưu lại lý do từ chối  

---

**Last Updated**: March 11, 2026  
**Status**: ✅ Ready for Use

