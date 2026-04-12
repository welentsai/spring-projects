# Pattern: Gateway Implementation (adapter/out/gateway)

Extracted from `InlineRoutingGatewayImpl`, `PutLogGatewayImpl`, `VideoStorageGatewayImpl`.

---

## Full Pattern

```java
package com.example.demo.adapter.out.gateway;

import com.example.demo.usecase.ports.out.gateway.XxxGateway;
import com.example.demo.usecase.ports.out.gateway.XxxInput;
import com.example.demo.usecase.ports.out.gateway.XxxOutput;

// No @Component or @Service — wired as @Bean in framework/di/BeanInjection
public class XxxGatewayImpl implements XxxGateway {

    private final ExternalClient client;  // injected via constructor in BeanInjection

    public XxxGatewayImpl(ExternalClient client) {
        this.client = client;
    }

    @Override
    public XxxOutput execute(XxxInput input) {
        // Call external service / system
        // Map result to XxxOutput record
        return new XxxOutput(/* result */);
    }
}
```

---

## Simple Gateway (no external dependency)

```java
// InlineRoutingGatewayImpl — makes a routing decision, no injected client
public class InlineRoutingGatewayImpl implements InlineRoutingGateway {

    @Override
    public InlineRoutingOutput execute(InlineRoutingInput input) {
        boolean usePrimary = /* decision logic */;
        return new InlineRoutingOutput(usePrimary ? "PRIMARY" : "SECONDARY");
    }
}
```

## Side-effect Gateway (writes to LoggingContext)

```java
// PutLogGatewayImpl — writes to ThreadLocal logging context
public class PutLogGatewayImpl implements PutLogGateway {

    @Override
    public PutLogOutput execute(PutLogInput input) {
        return PutLogOutput.of(LoggingContext.put(input.key(), input.value()));
    }
}
```

---

## AOP Coverage

`LoggingAspect` automatically wraps **all methods** in `adapter.out.gateway.*`:
- Logs input args, execution duration, result, and errors
- No manual logging needed in gateway implementations (for the gateway call itself)

```java
// LoggingAspect pointcut:
@Around("execution(* com.example.demo.adapter.out.gateway.*.*(..))")
```

---

## Wiring in framework/di/BeanInjection

```java
@Bean
public XxxGateway xxxGateway(ExternalClient client) {
    return new XxxGatewayImpl(client);
}
```

---

## Key Rules

- **No Spring annotations** on the implementation class itself
- Implements exactly one gateway interface from `usecase.ports.out.gateway`
- All external I/O (HTTP, file, queue, DB) happens here — never in use case or domain
- Constructor injection only — dependencies supplied by `BeanInjection`
- AOP logging is automatic — no need to add duration/error logging manually
