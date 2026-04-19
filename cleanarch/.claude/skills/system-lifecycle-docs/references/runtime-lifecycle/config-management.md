# Reference: Configuration Management

## 12-Factor App Principle

Store config in the environment. Code never changes between deployments; config does.
This means: **no hardcoded values, no environment-specific code branches, no config in source control**.

The hierarchy (last wins):
```
application.yml (defaults)
  ↓
application-{profile}.yml (profile overrides)
  ↓
Environment variables (K8s ConfigMap / Secret)
  ↓
Command-line args (rarely used)
```

---

## @ConfigurationProperties (Preferred Pattern)

Bind config to typed, validated POJOs — never use `@Value` for groups of related properties:

```java
@ConfigurationProperties(prefix = "app.minio")
@Validated
public record MinioProperties(
    @NotBlank String endpoint,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotBlank String bucket,
    @Positive int connectTimeoutSeconds
) {}
```

```yaml
# application.yml
app:
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket: videos
    connect-timeout-seconds: 5
```

Enable scanning:
```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class DemoApplication {}
```

Validation failures at startup (`@Validated`) make misconfiguration immediately visible — far
better than a NullPointerException at runtime when a property is first accessed.

---

## Profile Strategy

| Profile | Purpose | Config file |
|---------|---------|-------------|
| `(default)` | Local dev with H2 | `application.yml` |
| `test` | Integration tests | `application-test.yml` |
| `staging` | Pre-prod verification | Environment variables only |
| `prod` | Production | Environment variables only |

**Rule:** staging and prod get all config from environment variables (K8s ConfigMap/Secret).
No profile-specific YAML files for non-local environments — it prevents "works in staging" bugs.

---

## Kubernetes ConfigMap (Non-Secret Config)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: demo-app-config
data:
  SPRING_DATASOURCE_URL: "jdbc:postgresql://postgres-svc:5432/demo"
  APP_MINIO_ENDPOINT: "http://minio-svc:9000"
  APP_MINIO_BUCKET: "videos"
  LOGGING_LEVEL_COM_EXAMPLE: "INFO"
```

Mount as environment variables in the Deployment:

```yaml
envFrom:
  - configMapRef:
      name: demo-app-config
```

---

## Kubernetes Secret (Sensitive Config)

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: demo-app-secret
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: "appuser"
  SPRING_DATASOURCE_PASSWORD: "strongpassword"
  APP_MINIO_ACCESS_KEY: "accesskey"
  APP_MINIO_SECRET_KEY: "secretkey"
```

Mount as environment variables:
```yaml
envFrom:
  - secretRef:
      name: demo-app-secret
```

**Never log secret values.** Spring Boot masks secrets in `/actuator/env` by default for
properties containing "password", "secret", "key", "token". Verify with:
`curl localhost:8080/actuator/env | grep -i password`

---

## Dynamic Datasource Routing Config

For multi-datasource setups, configure each datasource separately:

```yaml
spring:
  datasource:
    primary:
      url: jdbc:h2:mem:primarydb
      driver-class-name: org.h2.Driver
    secondary:
      url: jdbc:h2:mem:secondarydb
      driver-class-name: org.h2.Driver
```

The routing key (`PRIMARY` / `SECONDARY`) is set per-request via `DataSourceContextHolder`
(ThreadLocal) — documented in the clean architecture doc.

---

## Actuator Config Endpoint

Expose config for debugging (never in production without auth):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, env, configprops
  endpoint:
    env:
      show-values: WHEN_AUTHORIZED   # requires authentication
```

```bash
# Check active profiles
curl localhost:8080/actuator/env | jq '.activeProfiles'

# Check a specific property
curl localhost:8080/actuator/env/app.minio.endpoint
```

---

## Property Naming Conventions

Spring Boot's relaxed binding maps between formats automatically:

| YAML key | Env variable | @Value |
|----------|-------------|--------|
| `app.minio.endpoint` | `APP_MINIO_ENDPOINT` | `${app.minio.endpoint}` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `${spring.datasource.url}` |

Always use **kebab-case in YAML**, **SCREAMING_SNAKE_CASE in env vars**. Never mix formats.

---

## Doc Template Sections

When generating `docs/config-management.md`, include:
1. All `@ConfigurationProperties` classes with their properties (scan `framework/di/config/`)
2. Profile strategy — what profiles exist, when each is used
3. Table of all externalized config keys with description, type, default, and whether it's a secret
4. K8s ConfigMap and Secret YAML examples (or real ones if they exist in `k8s/`)
5. How to add a new config property (checklist)
6. How to verify config at runtime (Actuator env endpoint)
