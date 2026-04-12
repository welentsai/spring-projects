# Pattern: Repository Interface (usecase/ports/out/repository)

Extracted from `CitiesQueryRepository` and `CitiesRepository`.

---

## Query Repository Interface (JdbcClient-backed reads)

```java
package com.example.demo.usecase.ports.out.repository;

import com.example.demo.usecase.ports.out.entity.XxxJpaEntity;

import java.util.List;
import java.util.Optional;

public interface XxxQueryRepository {
    /**
     * Find all items.
     */
    List<XxxJpaEntity> findAll();

    /**
     * Find by primary key.
     */
    Optional<XxxJpaEntity> findById(String id);

    /**
     * Find by a filter field.
     */
    List<XxxJpaEntity> findAllByField(String value);
}
```

## Write Repository Interface (JpaRepository-backed writes)

```java
package com.example.demo.usecase.ports.out.repository;

import com.example.demo.usecase.ports.out.entity.XxxJpaEntity;

import org.springframework.data.jpa.repository.JpaRepository;

// Extends JpaRepository directly — provides save(), delete(), etc.
public interface XxxRepository extends JpaRepository<XxxJpaEntity, String> {}
```

---

## CQRS Split

| Interface | Suffix | Backed by | Purpose |
|-----------|--------|-----------|---------|
| `XxxQueryRepository` | `QueryRepository` | `JdbcClient` impl | Reads — flexible SQL, dynamic datasource |
| `XxxRepository` | `Repository` | `JpaRepository` | Writes — insert / update / delete |

---

## Key Rules

- Interfaces live in `usecase.ports.out.repository` — no Spring annotations here
- `XxxQueryRepository` returns JPA entities (from `usecase.ports.out.entity`) — mapping to DTOs happens in the use case
- `XxxRepository extends JpaRepository` is acceptable even in the usecase layer because Spring Data is considered infrastructure that the port abstracts
- No Spring annotations on the interface itself (for `QueryRepository`)
