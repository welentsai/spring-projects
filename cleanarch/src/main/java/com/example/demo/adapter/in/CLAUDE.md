# Inbound Adapter 規範

## 1. 職責定義

- 接收外部輸入，轉換成 Application layer 可用的 Command / Query
- **禁止包含任何業務邏輯**
- 輸入驗證（格式、必填、範圍）屬於 Adapter/In 層的職責，在 Request DTO 完成，不得下放至 UseCaseInput

## 2. 依賴方向
```
Adapter/In → Port/In (UseCase Interface)
```

只允許依賴 Port/In 的 interface，不得跨層依賴 Service 或 Domain。

---

## 3. 套件結構
```
{projectroot}/
├── exception/                         ← 所有自定義 Exception 統一放這裡（與 adapter 同層）
│   └── BadRequestException.java
└── adapter/
    └── in/
        ├── {Domain}Controller.java
        ├── dto/
        │   ├── {Domain}Request.java
        │   └── {Domain}Response.java
        └── mapper/
            └── {Domain}RequestMapper.java
```

### 範例（order domain）
```
{projectroot}/
├── exception/
│   └── BadRequestException.java
└── adapter/
    └── in/
        ├── OrderController.java
        ├── dto/
        │   ├── CreateOrderRequest.java
        │   └── OrderResponse.java
        └── mapper/
            └── OrderRequestMapper.java
```

---

## 4. 命名規則

| 元件 | 所在 package | 命名格式 | 備註 |
|------|-------------|----------|------|
| Controller | `adapter.in` | `{Domain}Controller` | |
| Request DTO | `adapter.in.dto` | `{Domain}Request` | 使用 Java `record`，須實作 `validate()` |
| Response DTO | `adapter.in.dto` | `{Domain}Response` | 使用 Java `record` |
| Mapper | `adapter.in.mapper` | `{Domain}RequestMapper` | static methods，非 Spring bean |
| 自定義 Exception | `exception` | `{Name}Exception` | 與 adapter 同層，所有層皆可引用 |

---

## 5. Controller 實作規則

### 5.1 必須遵守

1. 使用 **Constructor Injection**，不直接使用 `@Value` 等 Spring annotation
2. 只注入 **Port/In interface**（UseCase）
3. Request → Response 完整轉換流程為一條 Optional 串鏈，依序為：
    1. `request.validate()` 在 Adapter/In 層完成驗證，回傳 `Optional<{Domain}Request>`
    2. `.map(Mapper::toInput)` 將 Request 轉換為 `UseCaseInput`
    3. `.map(useCase::execute)` 執行 UseCase，回傳 `XxxResult`
    4. `.map(Mapper::toResponse)` 將 Result 轉換為 Response DTO（**仍在 Optional 鏈內**）
    5. `.orElseThrow(BadRequestException::new)` 驗證失敗拋出例外，由 `GlobalExceptionHandler` 統一處理
4. 如需系統層級 Config，透過 Constructor Injection 注入 `framework.config` 中的 Config bean

### 5.2 正確範例
```java
// package: adapter.in
import exception.BadRequestException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // ✅ 只依賴 Port/In 的 interface
    private final CreateOrderUseCase createOrderUseCase;

    // ✅ Constructor Injection
    OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestBody CreateOrderRequest request) {

        // ✅ validate() 在 Request DTO 執行（Adapter/In 層）
        // ✅ Result → Response 的轉換也在 Optional 鏈內（第 4 個 map）
        // ✅ 驗證失敗由 orElseThrow 拋出，GlobalExceptionHandler 統一處理
        OrderResponse response = request.validate()
                .map(OrderRequestMapper::toInput)        // Request → UseCaseInput
                .map(createOrderUseCase::execute)        // 執行 UseCase → XxxResult
                .map(OrderRequestMapper::toResponse)     // Result → Response DTO（在鏈內）
                .orElseThrow(BadRequestException::new);  // 驗證失敗拋出 BadRequestException

        return ResponseEntity.ok(response);
    }
}
```

