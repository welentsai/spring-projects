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

## 3. 命名規則

| 元件 | 命名格式 | 備註 |
|------|----------|------|
| Controller | `{Domain}Controller` | |
| Request DTO | `{Domain}Request` | 使用 Java `record`，須實作 `validate()` |
| Response DTO | `{Domain}Response` | 使用 Java `record` |
| Mapper | `{Domain}RequestMapper` | static methods，非 Spring bean |

---

## 4. Controller 實作規則

### 4.1 必須遵守

1. 使用 **Constructor Injection**，不直接使用 `@Value` 等 Spring annotation
2. 只注入 **Port/In interface**（UseCase）
3. Request → Input 轉換流程須依序為：
    1. `request.validate()` 在 Adapter/In 層完成驗證，回傳 `Optional<{Domain}Request>`
    2. `.map(Mapper::toInput)` 將 Request 轉換為 `UseCaseInput`
    3. `.map(useCase::execute)` 執行 UseCase
    4. `.orElseThrow(BadRequestException::new)` 驗證失敗拋出例外，由 `GlobalExceptionHandler` 統一處理
4. 在 Controller 方法中完成 `Result → Response` 的轉換
5. 如需系統層級 Config，透過 Constructor Injection 注入 `framework.config` 中的 Config bean

### 4.2 正確範例
```java
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
        // ✅ 驗證通過後才進行 Mapping 與 UseCase 執行
        // ✅ 驗證失敗由 orElseThrow 拋出，GlobalExceptionHandler 統一處理
        OrderResult result = request.validate()
                .map(OrderRequestMapper::toInput)        // Request → UseCaseInput
                .map(createOrderUseCase::execute)        // 執行 UseCase
                .orElseThrow(BadRequestException::new);  // 驗證失敗拋出 BadRequestException

        // ✅ Result → Response 轉換
        return ResponseEntity.ok(OrderRequestMapper.toResponse(result));
    }
}
```

### 4.3 禁止事項
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
```

---

## 5. Request DTO 規範

- 使用 Java `record` 定義
- 須實作 `validate()` method，負責格式、必填、範圍等輸入驗證
- 簽章：`Optional<{Domain}Request> validate()`
    - 驗證通過：回傳 `Optional.of(this)`
    - 驗證失敗：回傳 `Optional.empty()`
- **不得包含業務邏輯**，僅驗證輸入資料的合法性
- **不得在 `validate()` 內部拋出 exception**，失敗一律回傳 `Optional.empty()`，由 Controller 的 `orElseThrow()` 負責拋出
```java
public record CreateOrderRequest(String productId, int quantity) {

    public Optional<CreateOrderRequest> validate() {
        if (productId == null || productId.isBlank()) return Optional.empty();
        if (quantity <= 0) return Optional.empty();
        return Optional.of(this);
    }
}
```

---

## 6. Mapper 規範

1. 每個 Controller 對應**一個** Mapper class
2. 命名：`{Domain}RequestMapper`
3. 使用 **static method**，不需要註冊為 Spring bean

---

## 7. 快速檢核清單（Agent Checklist）

在產生或審查 Inbound Adapter 程式碼時，逐項確認：

- [ ] Controller 只依賴 UseCase interface，未注入 Service 或 Repository
- [ ] 使用 Constructor Injection，無 `@Value` 直接用於 Controller
- [ ] Request / Response DTO 為 Java `record`
- [ ] `validate()` 實作在 **Request DTO**，回傳 `Optional<{Domain}Request>`，非 UseCaseInput
- [ ] `validate()` 內部只回傳 `Optional.empty()`，不拋出 exception
- [ ] Controller 使用 `.validate().map(Mapper::toInput).map(useCase::execute).orElseThrow(BadRequestException::new)` 串接
- [ ] 驗證失敗統一由 `orElseThrow(BadRequestException::new)` 拋出，不在 Controller 內自行處理 error response
- [ ] 回傳型別為 Response DTO，非 Domain Entity
- [ ] 對應的 `{Domain}RequestMapper` 以 static method 實作