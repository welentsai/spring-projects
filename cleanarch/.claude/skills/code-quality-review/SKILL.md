---
name: code-quality-review
description: >
  Strict maintainability review of a PR / branch diff / changeset in the cleanarch
  project (Spring Boot 3.5.x, Java 17, clean architecture: domain ← usecase ← adapter).
  Use this skill whenever the user asks to review a PR, review a diff, review a branch,
  do a code quality review, check a changeset before merge, or says things like
  "review this PR", "幫我 review 這個 PR / diff", "這個 changeset 可以 merge 嗎",
  "code quality review", or "stop the slop". This is a REVIEW skill (approve/block
  judgment on changes) — for rewriting existing code on request, use `code-refactor`
  instead.
---

# Code Quality Review

Review a diff the way a demanding senior maintainer would: the question is not "does it
work?" but **"does merging this make the codebase easier or harder to live with?"**
Functional correctness is table stakes, not the bar. This skill exists to stop slop —
the pattern where each change is locally reasonable but the codebase degrades one merge
at a time.

Two rules of engagement before anything else:

1. **Review the diff in context, not in isolation.** Read enough of the surrounding
   files to know whether a helper already exists, which layer the touched code lives in,
   and how big files were *before* the change.
2. **Report little, but report it hard.** A handful of high-confidence structural
   findings beats thirty nits. Never bury a blocker under formatting comments. If a
   finding is style-only and no real issue exists, don't write it.
## The bar: presumptive blockers

Each of the following is a **blocker by default** — the author must either fix it or
give a convincing justification. "It works" and "it was faster this way" are not
justifications.

1. **Dependency-rule violation.** Any import that points inward-out: `domain` importing
   Spring / `usecase` / `adapter` types, or `usecase` importing `adapter`. This is the
   single most expensive kind of slop in this codebase because it silently converts
   pure, testable code into untestable code. No exceptions.
2. **Logic in the wrong layer.** Business decisions (a *calculation* in Grokking
   Simplicity terms) written inline in a controller, repository impl, or other adapter;
   or I/O (an *action*) performed inside `domain`. The test: could this decision be a
   pure function in `domain`? Then it must be.
3. **Ad-hoc conditionals in shared paths.** A special-case `if` for one feature dropped
   into a shared flow (a common service method, a filter, a mapper used by everyone).
   This is how spaghetti grows: each branch is small, the sum is unownable. The fix is
   almost always to model the variation properly — a sealed interface + pattern
   matching, a strategy port, or moving the branch to the caller that owns the context.
4. **Duplicated canonical helper.** The diff re-implements something that already exists
   (date formatting, pagination mapping, error-to-ProblemDetail translation, an existing
   domain calculation). Search before approving: if a canonical version exists, the new
   copy is a blocker; if the *new* version is better, the finding is "replace the old
   one", not "keep both".
5. **File size explosion.** A file crossing the **1000-line** threshold in this diff, or
   a class visibly accumulating unrelated responsibilities, is presumed to be a missing
   decomposition. The burden of proof is on keeping it whole.
6. **Needless indirection.** A wrapper, interface, or "manager" that adds a layer
   without adding meaning: an interface with one impl and no port semantics, a
   pass-through service method, a helper that renames its argument. Deleting indirection
   is a valid and encouraged review outcome — over-abstraction is slop too.
7. **Java type-hygiene escapes.** Raw types; unchecked casts; `@SuppressWarnings`
   without a one-line justification comment; `Object`/`Map<String,Object>` as a domain
   data shape where a record should exist; `Optional` used as a field or parameter
   type; `null` returned where `Optional` or a sealed result type is the project idiom.
8. **New mutable shared state.** Mutable statics, singleton beans holding per-request
   state, setters added to something that was (or should be) a value/record, collections
   returned by reference from domain objects without defensive copies.
## What to actively look for: code judo

Don't only ask "is this change clean?" — ask **"is there a nearby move that makes this
change mostly unnecessary?"** The best review comment is the one that shrinks the PR.
Typical judo moves in this codebase:

- The diff adds a fourth boolean flag to a method → propose a sealed command type and
  a `switch` expression; three call sites get simpler at once.
- The diff bolts retry/logging/mapping into a use case → propose extracting the pure
  calculation to `domain` and leaving the action thin; the new tests become trivial.
- The diff copies a mapper because the original didn't quite fit → propose the one
  generalization the original needed; both call sites converge.
  Propose judo when the simplification is clearly reachable and behavior-preserving.
  For anything bigger — restructuring that changes shape or spans many files — do **not**
  demand it in review. Flag it as a follow-up and point the user at `code-refactor` (it
  will survey and size the change before executing). Review is a gate, not a rewrite.

## What NOT to flag

- Formatting, import order, naming taste when the name is adequate — assume tooling
  owns this.
- Java 17 features for their own sake. A two-case `if` does not need to become a
  `switch` expression. Idiom findings must earn their place via readability or safety.
- Hypothetical flexibility ("what if we someday need…"). YAGNI applies to reviewers.
- Anything you are not confident about after reading the surrounding code. Low-confidence
  hunches go in a single "worth a look" line at most, or nowhere.
## Procedure

1. **Establish the diff.** Ask for / locate the branch, PR, or pasted diff. Identify
   the touched files and read them *plus* their immediate neighbors (callers, the layer
   they sit in, any existing helper they might duplicate).
2. **Layer map.** For each touched unit, note its layer and whether it is Data /
   Calculation / Action. Mismatches feed blockers #1–#2.
3. **Run the blocker list**, then the judo pass, then (sparingly) suggestions.
4. **Write the verdict** using the report structure below. Every blocker names the
   file/line, the rule it trips, and the concrete fix — not just the complaint.
5. **Stop.** Don't start rewriting code. If the user wants the fixes applied, hand off
   to `code-refactor` (it will re-survey and apply changes with approval).
## Report structure

```
## Code Quality Review: <branch / PR>
 
### Verdict
APPROVE | APPROVE WITH SUGGESTIONS | REQUEST CHANGES
<1–2 sentences: the single most important thing about this diff.>
 
### Blockers (must fix or justify)
1. **<rule tripped>** — `<file>:<line/area>`
   - What: <the problem, concretely>
   - Why it's a blocker: <which rule, what it costs the codebase>
   - Fix: <the specific change; if judo, show the smaller shape>
 
### Suggestions (author's call)
1. **<short title>** — `<file>` — <one or two lines each; keep this section short>
 
### Follow-ups (out of scope for this PR)
- <bigger restructures worth doing later — note if code-refactor should pick them up>
```

Tone in findings: direct and specific. Say "this duplicates `PageMapper.toResponse`;
delete this copy and reuse it" — not "maybe consider possibly reusing existing code if
convenient". Do not soften blockers into suggestions to be polite; the two-bucket split
*is* the politeness — it tells the author exactly what is negotiable.

## When to run this skill

This review is deliberately strict, so it is best used as a **manual gate**: sizeable
PRs, changes touching `domain` or shared paths, anything AI-generated in bulk, or when
the team explicitly wants a slop check. For a one-line bugfix PR, a full thermo-nuclear
pass is noise — say so and do a proportionate check instead.
 