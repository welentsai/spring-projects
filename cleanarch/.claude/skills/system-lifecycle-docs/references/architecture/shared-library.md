# Reference: Shared & Common Library

## Purpose

A shared library prevents every service or module from reinventing the same utility code.
In a Clean Architecture project, shared utilities live in `util/` (project-level) or a dedicated
Maven module (multi-module / multi-service). They must be:

- **Framework-agnostic**: usable in domain and use case layers (no Spring imports)
- **Null-safe**: never throw NullPointerException
- **Immutable**: return new values, never mutate input
- **Well-tested**: 90%+ branch coverage via JUnit 5 + AssertJ

---

## Result Type Pattern

The most impactful shared type is a functional `Result<T>` — it replaces exceptions as the
primary error-signaling mechanism across use case boundaries:

```java
public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String code, String message) implements Result<T> {}

    static <T> Result<T> success(T value) { return new Success<>(value); }
    static <T> Result<T> failure(String code, String message) {
        return new Failure<>(code, message);
    }

    default boolean isSuccess() { return this instanceof Success<T>; }
    default boolean isFailure() { return this instanceof Failure<T>; }

    default T getValue() {
        if (this instanceof Success<T> s) return s.value();
        throw new NoSuchElementException("Result is a Failure");
    }

    default <U> Result<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T> s -> Result.success(mapper.apply(s.value()));
            case Failure<T> f -> Result.failure(f.code(), f.message());
        };
    }

    default <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        return switch (this) {
            case Success<T> s -> mapper.apply(s.value());
            case Failure<T> f -> Result.failure(f.code(), f.message());
        };
    }
}
```

Usage in use case:

```java
public Result<CityDto> findCity(String id) {
    return cityRepository.findById(id)
        .map(Result::success)
        .orElse(Result.failure("CITY_NOT_FOUND", "City %s not found".formatted(id)));
}
```

---

## StringUtils

```java
public final class StringUtils {
    private StringUtils() {}

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String truncate(String s, int maxLength) {
        if (s == null) return "";
        return s.length() <= maxLength ? s : s.substring(0, maxLength);
    }

    public static String toSnakeCase(String camelCase) {
        if (isBlank(camelCase)) return "";
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    public static Optional<String> toOptional(String s) {
        return isBlank(s) ? Optional.empty() : Optional.of(s.trim());
    }
}
```

---

## CollectionUtils

```java
public final class CollectionUtils {
    private CollectionUtils() {}

    public static <T> boolean isEmpty(Collection<T> c) {
        return c == null || c.isEmpty();
    }

    public static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? List.of() : list;
    }

    public static <T> Optional<T> findFirst(Collection<T> c, Predicate<T> predicate) {
        if (isEmpty(c)) return Optional.empty();
        return c.stream().filter(predicate).findFirst();
    }

    public static <T, K, V> Map<K, V> toMap(
            Collection<T> c, Function<T, K> keyMapper, Function<T, V> valueMapper) {
        if (isEmpty(c)) return Map.of();
        return c.stream().collect(Collectors.toMap(keyMapper, valueMapper));
    }
}
```

---

## Pagination Wrapper

```java
public record PageResult<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    public static <T> PageResult<T> empty(int pageNumber, int pageSize) {
        return new PageResult<>(List.of(), pageNumber, pageSize, 0, 0);
    }

    public boolean hasContent() { return !content.isEmpty(); }
}
```

---

## HTTP Response Wrapper

Standardize all API responses with a consistent envelope:

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String traceId
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, MDC.get("traceId"));
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, MDC.get("traceId"));
    }
}
```

---

## Validation Utilities

```java
public final class ValidationUtils {
    private ValidationUtils() {}

    public static void requireNonBlank(String value, String fieldName) {
        if (StringUtils.isBlank(value))
            throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    public static <T> void requireNonNull(T value, String fieldName) {
        if (value == null)
            throw new IllegalArgumentException(fieldName + " must not be null");
    }

    public static void requirePositive(long value, String fieldName) {
        if (value <= 0)
            throw new IllegalArgumentException(fieldName + " must be positive");
    }
}
```

---

## Project Structure for Shared Utilities

**Single-module project** — place in `src/main/java/.../util/`:
```
util/
├── StringUtils.java
├── CollectionUtils.java
├── ValidationUtils.java
├── result/
│   └── Result.java
├── pagination/
│   └── PageResult.java
└── http/
    └── ApiResponse.java
```

**Multi-module project** — extract to a `common-lib` Maven module:
```
my-project/
├── common-lib/          ← shared Maven module
│   └── src/main/java/com/example/common/
└── service-a/
    └── pom.xml          ← depends on common-lib
```

---

## Testing Standard

Every utility class needs a dedicated test class:

```java
class StringUtilsTest {

    @Test
    void truncate_shouldReturnEmptyForNull() {
        assertThat(StringUtils.truncate(null, 10)).isEqualTo("");
    }

    @Test
    void truncate_shouldTruncateLongStrings() {
        assertThat(StringUtils.truncate("Hello World", 5)).isEqualTo("Hello");
    }

    @Test
    void truncate_shouldReturnFullStringWhenWithinLimit() {
        assertThat(StringUtils.truncate("Hi", 10)).isEqualTo("Hi");
    }
}
```

---

## Doc Template Sections

When generating `docs/shared-library.md`, include:
1. Location of shared utilities in this project (`util/` package or separate module)
2. Result type — show actual implementation and usage example from the codebase
3. Table of all utility classes with one-line descriptions
4. Key patterns: null safety, immutability, Java 17 features used (records, sealed, pattern matching)
5. How to add a new utility (checklist: interface design → implementation → tests → doc)
6. Testing conventions with example
