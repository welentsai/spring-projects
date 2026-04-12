# Module Rules: usecase/ports/in (Use Case Interfaces & Implementations)

## Packages

| Package | Contents |
|---------|----------|
| `usecase.ports.in` | Use case interfaces, `Input`/`Output`/`Result<T>` base types, concrete `XxxInput`/`XxxResult` |
| `usecase.ports.in.dto` | Use-case-level DTOs (records) shared between use case and adapter response |
| `usecase.ports.in.impl` | Use case implementations (plain Java, no Spring) |

---

## Responsibility

Define what the application can do (interfaces) and implement the business workflows
(implementations). Orchestrate calls to domain objects and output ports (repositories, gateways).

---

## Allowed Imports (interfaces + DTOs)

```
✅ com.example.demo.usecase.ports.in.*        (sibling types — Input, Result, etc.)
✅ com.example.demo.usecase.ports.out.*       (output port interfaces — never impls)
✅ com.example.demo.domain.model.*            (domain objects)
✅ java.*                                     (standard library)
✅ com.fasterxml.jackson.*                    (only for @JsonPropertyOrder on Result types)
```

## Allowed Imports (impl only — additional)

```
✅ org.slf4j.Logger / LoggerFactory           (logging — SLF4J only, not Spring's)
```

## Forbidden Imports (everywhere in this module)

```
❌ org.springframework.*                      (no Spring annotations — no @Service, @Component)
❌ com.example.demo.adapter.*                 (never depend on adapter layer)
❌ com.example.demo.framework.*               (no framework internals — especially DataSourceContextHolder)
❌ jakarta.persistence.*                      (persistence belongs in entity/adapter layer)
```

---

## Constraints

- **Single `execute()` method** per use case interface — always named `execute`, always takes `XxxInput`
- **`XxxResult` always extends `Result<T>`** — never return raw data or throw checked exceptions from a use case
- **Use case impls have NO Spring annotations** — they are wired as `@Bean` in `framework/di/BeanInjection`
- **Always catch `Exception`** in impl and return `XxxResult.failure(...)` — no propagation
- **DTOs are records** in `usecase.ports.in.dto` — never mutable POJOs
- **Map entities to DTOs inside the impl** — JPA entities must not escape the use case boundary

---

## Pattern References

- Use case interface + Input + Result: `references/patterns/usecase-interface.md`
- Use case implementation: `references/patterns/usecase-impl.md`

---

## Checklist for New Use Case

- [ ] Interface `XxxUseCase` with single `execute(XxxInput)` method
- [ ] `XxxInput` class implementing `Input` marker
- [ ] `XxxResult` class extending `Result<List<XxxDto>>` (or `Result<XxxDto>`)
- [ ] DTO record `XxxDto` in `usecase.ports.in.dto`
- [ ] Implementation `XxxUseCaseImpl` — no Spring annotations
- [ ] Constructor injection for all dependencies
- [ ] `try/catch(Exception)` around business logic → `XxxResult.failure(...)`
- [ ] `@Bean` registration in `framework/di/BeanInjection`
- [ ] Test: `@ExtendWith(MockitoExtension.class)` + `@Mock` ports + `@InjectMocks` impl
