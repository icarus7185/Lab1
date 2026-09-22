# ScratchHandler Rule Review

## Pham vi danh gia

- File duoc danh gia: [ScratchHandler.java](../src/main/java/controller/ScratchHandler.java)
- Tai lieu doi chieu: [api-rules.md](api-rules.md), [coding-rules.md](coding-rules.md), [security-rules.md](security-rules.md)
- Trang thai: danh gia theo noi dung hien co trong file Java va cau hinh Maven cua project.

## Ket qua tong quat

| Nhom rule | Dat | Khong dat | Khong ap dung | Ket luan |
|---|---:|---:|---:|---|
| API Design Rules | 4 | 1 | 0 | Can sua phan hoi loi RFC 7807 |
| Java Coding & Logging Rules | 7 | 0 | 0 | Dat trong pham vi file |
| Security Rules | 2 | 2 | 0 | Can bo sung sanitize va RBAC |
| **Tong cong** | **13** | **3** | **0** | **Chua dat toan bo rule** |

## API Design Rules

| # | Tieu chi | Trang thai | Nhan xet |
|---:|---|---|---|
| 1 | Dung danh tu so nhieu cho REST resource | **Dat** | `@RequestMapping("/api/workorders")` dung danh tu so nhieu `workorders`. |
| 2 | Dung HTTP verb chuan | **Dat** | `@PostMapping` duoc dung cho thao tac tao work order. |
| 3 | Tuan thu schema, khong tu them JSON field | **Dat** | DTO chi khai bao cac field `title`, `description`, `estimate_minutes`, `priority` va `due_date`; viec doi chieu day du voi API spec can duoc xac nhan them. |
| 4 | Loi tra ve Problem Details gom `type`, `title`, `status`, `detail` | **Khong dat** | Code tao `ProblemDetail` voi `status` va `detail`, nhung khong thiet lap ro `type` va `title`. Ngoai ra chi co hai loai exception duoc xu ly tai controller. |
| 5 | Request body co `@Valid` va constraint phu hop | **Dat** | Request dung `@Valid`; cac field co `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max` va `@Pattern`. |

## Java Coding & Logging Rules

| # | Tieu chi | Trang thai | Nhan xet |
|---:|---|---|---|
| 1 | Dung Java 17+ va Spring Boot 3.3+ | **Dat** | Project cau hinh Java 17 va Spring Boot 3.5.6 trong `pom.xml`. |
| 2 | Dung quy uoc dat ten Java | **Dat** | `ScratchHandler`, `CreateWorkOrderRequest` dung PascalCase; method va parameter dung camelCase. |
| 3 | Khong nem `RuntimeException` hoac `Exception` tong quat | **Dat** | File khong tu nem exception tong quat. |
| 4 | Tuan thu logging va khong log du lieu nhay cam | **Dat** | File khong ghi log va khong log PII hay secret. |
| 5 | Dung constructor injection, tranh field injection | **Dat** | Class khong co dependency can inject va khong dung field injection. |
| 6 | Code don gian, tranh over-engineering | **Dat** | Controller co mot endpoint va cac DTO/handler can thiet, khong co abstraction thua. |
| 7 | Khong khai bao bien trong loop | **Dat** | File khong co `for`, `while` hay `do-while`. |

## Security Rules

| # | Tieu chi | Trang thai | Nhan xet |
|---:|---|---|---|
| 1 | Khong hardcode secret | **Dat** | Khong tim thay API key, password, token hay connection string trong file. |
| 2 | Validate va sanitize input tai controller boundary | **Khong dat** | File co validate input bang Bean Validation, nhung khong co buoc sanitize input. Can xac dinh va ap dung quy tac sanitize phu hop cho cac truong text neu yeu cau bao mat bat buoc dieu nay. |
| 3 | Bao ve SQL/injection bang ORM hoac parameterized query | **Dat** | File khong thuc hien truy van SQL/JPQL nao; tieu chi nay khong phat sinh rui ro trong pham vi file. |
| 4 | Co authorization/RBAC tren endpoint | **Khong dat** | `POST /api/workorders` khong co `@PreAuthorize` hoac co che RBAC tuong duong. |

## Viec can lam

1. Tao Problem Details day du voi `type`, `title`, `status` va `detail`, dong thoi xem xet xu ly loi tap trung cho cac exception con lai.
2. Bo sung co che sanitize input theo quy dinh cua ung dung, neu cac truong text duoc phep nhan noi dung co the chua HTML/script.
3. Xac dinh role duoc phep tao work order va them annotation authorization, vi du `@PreAuthorize(...)`, tren endpoint.