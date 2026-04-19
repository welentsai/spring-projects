# Reference: Distributed Tracing

## What Distributed Tracing Solves

In a microservices environment (or even a single service calling external APIs), a single user
request may touch multiple services, databases, and queues. When something is slow or broken,
which hop caused the problem? Distributed tracing answers this by attaching a **trace ID** to
every operation and recording timing spans for each step.

Even in a single service, tracing gives you a flame graph of exactly where time was spent:
controller → use case → repository → SQL → response.

---

## Core Concepts

```
Trace: one end-to-end user request
  └─ Span: one unit of work within the trace
       ├─ Span: controller layer          [0ms → 45ms]
       │    └─ Span: use case            [2ms → 43ms]
       │         └─ Span: DB query       [5ms → 40ms]
       └─ Span: MDC enrichment           [0ms → 1ms]
```

Each span carries: `traceId`, `spanId`, `parentSpanId`, start time, duration, status, tags.

---

## Spring Boot 3.5 Setup (Micrometer Tracing)

Spring Boot 3.x uses **Micrometer Tracing** (replaces Spring Cloud Sleuth):

```xml
<!-- Micrometer Tracing with Brave (Zipkin-compatible) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

For OpenTelemetry (preferred for Jaeger / OTLP):
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0      # 100% in dev; use 0.1 (10%) in prod
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

# Or for OTLP (Jaeger / Grafana Tempo):
# otel:
#   exporter:
#     otlp:
#       endpoint: http://jaeger:4317
```

---

## What Gets Traced Automatically

Spring Boot auto-instruments:

| Component | What's traced |
|-----------|--------------|
| `@RestController` | Incoming HTTP requests |
| `RestTemplate` / `WebClient` | Outgoing HTTP calls |
| `JdbcTemplate` / `JdbcClient` | Database queries |
| `@Async` methods | Async execution (trace propagated) |
| Spring Data repositories | Repository method calls |

The `traceId` is also automatically added to MDC, so every log line within a traced operation
includes the trace ID — making it trivial to correlate logs with traces:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "message": "City search executed",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "requestId": "req-001"
}
```

---

## Trace Propagation

Trace context is propagated via HTTP headers:

| Header | Format | Used by |
|--------|--------|---------|
| `traceparent` | W3C Trace Context | OpenTelemetry, Jaeger |
| `X-B3-TraceId` | Zipkin B3 | Zipkin, Brave |
| `X-B3-SpanId` | Zipkin B3 | Zipkin, Brave |

When your service calls another service, these headers are automatically forwarded by
`RestTemplate`/`WebClient` — the downstream service continues the same trace.

---

## Custom Spans

Add spans for business-significant operations:

```java
@Component
public class VideoGatewayImpl implements VideoGateway {
    private final Tracer tracer;
    private final MinioClient minioClient;

    @Override
    public List<VideoDto> listVideos(String bucket) {
        Span span = tracer.nextSpan().name("minio.list-videos").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("bucket", bucket);
            var result = minioClient.listObjects(...);
            span.tag("count", String.valueOf(result.size()));
            return result;
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

## Sampling Strategy

| Environment | Rate | Rationale |
|-------------|------|-----------|
| Development | 100% | See everything |
| Staging | 100% | Catch issues before prod |
| Production | 5-10% | Cost vs visibility tradeoff |
| Production (errors) | 100% | Always trace errors |

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLE_RATE:0.1}
```

---

## Jaeger UI Queries

```
# Find slow traces for a specific endpoint
Service: demo-app
Operation: GET /api/v1/cities
Min Duration: 500ms

# Find all traces with errors
Tags: error=true

# Find traces by correlation ID
Tags: requestId=abc123
```

---

## Doc Template Sections

When generating `docs/tracing.md`, include:
1. Whether tracing is currently configured (check pom.xml for micrometer-tracing dependencies)
2. If configured: trace backend (Zipkin/Jaeger/Tempo), sampling rate, auto-instrumented components
3. If not configured: mark as "Not yet implemented" and show recommended setup
4. How traceId appears in logs (MDC field)
5. How to find traces for a specific request (using requestId or traceId)
6. Any custom spans in the codebase (scan for `Tracer`, `Span`)
7. Sampling strategy per environment
