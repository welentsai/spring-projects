# Module Rules: adapter/out/gateway (Outbound Gateway Adapters)

## Package

`adapter.out.gateway` — implementations of gateway port interfaces defined in `usecase.ports.out.gateway`.

---

## Responsibility

Call external systems: REST APIs, message queues, object storage, internal routing services,
logging sinks. All I/O that is not a database query lives here.

---

## Allowed Imports

```
✅ com.example.demo.usecase.ports.out.gateway.*  (the interface being implemented)
✅ com.example.demo.system.*                     (LoggingContext for side-effect gateways)
✅ Any external library (MinIO, HTTP client, etc.)
✅ org.springframework.*                         (if needed for HTTP or messaging)
```

## Forbidden Imports

```
❌ com.example.demo.usecase.ports.in.*   (no inbound port coupling)
❌ com.example.demo.adapter.in.*         (no inbound adapter coupling)
❌ com.example.demo.domain.*             (gateways work with port types, not domain objects)
❌ com.example.demo.framework.di.*       (no framework internals — especially DataSourceContextHolder)
```

---

## Constraints

- **No Spring annotations on the class** — `@Component`, `@Service` are forbidden;
  wiring happens in `framework/di/BeanInjection`
- **Implements exactly one gateway interface** from `usecase.ports.out.gateway`
- **AOP coverage is automatic** — `LoggingAspect` wraps every method here;
  no need to manually log input, duration, or errors for the gateway call itself
- **Naming**: class name must end in `GatewayImpl` and match the interface name exactly
  (`InlineRoutingGateway` → `InlineRoutingGatewayImpl`)

---

## Pattern Reference

See `references/patterns/gateway-impl.md` for the full implementation pattern.

---

## Checklist for New Gateway

- [ ] Interface + Input + Output defined in `usecase.ports.out.gateway`
- [ ] Implementation class `XxxGatewayImpl` in `adapter.out.gateway`
- [ ] No Spring annotations on implementation
- [ ] `@Bean` wiring added to `framework/di/BeanInjection`
- [ ] Test: unit test mocking external client; verify `execute()` maps output correctly
