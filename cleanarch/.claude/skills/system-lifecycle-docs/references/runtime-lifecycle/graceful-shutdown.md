# Reference: Graceful Shutdown

## Why Graceful Shutdown Matters

Without graceful shutdown, when Kubernetes rolls out a new version or scales down a pod:
- In-flight HTTP requests get a TCP reset (clients see "connection reset" errors)
- Database transactions may be left open
- Message consumers may drop unacknowledged messages
- File writes may be left incomplete

With graceful shutdown, the pod:
1. Stops accepting new requests
2. Finishes all in-flight requests
3. Releases resources cleanly
4. Exits with code 0

---

## Spring Boot Configuration

```yaml
# application.yml
server:
  shutdown: graceful              # default is "immediate" — change this

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s   # max wait for in-flight requests
```

That's the minimum. Spring Boot will:
- Stop accepting new connections on SIGTERM
- Wait up to 30s for active requests to complete
- Then shut down the context (destroy beans, close connections)

---

## Kubernetes SIGTERM Flow

```
kubectl delete pod / rolling update
  └─ K8s sends SIGTERM to container
       ├─ preStop hook runs (if configured) — wait for LB drain
       └─ Spring Boot starts graceful shutdown (30s window)
            ├─ New requests rejected (503)
            ├─ Existing requests complete
            └─ JVM exits → K8s removes pod from service endpoints
```

**The gap problem:** K8s removes the pod from the load balancer's endpoint list *asynchronously*.
There's a window where traffic still routes to a pod that has already received SIGTERM. The
`preStop` hook bridges this gap:

```yaml
lifecycle:
  preStop:
    exec:
      command: ["sh", "-c", "sleep 10"]   # wait 10s for LB to drain
```

Full deployment spec:

```yaml
spec:
  containers:
    - name: demo-app
      lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 10"]
  terminationGracePeriodSeconds: 60   # must be > preStop delay + shutdown timeout
```

`terminationGracePeriodSeconds` (60) > `preStop` sleep (10) + Spring shutdown timeout (30) = 40s.
Always leave margin.

---

## Bean Lifecycle Hooks

Register cleanup logic that runs during shutdown:

```java
@Component
public class VideoProcessingService {

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down video processing — waiting for active tasks...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(20, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in time — forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Video processing shutdown complete");
    }
}
```

For `SmartLifecycle` (more control over phase ordering):

```java
@Component
public class InboundGateway implements SmartLifecycle {
    private volatile boolean running = false;

    @Override
    public void stop(Runnable callback) {
        log.info("Stopping inbound gateway...");
        running = false;
        // drain queue
        callback.run();   // must call callback when done
    }

    @Override
    public int getPhase() { return Integer.MAX_VALUE; }  // stop last
    @Override
    public boolean isRunning() { return running; }
}
```

---

## Database Connection Cleanup

HikariCP closes connections cleanly during shutdown by default. Verify your pool config:

```yaml
spring:
  datasource:
    hikari:
      connection-timeout: 30000
      max-lifetime: 1800000
      keepalive-time: 600000
```

During shutdown, HikariCP:
1. Stops issuing new connections
2. Waits for active connections to be returned
3. Closes all connections

---

## Verifying Graceful Shutdown Locally

```bash
# Start the app
./mvnw spring-boot:run &
APP_PID=$!

# Send traffic
for i in {1..100}; do curl -s localhost:8080/api/v1/cities?region=US & done

# Trigger graceful shutdown while traffic is in-flight
kill -SIGTERM $APP_PID

# Observe: requests complete, no 5xx errors, "Graceful shutdown complete" in logs
```

Expected log output:
```
INFO Commencing graceful shutdown. Waiting for active requests to complete
INFO Graceful shutdown complete
```

---

## Checklist

- [ ] `server.shutdown=graceful` in `application.yml`
- [ ] `spring.lifecycle.timeout-per-shutdown-phase` set to a reasonable value (20-30s)
- [ ] `preStop` sleep hook configured in K8s Deployment
- [ ] `terminationGracePeriodSeconds` > preStop + shutdown timeout
- [ ] `@PreDestroy` on any long-running background services
- [ ] Executor pools shut down cleanly with `awaitTermination`
- [ ] Tested locally with in-flight traffic

---

## Doc Template Sections

When generating `docs/graceful-shutdown.md`, include:
1. Current shutdown config (from `application.yml`)
2. K8s Deployment lifecycle config (from `k8s/` manifests or document the recommended values)
3. All `@PreDestroy` / `SmartLifecycle` hooks in the codebase
4. Sequence diagram of shutdown steps
5. How to test graceful shutdown locally
6. `terminationGracePeriodSeconds` calculation for this app
