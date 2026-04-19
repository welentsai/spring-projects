# Reference: Metrics

## Why Metrics Matter

Logs tell you *what happened*. Metrics tell you *how the system is behaving over time*.
A well-instrumented service lets you answer: "Is latency increasing? Are error rates rising?
Is the connection pool exhausted?" — before users start complaining.

The SRE "Golden Signals" — instrument all four:
1. **Latency** — how long requests take (p50, p95, p99)
2. **Traffic** — how many requests per second
3. **Errors** — rate of failed requests
4. **Saturation** — how full is the system (pool usage, queue depth, CPU)

---

## Spring Boot + Micrometer Setup

Spring Boot auto-configures Micrometer. Add Prometheus registry:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, prometheus, metrics, info
  endpoint:
    prometheus:
      enabled: true
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true    # enables p50/p95/p99 histograms
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:default}
```

Prometheus scrapes: `GET /actuator/prometheus`

---

## Auto-Instrumented Metrics (Zero Code)

Spring Boot + Micrometer automatically instruments:

| Metric | Description |
|--------|-------------|
| `http.server.requests` | HTTP request count, latency, status by endpoint |
| `jvm.memory.used` | JVM heap/non-heap usage |
| `jvm.gc.pause` | GC pause duration |
| `jvm.threads.live` | Active thread count |
| `hikaricp.connections.active` | Active DB connections |
| `hikaricp.connections.pending` | Waiting for connection |
| `process.cpu.usage` | JVM CPU usage |
| `logback.events` | Log events by level |

---

## Custom Business Metrics

```java
@Component
public class CityMetrics {
    private final Counter citySearchTotal;
    private final Counter citySearchEmpty;
    private final Timer citySearchDuration;

    public CityMetrics(MeterRegistry registry) {
        this.citySearchTotal = Counter.builder("city.search.total")
            .description("Total city searches")
            .register(registry);

        this.citySearchEmpty = Counter.builder("city.search.empty")
            .description("City searches returning no results")
            .register(registry);

        this.citySearchDuration = Timer.builder("city.search.duration")
            .description("City search latency")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    public void recordSearch(boolean hasResults, Duration duration) {
        citySearchTotal.increment();
        if (!hasResults) citySearchEmpty.increment();
        citySearchDuration.record(duration);
    }
}
```

---

## Resilience4j Metrics (Automatic)

When Resilience4j is on the classpath with Micrometer, circuit breaker state is exposed automatically:

| Metric | Description |
|--------|-------------|
| `resilience4j.circuitbreaker.state` | CLOSED/OPEN/HALF_OPEN |
| `resilience4j.circuitbreaker.calls` | Successful/failed/not-permitted |
| `resilience4j.ratelimiter.available.permissions` | Remaining permits |
| `resilience4j.retry.calls` | Retry attempts |

Alert on: `resilience4j.circuitbreaker.state{state="open"}` > 0 for more than 30s.

---

## Prometheus Scrape Config (K8s)

```yaml
# K8s ServiceMonitor (if using Prometheus Operator)
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: demo-app
spec:
  selector:
    matchLabels:
      app: demo-app
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

Or annotate the pod directly (plain Prometheus):
```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/path: "/actuator/prometheus"
  prometheus.io/port: "8080"
```

---

## Key Grafana Dashboards

### Request Rate & Error Rate
```promql
# Requests per second
rate(http_server_requests_seconds_count{application="demo"}[1m])

# Error rate (5xx)
rate(http_server_requests_seconds_count{application="demo",status=~"5.."}[1m])
  / rate(http_server_requests_seconds_count{application="demo"}[1m])
```

### Latency Percentiles
```promql
# p99 latency per endpoint
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket{application="demo"}[5m]))
  by (le, uri)
)
```

### Connection Pool Saturation
```promql
# HikariCP pool usage
hikaricp_connections_active{application="demo"}
  / hikaricp_connections_max{application="demo"}
```

---

## Alerting Rules

```yaml
# prometheus-rules.yml
groups:
  - name: demo-app
    rules:
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m])
          / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Error rate > 5% for {{ $labels.application }}"

      - alert: HighP99Latency
        expr: |
          histogram_quantile(0.99,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "p99 latency > 2s for {{ $labels.application }}"
```

---

## Doc Template Sections

When generating `docs/metrics.md`, include:
1. Dependencies configured (Micrometer, Prometheus registry)
2. Prometheus endpoint URL and sample output
3. Table of auto-instrumented metrics relevant to this project
4. Any custom metrics in the codebase (scan for `MeterRegistry`, `Counter`, `Timer`, `Gauge`)
5. Grafana dashboard links (or recommended PromQL queries)
6. Alert rules in place (or recommended)
7. How to add a new business metric
