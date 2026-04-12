# Module Rules: adapter/out/repository (Outbound Repository Adapters)

## Package

`adapter.out.repository` — `JdbcClient`-based implementations of query repository interfaces.

---

## Responsibility

Execute SQL read queries against the routed datasource using `JdbcClient`.
Write operations are handled by `JpaRepository` (auto-implemented by Spring Data — no class needed).

---

## CQRS Separation

| Operation | Where | How |
|-----------|-------|-----|
| Reads | `XxxQueryRepositoryImpl` in `adapter.out.repository` | `JdbcClient` |
| Writes | Spring Data auto-impl of `XxxRepository` | `JpaRepository` |

---

## Allowed Imports

```
✅ com.example.demo.usecase.ports.out.repository.*  (interface being implemented)
✅ com.example.demo.usecase.ports.out.entity.*      (JPA entity types returned by queries)
✅ org.springframework.jdbc.core.simple.JdbcClient  (query execution)
✅ org.springframework.stereotype.Repository        (@Repository annotation — allowed here)
```

## Forbidden Imports

```
❌ com.example.demo.usecase.ports.in.*   (no use case coupling)
❌ com.example.demo.adapter.in.*         (no inbound adapter coupling)
❌ com.example.demo.domain.*             (return entity types, not domain objects)
❌ com.example.demo.framework.*          (no framework internals)
```

---

## Constraints

- **`@Repository` is the one allowed Spring annotation** — Spring needs it to discover the bean
  and to wrap `DataAccessException` translation
- **Constructor injection** for `JdbcClient` — no `@Autowired`
- **Named parameters only** — use `:paramName`, never positional `?`
- **Row mapper as inline lambda** — only extract to a named `RowMapper` if it's reused in 3+ queries
- **Never apply business logic** — filtering, sorting preference, data enrichment belong in the use case

---

## Pattern Reference

See `references/patterns/repository-impl.md` for the full JdbcClient pattern.

---

## Checklist for New Query Repository

- [ ] Interface defined in `usecase.ports.out.repository`
- [ ] Implementation `XxxQueryRepositoryImpl` in `adapter.out.repository` with `@Repository`
- [ ] `JdbcClient` injected via constructor
- [ ] All queries use named parameters
- [ ] Returns `List<XxxJpaEntity>` or `Optional<XxxJpaEntity>` — no DTO conversion here
- [ ] Test: `@JdbcTest` or `@SpringBootTest` with real H2; verify SQL results
