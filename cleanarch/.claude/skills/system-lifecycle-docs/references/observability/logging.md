# Reference: Logging

## Structured Logging Principle

Log in JSON, not plain text. Log aggregation systems (ELK, Loki, CloudWatch) can query
structured fields directly. Plain text requires fragile regex parsing and makes alerting unreliable.

Every log line should answer: **when, what happened, in which context, with what severity**.

---

## Logback JSON Configuration (Spring Boot 3.5)

Spring Boot 3.5 ships with built-in structured logging support — no extra library needed:

```yaml
# application.yml
logging:
  structured:
    format:
      console: ecs          # Elastic Common Schema JSON format
  level:
    root: INFO
    com.example.demo: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG        # log SQL in dev only
    org.hibernate.orm.jdbc.bind: TRACE  # log bind parameters
```

For environments without structured logging support (local dev), use plain text:

```yaml
# application-local.yml
logging:
  structured:
    format:
      console: none         # plain text in local dev
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

Alternatively, with Logback JSON encoder (more control):

```xml
<!-- logback-spring.xml -->
<configuration>
  <springProfile name="!local">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>requestId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
        <includeMdcKeyName>traceId</includeMdcKeyName>
      </encoder>
    </appender>
    <root level="INFO">
      <appender-ref ref="STDOUT" />
    </root>
  </springProfile>
</configuration>
```

---

## MDC (Mapped Diagnostic Context)

MDC propagates context across all log lines within a single request thread:

```java
// system/LoggingContext.java — set at request entry point
public class LoggingContext {
    private static final ThreadLocal<Map<String, String>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    public static void put(String key, String value) {
        MDC.put(key, value);
        CONTEXT.get().put(key, value);
    }

    public static void clear() {
        MDC.clear();
        CONTEXT.remove();
    }
}
```

Set in an HTTP filter (earliest possible point in request lifecycle):

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String requestId = Optional.ofNullable(request.getHeader("X-Request-ID"))
                .orElse(UUID.randomUUID().toString());
            String userId = Optional.ofNullable(request.getHeader("X-User-ID"))
                .orElse("anonymous");

            LoggingContext.put("requestId", requestId);
            LoggingContext.put("userId", userId);
            LoggingContext.put("method", request.getMethod());
            LoggingContext.put("path", request.getRequestURI());

            response.setHeader("X-Request-ID", requestId);
            chain.doFilter(request, response);
        } finally {
            LoggingContext.clear();    // always clear to prevent ThreadLocal leaks
        }
    }
}
```

Every log line in that request now automatically includes `requestId` and `userId`.

---

## AOP Logging for Gateway Calls

Log duration and errors for all external calls (database, MinIO, etc.) without cluttering
business logic with log statements:

```java
@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.example.demo.adapter.out.gateway..*GatewayImpl.*(..))")
    public Object logGatewayCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("Gateway call completed: method={} duration={}ms", method, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("Gateway call failed: method={} duration={}ms error={}",
                method, duration, e.getMessage(), e);
            throw e;
        }
    }
}
```

---

## Log Level Guidelines

| Level | When to use |
|-------|------------|
| `ERROR` | Unexpected failure requiring human attention. Alerts fire on ERROR. Include stack trace. |
| `WARN` | Degraded but recoverable state. Circuit breaker open, retry attempt, slow query. |
| `INFO` | Business-significant events: request received, use case executed, resource created. One line per event. |
| `DEBUG` | Implementation details useful during development: SQL queries, mapping steps, cache hits. |
| `TRACE` | Very verbose: parameter values, loop iterations. Never in production. |

**Anti-patterns:**
- ERROR without stack trace — use `log.error("message", e)`
- Log sensitive data (passwords, tokens, PII) at any level
- Log in a tight loop at INFO/DEBUG — use counters instead
- Catching and swallowing exceptions without logging

---

## Correlation ID Flow

```
Client → HTTP Header: X-Request-ID: abc123
  ↓ LoggingFilter sets MDC["requestId"] = "abc123"
    ↓ All logs in this request include "requestId": "abc123"
      ↓ HTTP Response Header: X-Request-ID: abc123 (echo back to client)
```

For async operations (new thread), explicitly copy MDC:

```java
Map<String, String> mdcCopy = MDC.getCopyOfContextMap();
CompletableFuture.runAsync(() -> {
    MDC.setContextMap(mdcCopy);
    try {
        // async work here
    } finally {
        MDC.clear();
    }
}, executor);
```

---

## Log Aggregation (ELK / Grafana Loki)

In Kubernetes, logs go to stdout — the container runtime collects them.

**ELK Stack:**
- Filebeat reads container logs from node
- Logstash parses and enriches
- Elasticsearch indexes
- Kibana queries: `requestId: "abc123"` to find all logs for one request

**Grafana Loki (lighter alternative):**
```logql
# Find all ERROR logs for a specific request
{app="demo-app"} |= `"level":"ERROR"` | json | requestId = "abc123"
```

---

## Doc Template Sections

When generating `docs/logging.md`, include:
1. Current logging config (`application.yml` logging section, `logback-spring.xml` if it exists)
2. MDC fields set by `LoggingFilter` — what each field means
3. `LoggingAspect` — which packages are covered, what's logged
4. Log level strategy for this project
5. How to correlate logs: find all logs for a request, find errors in a time window
6. Log aggregation setup (or recommended setup if not yet configured)
7. How to add logging to a new gateway method
