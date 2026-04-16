# Integrated Payment Flow with Transaction Tracking and Callback Handling

Tai lieu nay mo ta ly thuyet + huong dan thiet ke de tich hop thanh toan cho he thong e-Notary theo huong an toan, de mo rong va de doi soat.

## 1) Muc tieu

- Dam bao dong bo trang thai giua he thong e-Notary va cong thanh toan.
- Theo doi duoc toan bo vong doi giao dich (create -> pending -> success/failed -> reconcile).
- Xu ly callback/IPN an toan, idempotent (khong cap nhat trung khi callback gui lai).
- Co kha nang audit va truy vet su co.

## 2) Hien trang trong codebase

- Da co entity `Payment`:
  - `paymentId`
  - `request` (link den `NotaryRequest`)
  - `amount`
  - `paymentStatus` (`PENDING`, `SUCCESS`, `FAILED`)
  - `transactionReference`
  - `createdAt`
- Da co enum `PaymentStatus`.
- DB da co bang `payments` trong migration ban dau.

=> Nghia la da co nen du lieu, nhung can bo sung luong nghiep vu, API, callback verify, idempotency va doi soat.

## 3) Kien truc tong quan

Thanh phan chinh:

1. **Client/Frontend**: tao thanh toan, mo trang payment provider, xem ket qua.
2. **Payment Service (Backend)**:
   - Tao payment intent/transaction.
   - Luu ban ghi `payments` o trang thai `PENDING`.
   - Ky request (neu provider can).
3. **Payment Provider**: xu ly thanh toan, redirect user, gui callback/IPN.
4. **Callback Endpoint (Backend)**:
   - Verify chu ky.
   - Verify timestamp/nonce.
   - Idempotent update trang thai.
5. **Reconciliation Job**:
   - Doi soat cac giao dich `PENDING`/bat thuong.

## 4) State machine de tranh sai trang thai

### 4.1 Payment status

- `PENDING`: vua tao giao dich, chua co ket qua cuoi.
- `SUCCESS`: da thanh toan thanh cong va da verify callback.
- `FAILED`: thanh toan that bai/het han/huy.

### 4.2 Request status lien quan

Khuyen nghi mapping nghiep vu:

- `SCHEDULED` -> `AWAITING_PAYMENT` (khi den buoc can thu phi).
- `AWAITING_PAYMENT` -> `COMPLETED` (sau khi payment `SUCCESS` + hoan tat buoc nghiep vu cuoi).
- Neu payment `FAILED`, request thuong van o `AWAITING_PAYMENT` de cho retry (khong nen dong ho so qua som).

## 5) API design de trien khai

## 5.1 Tao giao dich thanh toan

`POST /api/payments`

Input goi y:

```json
{
  "requestId": "uuid",
  "amount": 500000,
  "paymentMethod": "VNPAY"
}
```

Output goi y:

```json
{
  "paymentId": "uuid",
  "transactionReference": "ENOTARY_20260414_0001",
  "status": "PENDING",
  "checkoutUrl": "https://provider/checkout/...",
  "expiredAt": "2026-04-14T10:30:00Z"
}
```

Luu y:

- `transactionReference` phai unique.
- Khong cho tao payment moi neu request dang o trang thai khong hop le.

## 5.2 Lay trang thai payment

`GET /api/payments/{paymentId}`

- Frontend poll endpoint nay de cap nhat UI sau khi user quay lai tu provider.

## 5.3 Callback/IPN tu cong thanh toan

`POST /api/payments/callback` (public endpoint)

- Khong su dung user JWT thong thuong cho callback.
- Bat buoc verify signature + timestamp + replay protection.

Response callback nen nhanh:

- `200 OK` neu xu ly hop le (ke ca duplicate da xu ly truoc do).
- `400/401` neu signature khong hop le.

## 6) Callback verification (ly thuyet quan trong nhat)

Can verify cac truong sau:

1. **Signature/HMAC**: tinh lai chu ky bang secret key server.
2. **Timestamp window**: callback qua cu (vd > 5 phut) co the reject.
3. **Nonce/Request ID**: chong replay attack.
4. **Amount/Currency**: phai dung voi ban ghi `Payment`.
5. **transactionReference**: map dung payment trong DB.

Neu 1 trong cac check fail -> khong cap nhat `SUCCESS`.

## 7) Idempotency va duplicate callback

