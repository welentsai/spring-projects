# Pattern: Domain Model (domain/model)

Extracted from `City`.

---

## Full Pattern

```java
package com.example.demo.domain.model;

// Pure Java record — no Spring, no JPA, no external framework imports
public record City(String id, String name, String country) {}
```

---

## When to Use a Record vs a Class

| Situation | Use |
|-----------|-----|
| Simple value object (data only) | `record` |
| Needs domain behavior / methods | `class` |
| Needs mutability | `class` |

---

## Domain Class with Behavior

```java
package com.example.demo.domain.model;

public class Order {
    private final String id;
    private final List<OrderItem> items;
    private OrderStatus status;

    public Order(String id, List<OrderItem> items) {
        this.id = id;
        this.items = List.copyOf(items);
        this.status = OrderStatus.PENDING;
    }

    // Domain behavior — business logic lives here, not in use case
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm order in status: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
    }
}
```

---

## Key Rules

- **Zero Spring imports** — `@Component`, `@Service`, `@Entity` are all forbidden
- **Zero JPA imports** — no `jakarta.persistence.*`
- Domain records/classes are **immutable by default** (prefer `record` or final fields)
- Business invariants and rules live **here**, not in the use case layer
- Domain objects are mapped to/from JPA entities in the use case layer — they never touch the DB directly
