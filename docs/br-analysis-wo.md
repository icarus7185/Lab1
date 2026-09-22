# Phan tich nghiep vu: Quan ly Phieu cong viec (Work Order)

## 1. Pham vi va muc do tin cay

Doan mo ta Product Owner duoc cung cap hien tai chi la placeholder:

> `[Dan doan mo ta tho cua Product Owner vao day]`

Vi vay, tai lieu nay khong coi placeholder la yeu cau nghiep vu da duoc phe duyet. Phan phan tich duoi day duoc xay dung tu:

- dac ta hien co trong `docs/work-order-decomposition.md`;
- ma nguon `src/main/java/controller/WorkOrderController.java`;
- cac quy tac API, coding va security trong thu muc `docs`.

Can PO xac nhan cac Open Questions truoc khi dung tai lieu nay lam baseline de phat trien.

## 2. Entities va thuoc tinh co ban

### 2.1. WorkOrder

| Thuoc tinh | Kieu du kien | Bat buoc | Mo ta / rang buoc |
|---|---|---:|---|
| `id` | UUID | Co | Dinh danh duy nhat cua work order. |
| `tenant_id` | UUID | Co | Tenant cua work order, lay tu ngu canh xac thuc, khong nhan tu client. |
| `parent_id` | UUID | Khong | Work order cha; work order goc phai co gia tri rong. |
| `root_id` | UUID | Co | Work order goc cua cay phan cap. |
| `title` | String(1..200) | Co | Tieu de, khong duoc rong sau khi trim. |
| `description` | String(0..2000) | Khong | Mo ta; co the chua du lieu kinh doanh mat. |
| `status` | Enum | Co | `draft`, `active`, `completed`, `cancelled`. |
| `priority` | Enum | Co | `low`, `medium`, `high`, `urgent`. |
| `estimate_minutes` | Integer | Co | Tu 1 den 31.680 phut. |
| `due_date` | Date | Khong | Khong som hon hom nay; khong muon hon due date cua parent neu parent co gia tri. |
| `created_by` | UUID | Co | Nguoi tao, lay tu session/token. |
| `created_at` | Timestamp | Co | Thoi diem tao. |
| `updated_at` | Timestamp | Co | Thoi diem cap nhat gan nhat. |

### 2.2. WorkOrderDecomposition

| Thuoc tinh | Kieu du kien | Bat buoc | Mo ta / rang buoc |
|---|---|---:|---|
| `id` | UUID | Co | Dinh danh lan phan ra. |
| `tenant_id` | UUID | Co | Tenant cua parent va request. |
| `parent_id` | UUID | Co | Work order cha duoc phan ra. |
| `requested_by` | UUID | Co | Nguoi yeu cau, lay tu access token. |
| `idempotency_key` | UUID v4 | Co | Khoa chong tao trung, pham vi theo tenant, user va parent. |
| `created_at` | Timestamp | Co | Thoi diem tao yeu cau phan ra. |

### 2.3. WorkOrderDecompositionItem

| Thuoc tinh | Kieu du kien | Bat buoc | Mo ta / rang buoc |
|---|---|---:|---|
| `decomposition_id` | UUID | Co | Tham chieu den lan phan ra. |
| `child_id` | UUID | Co | Work order con duoc tao. |
| `position` | Smallint | Co | Thu tu tu 0 den 19 trong request. |

### 2.4. Request va Response DTO

| DTO | Thuoc tinh chinh | Ghi chu |
|---|---|---|
| `DecompositionRequest` | `children[]` | Tu 2 den 20 child work orders. |
| `ChildWorkOrderRequest` | `title`, `description`, `estimate_minutes`, `priority`, `due_date` | Co validation tai API boundary. |
| `DecompositionResponse` | `decomposition_id`, `parent_id`, `children[]`, `created_at` | Tra ve toan bo child da tao. |
| `ProblemDetails` | `type`, `title`, `status`, `detail`, `errors[]`, `request_id` | Dung cho cac loi API. |

## 3. Open Questions va rui ro nghiep vu

