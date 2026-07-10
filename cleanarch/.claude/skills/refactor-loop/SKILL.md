---
name: refactor-loop
description: >
  Orchestrate an iterative refactor → review → fix loop on the cleanarch project
  (Spring Boot 3.5.x, Java 17, clean architecture: domain ← usecase ← adapter) by
  chaining the `code-refactor` and `code-quality-review` skills until the changes pass
  review. Use this skill whenever the user asks to run a refactor loop, an iterative
  refactor, a self-reviewing refactor, or says things like "跑 refactor loop",
  "refactor 到 review 過為止", "refactor and review until it passes", "loop engineering",
  "改到 APPROVE 為止", or wants refactored code to be automatically quality-gated and
  fixed in cycles. Do NOT use for a one-shot refactor (use `code-refactor`) or a
  review-only pass (use `code-quality-review`).
---

# Refactor Loop

Run `code-refactor` and `code-quality-review` as a closed loop on a target file or
package: refactor, review the resulting diff, fix any blockers, re-review — until the
review verdict is APPROVE (or APPROVE WITH SUGGESTIONS), or the iteration cap is hit.

This skill is an **orchestrator**. It contains no refactoring rules and no review rules
of its own — those live in the two skills it drives. Before the first iteration, read
both `code-refactor/SKILL.md` and `code-quality-review/SKILL.md` in full and follow them
faithfully inside each phase. If this skill and one of those skills ever seem to
conflict on *how* to refactor or *what* to flag, the inner skill wins; this skill only
governs sequencing, convergence, and stopping.

## The loop

```
① code-refactor Phase 1 — survey the target, produce the Refactor Report
② Approval gate — user picks items (see approval rules below)
③ code-refactor Phase 2 — apply approved items in small steps; run tests
④ Test gate — tests must be green before review; if red, fix or revert first
⑤ code-quality-review — review ONLY the diff produced this iteration
⑥ Verdict:
   - APPROVE / APPROVE WITH SUGGESTIONS → exit loop, write final summary
   - REQUEST CHANGES → blockers become the scope of the next iteration; go to ③
     (a new survey/report is NOT needed — the review's "Fix:" lines ARE the plan,
      but each fix is still applied with code-refactor Phase 2 discipline)
```

Hard limits and gates:

1. **Iteration cap: 3 review cycles** (unless the user sets a different number).
   If blockers remain after the cap, stop and report honestly: list what's still
   failing, why the loop couldn't converge, and recommend `impact-survey` if the
   remaining problems are structural rather than fixable-in-place. Never silently
   downgrade a blocker to "good enough" just to exit.
2. **Convergence condition is the verdict, not vibes.** Only `REQUEST CHANGES`
   continues the loop. `APPROVE WITH SUGGESTIONS` terminates it — suggestions are the
   author's call by definition; chasing them makes the loop non-convergent. Surface
   the suggestions in the final summary instead so the user can decide later.
3. **Review scope is the current iteration's diff only.** Never re-review code the
   loop already approved in an earlier cycle, and never expand review scope to
   untouched neighboring files (reading them for context is fine — that's required by
   the review skill — but findings must be about this iteration's changes). Full-scope
   re-review makes the loop oscillate: previously-passed code attracts fresh opinions
   and the loop never closes.
4. **Tests green before every review.** `code-refactor` Phase 2 already requires
   running tests after changes; the loop makes it a gate. If the project's tests
   cannot be run in this environment, say so explicitly at loop start, tell the user
   exactly what to run, and treat "user confirms tests pass" as the gate instead.
5. **Track the baseline.** At loop start, record the starting point (branch, commit,
   or a copy of the original files) so the final summary can show a true
   before/after and so any iteration can be reverted cleanly.
## Approval rules — what may be automated, what may not

The loop automates **execution and verification**, never **architectural judgment**:

- **Safe refactors** (behavior-preserving bucket in the Refactor Report): the user may
  pre-authorize these for the whole loop with one approval at step ②, e.g. "apply all
  safe refactors automatically." Confirm this pre-authorization explicitly at loop
  start — never assume it.
- **Design changes** (the report's structural bucket): ALWAYS pause and ask, in every
  iteration, no matter what blanket authorization was given. If a review blocker can
  only be fixed via a design change (e.g. moving logic across layers, splitting a
  class, introducing a port), pause the loop, present the trade-off, and wait.
- **Blocker fixes in later iterations**: fixes that are behavior-preserving and match
  the review's prescribed "Fix:" line fall under the safe-refactor pre-authorization.
  Anything that changes shape is a design change — pause and ask.
  If the user has not pre-authorized anything, run every iteration's step ② as a normal
  interactive approval, exactly as `code-refactor` Phase 1 prescribes.

## Role separation inside one session

The same model plays refactorer and reviewer, which risks the reviewer rubber-stamping
its own work. Counteract this deliberately:

- In step ⑤, re-read the actual changed files and the review skill's blocker list from
  scratch. Do not review from memory of what you "just did" — review what is on disk.
- Apply the review skill's bar at full strictness. The fact that the diff came from a
  disciplined refactor is not evidence it passes; dependency-rule violations and
  wrong-layer logic introduced *by a refactor* are exactly what this gate exists to
  catch.
- It is a legitimate and expected outcome for the reviewer to block the refactorer's
  work. A loop that always passes on iteration 1 is a sign the review phase is being
  performed too gently.
## Final summary

When the loop exits (converged or capped), report:

```
## Refactor Loop Summary: <target>
 
### Outcome
CONVERGED in N iteration(s) | STOPPED at iteration cap (N)
Final verdict: <APPROVE | APPROVE WITH SUGGESTIONS | REQUEST CHANGES (capped)>
 
### What changed
<Grouped list of applied changes across all iterations, smallest useful granularity;
 before/after or diff snippets for anything non-trivial.>
 
### Iteration history
- Iter 1: <items applied> → <verdict>; <blockers found, if any>
- Iter 2: ...
 
### Tests
<What was run, final status; or exactly what the user must run if tests couldn't run here.>
 
### Open items
<Unapplied design changes, review suggestions from the final APPROVE-WITH-SUGGESTIONS,
 anything deferred to impact-survey / a follow-up.>
```

## A note on restraint

The loop inherits the restraint clauses of both inner skills. Do not use the loop as an
excuse to expand scope iteration by iteration ("while I'm here…"): the target set at
loop start is the target until the loop ends. If mid-loop discoveries suggest a bigger
restructure, record it under Open items and finish the loop at the original scope.