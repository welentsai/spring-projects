# Module Rules: usecase/ports/out (Output Ports)

## Packages

| Package | Contents |
|---------|----------|
| `usecase.ports.out.gateway` | Gateway port interfaces + Input/Output records |
| `usecase.ports.out.repository` | Repository port interfaces |
| `usecase.ports.out.entity` | JPA entities (shared by JpaRepository and JdbcClient) |

---

## Responsibility

Define what the application **needs from the outside world** — the contracts that adapter
implementations must fulfill. These are pure Java interfaces and value types.

---

## Allowed Imports

```
✅ com.example.demo.usecase.ports.in.Input   (marker interface for input records)
✅ com.example.demo.usecase.ports.out.entity.* (entity types in repository interfaces)
✅ org.springframework.data.jpa.repository.JpaRepository  (for write repositories only)
✅ jakarta.persistence.*                     (only in entity classes — @Entity, @Id, @Table)
✅ java.*                                    (standard library)
```

## Forbidden Imports

```
❌ org.springframework.*                     (except JpaRepository and JPA annotations above)
❌ com.example.demo.adapter.*                (output ports must not know about adapter impls)
❌ com.example.demo.framework.*              (no framework internals)
```

---

## Gateway Port Constraints

- **Interface** + **Input record** + **Output record** always defined together in the same package
- Input and Output are **records** — immutable, no setters
- Interface has exactly **one method**: `execute(XxxInput)`
- Naming: `XxxGateway` / `XxxInput` / `XxxOutput` — always paired

## Repository Port Constraints

- `XxxQueryRepository` (read) — plain interface, methods return `List<XxxJpaEntity>` or `Optional<XxxJpaEntity>`
- `XxxRepository` (write) — extends `JpaRepository<XxxJpaEntity, IdType>`; no extra methods unless necessary

## Entity Constraints

- Class suffix must be `JpaEntity` (e.g., `CityJpaEntity`)
- Annotated with `@Entity` and `@Id` — minimal JPA annotations
- Must have a no-arg constructor (JPA requirement)
- Plain getters/setters — no Lombok

---

## Pattern References

- Gateway interface + I/O: `references/patterns/gateway-interface.md`
- Repository interface: `references/patterns/repository-interface.md`
- JPA entity: `references/patterns/jpa-entity.md`
