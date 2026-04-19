# Reference: API-First Design

## Core Principle

API-First means the **API contract is designed and agreed upon before implementation begins**.
The OpenAPI spec is the source of truth — not the code. This prevents ad-hoc API drift,
enables parallel frontend/backend development, and makes breaking changes visible in code review.

---

## Spring Boot Setup (SpringDoc OpenAPI)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

```yaml
# application.yml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
  default-produces-media-type: application/json
```

Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/api-docs`

---

## API Versioning Strategy

Use **URL path versioning** — it's explicit, cacheable, and gateway-friendly:

```
/api/v1/cities     ← current stable
/api/v2/cities     ← new version (breaking changes)
```

Avoid header versioning (`Accept: application/vnd.api.v2+json`) — it's invisible in browser
tooling and harder to route in K8s ingress/API gateway.

**Breaking change policy:**
- New optional fields → same version (non-breaking)
- New required fields, renamed fields, changed types, removed fields → bump version
- Run both versions in parallel during deprecation window (minimum 1 sprint)

---

## Standard Error Response

All error responses must follow a single envelope — never let Spring's default `/error` response
leak to clients:

```java
public record ErrorResponse(
    String timestamp,    // ISO-8601
    int status,
    String error,        // HTTP reason phrase
    String message,      // Human-readable, safe to display
    String path,         // Request URI
    String traceId       // MDC trace ID for log correlation
) {}
```

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Field 'region' must not be blank",
  "path": "/api/v1/cities",
  "traceId": "abc123def456"
}
```

Global exception handler:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            Instant.now().toString(), 400, "Bad Request",
            ex.getMessage(), request.getRequestURI(),
            MDC.get("traceId")
        ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(new ErrorResponse(
            Instant.now().toString(), 404, "Not Found",
            ex.getMessage(), request.getRequestURI(),
            MDC.get("traceId")
        ));
    }
}
```

---

## Pagination Standard

All list endpoints that can return more than 20 items must support pagination:

```
GET /api/v1/cities?page=0&size=20&sort=name,asc
```

Response envelope:

```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

Use Spring's `Pageable` and `Page<T>` — they serialize to this format automatically with
`SpringDataWebAutoConfiguration`.

---

## Controller Annotation Conventions

```java
@RestController
@RequestMapping("/api/v1/cities")
@Tag(name = "Cities", description = "City search operations")
public class CitiesController {

    @GetMapping
    @Operation(summary = "List cities by region")
    @ApiResponse(responseCode = "200", description = "Successful")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    public ResponseEntity<CitiesResponse> getCities(
            @RequestParam @NotBlank @Parameter(description = "Region code") String region) {
        ...
    }
}
```

---

## Request Validation

Use Bean Validation (Jakarta) on DTOs — never validate manually in controller logic:

```java
public record FindCitiesRequest(
    @NotBlank(message = "region must not be blank")
    @Size(max = 50, message = "region must not exceed 50 characters")
    String region
) implements Input {
    public Optional<FindCitiesRequest> validate() {
        return StringUtils.hasText(region) ? Optional.of(this) : Optional.empty();
    }
}
```

Enable `@Validated` at the controller level to trigger constraint violations as `MethodArgumentNotValidException`.

---

## HTTP Method Semantics

| Method | Use | Idempotent | Body |
|--------|-----|-----------|------|
| GET | Read | Yes | No |
| POST | Create | No | Yes |
| PUT | Full replace | Yes | Yes |
| PATCH | Partial update | No | Yes |
| DELETE | Remove | Yes | No |

Never use GET with a body. Never use POST for idempotent operations if PUT is appropriate.

---

## Doc Template Sections

When generating `docs/api-first-design.md`, include:
1. Current API inventory (table of all endpoints from controllers)
2. Swagger UI URL and API-docs URL
3. Error response format with real examples
4. Versioning policy and how to add a new version
5. Validation approach (Bean Validation + monad pipeline)
6. Pagination format if any list endpoints exist
7. How to add a new endpoint (step-by-step)
