# Reference: Docker & Kubernetes Deployment

## Multi-Stage Dockerfile

Build a minimal, secure production image. The build stage compiles the JAR; the runtime stage
contains only what's needed to run it — no Maven, no source code, no build tools.

```dockerfile
# ── Build stage ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Copy dependency descriptors first — layer caches when deps don't change
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# Extract layered JAR (faster startup, better layer caching)
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ── Runtime stage ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy layers in order of change frequency (dependencies change least)
COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

EXPOSE 8080

# JVM tuning for containers
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
```

**Key JVM flags explained:**
- `UseContainerSupport` — reads cgroup memory limits (not host RAM) for heap sizing
- `MaxRAMPercentage=75.0` — use 75% of container memory limit for heap
- `ExitOnOutOfMemoryError` — let K8s restart the pod instead of running out-of-memory silently
- `java.security.egd` — faster startup by using `/dev/urandom` for entropy

---

## Image Build & Tag Strategy

```bash
# Build
docker build -t demo-app:$(git rev-parse --short HEAD) .

# Tag convention: never use "latest" in production
# demo-app:a1b2c3d   ← git SHA (immutable, traceable)
# demo-app:1.2.3     ← semantic version for releases
```

Scan image for vulnerabilities before pushing:
```bash
docker scout cves demo-app:a1b2c3d
# or
trivy image demo-app:a1b2c3d
```

---

## Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: demo-app
  labels:
    app: demo-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: demo-app
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0      # always keep all replicas available during rollout
      maxSurge: 1            # add one extra pod during rollout
  template:
    metadata:
      labels:
        app: demo-app
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      terminationGracePeriodSeconds: 60
      containers:
        - name: demo-app
          image: demo-app:a1b2c3d
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: demo-app-config
            - secretRef:
                name: demo-app-secret
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            periodSeconds: 5
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]
          securityContext:
            runAsNonRoot: true
            runAsUser: 1000
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
```

---

## Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: demo-app-svc
spec:
  selector:
    app: demo-app
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

---

## Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: demo-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: demo-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

---

## Resource Sizing Guide

| App Size | Memory Request | Memory Limit | CPU Request | CPU Limit |
|----------|---------------|--------------|-------------|-----------|
| Small (< 100 RPS) | 256Mi | 512Mi | 100m | 500m |
| Medium (100-500 RPS) | 512Mi | 1Gi | 250m | 1000m |
| Large (> 500 RPS) | 1Gi | 2Gi | 500m | 2000m |

Always set **both** requests and limits. Without limits, one pod can starve other pods on the same node.
Without requests, the scheduler can't make placement decisions.

---

## Rolling Deployment Commands

```bash
# Deploy new version
kubectl set image deployment/demo-app demo-app=demo-app:a1b2c3d

# Watch rollout
kubectl rollout status deployment/demo-app

# Rollback if something's wrong
kubectl rollout undo deployment/demo-app

# Check rollout history
kubectl rollout history deployment/demo-app
```

---

## Operational Playbook

**Pod OOMKilled:**
```bash
kubectl describe pod <pod>   # look for "OOMKilled" in Last State
# Increase memory limit or fix memory leak (heap dump, GC logs)
```

**Pod CrashLoopBackOff:**
```bash
kubectl logs <pod> --previous   # logs from last crashed container
kubectl describe pod <pod>      # exit code (137=OOM, 1=app error, 143=SIGTERM)
```

**ImagePullBackOff:**
```bash
kubectl describe pod <pod>   # check image name and registry credentials
```

**Deployment stuck:**
```bash
kubectl rollout status deployment/demo-app   # shows which replica is failing
kubectl get events --sort-by=.metadata.creationTimestamp
```

---

## Doc Template Sections

When generating `docs/docker-kubernetes.md`, include:
1. Actual `Dockerfile` content with explanation of each layer
2. Image tag strategy and where images are stored (registry URL)
3. K8s manifests location in repo (`k8s/` directory) or document recommended manifests
4. Resource requests/limits configured (or recommended based on app profile)
5. HPA configuration (or recommendation)
6. Deployment procedure (commands to deploy, verify, rollback)
7. Operational playbook for common K8s failure scenarios
