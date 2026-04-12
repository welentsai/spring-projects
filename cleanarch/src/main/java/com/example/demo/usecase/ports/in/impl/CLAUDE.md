# usecase/ports/in/impl — Use Case Implementations

## 1. 職責定義

- 實作 `usecase.ports.in` 定義的 UseCase interface
- 協調（orchestrate）output ports（Gateway、Repository）完成業務流程
- 將 JPA Entity 轉換為 DTO，**不讓 Entity 流出此層**
- **禁止任何 Spring annotation**（由 `framework.di.BeanInjection` 以 `@Bean` 配線）
- **禁止直接 import** `adapter.*` 或 `framework.*` 下的任何類別
- **禁止讓例外傳播到 controller** — 一律 catch 後回傳 `XxxResult.failure(...)`

## 2. 依賴方向

```
usecase.ports.in.impl → usecase.ports.in          (implements interface，使用 Input/Result)
                      → usecase.ports.in.dto       (組裝 DTO)
                      → usecase.ports.out.gateway  (呼叫 gateway port interface)
                      → usecase.ports.out.repository (呼叫 repository port interface)
                      → usecase.ports.out.entity   (讀取 JPA entity，轉換後丟棄)
                      → domain.model               (使用 domain 物件)
                      → org.slf4j                  (SLF4J logging only)
```

**禁止：** `adapter.*`、`framework.*`（包含 `DataSourceContextHolder`、`DataSourceKey`）

---

## 3. 命名規則

| 元件 | 命名格式 | 對應 interface |
|------|----------|---------------|
| UseCase 實作 | `{Feature}UseCaseImpl` | `{Feature}UseCase`（位於 `usecase.ports.in`） |

---

## 4. 實作規則

### 4.1 必須遵守

1. **無 Spring annotation** — `@Service`、`@Component`、`@Transactional` 等一律禁止
2. **Constructor Injection** — 所有依賴宣告為 `private final`，透過 constructor 注入
3. **Exception 不外傳** — 所有 `execute()` 方法用 `try/catch(Exception e)` 包覆，失敗回傳 `XxxResult.failure(...)`
4. **Entity 不出界** — JPA Entity 在此層轉換成 DTO，不回傳 Entity 型別
5. **只用 SLF4J Logger** — `LoggerFactory.getLogger(XxxUseCaseImpl.class)`

### 4.2 正確範例

```java
// package: usecase.ports.in.impl
import com.example.demo.usecase.ports.in.ListVideosInput;
import com.example.demo.usecase.ports.in.ListVideosResult;
import com.example.demo.usecase.ports.in.ListVideosUseCase;
import com.example.demo.usecase.ports.out.gateway.PutLogGateway;
import com.example.demo.usecase.ports.out.gateway.PutLogInput;
import com.example.demo.usecase.ports.out.gateway.VideoStorageGateway;
import com.example.demo.usecase.ports.out.gateway.VideoStorageInput;
import com.example.demo.usecase.ports.out.gateway.VideoStorageOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// ✅ 無 Spring annotation
public class ListVideosUseCaseImpl implements ListVideosUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ListVideosUseCaseImpl.class);

    private final VideoStorageGateway videoStorageGateway;
    private final PutLogGateway putLogGateway;

    // ✅ Constructor Injection
    public ListVideosUseCaseImpl(
            VideoStorageGateway videoStorageGateway, PutLogGateway putLogGateway) {
        this.videoStorageGateway = videoStorageGateway;
        this.putLogGateway = putLogGateway;
    }

    @Override
    public ListVideosResult execute(ListVideosInput input) {
        try {
            logger.info("Listing videos from bucket: {}", input.getBucketName());
            putLogGateway.execute(new PutLogInput("bucket", input.getBucketName()));

            VideoStorageOutput output =
                    videoStorageGateway.execute(new VideoStorageInput(input.getBucketName()));

            return ListVideosResult.success(output.videos());

        } catch (Exception e) {
            logger.error("Failed to list videos", e);
            putLogGateway.execute(new PutLogInput("error", e.getMessage()));
            return ListVideosResult.failure("INTERNAL_ERROR", "Failed: " + e.getMessage());
        }
    }
}
```

### 4.3 禁止事項

```java
// ❌ 禁止 Spring annotation
@Service
public class ListVideosUseCaseImpl implements ListVideosUseCase { ... }

// ❌ 禁止 import framework 內部類別
import com.example.demo.framework.di.dynamicdatasource.DataSourceContextHolder;
import com.example.demo.framework.di.dynamicdatasource.DataSourceKey;

// ❌ 禁止 import adapter 類別
import com.example.demo.adapter.out.repository.CitiesQueryRepositoryImpl;

// ❌ 禁止讓例外往外拋
public ListVideosResult execute(ListVideosInput input) throws Exception { ... }

// ❌ 禁止回傳 JPA Entity
public class ListVideosUseCaseImpl implements ListVideosUseCase {
    public List<CityJpaEntity> execute(...)  // ❌ entity 不出界
}

// ❌ 禁止用 System.out，應用 SLF4J
System.out.println("result: " + output);
```

---

## 5. 配線方式（framework/di/BeanInjection）

每新增一個 UseCaseImpl，須在 `BeanInjection` 加上對應 `@Bean`：

```java
@Bean
public ListVideosUseCase listVideosUseCase(
        VideoStorageGateway videoStorageGateway, PutLogGateway putLogGateway) {
    return new ListVideosUseCaseImpl(videoStorageGateway, putLogGateway);
}
```

---

## 6. 快速檢核清單（Agent Checklist）

- [ ] class 無任何 Spring annotation
- [ ] 所有依賴為 `private final`，透過 constructor 注入
- [ ] `execute()` 被 `try/catch(Exception e)` 完整包覆
- [ ] catch block 回傳 `XxxResult.failure(...)` 並記錄 `logger.error(...)`
- [ ] JPA Entity 在此層轉換為 DTO，不往外傳
- [ ] 無 `adapter.*` 或 `framework.*` 的 import
- [ ] 對應的 `@Bean` 已加入 `framework.di.BeanInjection`，回傳型別為 UseCase interface
