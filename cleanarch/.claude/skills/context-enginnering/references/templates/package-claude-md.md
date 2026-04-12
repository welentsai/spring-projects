# Package CLAUDE.md Template

用於生成放置在各 package 目錄下的 CLAUDE.md。
Claude Code 會在開啟該 package 內的檔案時自動載入此 context。

---

## Template 結構

```markdown
# {Package Path} — {短描述}

## 1. 職責定義

- {職責 1：這個 package 做什麼}
- {職責 2}
- **禁止包含 {不屬於此層的職責}**

## 2. 依賴方向

{箭頭圖說明}

只允許依賴 {允許的目標}，不得依賴 {禁止的目標}。

---

## 3. 套件結構

{目錄樹，含 {Domain} placeholder}

---

## 4. 命名規則

| 元件 | 所在 package | 命名格式 | 備註 |
|------|-------------|----------|------|

---

## 5. 實作規則

### 5.1 必須遵守

{規則清單 + 說明}

### 5.2 正確範例

{canonical code pattern，從實際 .java 萃取後抽象化}

### 5.3 禁止事項

{anti-pattern 程式碼片段 + 說明}

---

## 6. 快速檢核清單（Agent Checklist）

在產生或審查此 package 的程式碼時，逐項確認：

- [ ] {項目 1}
- [ ] {項目 2}
```

---

## 各 Package 對應的填充指引

| Package | 職責關鍵字 | 依賴箭頭 | 命名重點 |
|---------|-----------|----------|---------|
| `adapter.in` | 接收 HTTP、轉換、不含業務邏輯 | → `usecase.ports.in` | `{Domain}Controller`, `{Domain}Request`, `{Domain}RequestMapper` |
| `adapter.out.gateway` | 呼叫外部系統實作 | → `usecase.ports.out.gateway` | `{Name}GatewayImpl` |
| `adapter.out.repository` | JdbcClient SQL 查詢實作 | → `usecase.ports.out.repository`, `.entity` | `{Name}QueryRepositoryImpl` |
| `usecase.ports.in` | 定義 UseCase interface + Input/Result | → `domain.model`, `usecase.ports.out` | `{Feature}UseCase`, `{Feature}Input`, `{Feature}Result` |
| `usecase.ports.in.impl` | UseCase 實作，orchestrate ports | → `usecase.ports.in`, `usecase.ports.out` | `{Feature}UseCaseImpl` |
| `usecase.ports.out.gateway` | Gateway port interface + I/O | 無 Spring | `{Name}Gateway`, `{Name}Input`, `{Name}Output` |
| `usecase.ports.out.repository` | Repository port interface | 無 Spring | `{Name}QueryRepository`, `{Name}Repository` |
| `usecase.ports.out.entity` | JPA Entity 共用型別 | `jakarta.persistence.*` only | `{Name}JpaEntity` |
| `domain.model` | Pure Java 領域物件 | 零 Spring、零 JPA | `{Entity}` record 或 class |
| `framework.di` | Composition root，@Bean 配線 | 唯一允許同時 import adapter + usecase | `BeanInjection`, `DataSourceConfig` |

---

## 生成流程（供 Skill 使用）

1. 執行 `scripts/scan_package.sh <package-dir>` 取得該 package 所有 .java 原始碼
2. 識別最具代表性的 class（第一個 @RestController / implements XxxGateway / public class XxxImpl 等）
3. 抽取其結構骨架（package、imports、class 宣告、field、constructor、主要 method 簽章）
4. 填入 Template 的 §5.2 正確範例
5. 依照上表填入對應的職責、依賴、命名規則
6. 寫入 `{package-dir}/CLAUDE.md`
