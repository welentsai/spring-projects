# Pattern: Repository Implementation (adapter/out/repository)

Extracted from `CitiesQueryRepositoryImpl`.

---

## Full Pattern (JdbcClient)

```java
package com.example.demo.adapter.out.repository;

import com.example.demo.usecase.ports.out.entity.XxxJpaEntity;
import com.example.demo.usecase.ports.out.repository.XxxQueryRepository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository   // @Repository is the ONE exception — Spring needs it for JdbcClient bean injection
public class XxxQueryRepositoryImpl implements XxxQueryRepository {

    private final JdbcClient jdbcClient;

    public XxxQueryRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<XxxJpaEntity> findAll() {
        String sql = "SELECT id, name FROM xxx_jpa_entity";

        return jdbcClient
                .sql(sql)
                .query((rs, rowNum) -> {
                    XxxJpaEntity entity = new XxxJpaEntity();
                    entity.setId(rs.getString("id"));
                    entity.setName(rs.getString("name"));
                    return entity;
                })
                .list();
    }

    @Override
    public Optional<XxxJpaEntity> findById(String id) {
        String sql = "SELECT id, name FROM xxx_jpa_entity WHERE id = :id";

        return jdbcClient
                .sql(sql)
                .param("id", id)
                .query((rs, rowNum) -> {
                    XxxJpaEntity entity = new XxxJpaEntity();
                    entity.setId(rs.getString("id"));
                    entity.setName(rs.getString("name"));
                    return entity;
                })
                .optional();
    }

    @Override
    public List<XxxJpaEntity> findAllByField(String value) {
        String sql = "SELECT id, name FROM xxx_jpa_entity WHERE field = :value";

        return jdbcClient
                .sql(sql)
                .param("value", value)
                .query((rs, rowNum) -> {
                    XxxJpaEntity entity = new XxxJpaEntity();
                    entity.setId(rs.getString("id"));
                    entity.setName(rs.getString("name"));
                    return entity;
                })
                .list();
    }
}
```

---

## JdbcClient Query Methods

| Return type | Terminal method |
|-------------|----------------|
| `List<T>` | `.list()` |
| `Optional<T>` | `.optional()` |
| Single row | `.single()` |

---

## Key Rules

- `@Repository` is **allowed** here (it's in the adapter layer — Spring annotations are fine)
- Use **named parameters** (`:paramName`) — never positional `?` — for readability
- Row mapping is inline lambda — no separate `RowMapper` class unless reused in 3+ queries
- SQL table name matches the JPA entity's table: `XxxJpaEntity` → table `xxx_jpa_entity`
- Constructor injection for `JdbcClient` — no `@Autowired`
