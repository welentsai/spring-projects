# Pattern: Gateway Interface + Input / Output (usecase/ports/out/gateway)

Extracted from `InlineRoutingGateway`, `PutLogGateway`, `VideoStorageGateway` and their I/O types.

---

## Gateway Interface

```java
package com.example.demo.usecase.ports.out.gateway;

public interface XxxGateway {
    XxxOutput execute(XxxInput input);   // single method, always named execute()
}
```

---

## Input Type

Prefer **records** for immutability. Implement `Input` marker when used as use-case input.

```java
package com.example.demo.usecase.ports.out.gateway;

import com.example.demo.usecase.ports.in.Input;

// No-arg input
public record XxxInput() implements Input {}

// Input with data
public record XxxInput(String key, Object value) {}
```

---

## Output Type

Always a **record** — plain value carrier, no marker interface needed.

```java
package com.example.demo.usecase.ports.out.gateway;

public record XxxOutput(String resultKey) {}

// Static factory pattern (optional, for richer outputs)
public record XxxOutput(Map<String, Object> context) {
    public static XxxOutput of(Map<String, Object> context) {
        return new XxxOutput(context);
    }
}
```

---

## Naming Convention

| Artifact | Pattern | Example |
|----------|---------|---------|
| Interface | `XxxGateway` | `InlineRoutingGateway` |
| Input | `XxxInput` | `InlineRoutingInput` |
| Output | `XxxOutput` | `InlineRoutingOutput` |
| Implementation | `XxxGatewayImpl` | `InlineRoutingGatewayImpl` (in `adapter/out/gateway`) |

---

## Key Rules

- Interface + Input + Output all live together in `usecase.ports.out.gateway`
- Interface has **exactly one method**: `execute(XxxInput)`
- Input and Output are **records** — never mutable POJOs
- No Spring annotations — this is a pure Java port definition
- The implementation (`XxxGatewayImpl`) lives in `adapter.out.gateway` and is wired in `framework/di/`
