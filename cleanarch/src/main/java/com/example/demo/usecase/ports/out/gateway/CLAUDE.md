# usecase/ports/out/gateway — Gateway Port Interfaces

## 1. 職責定義

- 定義應用程式**需要外部系統提供**的 contract（Gateway interface）
- 定義每個 gateway 的 **Input** 與 **Output** 型別（一律 record）
- **純粹的介面定義** — 零實作、零 Spring annotation、零外部 library import
- 實作由 `adapter.out.gateway` 提供，配線由 `framework.di.BeanInjection` 完成

## 2. 依賴方向

```
usecase.ports.out.gateway → usecase.ports.in.Input  (Input marker interface，選用)
                          → java.*                   (standard library only)
```

不得依賴任何外部 library、Spring、adapter、framework。

---

## 3. 套件結構

每個 gateway 由**三個檔案**組成，放在同一 package：

```
usecase/ports/out/gateway/
├── {Name}Gateway.java     ← interface，execute() 方法
├── {Name}Input.java       ← record，implements Input（選用）
└── {Name}Output.java      ← record，純值容器
```

---

## 4. 命名規則

| 元件 | 命名格式 | 說明 |
|------|----------|------|
| Gateway interface | `{Name}Gateway` | e.g., `VideoStorageGateway` |
| Input record | `{Name}Input` | e.g., `VideoStorageInput` |
| Output record | `{Name}Output` | e.g., `VideoStorageOutput` |
| 實作（不在此 package） | `{Name}GatewayImpl` | 位於 `adapter.out.gateway` |

---

## 5. 實作規則

### 5.1 必須遵守

1. Gateway interface 只有**一個方法**，名稱固定為 `execute`
2. Input 與 Output **一律用 Java `record`** — 不可變，無 setter
3. 零 Spring annotation
4. Input/Output 不引用外部 library 的型別（MinioClient、HttpRequest 等）
5. Output 需要靜態工廠時，用 `public static XxxOutput of(...)` 模式

### 5.2 正確範例

**標準三件套：**
```java
// {Name}Gateway.java
public interface VideoStorageGateway {
    VideoStorageOutput execute(VideoStorageInput input);  // ✅ 唯一 execute() 方法
}

// {Name}Input.java
public record VideoStorageInput(String bucketName) {}

// {Name}Output.java
public record VideoStorageOutput(List<String> videos) {}
```

**無參數 Input（使用 Input marker）：**
```java
public record InlineRoutingInput() implements Input {}
```

**Output 帶靜態工廠：**
```java
public record PutLogOutput(Map<String, Object> context) {
    public static PutLogOutput of(Map<String, Object> context) {
        return new PutLogOutput(context);
    }
}
```

### 5.3 禁止事項

```java
// ❌ 禁止 Gateway 有多個方法
public interface StorageGateway {
    StorageOutput upload(StorageInput input);
    StorageOutput download(StorageInput input);  // ❌ 應拆成兩個 Gateway
}

// ❌ 禁止 Input/Output 用 class（需用 record）
public class VideoStorageInput {
    private String bucketName;
    public void setBucketName(String b) { this.bucketName = b; }  // ❌ mutable
}

// ❌ 禁止 Input/Output 引用外部 library 型別
public record VideoStorageOutput(io.minio.messages.Item minioItem) {}  // ❌ 洩漏 infrastructure

// ❌ 禁止 Spring annotation
@Component
public interface VideoStorageGateway { ... }
```

---

## 6. 快速檢核清單（Agent Checklist）

- [ ] Gateway interface 只有一個 `execute({Name}Input)` 方法
- [ ] Input 為 `record`，無 mutable state
- [ ] Output 為 `record`，無 mutable state
- [ ] 三個檔案（interface + Input + Output）放在同一 package
- [ ] 無 Spring annotation
- [ ] Input/Output 無外部 library 型別（只有 java.* 與標準型別）
- [ ] 對應的 `{Name}GatewayImpl` 在 `adapter.out.gateway` 已建立
- [ ] 對應的 `@Bean` 已在 `framework.di.BeanInjection` 配線