| # | Cau hoi / rui ro | Tac dong | Chu so huu xac nhan | Trang thai |
|---:|---|---|---|---|
| 1 | PO chua cung cap mo ta nghiep vu thuc te. Tinh nang can tao WO don le, phan ra WO, hay ca hai? | Co the xay dung sai scope va API. | PO / BA | Mo |
| 2 | Ai duoc phep tao va phan ra Work Order? Role nao duoc ap dung? | Anh huong RBAC va danh sach endpoint duoc phep goi. | PO / Security | Mo |
| 3 | Quy tac authorization phu thuoc tenant, project, ownership hay role? | Rui ro lo du lieu cross-tenant va truy cap trai phep. | PO / Security | Mo |
| 4 | Parent co phai luon la work order goc va phai o trang thai `active` khong? | Anh huong validation va transaction. | PO | Mo |
| 5 | So luong child toi thieu/toi da co phai 2/20 khong? | Anh huong UI validation va database transaction. | PO | Tam dung theo dac ta hien co |
| 6 | Due date cua child co bat buoc khong muon hon parent va khong som hon ngay hien tai khong? | Anh huong business validation. | PO | Tam dung theo dac ta hien co |
| 7 | Khi retry voi cung idempotency key nhung request body khac, phai tra `409` dung khong? | Anh huong idempotency va xu ly retry cua UI. | PO / API Owner | Tam dung theo dac ta hien co |
| 8 | Work order title/description co can cho phep HTML, Markdown hay chi plain text? | Anh huong sanitize, XSS va cach hien thi. | PO / Security | Mo |
| 9 | Co can audit log cho tao WO, phan ra WO va retry khong? Log duoc phep chua ID nao? | Anh huong truy vet va PII/confidential data. | PO / Security | Mo |
| 10 | Co can assignment, scheduling, attachment, notification trong scope hien tai khong? | Anh huong model va pham vi UI. | PO | Tam dung ngoai scope theo dac ta hien co |
| 11 | `WorkOrderController.java` dang dung route `/api/work-orders/create-wo`, trong khi dac ta dung `/v1/work-orders/{parent_id}/decompositions`. Endpoint nao la contract chinh thuc? | Rui ro breaking API va test khong dong bo. | API Owner / PO | Can xu ly |
| 12 | Database nao la target va service nao quan ly migration/schema? | Anh huong JPA/JDBC, transaction va deployment. | Architect / DBA | Mo |
| 13 | Loi validation dung `400` hay `422`? | Anh huong client mapping va API compatibility. | API Owner | Dac ta hien co quy dinh `422` cho validation |

## 4. Phan ra 3 tang UI / Data / API

### 4.1. Ma tran phan ra

| Capability / User Story | UI layer | Data layer | API layer | Acceptance signal |
|---|---|---|---|---|
| Mo man hinh phan ra WO | Mo tu man hinh chi tiet parent; hien parent ID o route va khong cho sua. | Doc parent theo tenant va authorization context. | `GET` detail hoac endpoint hien co cung cap du lieu parent; tra `404` neu khong visible. | User chi thay parent ma ho co quyen thao tac. |
| Nhap danh sach child | Form repeatable row gom title, description, estimate, priority, due date. | Chua persist khi user dang nhap. | Request gom `children[]`; client khong gui `tenant_id`, `requested_by`. | Co the them/xoa row; submit chi mo khi co 2-20 row hop le. |
| Validate du lieu child | Hien loi tai tung row va focus field loi dau tien. | DB constraint cho do dai, enum, estimate; service validate quy tac cross-row. | Tra `422` voi `errors[].path` nhu `children[1].title`. | Moi loi map dung ve row/field tuong ung. |
| Gui request an toan | Hien loading, khoa submit va giu gia tri da nhap. | Tao idempotency record va child trong cung transaction. | `POST /v1/work-orders/{parent_id}/decompositions`; gui `Idempotency-Key` UUID v4. | Retry khong tao duplicate. |
| Xac thuc va phan quyen | Khong tu quyet dinh authorization chi bang client validation. | Kiem tra tenant, user, ownership/policy va parent status. | Yeu cau authentication; tra `401`, `403` hoac `404` theo policy. | User khong quyen khong doc/tao duoc du lieu. |
| Kiem tra parent | Hien thong bao neu parent khong ton tai, da hoan tat, huy hoac da la child. | Lock parent `SELECT ... FOR UPDATE`; kiem tra `active` va `parent_id IS NULL`. | Tra `404` cho parent khong visible; `409` cho parent khong actionable. | Khong co child nao duoc tao khi parent khong hop le. |
| Tao quan he parent-child | Chuyen ve detail parent va hien so child da tao. | Luu `parent_id`, `root_id`, tenant, order position va foreign key. | Tra `201` gom `decomposition_id`, parent va toan bo child; co `Location`. | Tat ca child cung parent/root/tenant dung. |
| Xu ly loi he thong | Giữ du lieu form; cho retry theo cung idempotency key; khong hien raw exception. | Rollback toan bo neu mot insert that bai. | Tra Problem Details; khong tra SQL, stack trace hay request body nhay cam. | Retry an toan va khong lo thong tin he thong. |
| Xem ket qua / audit | Hien child, status, priority, estimate va due date; khong hien du lieu ngoai quyen. | Query theo tenant va authorization filter. | `GET /v1/work-orders/{parent_id}/decompositions/{decomposition_id}`. | Ket qua truy xuat lai giong ket qua tao. |

