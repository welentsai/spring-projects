# Pattern: JPA Entity (usecase/ports/out/entity)

Extracted from `CityJpaEntity`.

---

## Full Pattern

```java
package com.example.demo.usecase.ports.out.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class XxxJpaEntity {

    @Id
    private String id;

    private String fieldA;
    private String fieldB;

    // Required no-arg constructor for JPA
    public XxxJpaEntity() {}

    // All-args constructor for programmatic creation
    public XxxJpaEntity(String id, String fieldA, String fieldB) {
        this.id = id;
        this.fieldA = fieldA;
        this.fieldB = fieldB;
    }

    // Getters and setters for all fields (required by JPA + JdbcClient row mapping)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFieldA() { return fieldA; }
    public void setFieldA(String fieldA) { this.fieldA = fieldA; }

    public String getFieldB() { return fieldB; }
    public void setFieldB(String fieldB) { this.fieldB = fieldB; }
}
```

---

## Naming

- Class name: `XxxJpaEntity` (always suffixed with `JpaEntity`)
- Table name (H2 default): Spring derives from class name → `xxx_jpa_entity`
- If custom table name is needed: `@Table(name = "custom_table")`

---

## Why Entities Live in usecase/ports/out/entity

JPA entities are technically a persistence concern, but this project places them in the use case
output port layer so that both `JpaRepository` (write) and `JdbcClient` (read) implementations
can share the same entity type without a cross-layer dependency.

The use case implementation maps these entities to DTOs (`usecase.ports.in.dto`) before
returning them — entities never escape the use case boundary.

---

## Key Rules

- Always `@Entity` + `@Id` — no `@GeneratedValue` unless auto-increment PK is needed
- Must have a **no-arg constructor** (JPA requirement)
- Use plain getters/setters — no Lombok in this project
- Never use these entities directly in controllers or response types — map to DTOs first
