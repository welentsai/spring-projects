# Reference: Resilience Patterns

## Why Resilience Matters

In a distributed system, failures are not exceptional — they are normal. A database goes slow,
an external API times out, a downstream service deploys bad code. Without resilience patterns,
one slow dependency can exhaust your thread pool, cause cascading failures, and bring down the
entire application. Resilience4j gives you the tools to fail fast, recover gracefully, and
protect your system from cascade failure.

---

## Resilience4j Setup

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.3.0</version>
</dependency>
<!-- Micrometer integration for metrics -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-micrometer</artifactId>
    <version>2.3.0</version>
</dependency>
```

---

## Circuit Breaker

Prevents calling a failing service repeatedly. After a threshold of failures, the circuit
"opens" and requests are rejected immediately (fast fail) rather than waiting for timeouts.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      minioGateway:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10           # evaluate last 10 calls
        failureRateThreshold: 50        # open if >50% fail
        slowCallDurationThreshold: 2s   # calls >2s count as slow
        slowCallRateThreshold: 50       # open if >50% are slow
        waitDurationInOpenState: 30s    # wait before trying half-open
        permittedNumberOfCallsInHalfOpenState: 3
        registerHealthIndicator: true   # expose to /actuator/health
```

```java
@GatewayImpl
public class VideoGatewayImpl implements VideoGateway {

    @CircuitBreaker(name = "minioGateway", fallbackMethod = "listVideosFallback")
    @Override
    public List<VideoDto> listVideos(String bucket) {
        // call MinIO
    }

    private List<VideoDto> listVideosFallback(String bucket, Exception e) {
        log.warn("Circuit open for MinIO — returning empty list. bucket={}", bucket, e);
        return List.of();    // graceful degradation
    }
}
```

**States:**
- `CLOSED` → normal operation, calls pass through
- `OPEN` → fast fail, fallback invoked, no calls to downstream
- `HALF_OPEN` → limited test calls to check if downstream recovered

---

## Retry

Automatically retry transient failures (network blips, temporary service unavailability):

```yaml
resilience4j:
  retry:
    instances:
      cityRepository:
        maxAttempts: 3
        waitDuration: 500ms
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2     # 500ms → 1s → 2s
        retryExceptions:
          - java.sql.SQLException
          - org.springframework.dao.TransientDataAccessException
        ignoreExceptions:
          - com.example.demo.exception.UserNotFoundException  # don't retry business errors
```

```java
@Retry(name = "cityRepository", fallbackMethod = "findByRegionFallback")
public List<CityDto> findByRegion(String region) {
    return jdbcClient.sql("SELECT ...").query(CityDto.class).list();
}
```

**Key distinction:** Retry is for transient failures. Do not retry business errors (404, 400)
or non-idempotent write operations unless you're certain of idempotency.

---

## Rate Limiter

Protects downstream services from being overwhelmed and enforces fair usage:

```yaml
resilience4j:
  ratelimiter:
    instances:
      videoApi:
        limitForPeriod: 100           # 100 calls allowed
        limitRefreshPeriod: 1s        # per second
        timeoutDuration: 0s           # fail immediately if rate exceeded
```

```java
@RateLimiter(name = "videoApi", fallbackMethod = "listVideosFallback")
public List<VideoDto> listVideos(String bucket) { ... }
```

---

## Bulkhead

Limits concurrent calls to a downstream service, preventing one slow dependency from
consuming all threads:

```yaml
resilience4j:
  bulkhead:
    instances:
      minioGateway:
        maxConcurrentCalls: 20
        maxWaitDuration: 100ms    # wait up to 100ms for a slot, then reject
```

---

## Timeout

```yaml
resilience4j:
  timelimiter:
    instances:
      minioGateway:
        timeoutDuration: 3s
        cancelRunningFuture: true
```

Use with `@TimeLimiter` on async methods. For synchronous calls, configure timeouts at
the HTTP client / JDBC connection level instead.

---

## Combining Patterns (Decorator Order)

When using multiple patterns together, order matters. Recommended order (outermost → innermost):

```
Bulkhead → CircuitBreaker → RateLimiter → Retry → TimeLimiter → actual call
```

Using `@` annotations, apply in reverse order (innermost annotation executes first):

```java
@Bulkhead(name = "minioGateway")
@CircuitBreaker(name = "minioGateway", fallbackMethod = "fallback")
@Retry(name = "minioGateway")
public List<VideoDto> listVideos(String bucket) { ... }
```

---

## Operational Playbook

**Circuit breaker is OPEN:**
```bash
# Check circuit breaker state
curl localhost:8080/actuator/health | jq '.components.circuitBreakers'

# Check metrics
curl localhost:8080/actuator/prometheus | grep resilience4j_circuitbreaker_state
```

**Excessive retries causing load:**
- Check `resilience4j_retry_calls_total{kind="failed_with_retry"}` in Prometheus
- If high: the downstream is genuinely unhealthy → investigate the dependency, not the retry config

---

## Doc Template Sections

When generating `docs/resilience.md`, include:
1. Resilience4j dependency version in this project
2. All configured instances (from `application.yml` — scan for `resilience4j:`)
3. All `@CircuitBreaker` / `@Retry` / `@RateLimiter` / `@Bulkhead` annotations in the codebase
4. Fallback methods and their behavior (graceful degradation strategy)
5. Metrics exposed and how to check circuit breaker state
6. Operational runbook: what to do when a circuit opens
7. Guidelines for adding resilience to a new gateway
