# adapter/out/repository — Outbound Repository Adapters

## 1. 職責定義

- 實作 `usecase.ports.out.repository` 定義的 QueryRepository interface
- 使用 `JdbcClient` 執行 **SQL 讀取**查詢（CQRS read side）
- 回傳 `usecase.ports.out.entity` 的 JPA Entity 型別 — **不在此做 DTO 轉換**
- **寫入操作** 由 `JpaRepository`（Spring Data 自動實作）處理，**此 package 不需要寫入 class**
- **禁止包含業務邏輯** — 不做過濾、排序偏好、資料富化

## 2. 依賴方向

```
adapter.out.repository → usecase.ports.out.repository  (實作的 interface)
                       → usecase.ports.out.entity       (回傳的 Entity 型別)
                       → org.springframework.jdbc.core.simple.JdbcClient
```

不得依賴 `adapter.in.*`、`usecase.ports.in.*`、`domain.*`、`framework.*`。

---

## 3. CQRS 分工

| 操作 | 實作位置 | 技術 |
|------|---------|------|
| 讀取（findAll、findBy*） | `XxxQueryRepositoryImpl`（本 package） | `JdbcClient` |
| 寫入（save、delete） | Spring Data 自動實作 | `JpaRepository` |

---

## 4. 命名規則

| 元件 | 命名格式 | 對應 interface |
|------|----------|---------------|
| Query Repository 實作 | `{Name}QueryRepositoryImpl` | `{Name}QueryRepository`（位於 `usecase.ports.out.repository`） |

---

## 5. 實作規則

### 5.1 必須遵守

1. **`@Repository`** 是此 package **唯一允許**的 Spring annotation — Spring 需要它做例外轉譯與 Bean 掃描
2. 使用 **Named Parameters**（`:paramName`）— 禁止 positional `?`
3. Row mapper 寫成 **inline lambda** — 只有 3 個以上 query 共用時才抽成 named `RowMapper`
4. Constructor Injection 注入 `JdbcClient` — 禁止 `@Autowired`
5. 回傳型別一律為 `List<XxxJpaEntity>` 或 `Optional<XxxJpaEntity>` — 不回傳 DTO

### 5.2 正確範例

```java
// package: adapter.out.repository
import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository   // ✅ 唯一允許的 Spring annotation
public class CitiesQueryRepositoryImpl implements CitiesQueryRepository {

    private final JdbcClient jdbcClient;  // ✅ Constructor Injection

    public CitiesQueryRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<CityJpaEntity> findAll() {
        return jdbcClient
                .sql("SELECT id, name, country FROM city_jpa_entity")
                .query((rs, rowNum) -> {
                    CityJpaEntity e = new CityJpaEntity();
                    e.setId(rs.getString("id"));
                    e.setName(rs.getString("name"));
                    e.setCountry(rs.getString("country"));
                    return e;
                })
                .list();   // ✅ .list() for List, .optional() for Optional
    }

    @Override
    public Optional<CityJpaEntity> findByName(String name) {
        return jdbcClient
                .sql("SELECT id, name, country FROM city_jpa_entity WHERE name = :name")
                .param("name", name)   // ✅ named parameter
                .query((rs, rowNum) -> {
                    CityJpaEntity e = new CityJpaEntity();
                    e.setId(rs.getString("id"));
                    e.setName(rs.getString("name"));
                    e.setCountry(rs.getString("country"));
                    return e;
                })
                .optional();
    }
}
```

### 5.3 禁止事項

```java
// ❌ 禁止 positional parameter
jdbcClient.sql("SELECT * FROM city WHERE name = ?").param(name)...

// ❌ 禁止在 repository 做 DTO 轉換
return jdbcClient.sql(...).query(...).list().stream()
        .map(e -> new CityDto(e.getId(), e.getName()))  // ❌ 轉換屬於 use case 的職責
        .toList();

// ❌ 禁止業務邏輯
if (country.equals("TW")) {
    return jdbcClient.sql("SELECT ... WHERE priority = 1 ...").list(); // ❌ 業務判斷
}

// ❌ 禁止 @Autowired
@Autowired
private JdbcClient jdbcClient;

// ❌ 禁止使用 @Service、@Component
@Service
public class CitiesQueryRepositoryImpl implements CitiesQueryRepository { ... }
```

---

## 6. JdbcClient Terminal Methods 速查

| 回傳型別 | 方法 |
|---------|------|
| `List<T>` | `.list()` |
| `Optional<T>` | `.optional()` |
| 單筆（不存在時拋例外） | `.single()` |
| 純 update/insert rowcount | `.update()` |

---

## 7. 快速檢核清單（Agent Checklist）

- [ ] class 只有 `@Repository` annotation，無其他 Spring annotation
- [ ] 只實作一個 QueryRepository interface，interface 位於 `usecase.ports.out.repository`
- [ ] `JdbcClient` 透過 Constructor Injection 注入
- [ ] 所有 SQL 參數使用 named parameter（`:param`），無 `?`
- [ ] 回傳型別為 Entity（`List<XxxJpaEntity>` 或 `Optional<XxxJpaEntity>`），無 DTO 轉換
- [ ] 無業務邏輯
- [ ] SQL table 名稱與 JPA Entity class 名稱對應（`CityJpaEntity` → `city_jpa_entity`）
