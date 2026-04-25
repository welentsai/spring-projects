---
name: cleanarch-context-engineering
description: >
  Context engineering skill for a Spring Boot 3.5.x / Java 17 project using Clean Architecture
  (3 layers: domain → usecase → adapter). Use this skill whenever the user wants to:
  - Generate or refresh CLAUDE.md / project context documents
  - Analyze the current codebase structure
  - Generate new feature development guides
  - Create or update Architecture Decision Records (ADR)
  - Onboard a new AI session with full project context
  - Ask "what does this project do", "how is it structured", "help me add a feature"
  Always trigger this skill when the user is working on the cleanarch Spring Boot project,
  even if they don't explicitly mention "context engineering".
---

# Clean Architecture Context Engineering Skill

## Project Metadata

| Field | Value |
|-------|-------|
| **Path** | `/Users/welentsai/Workspace/spring-projects/cleanarch` |
| **Framework** | Spring Boot 3.5.x |
| **Java Version** | Java 17 |
| **Architecture** | Clean Architecture (3-layer) |

---

## Architecture Overview

```
┌─────────────────────────────────────┐
│         adapter layer (outer)        │  ← Controllers, Repositories impl,
│                                     │    External service adapters, Config
├─────────────────────────────────────┤
│        usecase layer (middle)        │  ← Application services, Use case
│                                     │    interactors, Port interfaces
├─────────────────────────────────────┤
│         domain layer (inner)         │  ← Entities, Value objects,
│                                     │    Domain services, Domain events
└─────────────────────────────────────┘
```

**Dependency rule**: inner layers must never depend on outer layers.
- `domain` has zero Spring/framework dependencies
- `usecase` depends only on `domain`
- `adapter` depends on `usecase` and `domain`

---

## Step 1: Auto-Scan Codebase (Always Run First)

Every time this skill is triggered, scan the project before doing anything else.

```bash
PROJECT=/Users/welentsai/Workspace/spring-projects/cleanarch

# 1. Directory tree (exclude build artifacts)
find $PROJECT -type d | grep -v -E '(target|\.git|\.idea|build|node_modules)' | sort

# 2. All Java source files
find $PROJECT/src/main/java -name "*.java" | sort

# 3. Build file
cat $PROJECT/pom.xml 2>/dev/null || cat $PROJECT/build.gradle 2>/dev/null

# 4. Existing CLAUDE.md if present
cat $PROJECT/CLAUDE.md 2>/dev/null || echo "(no CLAUDE.md yet)"

# 5. Application properties
cat $PROJECT/src/main/resources/application.yml 2>/dev/null || \
cat $PROJECT/src/main/resources/application.properties 2>/dev/null
```

After scanning, identify and record:
- Root package name (e.g., `com.example.cleanarch`)
- Module/bounded context names
- Which packages map to which layer
- Key domain entities
- Exposed HTTP endpoints (from `@RestController` classes)
- External dependencies (DBs, message queues, external APIs)

---

## Step 2: Determine Task

Based on what the user is asking, pick the appropriate output mode:

