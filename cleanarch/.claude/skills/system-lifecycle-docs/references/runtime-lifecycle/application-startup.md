# Reference: Application Startup

## Spring Boot Startup Sequence

Understanding the startup order is critical for debugging "bean not found", circular dependencies,
and race conditions where a bean tries to use a resource that isn't ready yet.

```
JVM starts
  └─ SpringApplication.run()
       ├─ 1. Load ApplicationContext
       ├─ 2. Process @Configuration classes
       ├─ 3. Register beans (BeanDefinition phase)
       ├─ 4. Instantiate beans (constructor injection)
       ├─ 5. @PostConstruct methods
       ├─ 6. ApplicationRunner / CommandLineRunner (ordered)
       ├─ 7. ApplicationReadyEvent fired
       └─ 8. Server accepts traffic
```

The key insight: **no traffic is accepted until step 8**. If your health check endpoint returns
200 before step 6 completes, Kubernetes will route traffic to an app that isn't fully initialized.
Use readiness probes correctly (see `health-readiness.md`) to prevent this.

---

## Startup Hooks

### ApplicationRunner (preferred)

Use `ApplicationRunner` when you need access to `ApplicationArguments`:

```java
@Component
@Order(1)
public class DatabaseHealthCheck implements ApplicationRunner {
    private final DataSource dataSource;

    public DatabaseHealthCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var conn = dataSource.getConnection()) {
            log.info("Database connectivity verified: {}", conn.getMetaData().getURL());
        } catch (SQLException e) {
            log.error("Database not reachable at startup — failing fast", e);
            throw new IllegalStateException("Cannot connect to database", e);
        }
    }
}
```

### CommandLineRunner (simple use cases)

```java
@Bean
@Order(2)
CommandLineRunner loadReferenceData(CityRepository cityRepository) {
    return args -> {
        if (cityRepository.count() == 0) {
            log.info("Loading initial reference data...");
            // seed data
        }
    };
}
```

### @PostConstruct (bean-level init)

```java
@Component
public class VideoConfig {
    private MinioClient client;

    @PostConstruct
    public void init() {
        this.client = buildClient();
        log.info("MinIO client initialized: {}", endpoint);
    }
}
```

**Ordering:** `@PostConstruct` runs before `ApplicationRunner`. Use `@Order` to control runner sequence.

---

## Fail-Fast Strategy

**Crash on startup if critical dependencies are unavailable.** A pod in `CrashLoopBackOff` is
easier to diagnose than a pod that starts healthy but silently fails on first request.

```yaml
# application.yml — validate config at startup
spring:
  jpa:
    properties:
      hibernate:
        hbm2ddl:
          auto: validate   # fails startup if schema doesn't match entities
```

```java
// Validate @ConfigurationProperties at startup
@ConfigurationProperties(prefix = "app.minio")
@Validated
public record MinioProperties(
    @NotBlank String endpoint,
    @NotBlank String accessKey,
    @NotBlank String secretKey,
    @NotBlank String bucket
) {}
```

---

## Startup Probes in Kubernetes

Spring Boot needs time to start — typically 10-30s. Without a startup probe, K8s may kill the
pod before it's ready. Configure a startup probe with a generous `failureThreshold`:

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 30     # 30 × 5s = 150s max startup time
```

Once the startup probe succeeds, K8s switches to the liveness probe.

---

## Logging Startup Events

Log key milestones so you can reconstruct the startup sequence from logs:

```java
@EventListener(ApplicationReadyEvent.class)
public void onApplicationReady() {
    log.info("Application started successfully. Profile(s): {}",
        Arrays.toString(environment.getActiveProfiles()));
}

@EventListener(ApplicationFailedEvent.class)
public void onApplicationFailed(ApplicationFailedEvent event) {
    log.error("Application startup failed", event.getException());
}
```

---

## Common Startup Failures

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `NoSuchBeanDefinitionException` | Missing `@Bean` or wrong package scan | Check `framework/di/config/` wiring |
| `BeanCurrentlyInCreationException` | Circular dependency | Introduce interface or restructure |
| `DataSourceNotFoundException` | DB config missing | Check `spring.datasource.*` properties |
| `UnsatisfiedDependencyException` | Constructor arg type mismatch | Check `@Configuration` return types |
| Pod stuck in `Init` | `ApplicationRunner` blocking | Add timeout or async init |

---

## Doc Template Sections

When generating `docs/application-startup.md`, include:
1. Startup sequence diagram (customized to this project's actual beans)
2. All `ApplicationRunner` / `CommandLineRunner` / `@PostConstruct` hooks in this project
3. Fail-fast validations configured (schema validation, config validation)
4. Startup time observed (if measurable from logs or actuator metrics)
5. K8s startup probe configuration
6. Common startup failures specific to this project's tech stack
