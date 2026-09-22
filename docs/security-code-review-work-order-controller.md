# Security Code Review: WorkOrderController

## Pham vi

File duoc review: [WorkOrderController.java](../src/main/java/controller/WorkOrderController.java)

Tai lieu doi chieu:

- [api-rules.md](api-rules.md)
- [security-rules.md](security-rules.md)
- [coding-rules.md](coding-rules.md)
- [work-order-decomposition.md](work-order-decomposition.md)

## Ket luan tong quat

Code hien tai chua du dieu kien dua vao moi truong tich hop hoac production. Hai loi can xu ly ngay la SQL Injection va hardcode JWT secret. Ngoai ra code con thieu authentication, authorization, validation, transaction, idempotency va response contract theo dac ta.

## 1. Spec Delta

### SD-01 - Critical: Sai contract nghiep vu Work Order

Endpoint hien tai la `/api/work-orders/create-wo`, trong khi dac ta WO-201 yeu cau:

```text
POST /v1/work-orders/{parent_id}/decompositions
```

Code dang tao mot Work Order doc lap, chua ho tro phan ra parent thanh 2-20 child Work Order.

**Khuyen nghi:** Xac nhan lai scope voi Product Owner. Neu trien khai WO-201, can co `parent_id`, danh sach `children[]`, quan he parent-child va transaction atomic.

### SD-02 - High: Sai schema request

Code dang su dung `equipmentId`, `description` va `priority`. Dac ta yeu cau `title`, `description`, `estimate_minutes`, `priority`, `due_date` trong moi child.

**Khuyen nghi:** Tao DTO rieng nhu `CreateWorkOrderRequest` hoac `DecomposeWorkOrderRequest`, khong nhan truc tiep object khong ro contract.

### SD-03 - High: Sai HTTP response contract

Code tra `200 OK`, trong khi thao tac tao moi phai tra `201 Created`. Response cung khong co `decomposition_id`, `parent_id`, danh sach child va `Location` header.

**Khuyen nghi:** Dung response DTO ro rang va tra `201 Created` cho thao tac tao moi.

### SD-04 - High: Thieu cac yeu cau nghiep vu bat buoc

Code chua thuc hien:

- Kiem tra parent ton tai va dang `active`.
- Kiem tra parent khong phai la child.
- Gioi han so child tu 2 den 20.
- Idempotency bang `Idempotency-Key`.
- Transaction atomic.
- Kiem tra tenant va ownership.
- Tra loi `403`, `404`, `409`, `422` theo contract.

## 2. Bao mat

### SEC-01 - Critical: Hardcode JWT secret

`JWT_SECRET` dang duoc luu truc tiep trong source code. Day la vi pham security rule va co the lam lo bi mat qua Git, log, artifact hoac IDE.

**Khuyen nghi:** Xoa constant nay. Quan ly secret bang secret manager, environment variable hoac Spring externalized configuration. Khong commit secret that vao repository.

### SEC-02 - Critical: SQL Injection

Query duoc ghep chuoi truc tiep tu `equipmentId`, `description` va `priority`. Attacker co the chen SQL thong qua payload client.

**Khuyen nghi:** Uu tien Spring Data JPA. Neu phai dung JDBC, dung `JdbcTemplate` voi parameter binding:

```java
jdbcTemplate.update(
        "INSERT INTO work_orders (equipment_id, description, priority, status) "
                + "VALUES (?, ?, ?, ?)",
        request.equipmentId(),
        request.description(),
        request.priority(),
        "NEW");
```

Khong dung `Statement` voi du lieu den tu client.

### SEC-03 - Critical: Khong co Authentication va Authorization

Endpoint khong co co che xac thuc hoac RBAC. Bat ky client nao cung co the goi endpoint neu khong co lop bao ve ben ngoai.

**Khuyen nghi:** Dung `@PreAuthorize` voi role phu hop va kiem tra them tenant, ownership va policy trong service layer.

```java
@PreAuthorize("hasRole('TECHNICIAN')")
@PostMapping
public ResponseEntity<WorkOrderResponse> create(...) {
    ...
}
```

### SEC-04 - Critical: Lam lo SQL va exception noi bo

Code tra `e.getMessage()` va toan bo query ra client. Dieu nay co the lo ten bang, ten cot, du lieu client, schema database va thong tin driver.

**Khuyen nghi:** Client chi nhan Problem Details tong quat. Chi tiet noi bo chi duoc log co kiem soat va khong log request body nhay cam.

### SEC-05 - High: Log du lieu nghiep vu bang `System.out.println`

Log hien tai ghi `equipmentId`. Work Order title va description duoc xep la confidential business data va co the chua PII.

**Khuyen nghi:** Dung SLF4J va chi log ID duoc phep, request ID, status code, count va duration. Khong log description hoac raw request body.

### SEC-06 - High: Dung DataSource truc tiep trong Controller

Controller tu mo `Connection`, tao `Statement` va thuc thi SQL. Cach nay lam kho ap dung transaction, authorization, audit va retry mot cach nhat quan.

**Khuyen nghi:** Tach theo luong `Controller -> Application Service -> Repository`; transaction dat o service layer.

## 3. Kiem thu va rang buoc du lieu

