---
name: shared-lib-designer
description: >
  Design and generate production-ready shared utility library code for Spring Boot 3.5.3 / Java 17 teams.
  Use this skill whenever the user wants to: create shared utility classes (string, collection, validation,
  result types, HTTP wrappers, pagination), design fluent or functional APIs, generate utility code inspired
  by Apache Commons or Google Guava, write utility unit tests with JUnit 5 + AssertJ, or build a standalone
  Java library. Trigger even if the user says "help me write a utility class", "design a helper", "create a
  shared lib", "how should I handle errors functionally", or asks about fluent API design in Java. Always
  use this skill when the question is about shared library design, utility classes, or functional patterns
  in a Java / Spring Boot context.
---

# Shared Library Designer

You are helping design and generate a **standalone shared utility library** for Java 17 / Spring Boot 3.5.3
teams. The library should feel as natural and composable as the Java Stream API, take direct inspiration from
Apache Commons and Google Guava, and fully embrace Java 17 language features.

---

## Core Design Philosophy

### 1. Fluent API & Method Chaining
Support method chaining wherever it makes conceptual sense. The goal is readability — a chain should read
like a sentence. Think `StringUtils.of(value).trim().truncate(50).toLowerCase()` rather than a bag of static
methods the caller has to nest.

### 2. Null Safety First
Every operation must be null-safe. **Never throw `NullPointerException` from a utility method.** Follow
Apache Commons' discipline: return empty, identity, or wrapped results for null inputs. Annotate nullable
parameters with `@Nullable` (jakarta.annotation).

### 3. Functional Composition
Expose `Function<T,R>`, `Predicate<T>`, and `Consumer<T>` variants so utilities compose naturally with
`Stream`. A good test: can every method in a utility class be used as a method reference?

```java
list.stream()
    .filter(StringUtils::isNotBlank)
    .map(StringUtils::trimToEmpty)
    .collect(toList());
```

### 4. Immutability via Java 17 Records
Use `record` for all value and result types. No mutable state in utility classes. Prefer
`Collections.unmodifiableList` or `List.copyOf` when returning collections.

### 5. Sealed Interfaces for Sum Types
Model outcomes with a finite set of cases using `sealed interface`. Pattern matching with `switch` then
exhaustively covers every case at the call site — no `instanceof` chains, no casting.

### 6. Pure Java — No Spring Annotations
Utility classes must have **zero Spring annotations** (`@Component`, `@Service`, etc.). They should be
usable in any Java context, not just Spring applications.

---

## Utility Categories

Read `references/utility-patterns.md` for detailed implementation templates for each category. Here is the
API surface summary:

### A. `Result<T>` — Functional Error Handling

Model operation outcomes as a sealed type instead of throwing exceptions.

```java
public sealed interface Result<T> permits Result.Success, Result.Failure {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String errorCode, String message, @Nullable Throwable cause) implements Result<T> {}

    static <T> Result<T> success(T value);
    static <T> Result<T> failure(String errorCode, String message);
    static <T> Result<T> of(Supplier<T> supplier);          // catches any exception → Failure

    boolean isSuccess();
    boolean isFailure();
    T getOrElse(T fallback);
    T getOrThrow();                                          // throws ResultException on Failure
    <U> Result<U> map(Function<T, U> mapper);
    <U> Result<U> flatMap(Function<T, Result<U>> mapper);
    Result<T> onSuccess(Consumer<T> action);
    Result<T> onFailure(Consumer<Failure<T>> action);
    Optional<T> toOptional();
}
```

**Guava parallel:** analogous to `ListenableFuture` callbacks; **Commons parallel:** none — this is the
modern replacement for checked-exception-heavy APIs.

---

### B. `StringUtils` — Null-Safe Fluent Strings

Null-safe static methods (usable as Stream method references) **plus** a chainable `StringWrapper`.

```java
// Static (null-safe, Stream-friendly)
StringUtils.isBlank(s)
StringUtils.isNotBlank(s)
StringUtils.trimToEmpty(s)       // null → ""
StringUtils.trimToNull(s)        // blank → null
StringUtils.defaultIfBlank(s, fallback)
StringUtils.toOptional(s)        // blank/null → Optional.empty()
StringUtils.truncate(s, max)
StringUtils.capitalize(s)

// Fluent wrapper
StringUtils.of("  Hello World  ")
    .trim()
    .truncate(5)
    .toLowerCase()
    .orElse("")                   // → "hello"
```

---

### C. `CollectionUtils` — Null-Safe Collection Helpers

```java
CollectionUtils.isEmpty(c)             // null-safe
CollectionUtils.isNotEmpty(c)
CollectionUtils.emptyIfNull(list)      // null → List.of()
CollectionUtils.first(list)            // → Optional<T>
CollectionUtils.last(list)             // → Optional<T>
CollectionUtils.findFirst(list, pred)  // → Optional<T>
CollectionUtils.partition(list, size)  // Guava Lists.partition equivalent
CollectionUtils.transform(list, fn)    // null-safe map
CollectionUtils.filter(list, pred)     // null-safe filter
CollectionUtils.groupBy(list, fn)      // → Map<K, List<T>>
CollectionUtils.toUnmodifiable(list)   // defensive copy + lock
```

---

### D. `Validator<T>` — Fluent Validation Chain

Collects all errors before returning; no fail-fast exception throwing.

```java
Result<CreateUserRequest> result = Validator.of(request)
    .notNull("request")
    .matches(r -> r.email().contains("@"), "INVALID_EMAIL", "Email must be valid")
    .matches(r -> r.age() >= 18, "UNDERAGE", "User must be 18 or older")
    .when(request.isPremium(), r -> r.paymentToken() != null,
          "MISSING_TOKEN", "Premium accounts require a payment token")
    .validate();                   // → Result<T> with all errors in Failure

// Or throw immediately with all errors aggregated
CreateUserRequest valid = Validator.of(request)
    .notNull("request")
    .matches(...)
    .getOrThrow();                 // throws ValidationException(List<ValidationError>)
```

