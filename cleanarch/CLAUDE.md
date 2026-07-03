# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Build
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Format code (must pass before commit)
./mvnw spotless:apply

# Check formatting without applying
./mvnw spotless:check

# Run OpenRewrite modernization recipes
./mvnw rewrite:run
```

**H2 Console:** Available at `http://localhost:8080/h2-console` when running locally (JDBC URL: `jdbc:h2:mem:primarydb` or `jdbc:h2:mem:secondarydb`).

## Architecture Overview

This is a **Clean Architecture** (Hexagonal/Ports-and-Adapters) Spring Boot application.

- **Spring Boot:** 3.5.10
- **Java:** 17
- **Databases:** H2 in-memory (dual datasource: `primarydb`, `secondarydb`)
- **ORM:** Spring Data JPA (writes) + JdbcClient (reads)
- **External services:** MinIO (video object storage)
- **MCP server:** Spring AI 1.1.x (`spring-ai-starter-mcp-server-webmvc`) — Streamable HTTP at `/mcp`
- **Resilience:** Resilience4j 2.3.0 (circuit breaker / rate limiter)
- **Architecture tests:** ArchUnit 1.4.1
- **Code format:** Spotless + Palantir Java Format (AOSP style)

### Layer Structure

```
com.example.demo/
├── adapter/in/                   # REST controllers (inbound adapters) — no controller/ subfolder
│   ├── dto/                      # Request/Response DTOs
│   ├── mapper/                   # Request → Input mappers
│   └── mcp/                      # MCP tool adapters (@Tool methods calling use cases)
├── adapter/out/repository/       # JdbcClient query implementations (outbound adapters)
├── adapter/out/gateway/          # External service implementations (outbound adapters)
├── usecase/ports/in/             # Use case interfaces + Input/Result types
│   ├── dto/                      # CityDto, VideoDto (use-case-level data transfer)
│   └── impl/                     # Use case implementations (no Spring annotations)
├── usecase/ports/out/repository/ # Repository port interfaces
├── usecase/ports/out/gateway/    # Gateway port interfaces + Input/Output types
├── usecase/ports/out/entity/     # JPA entities
├── domain/model/                 # Pure Java domain objects (records, no Spring deps)
├── framework/di/                 # Spring @Configuration classes and bean wiring
│   ├── config/                   # AppConfig, VideoConfig
│   └── dynamicdatasource/        # AbstractRoutingDataSource for dual-DB routing
├── exception/                    # BadRequestException, UserNotFoundException
├── util/
│   └── multiphaseterator/        # PhaseTaskIterator — fluent async task runner over phase lists
└── system/                       # Cross-cutting: AOP logging, HTTP filter, MDC context
```

### API Endpoints

| Method | Path | Controller | Use Case |
|--------|------|------------|----------|
| GET | `/api/v1/cities` | `CitiesController` | `FindCitiesUseCase` |
| GET | `/api/v1/videos` | `VideoController` | `ListVideosUseCase` |

### MCP Tools (Streamable HTTP at `/mcp`)

| Tool | Adapter | Use Case |
|------|---------|----------|
| `find_cities` | `CitiesToolAdapter` | `FindCitiesUseCase` |
| `list_videos` | `VideoToolAdapter` | `ListVideosUseCase` |

The MCP server (Spring AI) is a second inbound adapter: tool adapters in `adapter.in.mcp` reuse the same Request-DTO `validate()` → mapper → use case chain as controllers, and are wired (with the `ToolCallbackProvider`) in `framework/config/McpConfig`. Server settings live under `spring.ai.mcp.server.*` in `application.properties`. Register locally with `claude mcp add --transport http cleanarch http://localhost:8080/mcp`.

### Dependency Flow

`Controller → UseCase (interface) → Repository/Gateway (interfaces) → Implementations`

The domain and use case layers have **no Spring annotations** (`@Service`, `@Component`, etc.). All wiring happens in `framework/di/`.

> **Known violation:** `FindCitiesUseCaseImpl` currently imports `DataSourceContextHolder` and `DataSourceKey` from `framework.di.dynamicdatasource`. This breaks the dependency rule (usecase → framework). The routing decision should be pushed into the gateway layer instead.

### Key Architectural Patterns

**Dynamic Data Source Routing:** `DynamicRoutingDataSource` extends `AbstractRoutingDataSource` to route queries to PRIMARY or SECONDARY H2 databases at runtime. `DataSourceContextHolder` (ThreadLocal) holds the routing key per request. The use case calls `InlineRoutingGateway` first to decide which datasource to use.

**CQRS-style Data Access:**
- Reads: `JdbcClient` in repository implementations
- Writes: `JpaRepository` for insert/update/delete

**Use Case Contract Pattern:**
- All use cases have an interface with a single `execute(XxxInput)` method returning `XxxResult`
- `Input` and `Output` are marker interfaces
- `Result<T>` wraps `returnCode`, `errorMessage`, and `data`

**Structured Logging:** `LoggingAspect` (AOP) wraps all gateway calls to log duration/errors. `LoggingContext` uses ThreadLocal + MDC to propagate `requestId` and `userId` through the call stack.

**Request Validation Chain:** Controllers use a fluent monad-style pipeline instead of exception-first validation:
```java
new XxxRequest(...)
    .validate()                    // returns Optional<XxxRequest>
    .map(mapper::toInput)
    .map(useCase::execute)
    .map(mapper::toResponse)       // Response mapping inside the chain
    .orElseThrow(BadRequestException::new);
```

**PhaseTaskIterator (`util.multiphaseterator`):** Fluent builder for running an async task over a list of named phases and collecting typed `PhaseTaskOutput<K,V>` results. Supports sequential (`execute()`) and parallel fan-out (`executeParallel()`), phase skipping (`skipPhases()`), early-stop on failure (`stopEarly()`), sequential composition (`thenMap`, `thenMapAsync`), and parallel composition (`andMap`, `andMapAsync`). Used for multi-environment deployment pipelines or any N-phase async workflow. `Try<T>` (sealed interface) and `ApmConfigHolder` are companion utilities in the same package.

## Naming Conventions (enforced by ArchUnit tests)

| Layer | Suffix | Location |
|-------|--------|----------|
| REST controllers | `Controller` | `adapter.in` |
| MCP tool adapters | `ToolAdapter` | `adapter.in.mcp` |
| Use case interfaces | `UseCase` | `usecase.ports.in` |
| Use case implementations | `UseCaseImpl` | `usecase.ports.in.impl` |
| Repository interfaces | `Repository` | `usecase.ports.out.repository` |
| Repository implementations | `RepositoryImpl` | `adapter.out.repository` |
| Gateway interfaces | `Gateway` | `usecase.ports.out.gateway` |
| Gateway implementations | `GatewayImpl` | `adapter.out.gateway` |

## Dependency Constraints

- **No `@Autowired`** anywhere except test classes — use constructor injection
- **Domain layer** (`domain.model`): zero Spring framework imports
- **Application layer** (`usecase`): no `@Service`, `@Component`, `@Repository` annotations
- **Controllers**: no controller-to-controller dependencies

## Testing Conventions

- Controllers: `@WebMvcTest` + `@MockitoBean` for dependencies
- Use cases: `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`
- Architecture rules: `ArchunitRuleTest` validates naming and layer constraints — run this after any structural changes
- Integration: `DynamicDataSourceIntegrationTest` verifies datasource routing end-to-end

## Code Style

Spotless enforces **Palantir Java Format (AOSP style)**. Always run `./mvnw spotless:apply` before committing. JSON files are also auto-formatted.
