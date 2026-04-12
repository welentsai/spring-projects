# Module Rules: framework/di (Dependency Injection & Configuration)

## Packages

| Package | Contents |
|---------|----------|
| `framework.di` | `BeanInjection` — wires use case impls and gateway impls as `@Bean` |
| `framework.di.dynamicdatasource` | `DataSourceConfig`, `DynamicRoutingDataSource`, `DataSourceContextHolder`, `DataSourceKey` |
| `framework.config` | `AppConfig`, `VideoConfig` — external client configuration and properties |

---

## Responsibility

This is the **composition root** — the only place in the codebase where adapter classes and
use case classes are imported together. Its job is to assemble the object graph and bind
infrastructure configuration to domain ports.

---

## Allowed Imports

```
✅ com.example.demo.adapter.out.*          (concrete implementations being wired)
✅ com.example.demo.usecase.ports.in.*     (use case interfaces — return type of @Bean)
✅ com.example.demo.usecase.ports.in.impl.* (concrete use case implementations)
✅ com.example.demo.usecase.ports.out.*    (output port interfaces — injected into use cases)
✅ org.springframework.context.annotation.* (@Bean, @Configuration)
✅ org.springframework.boot.*              (@ConfigurationProperties etc.)
✅ External library clients (MinioClient, etc.)
```

## Forbidden Imports

```
❌ com.example.demo.adapter.in.*    (inbound adapters wire themselves via @RestController)
❌ com.example.demo.domain.*        (no domain objects needed here)
```

---

## Constraints

- **`BeanInjection` is the only class** that should import both adapter and usecase in the same file
  — if you find this pattern elsewhere, it's a violation
- Always declare the `@Bean` return type as the **interface**, not the concrete implementation:
  ```java
  // Correct:
  @Bean public XxxGateway xxxGateway(...) { return new XxxGatewayImpl(...); }

  // Wrong:
  @Bean public XxxGatewayImpl xxxGatewayImpl(...) { return new XxxGatewayImpl(...); }
  ```
- `DataSourceContextHolder` and `DataSourceKey` must **not be imported** in `usecase.*` — they
  are framework internals. If routing logic needs to affect the datasource, the gateway impl
  (`InlineRoutingGatewayImpl`) should handle it, and the datasource switch should happen in
  `DataSourceConfig` or an AOP advice — not in the use case

---

## Dynamic Datasource Routing

The routing works as follows:
1. Use case calls `InlineRoutingGateway.execute()` → gets back `"PRIMARY"` or `"SECONDARY"`
2. _(Currently violated)_ Use case sets `DataSourceContextHolder` — this should move to gateway/AOP
3. `DynamicRoutingDataSource.determineCurrentLookupKey()` reads the ThreadLocal key
4. Spring routes the JPA/JDBC call to the correct H2 datasource

## Pattern Reference

See `references/patterns/di-wiring.md` for the full `@Bean` wiring pattern.
