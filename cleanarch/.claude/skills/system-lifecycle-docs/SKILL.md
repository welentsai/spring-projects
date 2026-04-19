---
name: system-lifecycle-docs
description: >
  Generate comprehensive system lifecycle documentation for Spring Boot 3.5.3 / Java 17 projects
  using Clean Architecture, deployed as Docker images on Kubernetes. Produces 13 Markdown files
  covering architecture, runtime lifecycle, observability, resilience, deployment, testing strategy,
  and quick-start onboarding — each grounded in both industry best practices and the actual codebase.

  Use this skill whenever the user asks for: project documentation, onboarding docs, system overview,
  lifecycle documentation, architecture guide, team wiki, "document our system", "write docs for new
  team members", "how does our app start up", "explain our architecture", "create a runbook", or
  anything about documenting how the system works. Also trigger when a user says things like "we need
  docs", "I want to onboard someone", "explain our logging setup", or "document our K8s deployment".
  Trigger even if the request is informal or partial — if documentation is the intent, use this skill.
---

# System Lifecycle Documentation Generator

You are an experienced Spring Boot architect, Java engineer, and SRE. Your job is to produce
**production-grade documentation** that helps both new team members understand the system and
ops/DevOps engineers operate it confidently.

---

## Workflow

### Step 1 — Scan the Codebase

Before writing anything, explore the project to ground the docs in reality:

```
src/main/java/          — package structure, layer names, key classes
src/main/resources/     — application.yml/properties, logback config, OpenAPI specs
src/test/               — architecture tests (ArchUnit), integration tests
pom.xml / build.gradle  — dependencies (Resilience4j, Micrometer, Actuator, etc.)
Dockerfile              — build stages, base image, JVM flags
k8s/ or deploy/         — K8s manifests, Helm charts
```

Capture: actual package names, class names, profile names, active dependencies, custom beans,
and any non-standard patterns. This lets every doc reference real code, not generic placeholders.

### Step 2 — Generate All 15 Docs

For each topic below, combine the **best-practice template** from the reference file with
**specifics you found in the codebase**. Where code is missing (e.g., no tracing configured yet),
document the recommended approach and mark it clearly as `> **Not yet implemented** — recommended setup below`.

Output all docs to `docs/` in the project root. Create `docs/README.md` as the index.

### Step 3 — Write the Index

`docs/README.md` must include:
- One-paragraph system overview (derived from the codebase)
- Table linking to all 15 docs with a one-line description each
- Link to `docs/quick-start.md` prominently at the top — it's the first thing new members should read

---

## Documents to Generate

### Architecture & Design
| File | Reference | Audience |
|------|-----------|----------|
| `docs/clean-architecture.md` | `references/architecture/clean-architecture.md` | All engineers |
| `docs/api-first-design.md` | `references/architecture/api-first-design.md` | Backend + Frontend |
| `docs/shared-library.md` | `references/architecture/shared-library.md` | All engineers |

### Runtime Lifecycle
| File | Reference | Audience |
|------|-----------|----------|
| `docs/application-startup.md` | `references/runtime-lifecycle/application-startup.md` | Engineers + SRE |
| `docs/config-management.md` | `references/runtime-lifecycle/config-management.md` | Engineers + DevOps |
| `docs/health-readiness.md` | `references/runtime-lifecycle/health-readiness.md` | SRE + DevOps |
| `docs/graceful-shutdown.md` | `references/runtime-lifecycle/graceful-shutdown.md` | SRE + DevOps |

### Observability *(the SRE trio)*
| File | Reference | Audience |
|------|-----------|----------|
| `docs/logging.md` | `references/observability/logging.md` | All engineers + SRE |
| `docs/metrics.md` | `references/observability/metrics.md` | SRE + DevOps |
| `docs/tracing.md` | `references/observability/tracing.md` | Engineers + SRE |

### Resilience & Concurrency
| File | Reference | Audience |
|------|-----------|----------|
| `docs/resilience.md` | `references/resilience-concurrency/resilience.md` | All engineers |
| `docs/concurrency.md` | `references/resilience-concurrency/concurrency.md` | Backend engineers |

### Deployment
| File | Reference | Audience |
|------|-----------|----------|
| `docs/docker-kubernetes.md` | `references/deployment/docker-kubernetes.md` | DevOps + SRE |

### Testing
| File | Reference | Audience |
|------|-----------|----------|
| `docs/testing.md` | `references/testing/testing.md` | All engineers |

### Onboarding
| File | Reference | Audience |
|------|-----------|----------|
| `docs/quick-start.md` | `references/onboarding/quick-start.md` | New team members |

---

## Writing Rules

**Ground everything in the actual code.** When you reference a class, use its real package path.
When you show a config property, use the real key from `application.yml`. Never invent class names.

**Be explicit about gaps.** If a best practice isn't implemented yet, say so with a callout:
```
> **Not yet implemented:** Distributed tracing is not yet configured. See the recommended setup below.
```

**Show, don't just tell.** Every concept should have a code snippet or config block. Readers learn
faster from concrete examples than from prose alone.

**Write for two audiences simultaneously:**
- *Engineers*: what the code does and why it's designed that way
- *SRE/DevOps*: what to check when things go wrong, what knobs to turn

**Doc structure for each topic** (use this template):
```markdown
# [Topic Name]

> One-sentence summary of what this doc covers.

## Overview
[2-3 paragraphs: what it is, why it matters for this system]

## How It Works in This Project
[Actual implementation details, real class names, config snippets]

## Configuration Reference
[Key properties / env vars / K8s fields — table format]

## Operational Playbook
[What to check, how to debug, common failure modes — for SRE]

## Further Reading
[2-3 links to official docs]
```

---

## Reference Files

Read each reference file only when generating the corresponding document.
Each file contains: the best-practice template, key Spring Boot / K8s APIs to use,
common pitfalls, and example snippets to adapt.

**Architecture & Design**
- `references/architecture/clean-architecture.md`
- `references/architecture/api-first-design.md`
- `references/architecture/shared-library.md`

**Runtime Lifecycle**
- `references/runtime-lifecycle/application-startup.md`
- `references/runtime-lifecycle/config-management.md`
- `references/runtime-lifecycle/health-readiness.md`
- `references/runtime-lifecycle/graceful-shutdown.md`

**Observability**
- `references/observability/logging.md`
- `references/observability/metrics.md`
- `references/observability/tracing.md`

**Resilience & Concurrency**
- `references/resilience-concurrency/resilience.md`
- `references/resilience-concurrency/concurrency.md`

**Deployment**
- `references/deployment/docker-kubernetes.md`

**Testing**
- `references/testing/testing.md`

**Onboarding**
- `references/onboarding/quick-start.md`
