# RBAC trong e-Notary: Cach he thong phan quyen

Tai lieu nay dung de giai thich nhanh voi nha tuyen dung ve co che phan quyen hien tai trong du an.

## 1) Tong quan mo hinh phan quyen

He thong dang ap dung **RBAC (Role-Based Access Control)** voi 3 role co dinh:
- `CLIENT`
- `NOTARY`
- `ADMIN`

Nguon code:
- `src/main/java/com/actvn/enotary/enums/Role.java`

Luong co ban:
1. User dang nhap thanh cong.
2. Backend phat JWT co claim `role` va `userId`.
3. Moi request di qua `JwtFilter`, nap user vao SecurityContext.
4. Spring Security + business service kiem tra quyen truy cap theo role va theo ngu canh tai nguyen.

Nguon code:
- `src/main/java/com/actvn/enotary/service/AuthService.java`
- `src/main/java/com/actvn/enotary/security/CustomUserDetails.java`
- `src/main/java/com/actvn/enotary/config/SecurityConfig.java`

## 2) Phan quyen o tang backend (API)

### 2.1. Gate o SecurityFilterChain
Backend chan truy cap theo endpoint:
- `/api/admin/**` -> chi `ADMIN`
- `POST /api/requests` -> `CLIENT`
- `/api/profile/**` -> `CLIENT`
- Cac endpoint con lai can dang nhap (`authenticated`), sau do tiep tuc kiem tra business rule trong service/controller.

Nguon code:
- `src/main/java/com/actvn/enotary/config/SecurityConfig.java`

### 2.2. Kiem tra bo sung theo ngu canh nghiep vu
Ngoai role, he thong co cac check theo du lieu thuc te (contextual authorization), vi du:
- Notary chi duoc tiep nhan ho so dang cho tiep nhan va du tai lieu.
- Nguoi dung chi duoc upload/replace tai lieu cua chinh ho so cua minh, hoac notary da duoc gan, hoac admin.
- Notary khong duoc thao tac ho so da bi notary khac nhan.

Nguon code:
- `src/main/java/com/actvn/enotary/service/NotaryRequestService.java`
- `src/main/java/com/actvn/enotary/controller/AdminController.java`
- `src/main/java/com/actvn/enotary/controller/VideoSessionController.java`

## 3) Phan quyen o frontend (UI)

Frontend dung route guard de an/hien trang theo role:
- `ProtectedRoute` kiem tra `isAuthenticated` va `allowedRoles`.
- Dieu huong dashboard theo role (`ADMIN`, `NOTARY`, `CLIENT`).

Nguon code:
- `frontend/src/components/ProtectedRoute.tsx`
- `frontend/src/lib/roleRedirect.ts`
- `frontend/src/App.tsx`

Luu y: frontend guard de cai thien UX, **khong thay the bao mat backend**.

## 4) Tra loi cau hoi: "He thong co phan quyen dong khong?"

### Tra loi ngan gon de phong van
**Hien tai he thong chua co phan quyen dong theo policy engine.**

Cu the:
- Co: RBAC co dinh theo role (`CLIENT`, `NOTARY`, `ADMIN`) + cac check theo ngu canh nghiep vu trong service.
- Chua co: mo hinh dynamic permission day du (VD: bang `permissions`, `roles_permissions`, runtime policy update, ABAC engine, OPA/Casbin, field-level policy config tu admin).

=> Co the mo ta dung nhat la: **"Static RBAC + contextual authorization checks"**, chua phai dynamic authorization hoan chinh.

## 5) Neu can nang cap len phan quyen dong

Lo trinh de nang cap:
1. Tach `Role` va `Permission` thanh model rieng trong DB.
2. Tao mapping `roles_permissions` (nhieu-nhieu).
3. Dua check quyen vao annotation/symbol thong nhat (VD `@RequiresPermission("user.delete")`).
4. Ho tro cap quyen runtime tu trang admin (khong can deploy lai).
5. Bo sung audit log cho thay doi permission + test bao mat hoi quy.

---

## Cau mo ta 1 cau de tra loi nha tuyen dung

"He thong cua em dang dung RBAC voi 3 role co dinh (CLIENT/NOTARY/ADMIN), backend enforce bang Spring Security va bo sung check theo ngu canh nghiep vu o service. Hien tai chua co policy engine de cap nhat permission runtime, nen em dinh nghia no la static RBAC co kem contextual checks, va da co lo trinh de nang cap len dynamic authorization."
