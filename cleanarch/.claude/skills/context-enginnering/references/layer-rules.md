# Clean Architecture Layer Rules (project-specific)

> These rules reflect what `ArchunitRuleTest` and the package CLAUDE.md files actually enforce
> in this repo. Where they differ from textbook Clean Architecture, the project rule wins.

## Dependency Direction

```
adapter → usecase → domain        framework → (everything, wiring only)
```

Each layer may only depend on layers **inner** to it. Never the reverse.

---

## Domain Layer Rules (`domain.model`)

✅ Allowed:
- Plain Java records / classes, enums, value objects
- Pure logic, no I/O

❌ Forbidden:
- Any `org.springframework.*` import
- Any `jakarta.persistence.*` annotation
- Any reference to usecase, adapter, or framework classes

---

## UseCase Layer Rules (`usecase.ports.*`)

✅ Allowed:
- Imports from `domain` layer
- Port interfaces: `ports.in` (`XxxUseCase`) and `ports.out` (`XxxRepository`, `XxxGateway`)
- `XxxInput` / `XxxResult` / `XxxOutput` records, `Input`/`Output` marker interfaces, `Result<T>`
- JPA entities in `usecase.ports.out.entity` — **deliberate project choice** (textbook Clean
  Architecture would put them in the adapter layer; here `CityJpaEntity` lives with the ports)
- `util.multiphaseterator` (only `usecase` and `framework` may use it)

❌ Forbidden:
- **Any Spring stereotype annotation** — no `@Service`, `@Component`, `@Repository`,
  `@Transactional`. All wiring happens in `framework/di/BeanInjection` (this is stricter than
  generic Clean Architecture advice: do NOT suggest `@Service` on a UseCaseImpl)
- Any reference to `adapter` or `framework` classes
- HTTP-specific classes (`HttpServletRequest`, `ResponseEntity`, etc.)
- Inside `PhaseTaskIterator` map lambdas (`thenMap`/`andMap`/async variants): calling action
  gateways, directly or via private helpers — only read-style gateways whose names start with
  `Inquire`/`Get` are allowed there

> **Known violation (documented, to be fixed):** `FindCitiesUseCaseImpl` imports
> `DataSourceContextHolder`/`DataSourceKey` from `framework.di.dynamicdatasource`.
> Do not copy this pattern into new use cases; the routing decision belongs in a gateway.

---

## Adapter Layer Rules (`adapter.in`, `adapter.out`)

✅ Allowed:
- Spring MVC (`adapter.in`), Spring AI `@Tool` (`adapter.in.mcp`), `JdbcClient`
  (`adapter.out.repository`), external clients like MinIO (`adapter.out.gateway`)
- Depending on `usecase.ports.in` (inbound) or implementing `usecase.ports.out` (outbound)

❌ Forbidden:
- Business logic (belongs in domain or usecase)
- `adapter.in` (controllers AND tool adapters) depending on `adapter.out`, `usecase.ports.out`,
  `usecase.ports.in.impl`, `framework`, or `domain`
- Inbound adapters depending on each other (no Controller↔ToolAdapter or Controller↔Controller)
- `@Autowired` — constructor injection only (everywhere except tests)
- Defining exception classes outside the top-level `exception` package

### MCP Inbound Adapters (`adapter.in.mcp`)

- Class name must end with `ToolAdapter` (ArchUnit-enforced)
- Reuse the same pipeline as controllers:
  `new XxxRequest(...).validate().map(Mapper::toInput).map(useCase::execute).map(Mapper::toResponse).orElseThrow(BadRequestException::new)`
- Not annotated with Spring stereotypes — wired as beans (with the `ToolCallbackProvider`)
  in `framework/config/McpConfig`

---

## Framework Layer Rules (`framework`)

- Composition root: `framework/di/BeanInjection` wires use cases; `framework/config/*` wires
  MCP, MinIO, datasources; `@Bean` methods return the **interface** type
- `DemoGlobalExceptionHandler` is the only global exception handler
- Only `framework` and `usecase` may depend on `util.multiphaseterator`

---

## Common Violation Patterns to Detect

| Violation | Example | Fix |
|-----------|---------|-----|
| Spring annotation in usecase | `@Service` on `XxxUseCaseImpl` | Remove; wire in `framework/di/BeanInjection` |
| Domain imports Spring | `@Component` on `City` | Remove — domain is framework-free |
| UseCase imports adapter/framework | `import ...framework.di.dynamicdatasource.*` | Push behind a gateway port (see known violation) |
| Controller bypasses ports | Controller injects `CitiesQueryRepository` | Inject the `XxxUseCase` interface instead |
| ToolAdapter reaches too far | `CitiesToolAdapter` imports `domain.model.City` | Only use `adapter.in` DTOs + `usecase.ports.in` |
| Adapter has business logic | `if (city.population > 1_000_000)` in Controller | Move to domain or use case |
| UseCase returns HTTP response | `ResponseEntity<>` return type | Return `XxxResult`; adapter maps to Response DTO |
| Exception outside `exception` pkg | `adapter.in.SomeException` | Move to top-level `exception` package |
| Action gateway in iterator lambda | `thenMap(x -> putLogGateway.execute(...))` | Only `Inquire*`/`Get*` gateways inside iterator lambdas |
