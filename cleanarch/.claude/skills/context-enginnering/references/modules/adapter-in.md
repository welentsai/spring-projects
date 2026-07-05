# Module Rules: adapter/in (Inbound Adapters)

## Packages

| Package | Contents |
|---------|----------|
| `adapter.in` | `@RestController` classes |
| `adapter.in.dto` | Request records (`XxxRequest`) + Response records (`XxxResponse`) |
| `adapter.in.mapper` | Static mapper utilities (`XxxRequestMapper`) |
| `adapter.in.mcp` | MCP tool adapters (`XxxToolAdapter`, Spring AI `@Tool` methods) |

---

## Responsibility

Translate external input into use case inputs, delegate to the use case, translate the result
back into a response. **No business logic.** Two inbound transports exist:
HTTP (`adapter.in` controllers) and MCP (`adapter.in.mcp` tool adapters) — both reuse the
same Request DTO → mapper → use case chain.

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
- **Inbound adapters stay independent** — controllers and MCP tool adapters never depend on
  each other (ArchUnit-enforced)
- **MCP tool adapters** (`adapter.in.mcp`): name must end with `ToolAdapter`; may only depend
  on `usecase.ports.in` + `adapter.in` DTOs/mappers (never `adapter.out`, `usecase.ports.out`,
  `usecase.ports.in.impl`, `framework`, `domain`); no Spring stereotype annotations — wired
  as beans in `framework/config/McpConfig` together with the `ToolCallbackProvider`
- **No business logic** — `if (city.population > 1_000_000)` belongs in domain or use case
- **`validate()` is the only input check** controllers do — it returns `Optional<XxxRequest>`;
  an empty Optional throws `BadRequestException`
- **Mappers are utility classes** — private constructor, all-static methods, no state

---

## Pattern Reference

See `references/patterns/controller.md` for the full controller + DTO + mapper pattern, and
`references/patterns/mcp-tool-adapter.md` for the MCP tool adapter pattern.

---

## Checklist for New Endpoint

- [ ] `XxxRequest` record with `validate()` method in `adapter.in.dto`
- [ ] `XxxResponse` record in `adapter.in.dto`
- [ ] `XxxRequestMapper` utility class in `adapter.in.mapper`
- [ ] `XxxController` with `@RestController`, `@RequestMapping`, constructor injection
- [ ] Use the validate → map → execute → map → orElseThrow pipeline
- [ ] No `@Autowired` anywhere
- [ ] Test: `@WebMvcTest(XxxController.class)` + `@MockitoBean` for use case

## Checklist for New MCP Tool

- [ ] `XxxToolAdapter` class in `adapter.in.mcp` (suffix enforced by ArchUnit)
- [ ] `@Tool(name = "snake_case_name", description = "...")` on the tool method
- [ ] Reuse the existing `XxxRequest.validate()` → mapper → use case → response chain
- [ ] Register the adapter bean + tool callback in `framework/config/McpConfig`
- [ ] Test: plain unit test with mocked use case; verify registration in `McpConfigTest`