Cong thanh toan thuong gui callback nhieu lan. Can dam bao:

- Neu payment da `SUCCESS`, callback lap lai van tra `200` va bo qua update.
- Trang thai chi duoc chuyen theo huong hop le:
  - `PENDING -> SUCCESS`
  - `PENDING -> FAILED`
  - Khong cho `SUCCESS -> FAILED`.
- Dung lock/transaction (`SELECT FOR UPDATE` hoac optimistic locking) de tranh race condition.

## 8) Mo rong schema de theo doi giao dich tot hon

Bang `payments` nen bo sung:

- `provider` (VNPAY/MOMO/...)
- `provider_transaction_id`
- `idempotency_key`
- `callback_received_at`
- `raw_callback_payload` (jsonb)
- `updated_at`

Them bang lich su callback (goi y `payment_callbacks`):

- `callback_id`
- `payment_id`
- `provider_event_id`
- `signature_valid` (boolean)
- `payload` (jsonb)
- `received_at`
- `processed_result`

Muc dich: debug nhanh, audit ro rang, de doi soat.

## 9) Error handling matrix (khuyen nghi)

- Invalid signature -> `401`/`400`, log security event.
- Unknown transaction reference -> `404` hoac `200` + mark orphan (tuy provider contract).
- Amount mismatch -> reject + canh bao nghiem trong.
- Database timeout -> return retryable status (neu provider ho tro), ghi log de xu ly lai.

## 10) Reconciliation (doi soat) bat buoc co

Vi callback co the mat/goi tre, can job dinh ky:

- Quet payment `PENDING` qua nguong thoi gian (vd 15 phut, 1 gio).
- Query trang thai tu provider API.
- Cap nhat lai DB neu co sai lech.
- Ghi audit log cho moi lan doi soat co thay doi.

## 11) Security policy de tranh loi 401/UX kho hieu

Tach ro 2 loai endpoint:

1. **User endpoints** (`/api/payments`, `/api/payments/{id}`): yeu cau JWT cua user dang dang nhap.
2. **Provider callback endpoint** (`/api/payments/callback`): khong dung JWT user, thay bang verify signature provider.

Khong tron 2 co che xac thuc vao 1 endpoint.

## 12) Logging va audit

Moi su kien can log theo correlation id:

- payment created
- callback received
- signature verified/failed
- status transitioned
- reconciliation corrected

Neu he thong da co `audit_logs`, nen ghi su kien quan trong de truy vet tranh chap.

## 13) Test strategy

### Unit tests

- Signature verify dung/sai.
- State transition hop le/khong hop le.
- Idempotent callback duplicate.

### Integration tests

- Tao payment -> callback success -> status update.
- Callback amount mismatch -> reject.
- Callback duplicate 3 lan -> ket qua cuoi van dung.

### E2E tests

- User den buoc thanh toan, thanh cong, trang thai request cap nhat dung.
- Luong retry payment khi that bai.

## 14) Rollout theo phase

### Phase 1 (MVP)

- Tao payment, luu `PENDING`.
- Callback verify signature co ban.
- Update `PENDING -> SUCCESS/FAILED`.

### Phase 2 (On dinh)

- Idempotency key + callback history table.
- Reconciliation job.
- Dashboard theo doi payment bat thuong.

### Phase 3 (San sang production lon)

- Alerting (Slack/Email) cho mismatch/failed callback burst.
- Multi-provider failover.
- Bao cao doi soat ngay/thang.

## 15) Pseudo sequence (de team thong nhat)

1. Client chon thanh toan cho `requestId`.
2. Backend tao `Payment(PENDING)` + `transactionReference`.
3. Backend tra `checkoutUrl` cho frontend.
4. User thanh toan tren provider.
5. Provider goi callback/IPN vao backend.
6. Backend verify signature + amount + timestamp.
7. Backend update `Payment` sang `SUCCESS`/`FAILED` (idempotent).
8. Backend cap nhat `NotaryRequest` theo business rule.
9. Reconciliation job doi soat cac case tre/mat callback.

---

## Ket luan

"Integrated payment flow with transaction tracking and callback handling" khong chi la goi API thanh toan, ma la mot bai toan **consistency + security + idempotency + reconciliation**. Neu trien khai theo tai lieu nay, he thong se on dinh hon khi tai lon, de debug, va han che mat tien do callback loi/duplicate.

