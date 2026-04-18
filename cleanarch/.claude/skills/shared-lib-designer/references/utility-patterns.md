# Utility Implementation Patterns

Full implementation templates for each utility category. Read the relevant section when generating code.

## Table of Contents
1. [Result\<T\>](#result)
2. [StringUtils](#stringutils)
3. [CollectionUtils](#collectionutils)
4. [Validator\<T\>](#validator)
5. [PageResult\<T\>](#pageresult)
6. [HttpClientUtils](#httpclientutils)

---

## Result<T> {#result}

```java
package com.yourteam.shared.result;

import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {}

    record Failure<T>(String errorCode, String message, @Nullable Throwable cause)
            implements Result<T> {
        public Failure(String errorCode, String message) {
            this(errorCode, message, null);
        }
    }

    // --- Factories ---

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(String errorCode, String message) {
        return new Failure<>(errorCode, message);
    }

    static <T> Result<T> failure(String errorCode, String message, Throwable cause) {
        return new Failure<>(errorCode, message, cause);
    }

    /** Wraps a supplier; any exception becomes a Failure. */
    static <T> Result<T> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure("UNEXPECTED_ERROR", e.getMessage(), e);
        }
    }

    // --- Terminal queries ---

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }

    default T getOrElse(T fallback) {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> ignored -> fallback;
        };
    }

    default T getOrThrow() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new ResultException(f.errorCode(), f.message(), f.cause());
        };
    }

    default Optional<T> toOptional() {
        return switch (this) {
            case Success<T> s -> Optional.ofNullable(s.value());
            case Failure<T> ignored -> Optional.empty();
        };
    }

    // --- Transformations ---

    default <U> Result<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T> s -> Result.of(() -> mapper.apply(s.value()));
            case Failure<T> f -> new Failure<>(f.errorCode(), f.message(), f.cause());
        };
    }

    default <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        return switch (this) {
            case Success<T> s -> mapper.apply(s.value());
            case Failure<T> f -> new Failure<>(f.errorCode(), f.message(), f.cause());
        };
    }

    // --- Side effects (return this for chaining) ---

    default Result<T> onSuccess(Consumer<T> action) {
        if (this instanceof Success<T> s) action.accept(s.value());
        return this;
    }

    default Result<T> onFailure(Consumer<Failure<T>> action) {
        if (this instanceof Failure<T> f) action.accept(f);
        return this;
    }
}
```

```java
package com.yourteam.shared.result;

public class ResultException extends RuntimeException {
    private final String errorCode;

    public ResultException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

---

## StringUtils {#stringutils}

```java
package com.yourteam.shared.util;

import jakarta.annotation.Nullable;
import java.util.Optional;

public final class StringUtils {

    private StringUtils() {}

    // --- Null-safe static methods (Stream method reference friendly) ---

    public static boolean isBlank(@Nullable String s) {
        return s == null || s.isBlank();
    }

    public static boolean isNotBlank(@Nullable String s) {
        return !isBlank(s);
    }

    public static String trimToEmpty(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    @Nullable
    public static String trimToNull(@Nullable String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String defaultIfBlank(@Nullable String s, String defaultValue) {
        return isBlank(s) ? defaultValue : s;
    }

    public static Optional<String> toOptional(@Nullable String s) {
        return isBlank(s) ? Optional.empty() : Optional.of(s.trim());
    }

    public static String truncate(@Nullable String s, int maxLength) {
        if (s == null) return "";
        return s.length() <= maxLength ? s : s.substring(0, maxLength);
    }

    public static String capitalize(@Nullable String s) {
        if (isBlank(s)) return trimToEmpty(s);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // --- Fluent wrapper factory ---

    public static StringWrapper of(@Nullable String value) {
        return new StringWrapper(value);
    }

    // --- Chainable wrapper ---

    public static final class StringWrapper {

        @Nullable private final String value;

        private StringWrapper(@Nullable String value) {
            this.value = value;
        }

        public StringWrapper trim() {
            return new StringWrapper(value == null ? null : value.trim());
        }

        public StringWrapper truncate(int maxLength) {
            return new StringWrapper(StringUtils.truncate(value, maxLength));
        }

        public StringWrapper toLowerCase() {
            return new StringWrapper(value == null ? null : value.toLowerCase());
        }

        public StringWrapper toUpperCase() {
            return new StringWrapper(value == null ? null : value.toUpperCase());
        }

        public StringWrapper capitalize() {
            return new StringWrapper(StringUtils.capitalize(value));
        }

        public StringWrapper replaceAll(String regex, String replacement) {
            return new StringWrapper(value == null ? null : value.replaceAll(regex, replacement));
        }

        public boolean isBlank() {
            return StringUtils.isBlank(value);
        }

        public Optional<String> toOptional() {
            return StringUtils.toOptional(value);
        }

        public String orElse(String fallback) {
            return isBlank() ? fallback : value;
        }

        /** @throws IllegalStateException if value is null or blank */
        public String get() {
            if (isBlank()) throw new IllegalStateException("StringWrapper value is blank or null");
            return value;
        }
    }
}
```

---

## CollectionUtils {#collectionutils}

```java
package com.yourteam.shared.util;

import jakarta.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class CollectionUtils {

    private CollectionUtils() {}

    public static boolean isEmpty(@Nullable Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static boolean isNotEmpty(@Nullable Collection<?> c) {
        return !isEmpty(c);
    }

    public static <T> List<T> emptyIfNull(@Nullable List<T> list) {
        return list == null ? List.of() : list;
    }

    public static <T> Optional<T> first(@Nullable List<T> list) {
        return isEmpty(list) ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

    public static <T> Optional<T> last(@Nullable List<T> list) {
        return isEmpty(list) ? Optional.empty()
                : Optional.ofNullable(list.get(list.size() - 1));
    }

    public static <T> Optional<T> findFirst(@Nullable List<T> list, Predicate<T> predicate) {
        if (isEmpty(list)) return Optional.empty();
        return list.stream().filter(predicate).findFirst();
    }

    /** Guava Lists.partition equivalent. */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        Objects.requireNonNull(list, "list");
        if (size <= 0) throw new IllegalArgumentException("Partition size must be > 0");
        return IntStream.range(0, (list.size() + size - 1) / size)
                .mapToObj(i -> list.subList(i * size, Math.min(i * size + size, list.size())))
                .map(List::copyOf)
                .toList();
    }

    public static <T, R> List<R> transform(@Nullable List<T> list, Function<T, R> fn) {
        if (isEmpty(list)) return List.of();
        return list.stream().map(fn).toList();
    }

    public static <T> List<T> filter(@Nullable List<T> list, Predicate<T> predicate) {
        if (isEmpty(list)) return List.of();
        return list.stream().filter(predicate).toList();
    }

    public static <T, K> Map<K, List<T>> groupBy(@Nullable List<T> list, Function<T, K> classifier) {
        if (isEmpty(list)) return Map.of();
        return list.stream().collect(Collectors.groupingBy(classifier));
    }

    public static <T> List<T> toUnmodifiable(@Nullable List<T> list) {
        return isEmpty(list) ? List.of() : Collections.unmodifiableList(new ArrayList<>(list));
    }
}
```

---

## Validator<T> {#validator}

```java
package com.yourteam.shared.validation;

import com.yourteam.shared.result.Result;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class Validator<T> {

    private final T value;
    private final List<ValidationError> errors = new ArrayList<>();

    private Validator(@Nullable T value) {
        this.value = value;
    }

    public static <T> Validator<T> of(@Nullable T value) {
        return new Validator<>(value);
    }

    /** Fails immediately if value is null — remaining rules are skipped. */
    public Validator<T> notNull(String field) {
        if (value == null) {
            errors.add(new ValidationError(field, "NULL_VALUE", field + " must not be null"));
        }
        return this;
    }

    /** String-specific: fails if value is null or blank. */
    public Validator<T> notBlank(String field) {
        if (value instanceof String s && (s == null || s.isBlank())) {
            errors.add(new ValidationError(field, "BLANK_VALUE", field + " must not be blank"));
        }
        return this;
    }

    public Validator<T> matches(Predicate<T> rule, String errorCode, String message) {
        if (value != null && !rule.test(value)) {
            errors.add(new ValidationError("", errorCode, message));
        }
        return this;
    }

    public Validator<T> matches(String field, Predicate<T> rule, String errorCode, String message) {
        if (value != null && !rule.test(value)) {
            errors.add(new ValidationError(field, errorCode, message));
        }
        return this;
    }

    /** Applies a rule only when the condition is true. */
    public Validator<T> when(boolean condition, Predicate<T> rule, String errorCode, String message) {
        if (condition) return matches(rule, errorCode, message);
        return this;
    }

    public List<ValidationError> errors() {
        return List.copyOf(errors);
    }

    /** Returns Result.success(value) if no errors, Result.failure otherwise. */
    public Result<T> validate() {
        if (errors.isEmpty()) {
            return Result.success(value);
        }
        String summary = errors.stream()
                .map(ValidationError::message)
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return Result.failure("VALIDATION_ERROR", summary);
    }

    /** @throws ValidationException with all errors if validation fails. */
    public T getOrThrow() {
        if (!errors.isEmpty()) throw new ValidationException(List.copyOf(errors));
        return Objects.requireNonNull(value);
    }
}
```

```java
package com.yourteam.shared.validation;

public record ValidationError(String field, String code, String message) {}
```

```java
package com.yourteam.shared.validation;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super(errors.stream().map(ValidationError::message).reduce((a, b) -> a + "; " + b).orElse("Validation failed"));
        this.errors = errors;
    }

    public List<ValidationError> getErrors() { return errors; }
}
```

---

## PageResult<T> {#pageresult}

```java
package com.yourteam.shared.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResult<>(List.copyOf(content), page, size, totalElements, totalPages);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0, 0);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public boolean hasNext() {
        return page < totalPages - 1;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    /** Transform content while preserving pagination metadata. */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
```

---

## HttpClientUtils {#httpclientutils}

> Requires Spring Boot 3.5 (`spring-web` on the classpath for `RestClient`).

```java
package com.yourteam.shared.http;

import com.yourteam.shared.result.Result;
import jakarta.annotation.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class HttpClientUtils {

    private static final RestClient DEFAULT_CLIENT = RestClient.create();

    private HttpClientUtils() {}

    public static HttpRequestBuilder get(String url) {
        return new HttpRequestBuilder(url, "GET");
    }

    public static HttpRequestBuilder post(String url) {
        return new HttpRequestBuilder(url, "POST");
    }

    public static HttpRequestBuilder put(String url) {
        return new HttpRequestBuilder(url, "PUT");
    }

    public static HttpRequestBuilder delete(String url) {
        return new HttpRequestBuilder(url, "DELETE");
    }

    public static final class HttpRequestBuilder {

        private final String url;
        private final String method;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, Object> pathVariables = new HashMap<>();
        private final Map<String, Object> queryParams = new HashMap<>();
        @Nullable private Object body;
        @Nullable private Duration timeout;

        private HttpRequestBuilder(String url, String method) {
            this.url = url;
            this.method = method;
        }

        public HttpRequestBuilder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public HttpRequestBuilder bearerToken(String token) {
            return header("Authorization", "Bearer " + token);
        }

        public HttpRequestBuilder pathVariable(String name, Object value) {
            pathVariables.put(name, value);
            return this;
        }

        public HttpRequestBuilder queryParam(String name, Object value) {
            queryParams.put(name, value);
            return this;
        }

        public HttpRequestBuilder body(Object body) {
            this.body = body;
            return this;
        }

        public HttpRequestBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public <T> Result<T> execute(Class<T> responseType) {
            return Result.of(() -> buildAndSend(responseType, null));
        }

        public <T> Result<T> execute(ParameterizedTypeReference<T> typeRef) {
            return Result.of(() -> buildAndSend(null, typeRef));
        }

        @SuppressWarnings("unchecked")
        private <T> T buildAndSend(@Nullable Class<T> clazz, @Nullable ParameterizedTypeReference<T> typeRef) {
            var spec = DEFAULT_CLIENT.method(org.springframework.http.HttpMethod.valueOf(method))
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(url);
                        queryParams.forEach(builder::queryParam);
                        return builder.build(pathVariables);
                    });

            headers.forEach(spec::header);

            if (body != null) spec.body(body);

            if (clazz != null) {
                return spec.retrieve().body(clazz);
            } else {
                return spec.retrieve().body(typeRef);
            }
        }
    }
}
```
