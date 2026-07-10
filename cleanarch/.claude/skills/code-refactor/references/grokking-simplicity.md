# Grokking Simplicity — applied to this codebase

Detailed catalog for the three simplicity lenses, with Java examples in this project's
layers. The SKILL.md body has the mental model; this file has the specific code smells and
the fixes. Jump to the section you need.

- [Lens 1: Actions / Calculations / Data](#lens-1-actions--calculations--data)
- [Lens 2: Immutability](#lens-2-immutability)
- [Lens 3: Stratified design](#lens-3-stratified-design)
---

## Lens 1: Actions / Calculations / Data

The goal: shrink actions down to thin I/O shells, grow the calculations they wrap, and keep
data inert. Three recurring smells.

### Smell 1a — Calculation trapped in an action

Business logic living inside a method that also does I/O. The logic can't be unit-tested
without a database, and it can't be reused.

**Input (adapter `@Service` doing everything):**
```java
@Service
public class CheckoutService {
    private final OrderRepository orders;
 
    public BigDecimal checkout(Long cartId) {
        Cart cart = orders.findCart(cartId);          // action: DB read
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem i : cart.getItems()) {            // calculation, buried
            BigDecimal line = i.getPrice().multiply(BigDecimal.valueOf(i.getQty()));
            if (i.getQty() >= 10) line = line.multiply(new BigDecimal("0.9"));
            total = total.add(line);
        }
        orders.saveTotal(cartId, total);                // action: DB write
        return total;
    }
}
```

**Output (calculation extracted into `domain`, action stays thin):**
```java
// domain — a pure calculation, no Spring, trivially testable
public final class CartPricing {
    public static BigDecimal total(List<CartItem> items) {
        return items.stream()
            .map(CartPricing::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static BigDecimal lineTotal(CartItem i) {
        BigDecimal line = i.price().multiply(BigDecimal.valueOf(i.qty()));
        return i.qty() >= 10 ? line.multiply(new BigDecimal("0.9")) : line;
    }
}
 
// usecase / adapter — the action now only orchestrates I/O
public BigDecimal checkout(Long cartId) {
    Cart cart = orders.findCart(cartId);                // action
    BigDecimal total = CartPricing.total(cart.items()); // calculation
    orders.saveTotal(cartId, total);                    // action
    return total;
}
```

The pricing rule is now a calculation you can test with a plain list and no mocks.

### Smell 1b — Action leaking into the domain

Anything in `domain` that reads the clock, generates a random/UUID, logs, or touches a
repository is an action where there should be none. It also breaks the dependency rule.
Fix by passing the result of the action *in* as data:

```java
// smell: domain depends on the wall clock (an implicit input + an action)
public boolean isExpired() { return expiry.isBefore(LocalDate.now()); }
 
// fix: the caller (an action at the edge) supplies "now" as data
public boolean isExpiredAsOf(LocalDate today) { return expiry.isBefore(today); }
```

### Smell 1c — Implicit inputs and outputs

A method relies on or mutates something beyond its explicit parameters and return value:
mutable fields, statics, shared collections. Minimize them — each one removed makes the
method easier to call, test, and reason about, and may turn it into a pure calculation.

- Implicit **input**: reads a mutable field, a static, the clock, a system property →
  pass it as a parameter.
- Implicit **output**: mutates a field, a passed-in collection, or shared state → return a
  new value instead.
---

## Lens 2: Immutability

Immutable data can't be changed out from under you, which removes a whole category of bugs
and makes calculations possible (a calculation over mutable data isn't reliably pure).

### Prefer records and final fields for data

```java
// smell: mutable value object
public class Money {
    private BigDecimal amount;
    public void setAmount(BigDecimal a) { this.amount = a; }  // mutation
}
 
// fix: a record is immutable, gives equals/hashCode/toString for free,
// and is the idiomatic Java 17 value object
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other) {                  // copy-on-write style
        return new Money(amount.add(other.amount), currency);
    }
}
```

### Copy-on-write for "modifications"

Don't mutate — return a changed copy. This is the pattern behind `map`/`filter` and behind
record "wither" methods:

```java
public record Order(String id, List<Line> lines) {
    public Order withLine(Line line) {
        var next = new ArrayList<>(lines);  // shallow copy
        next.add(line);                     // change the copy
        return new Order(id, List.copyOf(next)); // return new value
    }
}
```

### Defensive copying at trust boundaries

When immutable code receives or hands out a collection across a boundary it doesn't
control (legacy code, a caller you can't trust, a constructor taking a `List`), copy it so
nobody can mutate your internals through a shared reference:

```java
public record Order(String id, List<Line> lines) {
    public Order {                                  // compact constructor
        lines = List.copyOf(lines);                 // defensive copy in
    }
    public List<Line> lines() {
        return Collections.unmodifiableList(lines); // safe out
    }
}
```

Use a shallow `List.copyOf` / `Map.copyOf` when the elements are themselves immutable
(records), which is the common, cheap case. Reach for a deep copy only when elements are
mutable — and prefer making the elements immutable instead.
 
---

## Lens 3: Stratified design

Organize code into layers by rate of change and level of detail, so that each layer reads
clearly in terms of the one below it.

### Keep one level of abstraction per method

A method that mixes high-level intent with low-level fiddling is hard to read. Extract the
low-level steps so the method tells a story at one altitude:

```java
// smell: byte-level string fiddling sitting next to business intent
public void register(SignupRequest req) {
    if (req.email() == null || !req.email().matches("^[^@]+@[^@]+$")) { ... }
    var hash = MessageDigest.getInstance("SHA-256").digest(...);  // too low-level here
    repo.save(new User(req.email(), hash));
}
 
// fix: each line is at the same altitude; details live one layer down
public void register(SignupRequest req) {
    Email email = Email.of(req.email());        // validation pushed into the value object
    PasswordHash hash = hasher.hash(req.password());
    repo.save(new User(email, hash));
}
```

### Abstraction barriers and minimal interfaces

A port (interface) is an abstraction barrier: callers above it should not care how it's
implemented. Keep ports **minimal** — only the operations callers actually need. A
repository port with fifteen methods because the JPA implementation happened to expose them
is a leaky barrier. Define the port from the use case's needs, not the adapter's
capabilities.

### Read the dependency graph

Code near the **bottom** of the graph (domain calculations) is reused widely and is the
most important to test. Code near the **top** (controllers) is easiest to change because
little depends on it. Use this to prioritize: a finding in a heavily-depended-on domain
calculation matters more than the same finding in a single controller.

### Don't over-engineer

Premature abstraction is a stratified-design failure too. An interface with one
implementation that will never have another, a factory that wraps a single constructor, a
generic that's only ever used with one type — these add layers without adding clarity.
"Comfortable layers" means stopping when the design is good enough, not gold-plating it.
When in doubt, the simpler structure that a new teammate could follow wins.