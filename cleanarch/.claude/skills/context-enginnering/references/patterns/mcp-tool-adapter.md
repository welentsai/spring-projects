# Pattern: MCP Tool Adapter

MCP tool adapters are a **second inbound adapter** next to REST controllers. They expose use
cases as MCP tools over Spring AI's streamable HTTP server (`/mcp`) and reuse the exact same
Request DTO → mapper → use case pipeline as controllers.

Canonical example: `adapter/in/mcp/CitiesToolAdapter.java` (extracted 2026-07-05).

---

## 1. Tool Adapter Class (`adapter.in.mcp`)

```java
package com.example.demo.adapter.in.mcp;

import com.example.demo.adapter.in.dto.FindCitiesRequest;
import com.example.demo.adapter.in.dto.FindCitiesResponse;
import com.example.demo.adapter.in.mapper.CitiesRequestMapper;
import com.example.demo.exception.BadRequestException;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;

import org.springframework.ai.tool.annotation.Tool;

public class CitiesToolAdapter {

    private final FindCitiesUseCase findCitiesUseCase;

    public CitiesToolAdapter(FindCitiesUseCase findCitiesUseCase) {
        this.findCitiesUseCase = findCitiesUseCase;
    }

    @Tool(
            name = "find_cities",
            description = "Find all cities. Returns id, name and country for each city.")
    public FindCitiesResponse findCities() {
        return new FindCitiesRequest()
                .validate()
                .map(CitiesRequestMapper::toInput)
                .map(findCitiesUseCase::execute)
                .map(CitiesRequestMapper::toResponse)
                .orElseThrow(BadRequestException::new);
    }
}
```

Key points:
- **No Spring stereotype annotation** on the class — it is wired as a bean in `McpConfig`
- Class name ends with `ToolAdapter` (ArchUnit-enforced)
- Tool name is `snake_case`; description is written for the LLM consuming the tool
- Depends only on `usecase.ports.in` + `adapter.in` DTOs/mappers/`exception`
- Same `validate() → toInput → execute → toResponse → orElseThrow` chain as the controller

---

## 2. Wiring (`framework.config.McpConfig`)

```java
@Configuration
public class McpConfig {

    @Bean
    public CitiesToolAdapter citiesToolAdapter(FindCitiesUseCase findCitiesUseCase) {
        return new CitiesToolAdapter(findCitiesUseCase);
    }

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            CitiesToolAdapter citiesToolAdapter, VideoToolAdapter videoToolAdapter) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(citiesToolAdapter, videoToolAdapter)
                .build();
    }
}
```

Adding a new tool = new `@Bean` for the adapter **and** adding it to `.toolObjects(...)`.

---

## 3. Server Configuration (`application.properties`)

```properties
spring.ai.mcp.server.name=cleanarch-mcp
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.protocol=STREAMABLE
spring.ai.mcp.server.streamable-http.mcp-endpoint=/mcp
```

Local registration: `claude mcp add --transport http cleanarch http://localhost:8080/mcp`

---

## 4. Tests

- Unit: plain Mockito test calling the `@Tool` method with a mocked use case
  (see `CitiesToolAdapterTest`)
- Registration: `McpConfigTest` verifies the `ToolCallbackProvider` exposes the tool
- ArchUnit: `mcp_tool_adapters_should_have_tool_adapter_suffix`,
  `mcp_tool_adapters_should_only_depend_on_inbound_ports`,
  `controllers_and_mcp_tool_adapters_should_not_depend_on_each_other`
