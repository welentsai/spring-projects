# Module Rules: domain/model (Domain Layer)

## Package

`domain.model` — pure Java domain objects: entities, value objects, domain services, domain events.

---

## Responsibility

Express the core business concepts of the system. This is the innermost layer — it knows nothing
about databases, HTTP, Spring, or any infrastructure concern.

---

## Allowed Imports

```
✅ java.*         (standard library only)
✅ Other domain model classes within this package
```

## Forbidden Imports — Absolutely

```
❌ org.springframework.*          (no Spring — zero)
❌ jakarta.persistence.*          (no JPA — use JpaEntity in usecase.ports.out.entity)
❌ com.example.demo.usecase.*     (no outward dependency)
❌ com.example.demo.adapter.*     (no outward dependency)
❌ com.example.demo.framework.*   (no outward dependency)
❌ Any external library           (Resilience4j, MinIO, Jackson, etc.)
```

---

## Constraints

- **Records for value objects** — prefer `record` unless mutable state or behavior is needed
- **Business invariants live here** — validation of domain rules (e.g., "an order can't be confirmed twice") belongs in domain classes, not in use cases
- **Domain exceptions** — may throw unchecked exceptions to signal domain rule violations (caught in use case)
- **Zero I/O** — no file, network, or database access ever
- **No static utility methods for business logic** — behavior belongs on domain objects, not in helpers

---

## Pattern Reference

See `references/patterns/domain-model.md` for record vs class examples.

---

## Checklist for New Domain Object

- [ ] Zero framework imports (verify with ArchUnit after adding)
- [ ] `record` if value object; `class` if behavior/mutability needed
- [ ] Business rules enforced in constructor or domain methods
- [ ] Test: plain JUnit5 unit test — no Spring context needed
