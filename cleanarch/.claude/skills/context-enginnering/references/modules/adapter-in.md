# Module Rules: adapter/in (Inbound Adapters)

## Packages

| Package | Contents |
|---------|----------|
| `adapter.in` | `@RestController` classes |
| `adapter.in.dto` | Request records (`XxxRequest`) + Response records (`XxxResponse`) |
| `adapter.in.mapper` | Static mapper utilities (`XxxRequestMapper`) |

---

## Responsibility

Translate HTTP requests into use case inputs, delegate to the use case, translate the result
back into an HTTP response. **No business logic.**

---

## Allowed Imports

```
✅ com.example.demo.usecase.ports.in.*       (use case interfaces + Input/Result)
✅ com.example.demo.usecase.ports.in.dto.*   (shared DTOs for response mapping)
✅ com.example.demo.exception.*              (BadRequestException etc.)
✅ org.springframework.web.*                 (Spring MVC annotations)
✅ org.springframework.http.*                (ResponseEntity, HttpStatus)
```

## Forbidden Imports

```
❌ com.example.demo.adapter.out.*            (no outbound adapter coupling)
❌ com.example.demo.usecase.ports.out.*      (no direct port bypass)
❌ com.example.demo.domain.*                 (domain objects don't surface here)
❌ com.example.demo.framework.*              (no framework internals)
```

---

## Constraints

- **No `@Autowired`** — constructor injection only; constructor can be package-private
- **No controller → controller dependencies** — controllers never call each other
- **No business logic** — `if (city.population > 1_000_000)` belongs in domain or use case
- **`validate()` is the only input check** controllers do — it returns `Optional<XxxRequest>`;
  an empty Optional throws `BadRequestException`
- **Mappers are utility classes** — private constructor, all-static methods, no state

---

## Pattern Reference

See `references/patterns/controller.md` for the full controller + DTO + mapper pattern.

---

## Checklist for New Endpoint

- [ ] `XxxRequest` record with `validate()` method in `adapter.in.dto`
- [ ] `XxxResponse` record in `adapter.in.dto`
- [ ] `XxxRequestMapper` utility class in `adapter.in.mapper`
- [ ] `XxxController` with `@RestController`, `@RequestMapping`, constructor injection
- [ ] Use the validate → map → execute → map → orElseThrow pipeline
- [ ] No `@Autowired` anywhere
- [ ] Test: `@WebMvcTest(XxxController.class)` + `@MockitoBean` for use case
