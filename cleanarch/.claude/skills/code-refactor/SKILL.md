---
name: code-refactor
description: >
  Refactor Java code in a Spring Boot 3.5.x / Java 17 hexagonal-architecture project
  for simplicity, readability, and maintainability. Use this skill whenever the user
  wants to refactor, clean up, simplify, tidy, or improve existing Java/Spring code —
  including phrases like "refactor this", "clean up this class", "make this more readable",
  "simplify this service", "improve this code", "what could be better here", or when they
  point at a file or package and ask for it to be improved. It applies Java 17 and Spring
  Boot 3.5.x best practices together with the Actions / Calculations / Data separation,
  immutability, and stratified-design principles from *Grokking Simplicity*. Trigger it
  even when the user only says "can you improve X" or pastes a class and asks what's wrong
  with it — refactoring is the intent even without the word "refactor".
---

# Code Refactor

Refactor Java code in the `cleanarch` project (Spring Boot 3.5.x, Java 17, hexagonal /
clean architecture) toward simplicity, readability, and maintainability. The skill works
in two phases: **survey and report first, then refactor on approval**. Never start
rewriting code before the user has seen the findings and chosen what to apply — the report
is where the user keeps control over scope and risk.

## The mental model: where simplicity comes from

The single most useful lens for this codebase is the *Grokking Simplicity* distinction,
because it maps almost one-to-one onto the project's layers. Internalize this mapping — it
turns "make it simpler" from a vague aesthetic goal into a concrete relocation problem:

| Grokking Simplicity | What it is | Where it belongs in this project |
|---------------------|------------|----------------------------------|
| **Data** | Inert facts — numbers, strings, records. Means the same thing every time. | `domain` (entities, value objects), DTOs/commands |
| **Calculation** | Pure function — same input always gives same output, no side effects. Easy to test. | `domain` services, pure helpers. The bottom of the dependency graph. |
| **Action** | Depends on *when* / *how many times* it runs — DB reads/writes, HTTP calls, sending email, mutating shared state. | `adapter` layer (controllers, repository impls, external clients). The edges. |

The core refactoring move is therefore almost always one of:

1. **Extract a calculation out of an action.** Business logic tangled inside a
   `@Service` that also talks to the database is a calculation trapped in an action. Pull
   the pure decision-making into a domain calculation; leave only the I/O behind.
2. **Push actions to the edges.** Actions *spread* — anything that calls an action becomes
   an action too. Keeping I/O in the adapter layer (and out of `domain`) is what stops the
   whole codebase from becoming one big untestable action.
3. **Make implicit inputs and outputs explicit.** A method that reads mutable shared state,
   a static field, the clock, or a hidden dependency has implicit inputs; one that mutates
   them has implicit outputs. Turn them into parameters and return values. This is what
   converts an action into a calculation, or at least tames it.
   The hexagonal **dependency rule** (`domain ← usecase ← adapter`, no reverse arrows) is the
   enforcement mechanism for this separation. If `domain` imports anything from Spring or
   from an outer layer, that is both an architecture violation *and* a sign that a calculation
   has been contaminated with an action. The two problems are the same problem.

## What this skill looks for

Findings come from four lenses. The first three are the simplicity principles; the fourth
is modern-Java/Spring hygiene. Read the reference files for the full catalogs with Java
examples — don't try to hold every rule in your head:

- **Actions / Calculations / Data separation** — calculations trapped in actions, actions
  leaking into `domain`, implicit inputs/outputs. → `references/grokking-simplicity.md`
- **Immutability** — mutable entities and value objects, missing defensive copies at
  boundaries, setters on things that should be values. → `references/grokking-simplicity.md`
- **Stratified design** — mixed levels of abstraction in one method, dependency-rule
  violations, god classes, over-broad interfaces, *and* over-engineering (premature
  abstraction is as much a problem as too little). → `references/grokking-simplicity.md`