### 5.3 禁止事項
```java
// ❌ 禁止直接注入 Service（跨層依賴）
private final OrderService orderService;

// ❌ 禁止在 Controller 內寫業務判斷
if (order.getStatus() == PAID) { ... }

// ❌ 禁止直接回傳 Domain Entity
public ResponseEntity<Order> create(...) { ... }

// ❌ 禁止跳過 validate()，直接執行 UseCase
createOrderUseCase.execute(OrderRequestMapper.toInput(request));

// ❌ 禁止在 Controller 內自行處理驗證錯誤的 response
if (request.validate().isEmpty()) {
    return ResponseEntity.badRequest().build();
}

// ❌ 禁止將 validate() 放在 UseCaseInput（驗證不得下放至 Application layer）
input.validate().map(...);

// ❌ 禁止在 exception package 以外自行定義 Exception
// adapter.in 或其他任何 package 內不得出現自定義 Exception class
```

---

## 6. Request DTO 規範

- 所在 package：`adapter.in.dto`
- 使用 Java `record` 定義
- 須實作 `validate()` method，負責格式、必填、範圍等輸入驗證
- 簽章：`Optional<{Domain}Request> validate()`
    - 驗證通過：回傳 `Optional.of(this)`
    - 驗證失敗：回傳 `Optional.empty()`
- **不得包含業務邏輯**，僅驗證輸入資料的合法性
- **不得在 `validate()` 內部拋出 exception**，失敗一律回傳 `Optional.empty()`，由 Controller 的 `orElseThrow()` 負責拋出
```java
// package: adapter.in.dto
public record CreateOrderRequest(String productId, int quantity) {

    public Optional<CreateOrderRequest> validate() {
        if (productId == null || productId.isBlank()) return Optional.empty();
        if (quantity <= 0) return Optional.empty();
        return Optional.of(this);
    }
}
```

---

## 7. Mapper 規範

- 所在 package：`adapter.in.mapper`
- 每個 Controller 對應**一個** Mapper class
- 命名：`{Domain}RequestMapper`
- 使用 **static method**，不需要註冊為 Spring bean
```java
// package: adapter.in.mapper
public class OrderRequestMapper {

    public static CreateOrderInput toInput(CreateOrderRequest request) {
        return new CreateOrderInput(request.productId(), request.quantity());
    }

    public static OrderResponse toResponse(OrderResult result) {
        return new OrderResponse(result.orderId(), result.status());
    }
}
```

---

## 8. Exception 規範

- 所在 package：`exception`（與 `adapter` 同層，flat）
- **禁止**在 `exception` package 以外的任何地方定義自定義 Exception class
- Exception 可被所有層引用（Adapter、Application、Domain），不屬於任何單一層
```java
// package: exception
public class BadRequestException extends RuntimeException {

    public BadRequestException() {
        super("Bad request");
    }

    public BadRequestException(String message) {
        super(message);
    }
}
```

---

## 9. 快速檢核清單（Agent Checklist）

在產生或審查 Inbound Adapter 程式碼時，逐項確認：

- [ ] Controller 位於 `adapter.in`（flat，不建立 domain subfolder）
- [ ] Request / Response DTO 位於 `adapter.in.dto`
- [ ] Mapper 位於 `adapter.in.mapper`
- [ ] 所有自定義 Exception 位於 `exception`（與 `adapter` 同層），不在其他 package 自行定義
- [ ] Controller 只依賴 UseCase interface，未注入 Service 或 Repository
- [ ] 使用 Constructor Injection，無 `@Value` 直接用於 Controller
- [ ] Request / Response DTO 為 Java `record`
- [ ] `validate()` 實作在 **Request DTO**，回傳 `Optional<{Domain}Request>`，非 UseCaseInput
- [ ] `validate()` 內部只回傳 `Optional.empty()`，不拋出 exception
- [ ] Controller 使用 `.validate().map(Mapper::toInput).map(useCase::execute).orElseThrow(BadRequestException::new)` 串接
- [ ] 驗證失敗統一由 `orElseThrow(BadRequestException::new)` 拋出，不在 Controller 內自行處理 error response
- [ ] 回傳型別為 Response DTO，非 Domain Entity
- [ ] `{Domain}RequestMapper` 以 static method 實作，非 Spring bean