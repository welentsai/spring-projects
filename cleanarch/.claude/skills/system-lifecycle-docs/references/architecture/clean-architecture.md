# Reference: Clean Architecture

## Core Concept

Clean Architecture (Hexagonal / Ports-and-Adapters) separates the system into concentric layers
where **inner layers know nothing about outer layers**. Business logic lives at the center and is
completely independent of frameworks, databases, and delivery mechanisms.

```
┌─────────────────────────────────────────┐
│           Adapter (in/out)              │  ← Spring MVC, JPA, MinIO, HTTP clients
│   ┌─────────────────────────────────┐   │
│   │         Use Case Layer          │   │  ← Business rules, orchestration
│   │   ┌─────────────────────────┐   │   │
│   │   │      Domain Layer       │   │   │  ← Pure Java, zero framework deps
│   │   └─────────────────────────┘   │   │
│   └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

**The golden rule:** dependencies point inward only. Domain never imports UseCase. UseCase never
imports Adapter. All wiring happens in the framework/DI layer.

---

## Standard Package Structure

```
com.example.demo/
├── adapter/
│   ├── in/                        # Inbound adapters (REST controllers)
│   │   ├── dto/                   # Request/Response DTOs
│   │   └── mapper/                # Request → UseCase Input mappers
│   └── out/
│       ├── repository/            # JdbcClient read implementations
│       └── gateway/               # External service implementations
├── usecase/
│   └── ports/
│       ├── in/                    # Use case interfaces + Input/Result types
│       │   ├── dto/               # Cross-layer DTOs (CityDto, VideoDto)
│       │   └── impl/              # Use case implementations (NO Spring annotations)
│       └── out/
│           ├── repository/        # Repository port interfaces
│           ├── gateway/           # Gateway port interfaces + I/O types
│           └── entity/            # JPA entities
├── domain/
│   └── model/                     # Pure Java records/classes — zero Spring imports
├── framework/
│   └── di/                        # @Configuration, bean wiring only
│       ├── config/
│       └── dynamicdatasource/
├── exception/                     # Domain exceptions
├── util/                          # Shared utilities
└── system/                        # Cross-cutting: AOP, filters, MDC
```

---

## Naming Conventions (ArchUnit enforced)

| Layer | Suffix | Package |
|-------|--------|---------|
| REST controllers | `Controller` | `adapter.in` |
| Use case interfaces | `UseCase` | `usecase.ports.in` |
| Use case implementations | `UseCaseImpl` | `usecase.ports.in.impl` |
| Repository interfaces | `Repository` | `usecase.ports.out.repository` |
| Repository implementations | `RepositoryImpl` | `adapter.out.repository` |
| Gateway interfaces | `Gateway` | `usecase.ports.out.gateway` |
| Gateway implementations | `GatewayImpl` | `adapter.out.gateway` |

---

## Use Case Contract Pattern

Every use case follows this contract — one interface, one `execute` method, typed I/O:

```java
// Port interface — lives in usecase.ports.in
public interface FindCitiesUseCase {
    FindCitiesResult execute(FindCitiesInput input);
}

// Input/Result are marker interfaces for type safety
public record FindCitiesInput(String region) implements Input {}
public record FindCitiesResult(List<CityDto> cities) implements Result {}
```

Implementation in `usecase.ports.in.impl` — **no Spring annotations**:

```java
public class FindCitiesUseCaseImpl implements FindCitiesUseCase {
    private final CityRepository cityRepository;
    // Constructor injection only — wiring happens in framework/di
    public FindCitiesUseCaseImpl(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }
    @Override
    public FindCitiesResult execute(FindCitiesInput input) { ... }
}
```

Wiring in `framework/di/config/AppConfig.java`:

```java
@Configuration
public class AppConfig {
    @Bean
    public FindCitiesUseCase findCitiesUseCase(CityRepository cityRepository) {
        return new FindCitiesUseCaseImpl(cityRepository);
    }
}
```

---

## Request Validation Chain (Monad Pipeline)

Controllers use a fluent pipeline instead of scattered try/catch:

```java
@GetMapping("/api/v1/cities")
public ResponseEntity<CitiesResponse> getCities(@RequestParam String region) {
    return new FindCitiesRequest(region)
        .validate()                    // Optional<FindCitiesRequest>
        .map(mapper::toInput)          // FindCitiesInput
        .map(findCitiesUseCase::execute)  // FindCitiesResult
        .map(mapper::toResponse)       // CitiesResponse
        .map(ResponseEntity::ok)
        .orElseThrow(BadRequestException::new);
}
```

---

## CQRS Data Access Pattern

- **Reads:** `JdbcClient` in `RepositoryImpl` — optimized SQL, no ORM overhead
- **Writes:** `JpaRepository` — entity lifecycle management

```java
// Read side — adapter.out.repository
@Override
public List<CityDto> findByRegion(String region) {
    return jdbcClient.sql("SELECT id, name, region FROM cities WHERE region = :region")
        .param("region", region)
        .query(CityDto.class)
        .list();
}
```

---

## Dependency Rules (ArchUnit Tests)

Add to `ArchunitRuleTest.java` to enforce constraints automatically:

```java
@ArchTest
static final ArchRule domainHasNoSpringDependency =
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("org.springframework..");

@ArchTest
static final ArchRule usecaseHasNoSpringAnnotations =
    noClasses().that().resideInAPackage("..usecase..impl..")
        .should().beAnnotatedWith(Service.class)
        .orShould().beAnnotatedWith(Component.class);

@ArchTest
static final ArchRule adaptersDoNotDependOnEachOther =
    noClasses().that().resideInAPackage("..adapter.in..")
        .should().dependOnClassesThat()
        .resideInAPackage("..adapter.out..");
```

---

## Common Violations & Fixes

| Violation | Symptom | Fix |
|-----------|---------|-----|
| UseCase imports framework class | `FindCitiesUseCaseImpl` imports `DataSourceContextHolder` | Push routing decision into a Gateway |
| Controller logic | Business rules inside `@RestController` | Extract to use case |
| Entity in use case return | JPA `@Entity` returned from use case | Map to DTO/domain object |
| `@Autowired` in use case | Framework coupling | Use constructor injection via DI config |

---

## Doc Template Sections

When generating `docs/clean-architecture.md`, include:
1. Architecture diagram (ASCII or Mermaid)
2. Actual package structure from the codebase
3. Real use case example (pick the simplest existing use case)
4. Naming convention table with real class examples
5. ArchUnit rules currently in place
6. Known violations (be honest — list them with remediation plan)
7. "Adding a new feature" step-by-step guide