- **Java 17 + Spring Boot 3.5.x idioms** — records, sealed types, pattern matching, switch
  expressions, constructor injection, `@ConfigurationProperties`, `RestClient`, Problem
  Details, the `jakarta.*` namespace, and so on. → `references/java17-springboot35.md`
  A good refactor usually touches several lenses at once: converting a mutable entity to a
  record (immutability + Java idiom) often also removes the setters that were the only reason
  a calculation had been written as a mutating action (A/C/D).

## Phase 1 — Survey and report

1. **Resolve the target.** The user points at either a single file or a package. For a
   single file, read it. For a package, list the Java files and read the ones in scope; if
   the package is large, summarize the structure first and confirm which files to dig into
   rather than reading dozens of files blindly.
2. **Classify the code.** For each meaningful unit, label it Action, Calculation, or Data,
   and note which layer it currently lives in versus where it belongs. Mismatches are your
   richest source of findings.
3. **Run the four lenses** over the target, consulting the reference files as needed.
4. **Write the report** using the structure below. Crucially, split findings into two
   buckets so the user can approve them separately:
    - **Safe refactors** — behavior-preserving. The kind you could apply with confidence
      that tests still pass: rename, extract method, convert to record, replace field
      injection with constructor injection, swap `RestTemplate` for `RestClient`.
    - **Design changes** — structural changes that alter shape or require judgment: moving
      logic across layers, splitting a class, introducing a port, collapsing an abstraction.
      These carry more risk and reward, so the user should opt in deliberately.
5. **Stop and ask** which items to apply. Do not proceed to Phase 2 unprompted.
### Report structure

Use this layout. Keep each finding tight — location, the problem, *why it matters*, and the
proposed change. The "why" is what lets the user judge whether a change earns its cost.

```
## Refactor Report: <target>
 
### Summary
<2–3 sentences: overall state, biggest opportunity, anything risky.>
 
### Actions / Calculations / Data map
<For the units in scope: what each is now, and any that sit in the wrong layer.>
 
### Safe refactors (behavior-preserving)
1. **<short title>** — `<file>:<line/method>`
   - Problem: <what's there now>
   - Why it matters: <readability / testability / immutability / idiom>
   - Change: <the concrete edit>
 
### Design changes (need your call)
1. **<short title>** — `<file>:<area>`
   - Problem: <the structural issue>
   - Why it matters: <coupling / spread of actions / abstraction level>
   - Change: <the proposed restructure>
   - Trade-off: <cost, blast radius, what could regress>
 
### Proposed order
<Smallest, safest steps first. Note which changes depend on others.>
```

## Phase 2 — Refactor on approval

Once the user has chosen items:

1. **Take many small steps.** Apply changes one at a time in the agreed order, smallest and
   safest first. This is directly from the book's chaining advice and it keeps each step
   easy to verify and easy to revert. Don't bundle five edits into one giant rewrite.
2. **Preserve behavior** for everything except the design changes the user explicitly
   approved. A behavior-preserving refactor that quietly changes behavior is a bug, not a
   refactor.
3. **Respect the dependency rule at all times.** Never introduce an import that points from
   an inner layer to an outer one. If a change seems to require it, that's a signal the
   change is mis-designed — surface it rather than forcing it.
4. **Keep the tests honest.** Adjust existing tests to match refactored signatures, and
   when you extract a calculation, it is now trivially unit-testable — add or note a test
   for it. If the project's tests can be run, run them after the changes; if they can't be
   run here, say so and tell the user exactly what to run.
5. **Report what changed.** Summarize the applied edits and show diffs or before/after for
   anything non-trivial, so the user can review without re-reading the whole file.
## A note on restraint

This skill exists to *remove* complexity, so hold yourself to the same standard you apply
to the code. Don't invent abstractions the code doesn't need, don't apply a Java 17 feature
just because it exists (a `switch` expression over two cases an `if` handled fine is not an
improvement), and don't flag style nitpicks as if they were structural problems. The
stratified-design idea of "comfortable layers you don't over-engineer" applies to your own
recommendations: prefer the smallest change that genuinely improves readability,
testability, or correctness.