### 4.2. Phan viec de xuat theo tang

| Tang | Pham vi implementation | Dieu kien bao ve bat buoc |
|---|---|---|
| UI | Decomposition form, client validation, loading/success/error states, field error mapping, retry. | Khong coi client validation la authorization; khong luu secret; khong hien raw error. |
| Data | `work_orders`, `work_order_decompositions`, `work_order_decomposition_items`, indexes, constraints, transaction va idempotency. | Tenant boundary, row lock parent, rollback atomic, khong SQL string concatenation. |
| API | Route, DTO, Bean Validation, authentication, authorization, Problem Details, status codes, idempotency header. | Dung JPA/PreparedStatement; khong nhan tenant/user tu client; khong hardcode JWT secret. |

## 5. Rui ro phat hien trong WorkOrderController.java

| Van de | Muc do | Anh huong |
|---|---|---|
| JWT secret hardcode trong source | Critical | Co the lam lo bi mat va khong the quan ly secret dung cach. |
| Noi chuoi truc tiep vao SQL va dung `Statement` | Critical | Co nguy co SQL Injection tu `equipmentId`, `description` va `priority`. |
| Khong co authentication/authorization | Critical | Bat ky client nao cung co the goi endpoint theo code hien tai. |
| Dung field injection voi `@Autowired` | Medium | Kho test, khong phu hop coding rule ve constructor injection. |
| Bat `Exception` tong quat | High | Che mat nguyen nhan va xu ly loi khong phan biet. |
| Tra `query` va `e.getMessage()` ra response | Critical | Co the lo schema, SQL va thong tin noi bo. |
| Dung `System.out.println` | Medium | Khong theo logging standard va co nguy co ghi PII. |
| Chua dung `@Valid` cho request body | High | Input boundary chua co validation theo API rule. |

## 6. Ket luan BA

Chua du co so de phe duyet yeu cau vi PO chua cung cap mo ta thuc te. Dac ta hien co du de lam baseline cho use case **phan ra mot Work Order goc thanh 2-20 child Work Order**, voi transaction atomic, idempotency va authorization.

Truoc khi chuyen sang solution design hoac development, can PO/API Owner chot toi thieu: scope tinh nang, role va policy, route contract, status code validation, quy tac due date, pham vi plain text/HTML, va audit requirement. `WorkOrderController.java` can duoc xu ly nhu mot implementation co rui ro bao mat cao truoc khi dua vao moi truong chay that.Dự án POSCO MCI: Cần làm gấp tính năng Quản lý Phiếu công việc (Work Order) trên ứng dụng di động cho thợ kỹ thuật ở công trường. Thợ vào app nhập mã thiết bị (equipment_id), chọn mức độ ưu tiên (low, medium, high, urgent) và viết mô tả công việc (description). Hệ thống lưu lại và hiển thị danh sách cho quản đốc xem. Yêu cầu làm nhanh trong tuần này, không cần phân quyền phức tạp vì ai đăng nhập app công trường cũng là thợ, miễn là nhập đúng mã thiết bị đang hoạt động.