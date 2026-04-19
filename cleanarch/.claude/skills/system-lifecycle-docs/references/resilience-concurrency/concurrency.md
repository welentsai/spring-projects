# Reference: Concurrency & Threading

## Threading Model Overview

Spring Boot's default threading model: one thread per HTTP request (Tomcat default pool = 200 threads).
Each thread handles the full request lifecycle synchronously. This works well until:
- A request triggers a slow external call (DB, MinIO, external API)
- Multiple slow requests pile up and exhaust the thread pool
- You need to parallelize independent operations within a single request

Understanding where threads come from, how they're sized, and how to use them safely is
essential for building a reliable system.

---

## Tomcat Thread Pool

```yaml
server:
  tomcat:
    threads:
      max: 200           # max concurrent requests (default: 200)
      min-spare: 10      # always-warm threads
    connection-timeout: 20000   # ms before idle connection is closed
    accept-count: 100    # queue length when all threads are busy
```

Monitor pool saturation:
```promql
# If this approaches max, you need more threads or faster responses
tomcat_threads_busy_threads / tomcat_threads_config_max_threads
```

---

## @Async for Background Tasks

Use `@Async` when you want to execute work in the background without blocking the request thread.
A common use case: fire-and-forget logging, sending notifications, triggering async processing.

```java
// framework/di/config/AppConfig.java
@Bean("videoProcessingExecutor")
public TaskExecutor videoProcessingExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("video-proc-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

```java
@Async("videoProcessingExecutor")
public CompletableFuture<Void> processVideoAsync(String videoId) {
    log.info("Processing video: {}", videoId);  // runs on video-proc- thread
    // heavy processing
    return CompletableFuture.completedFuture(null);
}
```

**Critical:** `@Async` only works when called from *outside* the bean (Spring proxy intercepts it).
Calling an `@Async` method from within the same class bypasses the proxy — the method runs synchronously.

Enable async in configuration:
```java
@Configuration
@EnableAsync
public class AppConfig { ... }
```

---

## MDC in Async Contexts

MDC is ThreadLocal — it does NOT propagate to child threads automatically. Always copy and restore:

```java
@Async("videoProcessingExecutor")
public CompletableFuture<Void> processVideoAsync(String videoId, Map<String, String> mdcContext) {
    MDC.setContextMap(mdcContext);
    try {
        log.info("Processing video: {}", videoId);  // now includes requestId etc.
        // processing
        return CompletableFuture.completedFuture(null);
    } finally {
        MDC.clear();
    }
}

// Caller:
Map<String, String> mdc = MDC.getCopyOfContextMap();
videoService.processVideoAsync(videoId, mdc);
```

---

## @Transactional in Async Methods

`@Transactional` and `@Async` require careful coordination:

- A transaction started in the calling thread is **not visible** to the async thread (different connection)
- If you need a transaction in async code, annotate the `@Async` method with `@Transactional`
- Never pass JPA-managed entities across thread boundaries — pass IDs instead

```java
// Wrong — entity may be detached by the time async runs
@Async
public void process(CityEntity entity) { ... }

// Correct — fetch fresh in the async thread
@Async
@Transactional
public void process(Long cityId) {
    CityEntity entity = cityRepository.findById(cityId).orElseThrow();
    // ...
}
```

---

## CompletableFuture for Parallel Calls

When a use case needs to call multiple independent gateways simultaneously:

```java
public FindDashboardResult execute(FindDashboardInput input) {
    CompletableFuture<List<CityDto>> citiesFuture =
        CompletableFuture.supplyAsync(() -> cityRepository.findAll(), executor);

    CompletableFuture<List<VideoDto>> videosFuture =
        CompletableFuture.supplyAsync(() -> videoGateway.listVideos(bucket), executor);

    return CompletableFuture.allOf(citiesFuture, videosFuture)
        .thenApply(v -> new FindDashboardResult(
            citiesFuture.join(),
            videosFuture.join()
        ))
        .get(5, TimeUnit.SECONDS);    // hard timeout prevents indefinite blocking
}
```

---

## Virtual Threads (Java 21+ / Project Loom)

Spring Boot 3.2+ supports virtual threads — enable for dramatic I/O-bound throughput improvement:

```yaml
spring:
  threads:
    virtual:
      enabled: true   # requires Java 21+
```

With virtual threads enabled, Tomcat uses virtual threads instead of platform threads.
Each blocked virtual thread (waiting for DB, HTTP, file I/O) parks cheaply — no OS thread consumed.
This effectively eliminates the "thread pool exhaustion" problem for I/O-bound workloads.

**Caution with Java 17:** Virtual threads require Java 21. On Java 17, use reactive (WebFlux)
or simply size your thread pool generously for I/O-bound workloads.

---

## Thread Safety Rules

| Scenario | Rule |
|----------|------|
| Spring beans | Stateless by default — no instance fields that change per request |
| `ThreadLocal` | Always clear in `finally` block to prevent leaks in thread pool reuse |
| `@Transactional` beans | Never share mutable state — each request gets its own transaction |
| Shared caches | Use `ConcurrentHashMap`, not `HashMap` |
| Counters | Use `AtomicLong`, not `long++` |
| Configuration (read-only) | Safe to share — loaded once at startup |

---

## Named Thread Pool Naming Convention

Name all executor beans descriptively — thread names appear in thread dumps:

```
http-nio-8080-exec-1    ← Tomcat request thread
video-proc-1            ← video processing pool
async-task-1            ← general async pool
```

Named threads make thread dumps actionable: if `video-proc-*` threads are all WAITING on
the same lock, you know exactly where the contention is.

---

## Doc Template Sections

When generating `docs/concurrency.md`, include:
1. Tomcat thread pool configuration (from `application.yml`)
2. All `@Async` methods in the codebase + which executor they use
3. All `TaskExecutor` / `ThreadPoolTaskExecutor` beans in `framework/di/`
4. MDC propagation pattern used in this project
5. Virtual thread status (Java 17 = not available; note upgrade path to Java 21)
6. Thread safety audit: any stateful beans? any shared mutable state?
7. How to profile thread pool utilization (Actuator metrics, thread dumps)
