# domain/model — Domain Model

## 1. 職責定義

- 定義系統的**核心業務概念**：Entity、Value Object、Domain Service、Domain Event
- 封裝**業務不變量**（invariant）與領域規則
- **完全獨立於技術框架** — 零 Spring、零 JPA、零外部 library
- 這是最穩定的層 — 修改頻率低，但影響全域

## 2. 依賴方向

```
domain.model → java.* (standard library only)
```

任何 `org.springframework.*`、`jakarta.persistence.*`、外部 library 的 import 均違規。

---

## 3. 命名規則

| 概念 | 形式 | 命名格式 | 說明 |
|------|------|----------|------|
| Entity / Value Object（純資料） | `record` | `{Entity}` | e.g., `City` |
| Entity / Value Object（含行為） | `class` | `{Entity}` | e.g., `Order` |
| Domain Service | `class` | `{Entity}DomainService` | 跨 entity 的業務邏輯 |
| Domain Event | `record` | `{Action}Event` | e.g., `OrderPlacedEvent` |
| Enum | `enum` | `{Context}Status` | e.g., `OrderStatus` |

---

## 4. 實作規則

### 4.1 必須遵守

1. **零 framework import** — `org.springframework.*`、`jakarta.*` 一律禁止
2. 優先用 **`record`** — 當物件只是值容器（無行為、無可變狀態）
3. 需要行為或可變狀態時用 **`class`**，並確保業務規則在 domain method 中執行
4. **業務不變量寫在 domain 物件裡** — 不要放在 UseCase 或 Controller
5. Domain 物件**不做 I/O** — 不呼叫 DB、不發 HTTP、不讀 file

### 4.2 正確範例

**純值物件（record）：**
```java
// package: domain.model
public record City(String id, String name, String country) {}
```

**含業務行為的 Entity（class）：**
```java
public class Order {
    private final String id;
    private final List<OrderItem> items;
    private OrderStatus status;

    public Order(String id, List<OrderItem> items) {
        this.id = id;
        this.items = List.copyOf(items);  // ✅ 防禦性複製
        this.status = OrderStatus.PENDING;
    }

    // ✅ 業務規則封裝在 domain 方法
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm order in status: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
    }
}
```

**Value Object（record with validation）：**
```java
public record Email(String value) {
    public Email {  // ✅ compact constructor for validation
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}
```

### 4.3 禁止事項

```java
// ❌ 禁止任何 Spring import
import org.springframework.stereotype.Component;
@Component
public record City(String id, String name) {}

// ❌ 禁止 JPA annotation
import jakarta.persistence.Entity;
@Entity
public class City { ... }   // ❌ JPA entity 放在 usecase.ports.out.entity

// ❌ 禁止 domain 物件做 I/O
public class City {
    public void save(JdbcClient db) { ... }  // ❌ I/O 屬於 adapter 層
}

// ❌ 禁止業務規則放在 UseCase 而非 domain
// UseCase 內：
if (order.getStatus() == PENDING && order.getItems().size() > 0) {
    order.setStatus(CONFIRMED);  // ❌ 這是業務邏輯，應在 Order.confirm() 內
}
```

---

## 5. 快速檢核清單（Agent Checklist）

- [ ] 零 `org.springframework.*` import
- [ ] 零 `jakarta.*` import
- [ ] 零外部 library import（MinIO、Jackson 等）
- [ ] 純值物件用 `record`；需要行為的用 `class`
- [ ] 業務不變量與領域規則在 domain 方法內執行，非 UseCase
- [ ] ArchUnit 測試通過（`ArchunitRuleTest`）
