# framework/di — Dependency Injection & Composition Root

## 1. 職責定義

- 這是整個應用程式的 **Composition Root** — 唯一允許同時 import `adapter.*` 與 `usecase.*` 的地方
- 以 `@Bean` 方法將 UseCase 實作、Gateway 實作組裝成 Spring managed bean
- 管理 `DataSource` 路由配置（`dynamicdatasource/`）
- **禁止包含業務邏輯** — 只做物件組裝與配置

## 2. 子套件分工

| Package | 職責 |
|---------|------|
| `framework.di` | `BeanInjection`：UseCase impl 與 Gateway impl 的 `@Bean` 配線 |
| `framework.di.dynamicdatasource` | 動態 DataSource 路由（`DynamicRoutingDataSource`、`DataSourceContextHolder`、`DataSourceKey`） |
| `framework.config` | 外部 client 配置（`AppConfig`、`VideoConfig`）與 `@ConfigurationProperties` |

---

## 3. 實作規則

### 3.1 BeanInjection — 配線原則

1. `@Bean` 方法的**回傳型別一律為 interface**，不用 concrete class
2. 注入 UseCase 時，參數使用 **output port interface**，不用 impl class
3. 新 UseCase 或 Gateway 加入後，**必須在此加 `@Bean`**（無 Spring annotation 的 class 無法自動掃描）

```java
@Configuration
public class BeanInjection {

    // ✅ 回傳型別為 interface（VideoStorageGateway），不是 impl
    @Bean
    public VideoStorageGateway videoStorageGateway(MinioClient minioClient) {
        return new VideoStorageGatewayImpl(minioClient);
    }

    // ✅ 參數使用 interface，不是 impl
    @Bean
    public ListVideosUseCase listVideosUseCase(
            VideoStorageGateway videoStorageGateway, PutLogGateway putLogGateway) {
        return new ListVideosUseCaseImpl(videoStorageGateway, putLogGateway);
    }
}
```

### 3.2 DataSourceContextHolder 使用限制

`DataSourceContextHolder` 與 `DataSourceKey` 屬於 **framework 內部實作細節**：

- **允許使用**：`framework.di.dynamicdatasource.*` 內部
- **禁止 import**：`usecase.*`、`adapter.*`（目前 `FindCitiesUseCaseImpl` 有此違規，待修正）

正確做法：datasource 切換應在 `InlineRoutingGatewayImpl` 執行後，
由 AOP advice 或 `DataSourceConfig` 根據 gateway 回傳結果設定，**不由 use case 直接操作**。

### 3.3 外部 Client 配置（framework/config）

```java
@Configuration
public class AppConfig {

    @Bean
    @ConfigurationProperties(prefix = "minio")
    public MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}
```

---

## 4. 禁止事項

```java
// ❌ @Bean 回傳 concrete class
@Bean
public ListVideosUseCaseImpl listVideosUseCase(...) { ... }

// ❌ 在 framework 以外 import DataSourceContextHolder
// usecase/ports/in/impl/FindCitiesUseCaseImpl.java
import com.example.demo.framework.di.dynamicdatasource.DataSourceContextHolder; // ❌ 違規

// ❌ 在 BeanInjection 寫業務邏輯
@Bean
public FindCitiesUseCase findCitiesUseCase(...) {
    if (env.equals("prod")) { ... }  // ❌ 業務判斷不在此
}
```

---

## 5. 快速檢核清單（Agent Checklist）

新增 UseCase 或 Gateway 時：

- [ ] `BeanInjection` 加上對應 `@Bean`，回傳型別為 **interface**
- [ ] `@Bean` 參數型別為 **interface**（不用 impl class）
- [ ] 若新 UseCase 依賴新 Gateway，兩個 `@Bean` 都在同一個 `@Configuration` class
- [ ] `DataSourceContextHolder` 的引用只存在於 `framework.di.dynamicdatasource` 內部
- [ ] `framework.config` 的 `@ConfigurationProperties` prefix 對應 `application.properties` 的 key
