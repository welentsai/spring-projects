# usecase/ports/in — Use Case Interfaces, Input & Result Types

## 1. 職責定義

- 定義應用程式**能做什麼**的 public contract（UseCase interface）
- 定義每個 use case 的 **Input**（輸入型別）與 **Result**（回傳型別）
- 定義 `usecase.ports.in.dto` 下的**跨層 DTO**（被 use case 與 adapter 共用）
- **禁止包含業務邏輯** — 只有型別定義與合約
- **禁止任何 Spring annotation**

## 2. 依賴方向

```
usecase.ports.in → domain.model       (Input 可引用 domain 物件)
                 → usecase.ports.out  (Result/DTO 不依賴 out，但 impl 層會)
```

不得依賴 `adapter.*`、`framework.*`。

---

## 3. 套件結構

```
usecase/ports/in/
├── {Feature}UseCase.java        ← interface，single execute() method
├── {Feature}Input.java          ← input 型別，implements Input marker
├── {Feature}Result.java         ← extends Result<T>
├── Input.java                   ← marker interface（勿修改）
├── Output.java                  ← marker interface（勿修改）
├── Result.java                  ← 泛型 base class（勿修改）
├── Try.java                     ← （保留，待使用）
└── dto/
    └── {Entity}Dto.java         ← record，use case 與 adapter response 共用
```

---

## 4. 命名規則

| 元件 | 命名格式 | 說明 |
|------|----------|------|
| UseCase interface | `{Feature}UseCase` | e.g., `FindCitiesUseCase` |
| Input class | `{Feature}Input` | implements `Input` marker |
| Result class | `{Feature}Result` | extends `Result<T>` |
| DTO record | `{Entity}Dto` | 位於 `dto/` 子目錄 |

---

## 5. 實作規則

### 5.1 必須遵守

1. UseCase interface 只有**一個方法**，名稱固定為 `execute`
2. `{Feature}Input` 實作 `Input` marker interface
3. `{Feature}Result` 繼承 `Result<T>`，提供型別安全的 `success()`、`failure()` 靜態工廠
4. DTO 一律用 Java `record` — 不可變，無 setter
5. 零 Spring annotation（`@Service`、`@Component` 等一律禁止）
6. `Result` 子類別加上 `@JsonPropertyOrder` 控制序列化欄位順序

### 5.2 正確範例

**UseCase Interface：**
```java
// package: usecase.ports.in
public interface FindCitiesUseCase {
    FindCitiesResult execute(FindCitiesInput input);  // ✅ 唯一 execute() 方法
}
```

**Input（無參數）：**
```java
public class FindCitiesInput implements Input {}
```

**Input（有參數）：**
```java
public class ListVideosInput implements Input {
    private final String bucketName;

    public ListVideosInput(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }
}
```

**Result：**
```java
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"returnCode", "errorMessage", "data", "cities", "success", "failure"})
public class FindCitiesResult extends Result<List<CityDto>> {

    private FindCitiesResult(String returnCode, String errorMessage, List<CityDto> data) {
        super(returnCode, errorMessage, data);
    }

    public static FindCitiesResult success(List<CityDto> cities) {
        return new FindCitiesResult("SUCCESS", null, cities);
    }

    public static FindCitiesResult failure(String errorMessage) {
        return new FindCitiesResult("FAILURE", errorMessage, null);
    }

    public static FindCitiesResult failure(String returnCode, String errorMessage) {
        return new FindCitiesResult(returnCode, errorMessage, null);
    }

    public List<CityDto> getCities() { return getData(); }  // ✅ 語意化 getter
}
```

**DTO：**
```java
// package: usecase.ports.in.dto
public record CityDto(String id, String name, String country) {}
```

### 5.3 禁止事項

```java
// ❌ 禁止 UseCase 有多個方法
public interface OrderUseCase {
    OrderResult create(CreateOrderInput input);
    OrderResult cancel(CancelOrderInput input);  // ❌ 應拆成兩個 interface
}

// ❌ 禁止 Spring annotation
@Service
public interface FindCitiesUseCase { ... }

// ❌ 禁止 Result 直接回傳 domain 物件或 Entity
public class FindCitiesResult extends Result<List<City>> { ... }       // ❌ domain 物件
public class FindCitiesResult extends Result<List<CityJpaEntity>> { ... } // ❌ JPA entity

// ❌ 禁止 DTO 用 class（需用 record）
public class CityDto {
    private String id;
    public void setId(String id) { this.id = id; }  // ❌ mutable DTO
}
```

---

## 6. 快速檢核清單（Agent Checklist）

- [ ] `{Feature}UseCase` interface 只有一個 `execute({Feature}Input)` 方法
- [ ] `{Feature}Input` 實作 `Input` marker
- [ ] `{Feature}Result` 繼承 `Result<T>`，有 `success()` 和 `failure()` 靜態工廠
- [ ] `{Feature}Result` 加上 `@JsonPropertyOrder`
- [ ] DTO 為 `record`，位於 `dto/` 子目錄
- [ ] 無 Spring annotation
- [ ] 無 adapter 或 framework 的 import
