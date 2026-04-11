# Clean Architecture Layer Rules

## Dependency Direction

```
adapter → usecase → domain
```

Each layer may only depend on layers **inner** to it. Never the reverse.

---

## Domain Layer Rules

✅ Allowed:
- Plain Java classes (POJO / Record)
- Domain exceptions (`extends RuntimeException`)
- Enums, Value Objects, Domain Events
- Domain Services (pure logic, no I/O)

❌ Forbidden:
- Any `org.springframework.*` import
- Any `javax.persistence.*` / `jakarta.persistence.*` annotation
- Any reference to usecase or adapter classes
- I/O operations (file, network, DB)

---

## UseCase Layer Rules

✅ Allowed:
- Imports from `domain` layer
- Port interfaces (input ports = what the use case exposes; output ports = what it needs)
- Command / Query objects (DTOs for use case input)
- `@Service`, `@Component` from Spring (acceptable for wiring)
- `@Transactional` is borderline — acceptable here if you prefer, or push to adapter

❌ Forbidden:
- Any reference to `adapter` layer classes
- HTTP-specific classes (`HttpServletRequest`, etc.)
- Persistence-specific annotations (`@Entity`, `@Column`)
- Framework-specific response objects

---

## Adapter Layer Rules

✅ Allowed:
- Everything — this is the integration layer
- Spring MVC, Spring Data, Spring Security
- External library clients (Kafka, Redis, S3, etc.)
- DTO mapping (adapter DTOs ↔ domain/usecase objects)

❌ Forbidden:
- Business logic (should live in domain or usecase)
- Direct domain mutation without going through a use case

---

## Common Violation Patterns to Detect

| Violation | Example | Fix |
|-----------|---------|-----|
| Domain uses JPA | `@Entity` on domain class | Move to adapter persistence model, map to domain |
| UseCase imports Controller | `import adapter.web.XController` | Never valid — reverse dependency |
| Adapter has business logic | `if (user.age > 18)` in Controller | Move to domain service or use case |
| Domain imports Spring | `@Component` on Entity | Remove — domain is framework-free |
| UseCase returns HTTP response | `ResponseEntity<>` return type | Return domain/DTO, let adapter wrap |
