# Pattern: Use Case Interface + Input / Result (usecase/ports/in)

Extracted from `FindCitiesUseCase`, `FindCitiesInput`, `FindCitiesResult`, `CityDto`.

---

## Use Case Interface

```java
package com.example.demo.usecase.ports.in;

public interface XxxUseCase {
    XxxResult execute(XxxInput input);   // single method, always named execute()
}
```

---

## Input Class

```java
package com.example.demo.usecase.ports.in;

// Empty input → plain class implementing marker interface
public class XxxInput implements Input {}

// Input with data → class with fields
public class XxxInput implements Input {
    private final String bucketName;

    public XxxInput(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }
}
```

---

## Result Class

Extends `Result<T>`. Provides typed static factory methods and a convenience getter.

```java
package com.example.demo.usecase.ports.in;

import com.example.demo.usecase.ports.in.dto.XxxDto;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"returnCode", "errorMessage", "data", "items", "success", "failure"})
public class XxxResult extends Result<List<XxxDto>> {

    private XxxResult(String returnCode, String errorMessage, List<XxxDto> data) {
        super(returnCode, errorMessage, data);
    }

    public static XxxResult success(List<XxxDto> items) {
        return new XxxResult("SUCCESS", null, items);
    }

    public static XxxResult failure(String errorMessage) {
        return new XxxResult("FAILURE", errorMessage, null);
    }

    public static XxxResult failure(String returnCode, String errorMessage) {
        return new XxxResult(returnCode, errorMessage, null);
    }

    // Convenience getter with semantic name
    public List<XxxDto> getItems() {
        return getData();
    }
}
```

---

## DTO (usecase/ports/in/dto)

Use-case-level data objects — shared between use case and adapter response mapping.

```java
package com.example.demo.usecase.ports.in.dto;

// Always a record — immutable, no boilerplate
public record XxxDto(String id, String name, String country) {}
```

---

## Result<T> Base Class (reference)

```java
public class Result<T> implements Output {
    // returnCode: "SUCCESS" | "FAILURE" | custom code
    // errorMessage: null on success
    // data: null on failure
    public boolean isSuccess()  { return "SUCCESS".equals(returnCode); }
    public boolean isFailure()  { return !isSuccess(); }
}
```

---

## Key Rules

- Use case interface has **exactly one method**: `execute(XxxInput)`
- `XxxResult` always extends `Result<T>` — never return raw data or throw checked exceptions
- DTOs in `usecase.ports.in.dto` are **records** — immutable value carriers
- No Spring annotations anywhere in this package
