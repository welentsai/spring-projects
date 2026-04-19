# Reference: Quick Start Guide

## Purpose

The Quick Start doc is the **first thing a new team member reads**. It should get them from
zero to a running local environment with a working API call in under 15 minutes. Every step
must be concrete and copy-pasteable. No assumptions about prior knowledge of the project.

Keep it short. Link to the detailed docs for deeper understanding — don't duplicate them here.

---

## Sections to Include

### 1. Prerequisites

List exactly what needs to be installed, with version requirements:

```markdown
## Prerequisites

| Tool | Minimum Version | Install |
|------|----------------|---------|
| Java | 17 | `brew install openjdk@17` |
| Maven | 3.9+ | bundled via `mvnw` (no install needed) |
| Docker | 24+ | docker.com/get-started |
| Git | 2.x | pre-installed on Mac/Linux |
| IntelliJ IDEA (recommended) | 2023.3+ | jetbrains.com |
```

Scan `pom.xml` for `<java.version>` and any Docker/K8s tooling in the project to derive real requirements.

### 2. Clone & Build

```bash
git clone <repo-url>
cd <project-name>
./mvnw clean install -DskipTests   # first build downloads dependencies (~2 min)
```

### 3. Run Locally

```bash
./mvnw spring-boot:run
```

Expected output (confirm app is ready):
```
Started DemoApplication in X.XXX seconds
```

### 4. Verify It Works — Make Your First API Call

Show the simplest possible working API call with the expected response:

```bash
# List cities
curl -s "http://localhost:8080/api/v1/cities?region=US" | jq .

# Expected response:
{
  "cities": [
    { "id": "1", "name": "New York", "region": "US" }
  ]
}
```

### 5. Explore the App

Useful local URLs:
| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/actuator/health` | Health status |
| `http://localhost:8080/h2-console` | H2 DB console (JDBC: `jdbc:h2:mem:primarydb`) |

### 6. Run the Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=FindCitiesUseCaseImplTest

# Architecture tests only
./mvnw test -Dtest=ArchunitRuleTest

# Skip tests (dev cycle)
./mvnw spring-boot:run -DskipTests
```

### 7. Code Formatting

The project enforces Spotless formatting — run before committing:

```bash
./mvnw spotless:apply   # auto-format all files
./mvnw spotless:check   # verify (fails if unformatted files exist)
```

### 8. Project Structure — 5-Minute Tour

Give a short opinionated tour:

```
src/main/java/com/example/demo/
├── adapter/in/          → Start here: REST controllers (entry points)
├── usecase/ports/in/    → Business logic interfaces + implementations
├── adapter/out/         → Database and external service implementations
├── domain/model/        → Pure domain objects (no framework)
└── framework/di/        → Spring wiring (the only place @Bean lives)
```

Point to `docs/clean-architecture.md` for the full explanation.

### 9. Add a Feature — Minimal Walkthrough

Give a concrete example of adding the smallest possible thing end-to-end:

```markdown
### Example: Add a new endpoint

1. Define the use case interface in `usecase/ports/in/`
2. Implement it in `usecase/ports/in/impl/` (no Spring annotations)
3. Wire it in `framework/di/config/AppConfig.java`
4. Add the controller in `adapter/in/`
5. Add repository/gateway if needed in `usecase/ports/out/` + `adapter/out/`
6. Run `./mvnw test` — ArchUnit catches misplaced classes automatically
```

### 10. Key Contacts & Resources

```markdown
## Getting Help

- Architecture questions → `docs/clean-architecture.md`
- Can't connect to a service → `docs/health-readiness.md`
- Debugging a request → `docs/logging.md` (search by `requestId`)
- Slack channel: #team-backend
- On-call runbook: `docs/docker-kubernetes.md`
```

---

## Writing Rules for quick-start.md

- **Every command must be copy-pasteable.** No placeholders like `<your-value>` unless absolutely
  necessary, and if so, call it out explicitly.
- **Show expected output** for the first API call and the startup log. New members need to know
  what "it's working" looks like.
- **No deep explanations here.** One sentence + link to the relevant doc. Keep it scannable.
- **Test every command yourself** (or note which ones depend on infrastructure not available locally).
- **Order matters** — each step must succeed before the next step makes sense.

---

## Doc Template Sections

When generating `docs/quick-start.md`, include:
1. Prerequisites table (derive from `pom.xml`, `Dockerfile`, CI config)
2. Clone → Build → Run steps (exact commands, expected output)
3. First working API call with real endpoint from the codebase + expected response
4. Local URLs table (Swagger, Actuator, H2 console)
5. Test commands (all tests, single test, format check)
6. Annotated package structure (5-minute tour)
7. "Add a feature" minimal checklist (concrete, not abstract)
8. Links to next docs to read (suggest: clean-architecture → api-first-design → logging)
9. Team contacts / help channels