### VAL-01 - High: Thieu `@Valid` va validation constraint

Request body khong co `@Valid`, do do chua co bang chung rang buoc:

- `equipmentId` khong rong.
- `description` co gioi han do dai.
- `priority` thuoc enum hop le.
- Truong bat buoc khong null.
- Payload dung schema.

**Khuyen nghi:** Dung `@Valid @RequestBody` va DTO co `@NotBlank`, `@Size`, `@NotNull`, `@Pattern` hoac enum type.

### VAL-02 - High: Thieu test cho cac nhanh bao mat

Can co test cho:

- Request khong authentication tra `401`.
- User khong co role tra `403`.
- Payload SQL injection khong lam thay doi query.
- Payload khong hop le tra `422`.
- Database exception khong lo SQL.
- Retry cung idempotency key khong tao duplicate.
- Transaction rollback khi mot insert that bai.
- Cross-tenant access bi tu choi.

### VAL-03 - Medium: Bat exception qua rong

`catch (Exception e)` che mat nguyen nhan va lam moi loi bi xu ly giong nhau.

**Khuyen nghi:** Xu ly exception cu the trong `@RestControllerAdvice`, vi du `DataAccessException`, `ConstraintViolationException`, `MethodArgumentNotValidException`, `AccessDeniedException` va cac domain exception.

## 4. Do phuc tap

### CMP-01 - High: Controller chiu qua nhieu trach nhiem

Class hien dong thoi xu ly routing, doc request, xay dung SQL, quan ly connection, ghi log, xu ly loi, tao response va persistence.

**Khuyen nghi:** Tach thanh `WorkOrderController`, `WorkOrderApplicationService`, `WorkOrderRepository`, DTO va `GlobalExceptionHandler`.

### CMP-02 - Medium: Khong co transaction boundary ro rang

Dac ta yeu cau tao du lieu atomic. Code chua the hien transaction nghiep vu, idempotency hoac rollback cho quy trinh nhieu buoc.

**Khuyen nghi:** Dat `@Transactional` tai application service, sau khi authorization va business validation duoc thuc hien.

## 5. Phong cach lap trinh va best practices

### STYLE-01 - Medium: Field injection

`@Autowired` tren field lam unit test kho hon va vi pham coding rule ve constructor injection.

**Khuyen nghi:** Dung constructor injection voi dependency bat bien.

### STYLE-02 - Low: Wildcard import

`import org.springframework.web.bind.annotation.*;` lam dependency cua class khong ro rang va co the gay xung dot ten.

**Khuyen nghi:** Import ro tung annotation duoc su dung.

### STYLE-03 - Medium: Dung `ResponseEntity<?>`

Return type qua tong quat lam mat API contract o compile-time.

**Khuyen nghi:** Dung `ResponseEntity<WorkOrderResponse>` cho success response va Problem Details rieng cho error response.

### STYLE-04 - High: Co kha nang project khong compile

Trong workspace hien thay `WorkOrderController.java`, `ScratchHandler.java` va `Application.java`, nhung chua thay cac class:

```text
WorkOrderRequest
ApiResponse
ErrorResponse
```

Neu cac class nay khong nam o module khac, project se fail tai buoc compile.

## Cau truc ma nguon de xuat

```text
src/main/java/controller/
    WorkOrderController.java

src/main/java/application/workorder/
    WorkOrderService.java
    CreateWorkOrderCommand.java
    WorkOrderAuthorizationService.java

src/main/java/domain/workorder/
    WorkOrder.java
    WorkOrderPriority.java
    WorkOrderStatus.java
    WorkOrderRepository.java

src/main/java/infrastructure/persistence/
    JpaWorkOrderRepository.java
    WorkOrderEntity.java

src/main/java/api/
    CreateWorkOrderRequest.java
    WorkOrderResponse.java
    ProblemDetailsResponse.java

src/main/java/security/
    SecurityConfig.java
    WorkOrderSecurityPolicy.java

src/main/java/exception/
    GlobalExceptionHandler.java
    WorkOrderNotFoundException.java
    WorkOrderForbiddenException.java
    WorkOrderConflictException.java

src/test/java/
    controller/WorkOrderControllerTest.java
    application/WorkOrderServiceTest.java
    security/WorkOrderAuthorizationTest.java
```

## Luong xu ly an toan de xuat

```text
HTTP Request
    -> WorkOrderController
       - @Valid
       - @PreAuthorize
       - DTO mapping
    -> WorkOrderService @Transactional
       - authorization
       - tenant boundary
       - business validation
       - idempotency
    -> WorkOrderRepository
       - JPA hoac parameterized query
       - khong noi chuoi SQL
    -> GlobalExceptionHandler
       - Problem Details
       - khong lo SQL, stack trace hoac secret
```

## Ket luan

Code chua du dieu kien dua vao moi truong tich hop hoac production. Thu tu uu tien xu ly de xuat:

1. Xoa hardcode secret.
2. Loai bo SQL Injection bang JPA hoac parameterized query.
3. Bo sung authentication va authorization.
4. Sua API contract theo dac ta.
5. Them validation, transaction, idempotency va security tests.
6. Tach persistence logic khoi Controller.