| User Intent | Go To |
|-------------|-------|
| Generate / refresh project context | → [Generate CLAUDE.md](#generate-claudemd) |
| Analyze structure or explain the project | → [Structure Analysis](#structure-analysis) |
| Help add a new feature | → [Feature Development Guide](#feature-development-guide) |
| Record an architecture decision | → [ADR Generation](#adr-generation) |
| Show code pattern for a specific layer | → [Code Patterns](#code-patterns) |
| Show module rules for a specific package | → [Module Rules](#module-rules) |
| Generate package-level CLAUDE.md files | → [Generate Package CLAUDE.md](#generate-package-claudemd) |
| Refresh stale package CLAUDE.md files | → [Refresh Mode](#refresh-mode) |
| Plan which folders should have CLAUDE.md | → [CLAUDE.md Placement Strategy](#claudemd-placement-strategy) |
| Full context dump for new AI session | → Run all outputs |

---

## CLAUDE.md Placement Strategy

當使用者詢問「哪些 folder 要加 CLAUDE.md」、「如何規劃 context 架構」時執行此流程。

### Claude Code 的讀取機制

Claude Code 在某個目錄工作時，**沿著路徑由根到當前目錄疊加讀取**所有 CLAUDE.md，不是只讀最近的那一個：

```
# 在 adapter/in/web/ 工作時，以下全部都會被讀取：
根目錄/CLAUDE.md          ✅ 讀
adapter/CLAUDE.md          ✅ 讀
adapter/in/CLAUDE.md       ✅ 讀
adapter/in/web/CLAUDE.md   ✅ 讀
```

### 判斷是否需要放置 CLAUDE.md 的三個問題

對每個目錄問以下三個問題，若全部都是「否」則不用放：

1. 這個 folder 有**獨特的規範**，上層 CLAUDE.md 沒說過的嗎？
2. 工程師或 AI 在這裡容易**犯特定的錯誤**嗎？
3. 這裡的 pattern 在看程式碼時**不容易自己推斷**嗎？

### 放置策略（Clean Architecture 專案）

```
your-project/
├── CLAUDE.md                          ✅ 必放：專案總綱、架構概覽、Dependency Rule
│
└── src/main/java/com/example/demo/
    ├── domain/model/
    │   └── CLAUDE.md                  ✅ 必放：最重要，零 framework import 規則、record vs class 規範
    │
    ├── usecase/ports/in/
    │   └── CLAUDE.md                  ✅ 必放：UseCase interface contract、Result 設計規範
    │
    ├── usecase/ports/in/impl/
    │   └── CLAUDE.md                  ✅ 必放：無 Spring annotation、Exception 不外傳、Entity 不出界
    │
    ├── usecase/ports/out/gateway/
    │   └── CLAUDE.md                  ✅ 必放：三件套（interface + Input + Output record）規範
    │
    ├── usecase/ports/out/repository/
    │   └── CLAUDE.md                  ⚠️ 選放：有 CQRS 分工（JdbcClient read / JpaRepository write）才放
    │
    ├── adapter/in/
    │   └── CLAUDE.md                  ✅ 必放：validate().map().map().map().orElseThrow() 完整管道
    │
    ├── adapter/out/gateway/
    │   └── CLAUDE.md                  ✅ 必放：無 Spring annotation、LoggingAspect 自動覆蓋、單一 interface
    │
    ├── adapter/out/repository/
    │   └── CLAUDE.md                  ✅ 必放：@Repository 唯一允許、JdbcClient named parameter 規範
    │
    ├── framework/di/
    │   └── CLAUDE.md                  ✅ 必放：Composition Root 規範、@Bean 回傳 interface 規則
    │
    ├── util/{complex-utility}/
    │   └── CLAUDE.md                  ⚠️ 選放：API 複雜（如 PhaseTaskIterator）才放使用範例
    │
    ├── exception/                     ❌ 不放：規則簡單，根目錄已說明
    └── system/                        ❌ 不放：AOP 規則已在 adapter/out/gateway CLAUDE.md 說明
```

### 每層 CLAUDE.md 的適當長度

| 層級 | 建議行數 | 內容重點 |
|------|---------|---------|
| 根目錄 | 100–200 行 | 架構總覽、Dependency Rule、命名規範、測試策略 |
| domain | 80–150 行 | 業務規則、record vs class、零 framework import |
| usecase | 80–120 行 | UseCase contract、Result 設計、CQRS 分工 |
| adapter | 60–100 行 | 實作規範、Spring annotation 限制、禁止事項 |
| framework/di | 40–80 行 | Composition Root、@Bean 配線原則 |
| util（複雜工具） | 30–60 行 | 使用範例、API 說明，不重複 Javadoc |

### 反模式（避免）

```
❌ 每個 folder 都放，內容都差不多
   → 浪費 context window，AI 讀到重複資訊

❌ 上下層說同一件事
   根目錄說「禁止跨層依賴」
   adapter/CLAUDE.md 又說一次
   adapter/in/CLAUDE.md 又說一次
   → 三次重複，沒有增加資訊

✅ 上層說原則，下層說具體規則，最深層說實作細節
   根目錄/CLAUDE.md    → 「禁止跨層依賴」（原則）
   adapter/in/CLAUDE.md → 「Controller 只能注入 Port/In interface」（具體規則）
```

### 自我驗證問題

放完之後問自己：

> 如果一個新工程師**只讀這個 folder 的 CLAUDE.md**，
> 他會得到在其他地方**讀不到**的資訊嗎？
>
> **YES → 值得放　　NO → 刪掉，讓上層覆蓋就好**

### 此專案的現況對照

| 路徑 | 狀態 | 說明 |
|------|------|------|
| `CLAUDE.md` | ✅ 已放 | 根目錄總綱 |
| `adapter/in/CLAUDE.md` | ✅ 已放 | validate 管道規範 |
| `adapter/out/gateway/CLAUDE.md` | ✅ 已放 | Gateway 實作規範 |
| `adapter/out/repository/CLAUDE.md` | ✅ 已放 | JdbcClient 規範 |
| `usecase/ports/in/CLAUDE.md` | ✅ 已放 | UseCase contract |
| `usecase/ports/in/impl/CLAUDE.md` | ✅ 已放 | UseCase 實作規範 |
| `usecase/ports/out/gateway/CLAUDE.md` | ✅ 已放 | Gateway port 規範 |
| `domain/model/CLAUDE.md` | ✅ 已放 | Domain model 規範 |
| `framework/di/CLAUDE.md` | ✅ 已放 | DI 配線規範 |
| `usecase/ports/out/repository/` | ⚠️ 可補 | CQRS 分工規則 |
| `util/multiphaseterator/` | ⚠️ 可補 | PhaseTaskIterator 使用範例 |
| `exception/`, `system/` | ❌ 不需放 | 規則已在上層說明 |

---

## Generate CLAUDE.md

Produce a `CLAUDE.md` at the project root. This file is the primary context document for AI sessions.

**Template:**

```markdown
# CLAUDE.md — {Project Name}

> Auto-generated by cleanarch-context-engineering skill. Last updated: {date}

## Project Overview
{1-2 sentences describing what the system does}

## Tech Stack
- Java 17, Spring Boot {version}
- {Database}: {driver/ORM}
- {Other notable dependencies from pom.xml}

## Architecture: Clean Architecture (3 Layers)

### Layer Map
| Layer | Package | Responsibility |
|-------|---------|----------------|
| domain | `{root}.domain` | Entities, Value Objects, Domain Services |
| usecase | `{root}.usecase` | Use Case interactors, Port interfaces (in/out) |
| adapter | `{root}.adapter` | Controllers, Repository impls, External adapters |

### Dependency Rule
domain ← usecase ← adapter  (arrows = "depends on")
No reverse dependencies allowed.

## Domain Model
{List key entities and their core attributes, discovered from scan}

## Use Cases
{List use case classes/interfaces discovered from scan}

## API Endpoints
{List all @RequestMapping / @GetMapping / @PostMapping etc. discovered}

## Key Conventions
{Naming patterns, port naming, use case naming observed in codebase}

## Running Locally
{From application.properties/yml: ports, profiles, required env vars}

## Testing Strategy
{From test directory structure}
```

Save the file to `{PROJECT}/CLAUDE.md` and confirm to the user.

---

## Structure Analysis

When the user wants to understand the codebase:

1. Show the layer breakdown with package names and file counts per layer
2. List all domain entities with brief descriptions
3. List all use cases
4. List all adapter entry points (REST controllers, message listeners, scheduled jobs)
5. Flag any **dependency rule violations** (e.g., domain importing Spring annotations, usecase importing adapter classes)
6. Note any patterns observed: ports & adapters naming, DTO conventions, exception handling strategy

Present as a structured markdown report in the conversation.

---

## Feature Development Guide

When the user wants to add a new feature, generate a step-by-step guide tailored to this project's structure.

**Gather from user:**
- Feature name (e.g., "user registration")
- Brief description of what it does

**Then generate:**

```markdown
# Feature Guide: {Feature Name}

## 1. Domain Layer
- [ ] Create entity: `{root}.domain.model.{Entity}.java`
- [ ] Create value objects if needed: `{root}.domain.model.{ValueObject}.java`
- [ ] Create domain service if needed: `{root}.domain.service.{Entity}DomainService.java`

## 2. Use Case Layer
- [ ] Define input port: `{root}.usecase.port.in.{FeatureName}UseCase.java`
- [ ] Define output port: `{root}.usecase.port.out.{Entity}Repository.java` (if new)
- [ ] Implement use case: `{root}.usecase.interactor.{FeatureName}Interactor.java`
- [ ] Create command/query DTO: `{root}.usecase.dto.{FeatureName}Command.java`

## 3. Adapter Layer
- [ ] REST controller: `{root}.adapter.in.web.{FeatureName}Controller.java`
- [ ] Request/Response DTO: `{root}.adapter.in.web.dto.{FeatureName}Request.java`
- [ ] Repository implementation: `{root}.adapter.out.persistence.{Entity}RepositoryImpl.java`

## 4. Tests
- [ ] Domain unit test: `{Entity}Test.java`
- [ ] Use case unit test: `{FeatureName}InteractorTest.java` (mock output ports)
- [ ] Integration test: `{FeatureName}ControllerIT.java`

## Dependency Rule Checklist
- domain classes import nothing from Spring / usecase / adapter ✓
- usecase classes import only domain + port interfaces ✓
- adapter classes wire everything via Spring @Component / @Service ✓
```

Adjust package names based on what was discovered in the scan.

---

## ADR Generation

Architecture Decision Records capture important decisions. Save them to `{PROJECT}/docs/adr/`.

**Template:**

```markdown
# ADR-{NNN}: {Title}

**Date:** {date}
**Status:** Proposed | Accepted | Deprecated | Superseded

## Context
{What situation or problem prompted this decision?}

## Decision
{What was decided?}

## Consequences
**Positive:**
- {benefit}

**Negative / Trade-offs:**
- {cost or limitation}

## Alternatives Considered
- {alternative 1}: rejected because {reason}
- {alternative 2}: rejected because {reason}
```

Number ADRs sequentially. Check existing ADRs in `docs/adr/` before assigning a number.

---

## Output Files

| Output | Path |
|--------|------|
| CLAUDE.md | `{PROJECT}/CLAUDE.md` |
| Structure report | In conversation (or `{PROJECT}/docs/context/structure.md`) |
| Feature guide | `{PROJECT}/docs/guides/{feature-name}.md` |
| ADR | `{PROJECT}/docs/adr/ADR-{NNN}-{title}.md` |

---

## Generate Package CLAUDE.md

當使用者要求「生成 package CLAUDE.md」或「為某個 package 建立 context」時執行此流程。

### 流程

**Step 1：確認目標 package**

詢問使用者（或從 context 推斷）要為哪個 package 生成 CLAUDE.md。
若使用者說「全部」，依序處理下表所有 package。

| Package | 目標路徑 |
|---------|---------|
| `adapter.in` | `src/main/java/com/example/demo/adapter/in/CLAUDE.md` |
| `adapter.out.gateway` | `src/main/java/com/example/demo/adapter/out/gateway/CLAUDE.md` |
| `adapter.out.repository` | `src/main/java/com/example/demo/adapter/out/repository/CLAUDE.md` |
| `usecase.ports.in` | `src/main/java/com/example/demo/usecase/ports/in/CLAUDE.md` |
| `usecase.ports.in.impl` | `src/main/java/com/example/demo/usecase/ports/in/impl/CLAUDE.md` |
| `usecase.ports.out.gateway` | `src/main/java/com/example/demo/usecase/ports/out/gateway/CLAUDE.md` |
| `domain.model` | `src/main/java/com/example/demo/domain/model/CLAUDE.md` |
| `framework.di` | `src/main/java/com/example/demo/framework/di/CLAUDE.md` |

**Step 2：掃描 package 取得 canonical example**

```bash
python .claude/skills/context-enginnering/scripts/scan_package.py <package-absolute-path>
```

**Step 3：萃取 canonical pattern**

從 scan 輸出中：
1. 找出最具代表性的 class（`@RestController` / `implements XxxGateway` / `implements XxxUseCase` 等）
2. 抽取結構骨架：package 宣告、主要 import、class 簽章、field、constructor、主要 method 簽章
3. 將完整實作細節省略為 `// ...`，保留結構

**Step 4：依 template 生成 CLAUDE.md**

讀取 `references/templates/package-claude-md.md`，填入：
- §1 職責定義（對照 `references/modules/{module}.md`）
- §2 依賴方向（allowed / forbidden imports）
- §3 套件結構（從 scan 結果的目錄樹）
- §4 命名規則（對照 `references/modules/{module}.md` 的命名表）
- §5.2 正確範例（從 Step 3 萃取的骨架）
- §5.3 禁止事項（從 `references/modules/{module}.md` 的 forbidden 清單）
- §6 快速檢核清單

**Step 5：寫入檔案**

將生成的 CLAUDE.md 寫入對應的 package 目錄。
若已存在，先比較差異，只在內容有意義的變化時才覆蓋。

---

## Refresh Mode

當使用者要求「重新整理 CLAUDE.md」、「更新 context」或「程式碼有變動後同步 CLAUDE.md」時執行。

### 流程

**Step 1：找出所有現有的 package CLAUDE.md**

```bash
find /Users/welentsai/Workspace/spring-projects/cleanarch/src -name "CLAUDE.md" | sort
```

**Step 2：逐一檢查是否過時**

對每個找到的 CLAUDE.md，執行：

```bash
bash .claude/skills/context-enginnering/scripts/scan_package.sh <package-dir>
```

與現有 CLAUDE.md 比對，判斷以下情況是否發生：
- 有新的 `.java` 檔案加入（新 class）
- 有 `.java` 檔案被移除
- 現有的 canonical example class 已被修改（import、method 簽章變化）
- 現有 CLAUDE.md 的禁止 import 清單與 `references/modules/` 不一致

**Step 3：分類處理**

| 狀態 | 動作 |
|------|------|
| 無變化 | 跳過，輸出「✓ up-to-date」 |
| 新 class 加入，但 canonical pattern 不變 | 只更新 §3 套件結構 |
| Canonical example class 有結構變化 | 重新萃取 §5.2，更新後輸出 diff |
| Package 完全重組（大量新增/刪除） | 重新執行完整 Generate 流程 |

**Step 4：輸出 Refresh 報告**

```
Refresh Report — 2026-04-11
─────────────────────────────────────────────────
✓ adapter/in/CLAUDE.md              up-to-date
✎ adapter/out/gateway/CLAUDE.md     updated (new file: XxxGatewayImpl.java)
✓ adapter/out/repository/CLAUDE.md  up-to-date
✎ usecase/ports/in/CLAUDE.md        updated (canonical example: FindCitiesInput changed)
✓ usecase/ports/in/impl/CLAUDE.md   up-to-date
✓ usecase/ports/out/gateway/CLAUDE.md up-to-date
✓ domain/model/CLAUDE.md            up-to-date
✓ framework/di/CLAUDE.md            up-to-date
─────────────────────────────────────────────────
Updated: 2 / 8
```

---

## Code Patterns

When the user asks "how do I write a controller / use case / gateway / repository" or wants to
add new code and needs a template, load the relevant pattern file and walk them through it.

| Layer / Component | Pattern File |
|-------------------|-------------|
| REST Controller + Request/Response DTOs + Mapper | `references/patterns/controller.md` |
| Use Case interface + Input + Result + DTO | `references/patterns/usecase-interface.md` |
| Use Case implementation | `references/patterns/usecase-impl.md` |
| Gateway interface + Input + Output records | `references/patterns/gateway-interface.md` |
| Gateway implementation | `references/patterns/gateway-impl.md` |
| Repository interface (reads + writes) | `references/patterns/repository-interface.md` |
| Repository implementation (JdbcClient) | `references/patterns/repository-impl.md` |
| JPA Entity | `references/patterns/jpa-entity.md` |
| Domain model (record / class) | `references/patterns/domain-model.md` |
| DI wiring in framework/di | `references/patterns/di-wiring.md` |

---

## Module Rules

When reviewing or generating code for a specific package, load the corresponding module rules
file to check allowed/forbidden imports and constraints before writing or suggesting code.

| Package | Module Rules File |
|---------|------------------|
| `adapter.in` (controllers, DTOs, mappers) | `references/modules/adapter-in.md` |
| `adapter.out.gateway` | `references/modules/adapter-out-gateway.md` |
| `adapter.out.repository` | `references/modules/adapter-out-repository.md` |
| `usecase.ports.in` + `usecase.ports.in.impl` | `references/modules/usecase-ports-in.md` |
| `usecase.ports.out` (gateway, repository, entity) | `references/modules/usecase-ports-out.md` |
| `domain.model` | `references/modules/domain.md` |
| `framework.di` + `framework.config` | `references/modules/framework-di.md` |

---

## Reference Files

See `references/` for additional detail:
- `references/conventions.md` — Naming conventions and patterns (populated after first scan)
- `references/layer-rules.md` — Detailed dependency rules and violation examples
- `references/patterns/` — Per-component canonical code patterns extracted from actual codebase
- `references/modules/` — Per-package rules: allowed/forbidden imports, constraints, checklists
