# adapter/out/gateway — Outbound Gateway Adapters

## 1. 職責定義

- 實作 `usecase.ports.out.gateway` 定義的 Gateway interface
- 負責所有**非資料庫**的外部 I/O：REST API、物件儲存、訊息佇列、路由決策、日誌寫入
- **禁止包含業務邏輯** — 只做「呼叫外部 → 回傳結果」
- **禁止加上 Spring 元件 annotation** — 由 `framework.di.BeanInjection` 統一 @Bean 配線

## 2. 依賴方向

```
adapter.out.gateway → usecase.ports.out.gateway (interface + I/O types)
                    → system.*                   (LoggingContext，限 side-effect gateway)
                    → 外部 library (MinIO、HTTP client 等)
```

不得依賴 `adapter.in.*`、`usecase.ports.in.*`、`domain.*`、`framework.di.*`。

---

## 3. 套件結構

```
adapter/out/gateway/
└── {Name}GatewayImpl.java    ← 一個 interface 對應一個 Impl
```

---

## 4. 命名規則

| 元件 | 命名格式 | 對應 interface |
|------|----------|---------------|
| Gateway 實作 | `{Name}GatewayImpl` | `{Name}Gateway`（位於 `usecase.ports.out.gateway`） |

---

## 5. 實作規則

### 5.1 必須遵守

1. class 上**不加任何 Spring annotation**（`@Component`、`@Service` 等一律禁止）
2. 只實作**一個** Gateway interface
3. 依賴透過 **Constructor Injection**，由 `BeanInjection` 的 `@Bean` 方法注入
4. `LoggingAspect` 自動包覆此 package 下所有方法 — **不需手動加 input/duration/error 日誌**
5. `execute()` 是唯一的公開方法名稱

### 5.2 正確範例

```java
// package: adapter.out.gateway
import com.example.demo.usecase.ports.out.gateway.VideoStorageGateway;
import com.example.demo.usecase.ports.out.gateway.VideoStorageInput;
import com.example.demo.usecase.ports.out.gateway.VideoStorageOutput;

// ✅ 無 Spring annotation
public class VideoStorageGatewayImpl implements VideoStorageGateway {

    private final MinioClient minioClient;  // ✅ Constructor Injection

    public VideoStorageGatewayImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public VideoStorageOutput execute(VideoStorageInput input) {
        // 呼叫外部系統，回傳 Output record
        List<String> videos = /* MinIO API 呼叫 */;
        return new VideoStorageOutput(videos);
    }
}
```

**Side-effect Gateway（寫入 LoggingContext）：**
```java
public class PutLogGatewayImpl implements PutLogGateway {

    @Override
    public PutLogOutput execute(PutLogInput input) {
        // ✅ 唯一允許引用 system.LoggingContext
        return PutLogOutput.of(LoggingContext.put(input.key(), input.value()));
    }
}
```

**無外部依賴的 Gateway（內部決策）：**
```java
public class InlineRoutingGatewayImpl implements InlineRoutingGateway {

    @Override
    public InlineRoutingOutput execute(InlineRoutingInput input) {
        boolean usePrimary = /* routing logic */;
        return new InlineRoutingOutput(usePrimary ? "PRIMARY" : "SECONDARY");
    }
}
```

### 5.3 禁止事項

```java
// ❌ 禁止 Spring annotation
@Component
public class VideoStorageGatewayImpl implements VideoStorageGateway { ... }

// ❌ 禁止依賴 framework 內部類別
import com.example.demo.framework.di.dynamicdatasource.DataSourceContextHolder;

// ❌ 禁止在 gateway 內寫業務判斷
if (output.datasourceKey().equals("PRIMARY") && someBusinessCondition) { ... }

// ❌ 禁止實作多個 Gateway interface
public class MultiGatewayImpl implements GatewayA, GatewayB { ... }

// ❌ 禁止手動加 try/catch 只為了記錄日誌（LoggingAspect 已自動處理）
try {
    Object result = minioClient.listObjects(...);
    logger.info("duration: {}ms", elapsed);  // ❌ 多餘
    return result;
} catch (Exception e) {
    logger.error("error: {}", e.getMessage()); // ❌ 多餘
    throw e;
}
```

---

## 6. 配線方式（framework/di/BeanInjection）

每新增一個 GatewayImpl，須在 `BeanInjection` 加上對應 `@Bean`：

```java
@Bean
public VideoStorageGateway videoStorageGateway(MinioClient minioClient) {
    return new VideoStorageGatewayImpl(minioClient);  // 回傳 interface 型別
}
```

---

## 7. 快速檢核清單（Agent Checklist）

- [ ] class 無任何 Spring annotation（`@Component`、`@Service`、`@Repository` 等）
- [ ] 只實作一個 Gateway interface，interface 位於 `usecase.ports.out.gateway`
- [ ] Constructor Injection 注入外部 client
- [ ] 對應的 `@Bean` 已加入 `framework.di.BeanInjection`，回傳型別為 interface
- [ ] 無手動 input/duration/error 日誌（AOP 自動處理）
- [ ] 無 `DataSourceContextHolder`、`DataSourceKey` 等 framework 內部 import
- [ ] 無業務邏輯
