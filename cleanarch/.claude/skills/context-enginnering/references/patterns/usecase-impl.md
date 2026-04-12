# Pattern: Use Case Implementation (usecase/ports/in/impl)

Extracted from `FindCitiesUseCaseImpl` and `ListVideosUseCaseImpl`.

---

## Full Pattern

```java
package com.example.demo.usecase.ports.in.impl;

import com.example.demo.usecase.ports.in.XxxInput;
import com.example.demo.usecase.ports.in.XxxResult;
import com.example.demo.usecase.ports.in.XxxUseCase;
import com.example.demo.usecase.ports.in.dto.XxxDto;
import com.example.demo.usecase.ports.out.gateway.YyyGateway;
import com.example.demo.usecase.ports.out.gateway.YyyInput;
import com.example.demo.usecase.ports.out.repository.XxxQueryRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// No @Service, @Component, or any Spring annotation — wired by framework/di/BeanInjection
public class XxxUseCaseImpl implements XxxUseCase {

    private static final Logger logger = LoggerFactory.getLogger(XxxUseCaseImpl.class);

    private final XxxQueryRepository xxxRepository;
    private final YyyGateway yyyGateway;

    public XxxUseCaseImpl(XxxQueryRepository xxxRepository, YyyGateway yyyGateway) {
        this.xxxRepository = xxxRepository;
        this.yyyGateway = yyyGateway;
    }

    @Override
    public XxxResult execute(XxxInput input) {
        try {
            // 1. Call gateways for external decisions / side effects
            YyyOutput gatewayOutput = yyyGateway.execute(new YyyInput(...));
            logger.info("Gateway result: {}", gatewayOutput);

            // 2. Execute business logic via repository / domain
            List<XxxEntity> entities = xxxRepository.findAll();

            // 3. Map to DTOs
            List<XxxDto> dtos = entities.stream()
                    .map(e -> new XxxDto(e.getId(), e.getName()))
                    .toList();

            return XxxResult.success(dtos);

        } catch (Exception e) {
            logger.error("Failed to execute XxxUseCase", e);
            return XxxResult.failure("INTERNAL_ERROR", "Failed: " + e.getMessage());
        }
    }
}
```

---

## Error Handling Pattern

Always catch broadly and return `XxxResult.failure(...)` — **never throw** from a use case:

```java
try {
    // ... business logic
    return XxxResult.success(data);
} catch (Exception e) {
    logger.error("Failed to ...", e);
    return XxxResult.failure("INTERNAL_ERROR", "Failed: " + e.getMessage());
}
```

---

## Known Violation to Avoid

`FindCitiesUseCaseImpl` currently imports `DataSourceContextHolder` and `DataSourceKey`
from `framework.di.dynamicdatasource`. This is a **dependency rule violation**
(usecase → framework is forbidden). The routing decision should be encapsulated
in the gateway implementation (`InlineRoutingGatewayImpl`), not exposed here.

---

## Key Rules

- **No Spring annotations** (`@Service`, `@Component`) — the class is a plain Java object
- All dependencies injected via **constructor** (wired as `@Bean` in `framework/di/`)
- Always **catch Exception** and return `XxxResult.failure(...)` — do not propagate exceptions upward
- Only import from `usecase.ports.in`, `usecase.ports.out.*`, `domain.model`
- **Never import** from `adapter.*` or `framework.*`
