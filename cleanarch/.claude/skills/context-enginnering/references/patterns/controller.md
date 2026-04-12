# Pattern: REST Controller (adapter/in)

Extracted from `CitiesController` and `VideoController`.

---

## Full Pattern

```java
package com.example.demo.adapter.in;

import com.example.demo.adapter.in.dto.XxxRequest;
import com.example.demo.adapter.in.dto.XxxResponse;
import com.example.demo.adapter.in.mapper.XxxRequestMapper;
import com.example.demo.exception.BadRequestException;
import com.example.demo.usecase.ports.in.XxxUseCase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/xxx")
public class XxxController {

    private final XxxUseCase xxxUseCase;

    XxxController(XxxUseCase xxxUseCase) {   // package-private, no @Autowired
        this.xxxUseCase = xxxUseCase;
    }

    @GetMapping
    public ResponseEntity<XxxResponse> getXxx() {
        XxxResponse response = new XxxRequest(/* params */)
                .validate()                          // returns Optional<XxxRequest>
                .map(XxxRequestMapper::toInput)      // XxxRequest → XxxInput
                .map(xxxUseCase::execute)            // XxxInput → XxxResult
                .map(XxxRequestMapper::toResponse)   // XxxResult → XxxResponse
                .orElseThrow(BadRequestException::new);

        return ResponseEntity.ok(response);
    }
}
```

---

## Request DTO (adapter/in/dto)

```java
package com.example.demo.adapter.in.dto;

import java.util.Optional;

public record XxxRequest(/* fields */) {

    public Optional<XxxRequest> validate() {
        // Return Optional.empty() if validation fails
        // Return Optional.of(this) if valid
        return Optional.of(this);
    }
}
```

## Response DTO (adapter/in/dto)

```java
package com.example.demo.adapter.in.dto;

import com.example.demo.usecase.ports.in.dto.XxxDto;

import java.util.List;

public record XxxResponse(List<XxxDto> items) {}
```

---

## Mapper (adapter/in/mapper)

```java
package com.example.demo.adapter.in.mapper;

import com.example.demo.adapter.in.dto.XxxRequest;
import com.example.demo.adapter.in.dto.XxxResponse;
import com.example.demo.usecase.ports.in.XxxInput;
import com.example.demo.usecase.ports.in.XxxResult;

public class XxxRequestMapper {

    private XxxRequestMapper() {}  // utility class — no instantiation

    public static XxxInput toInput(XxxRequest request) {
        return new XxxInput(/* map fields */);
    }

    public static XxxResponse toResponse(XxxResult result) {
        return new XxxResponse(result.getData());
    }
}
```

---

## Key Rules

- Controller constructor is **package-private** (no `public`, no `@Autowired`)
- **No business logic** in controllers — only the validate → map → execute → map pipeline
- `validate()` returns `Optional<XxxRequest>`; empty means bad request
- Mapper is a **utility class** (private constructor, all-static methods)
- `XxxResponse` depends on use-case DTOs (`usecase.ports.in.dto`) — that is acceptable
