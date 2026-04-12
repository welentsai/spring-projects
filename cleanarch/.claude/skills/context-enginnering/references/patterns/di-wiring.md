# Pattern: Dependency Injection Wiring (framework/di)

Extracted from `BeanInjection` and `DataSourceConfig`.

---

## Use Case + Gateway Wiring

```java
package com.example.demo.framework.di;

import com.example.demo.adapter.out.gateway.XxxGatewayImpl;
import com.example.demo.usecase.ports.in.XxxUseCase;
import com.example.demo.usecase.ports.in.impl.XxxUseCaseImpl;
import com.example.demo.usecase.ports.out.gateway.XxxGateway;
import com.example.demo.usecase.ports.out.gateway.YyyGateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanInjection {

    // 1. Wire gateway implementation
    @Bean
    public XxxGateway xxxGateway(ExternalClient client) {
        return new XxxGatewayImpl(client);
    }

    // 2. Wire use case — inject port interfaces (not concrete impls)
    @Bean
    public XxxUseCase xxxUseCase(
            XxxQueryRepository xxxQueryRepository,
            XxxGateway xxxGateway,
            YyyGateway yyyGateway) {
        return new XxxUseCaseImpl(xxxQueryRepository, xxxGateway, yyyGateway);
    }
}
```

---

## What Gets Wired Here vs Elsewhere

| Component | Wired by | Reason |
|-----------|----------|--------|
| `XxxUseCaseImpl` | `BeanInjection` (`@Bean`) | No Spring annotations on impl |
| `XxxGatewayImpl` | `BeanInjection` (`@Bean`) | No Spring annotations on impl |
| `XxxQueryRepositoryImpl` | Auto (`@Repository`) | Has `@Repository` — Spring picks it up |
| `XxxController` | Auto (`@RestController`) | Has `@RestController` |
| `DataSource` routing | `DataSourceConfig` | Separate config for clarity |

---

## External Client Config (framework/config)

```java
package com.example.demo.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    @ConfigurationProperties(prefix = "xxx")
    public XxxProperties xxxProperties() {
        return new XxxProperties();
    }

    @Bean
    public ExternalClient externalClient(XxxProperties props) {
        return ExternalClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}
```

---

## Key Rules

- `BeanInjection` is the **only place** where `adapter.*` and `usecase.*` classes are
  imported together — this is intentional (it's the composition root)
- Always inject **interfaces** into use cases, never concrete implementations
- `@Configuration` classes live in `framework.di` or `framework.config` — never in adapter or usecase
- Use case impls and gateway impls have **no Spring annotations** — they are wired exclusively here
