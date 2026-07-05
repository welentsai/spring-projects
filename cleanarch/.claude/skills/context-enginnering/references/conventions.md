# Naming Conventions

> Populated from an actual codebase scan (2026-07-05). Re-run the scan and refresh this file
> whenever packages are added/renamed — see Refresh Mode in SKILL.md.

---

## Package Structure (as-built)

```
com.example.demo/
├── adapter/
│   ├── in/                       ← @RestController classes (flat — no controller/ or web/ subfolder)
│   │   ├── dto/                  ← XxxRequest / XxxResponse records
│   │   ├── mapper/               ← XxxRequestMapper static utility classes
│   │   └── mcp/                  ← MCP tool adapters (@Tool methods, Spring AI)
│   └── out/
│       ├── gateway/              ← XxxGatewayImpl (external services: MinIO, routing decision)
│       └── repository/           ← XxxRepositoryImpl (JdbcClient reads)
├── usecase/
│   └── ports/
│       ├── in/                   ← XxxUseCase interfaces + XxxInput / XxxResult + Input/Output/Result markers
│       │   ├── dto/              ← CityDto, VideoDto (use-case-level DTOs)
│       │   └── impl/             ← XxxUseCaseImpl (no Spring annotations)
│       └── out/
│           ├── entity/           ← JPA entities (CityJpaEntity) — deliberate project choice
│           ├── gateway/          ← XxxGateway interfaces + XxxInput / XxxOutput records
│           └── repository/       ← XxxRepository / XxxQueryRepository interfaces
├── domain/model/                 ← Pure Java records (City) — zero framework imports
├── framework/
│   ├── DemoGlobalExceptionHandler
│   ├── config/                   ← AppConfig, McpConfig, VideoConfig
│   └── di/                       ← BeanInjection (composition root)
│       └── dynamicdatasource/    ← DynamicRoutingDataSource + ThreadLocal context holder
├── exception/                    ← ALL custom exceptions live here (BadRequestException, …)
├── system/                       ← LoggingAspect (AOP), LoggingContext (MDC), request logging filter
└── util/multiphaseterator/       ← PhaseTaskIterator, PhaseTaskOutput, Try, ApmConfigHolder
```

Note: the Spring Boot main class `DemoApplication` sits in `com.example` (one level above `demo`).

---

## Naming Patterns (as-built, enforced by ArchunitRuleTest)

| Concept | Pattern | Example |
|---------|---------|---------|
| Input Port (use case) | `{Feature}UseCase` | `FindCitiesUseCase` |
| Use Case Impl | `{Feature}UseCaseImpl` (NOT `Interactor`) | `FindCitiesUseCaseImpl` |
| Use Case Input / Result | `{Feature}Input` / `{Feature}Result` | `FindCitiesInput`, `FindCitiesResult` |
| Repository Port | `{Entity}Repository` (writes) / `{Entity}QueryRepository` (reads) | `CitiesRepository`, `CitiesQueryRepository` |
| Repository Impl | `{Entity}QueryRepositoryImpl` | `CitiesQueryRepositoryImpl` |
| Gateway Port | `{Feature}Gateway` + `{Feature}Input` + `{Feature}Output` | `VideoStorageGateway` |
| Gateway Impl | `{Feature}GatewayImpl` | `VideoStorageGatewayImpl` |
| REST Controller | `{Domain}Controller` | `CitiesController` |
| MCP Tool Adapter | `{Domain}ToolAdapter` in `adapter.in.mcp` | `CitiesToolAdapter` |
| Request / Response DTO | `{Feature}Request` / `{Feature}Response` (records) | `FindCitiesRequest` |
| Mapper | `{Domain}RequestMapper` (static methods, not a bean) | `CitiesRequestMapper` |
| Persistence Entity | `{Entity}JpaEntity` in `usecase.ports.out.entity` | `CityJpaEntity` |
| Custom Exception | `{Name}Exception` in top-level `exception` package only | `BadRequestException` |

---

## Observed Patterns

- **Root package**: `com.example.demo` (main class in `com.example`)
- **Naming style**: ports & adapters with `UseCase`/`UseCaseImpl`, `Gateway`/`GatewayImpl`, `Repository`/`RepositoryImpl` suffix pairs; camelCase methods enforced by ArchUnit
- **DI style**: constructor injection only, `@Autowired` forbidden outside tests; use case beans wired in `framework/di/BeanInjection`, MCP tools in `framework/config/McpConfig`
- **Exception handling**: all custom exceptions in top-level `exception` package; `DemoGlobalExceptionHandler` (`framework`) handles them globally
- **Validation location**: adapter/in — `XxxRequest.validate()` returns `Optional<XxxRequest>`; controllers/tool adapters chain `validate().map(toInput).map(execute).map(toResponse).orElseThrow(BadRequestException::new)`
- **Mapper strategy**: manual static mappers (`XxxRequestMapper`), no MapStruct/ModelMapper
- **Data access**: CQRS-style — `JdbcClient` for reads (`adapter.out.repository`), `JpaRepository` for writes
- **MCP**: Spring AI streamable HTTP server at `/mcp`; tool adapters reuse the same Request/mapper/use-case chain as controllers
- **Test naming**: `{Class}Test` unit tests; `{Feature}IntegrationTest` for integration (`DynamicDataSourceIntegrationTest`, `RepositoryIntegrationTest`)