```java
public record ValidationError(String field, String code, String message) {}
```

---

### E. `PageResult<T>` — Pagination Wrapper

```java
public record PageResult<T>(
    List<T> content, int page, int size, long totalElements, int totalPages
) {
    static <T> PageResult<T> of(List<T> content, int page, int size, long total);
    static <T> PageResult<T> empty(int page, int size);

    boolean hasNext();
    boolean hasPrevious();
    boolean isEmpty();
    <R> PageResult<R> map(Function<T, R> mapper);   // transform content, preserve metadata
}
```

---

### F. `HttpClientUtils` — Fluent HTTP with Result Error Handling

Thin wrapper over Spring Boot 3.5's `RestClient`. Returns `Result<T>` — never throws.

```java
Result<UserDto> result = HttpClientUtils.get("https://api.example.com/users/{id}")
    .pathVariable("id", userId)
    .header("X-Correlation-Id", correlationId)
    .bearerToken(token)
    .timeout(Duration.ofSeconds(5))
    .execute(UserDto.class);

result.onFailure(f -> log.warn("HTTP call failed [{}]: {}", f.errorCode(), f.message()))
      .map(UserDto::toUser);
```

---

## Code Generation Process

When asked to generate a utility class, follow this sequence:

### Step 1 — Identify the category
Map the request to one or more categories above (A–F). If the request spans multiple categories, generate
them in dependency order (e.g., `Result` before `Validator`).

### Step 2 — Show the public API first
Present the interface or class with method signatures and brief Javadoc. Ask the user to confirm before
writing the full implementation. This prevents wasted work on the wrong design.

### Step 3 — Generate the implementation
Apply these Java 17 idioms:
- `record` for value types and result carriers
- `sealed interface` + `permits` for sum types; use pattern matching `switch` at call sites
- Pattern matching `instanceof` (`if (obj instanceof Foo f)`) — no raw casts
- `switch` expressions with arrow syntax (`case X -> ...`)
- `Optional` for single nullable return values; never return `null` from a public method
- `@Nullable` on all parameters that accept null (jakarta.annotation)

### Step 4 — Generate unit tests
For every public method generate at minimum:
- Happy path
- Null input (should never throw NPE)
- Empty / boundary value

Use JUnit 5 + AssertJ. Prefer `@ParameterizedTest` for exhaustive value coverage.

```java
@Test
void trimToEmpty_returnsEmpty_whenInputIsNull() {
    assertThat(StringUtils.trimToEmpty(null)).isEmpty();
}

@Test
void fluentChain_lowercasesAndTruncates() {
    String result = StringUtils.of("  HELLO WORLD  ")
        .trim().toLowerCase().truncate(5).orElse("");
    assertThat(result).isEqualTo("hello");
}

@ParameterizedTest
@NullAndEmptySource
@ValueSource(strings = {"  ", "\t"})
void isBlank_returnsTrueForBlankInputs(String input) {
    assertThat(StringUtils.isBlank(input)).isTrue();
}
```

### Step 5 — Provide `pom.xml` snippet
Include the minimal Maven coordinates needed to use the generated code:

```xml
<!-- Core utility (no Spring needed unless HttpClientUtils) -->
<dependency>
    <groupId>jakarta.annotation</groupId>
    <artifactId>jakarta.annotation-api</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- Test -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Guava & Apache Commons Reference Map

When designing or reviewing utility methods, cite the reference library equivalent so the team
understands the lineage:

| Our API | Guava equivalent | Apache Commons equivalent |
|---|---|---|
| `StringUtils.isBlank()` | `Strings.isNullOrEmpty()` | `StringUtils.isBlank()` |
| `StringUtils.trimToEmpty()` | — | `StringUtils.trimToEmpty()` |
| `StringUtils.defaultIfBlank()` | `MoreObjects.firstNonNull()` | `StringUtils.defaultIfBlank()` |
| `CollectionUtils.partition()` | `Lists.partition()` | `ListUtils.partition()` |
| `CollectionUtils.emptyIfNull()` | — | `CollectionUtils.emptyIfNull()` |
| `CollectionUtils.first()` | `Iterables.getFirst()` | `CollectionUtils.get()` |
| `Validator.notNull()` | `Preconditions.checkNotNull()` | `Validate.notNull()` |
| `Result<T>` | (Guava `ListenableFuture` for async) | — |

---

## Recommended Package Structure

```
com.yourteam.shared/
├── result/
│   └── Result.java                 # sealed Result<T>
├── util/
│   ├── StringUtils.java
│   ├── CollectionUtils.java
│   └── ObjectUtils.java
├── validation/
│   ├── Validator.java
│   └── ValidationError.java
├── pagination/
│   └── PageResult.java
└── http/
    └── HttpClientUtils.java
```

Place tests in `src/test/java/` mirroring the above structure, one test class per utility class.

---

## Quality Checklist

Before delivering any code, verify:
- [ ] All public methods handle null inputs without throwing `NullPointerException`
- [ ] Java 17 features used where idiomatic (records, sealed, pattern matching, switch expressions)
- [ ] Fluent API supports method chaining where it aids readability
- [ ] No Spring `@Component` / `@Service` / `@Repository` annotations (pure Java)
- [ ] `@Nullable` annotates every parameter that accepts null
- [ ] Unit tests cover null, empty, and at least one happy-path scenario per method
- [ ] Apache Commons / Guava reference cited in Javadoc where relevant
