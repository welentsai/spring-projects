# Reference: Health & Readiness

## Liveness vs Readiness — the Critical Distinction

Most teams get this wrong. Getting it right prevents unnecessary pod restarts and traffic loss.

| Probe | Question it answers | Action on failure |
|-------|--------------------|--------------------|
| **Liveness** | "Is the app alive (not deadlocked/crashed)?" | Kill and restart the pod |
| **Readiness** | "Is the app ready to serve traffic?" | Remove from load balancer, do NOT restart |
| **Startup** | "Has the app finished starting?" | Wait (don't run liveness/readiness yet) |

**Example:** A pod connecting to a slow database on startup is **not ready** (readiness fails) but
**is alive** (liveness succeeds). Killing it would cause a restart loop. Only remove it from
the load balancer until the DB connection is established.

---

## Spring Boot Actuator Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true          # enables /actuator/health/liveness and /readiness
      show-details: always     # full details in dev; use WHEN_AUTHORIZED in prod
      show-components: always
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

This gives you:
- `GET /actuator/health` — aggregate status
- `GET /actuator/health/liveness` — liveness only
- `GET /actuator/health/readiness` — readiness only

---

## Custom Health Indicators

Implement `HealthIndicator` for any external dependency your app relies on:

```java
@Component("minioStorage")
public class MinioHealthIndicator implements HealthIndicator {
    private final MinioClient minioClient;
    private final String bucket;

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket).build());
            if (exists) {
                return Health.up()
                    .withDetail("bucket", bucket)
                    .withDetail("status", "reachable")
                    .build();
            }
            return Health.down()
                .withDetail("bucket", bucket)
                .withDetail("reason", "bucket not found")
                .build();
        } catch (Exception e) {
            return Health.down(e)
                .withDetail("bucket", bucket)
                .build();
        }
    }
}
```

Response when `GET /actuator/health`:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "H2", "validationQuery": "isValid()" } },
    "minioStorage": { "status": "UP", "details": { "bucket": "videos", "status": "reachable" } },
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

---

## Marking App as Not Ready Programmatically

When your app detects a recoverable error (e.g., downstream service temporarily down), you can
signal "not ready" without crashing:

```java
@Component
public class DownstreamHealthMonitor {
    private final ApplicationContext context;

    public void markNotReady(String reason) {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        log.warn("App marked as not ready: {}", reason);
    }

    public void markReady() {
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        log.info("App marked as ready");
    }
}
```

---

## Kubernetes Probe Configuration

```yaml
# Full probe config for a Spring Boot app
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 0    # startup probe handles the wait
  periodSeconds: 10
  failureThreshold: 3
  timeoutSeconds: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 0
  periodSeconds: 5
  failureThreshold: 3
  timeoutSeconds: 3

startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 30       # 150s max startup window
```

---

## Health Check Response Codes

Spring Actuator maps health status to HTTP status codes:

| Status | HTTP Code | Meaning |
|--------|-----------|---------|
| UP | 200 | Healthy |
| DOWN | 503 | Unhealthy — remove from LB |
| OUT_OF_SERVICE | 503 | Manually taken offline |
| UNKNOWN | 200 | Cannot determine (treated as up) |

Customize mappings:
```yaml
management:
  endpoint:
    health:
      status:
        http-mapping:
          DOWN: 503
          OUT_OF_SERVICE: 503
```

---

## Operational Playbook

**Pod in CrashLoopBackOff:**
```bash
kubectl logs <pod> --previous   # logs from crashed container
kubectl describe pod <pod>      # look at "Last State" and "Reason"
```

**Pod not receiving traffic (readiness failing):**
```bash
kubectl exec -it <pod> -- curl localhost:8080/actuator/health/readiness
# Check which component is DOWN
```

**Force pod out of rotation:**
```bash
# Manually mark not ready (useful during maintenance)
kubectl label pod <pod> app=demo-maintenance
```

---

## Doc Template Sections

When generating `docs/health-readiness.md`, include:
1. All health indicator components configured in this project
2. Actual health endpoint URLs and example responses
3. K8s probe YAML (from actual `k8s/` manifests if they exist)
4. Table: component → what it checks → failure behavior
5. How to add a new health indicator
6. Operational playbook for common probe failures
