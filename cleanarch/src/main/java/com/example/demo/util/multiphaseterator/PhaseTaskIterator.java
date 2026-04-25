package com.example.demo.util.multiphaseterator;

import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Fluent iterator that runs an async task over a list of phases and collects typed results.
 *
 * <p>Configuration methods ({@link #skipPhases}, {@link #stopEarly}) are available before
 * or after {@link #map} — the chain can be split into two steps using {@code var}:
 *
 * <pre>{@code
 * var iterator = PhaseTaskIterator
 *     .over(List.of("dev", "staging", "prod"))
 *     .skipPhases(apmConfigHolder::getApmPhaseList);   // PhaseTaskIterator<String, Void>
 *
 * iterator
 *     .map(phase -> deploy(phase))                      // PhaseTaskIterator<String, DeployResult>
 *     .isSuccessCriteria(r -> r.exitCode() == 0)
 *     .stopEarly()
 *     .execute();
 * }</pre>
 *
 * <p>Tasks can be pipelined sequentially or fanned out in parallel:
 *
 * <pre>{@code
 * // Sequential pipeline: each step receives the previous output
 * PhaseTaskIterator.over(phases)
 *     .map(phase -> deploy(phase))
 *     .thenMap(r -> runTests(r))
 *     .thenMapAsync(r -> notifyAsync(r))
 *     .isSuccessCriteria(r -> r.sent())
 *     .execute();
 *
 * // Parallel fan-out: two independent tasks per phase, results combined
 * PhaseTaskIterator.over(phases)
 *     .map(phase -> deployA(phase))
 *     .andMap(phase -> deployB(phase), (a, b) -> new Combined(a, b))
 *     .isSuccessCriteria(c -> c.bothOk())
 *     .executeParallel();
 * }</pre>
 *
 * @param <K> phase key type
 * @param <V> task output type; {@code Void} until {@link #map} is called
 */
public final class PhaseTaskIterator<K, V> {

    private final List<K> phases;
    private final Supplier<Set<K>> skipPhases;
    private final Function<K, CompletableFuture<V>> taskMapper;
    private final Predicate<V> successCriteria;
    private final boolean earlyStop;

    private PhaseTaskIterator(
            List<K> phases,
            Supplier<Set<K>> skipPhases,
            @Nullable Function<K, CompletableFuture<V>> taskMapper,
            Predicate<V> successCriteria,
            boolean earlyStop) {
        this.phases = List.copyOf(phases);
        this.skipPhases = skipPhases;
        this.taskMapper = taskMapper;
        this.successCriteria = successCriteria;
        this.earlyStop = earlyStop;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    /** Returns a pre-map stage ({@code V=Void}); call {@link #map} to bind the output type. */
    public static <K> PhaseTaskIterator<K, Void> over(List<K> phases) {
        Objects.requireNonNull(phases, "phases must not be null");
        return new PhaseTaskIterator<>(phases, Set::of, null, v -> true, false);
    }

    // ── Configuration (available before or after map) ─────────────────────────

    /** Phases in this list are marked SKIPPED and never executed (snapshot at call time). */
    public PhaseTaskIterator<K, V> skipPhases(List<K> toSkip) {
        Objects.requireNonNull(toSkip, "toSkip must not be null");
        Set<K> snapshot = Set.copyOf(toSkip);
        return new PhaseTaskIterator<>(phases, () -> snapshot, taskMapper, successCriteria, earlyStop);
    }

    /**
     * Live overload: the supplier is called fresh on every {@link #execute()} /
     * {@link #executeParallel()}, so changes to the source are reflected without
     * rebuilding the iterator. Accepts any {@code Collection<? extends K>} supplier,
     * e.g. {@code apmConfigHolder::getApmPhaseList} when {@code K=String}.
     */
    public PhaseTaskIterator<K, V> skipPhases(Supplier<? extends Collection<? extends K>> skipPhasesSupplier) {
        Objects.requireNonNull(skipPhasesSupplier, "skipPhasesSupplier must not be null");
        return new PhaseTaskIterator<>(
                phases, () -> Set.copyOf(skipPhasesSupplier.get()), taskMapper, successCriteria, earlyStop);
    }

    /**
     * When set, the first phase whose status is FAILED_BY_EXCEPTION or FAILED_BY_CRITERIA
     * causes all subsequent phases to be marked SKIPPED instead of executed.
     *
     * <p>In {@link #executeParallel()} mode all futures are still fired upfront — stopEarly
     * stops <em>collecting</em> results, not background task execution.
     */
    public PhaseTaskIterator<K, V> stopEarly() {
        return new PhaseTaskIterator<>(phases, skipPhases, taskMapper, successCriteria, true);
    }

    // ── Map (binds output type K → V) ─────────────────────────────────────────

    /**
     * Maps each phase key to a synchronous task using the common fork-join pool.
     * Suitable for CPU-bound work. For I/O-bound tasks use {@link #map(Function, Executor)}
     * or {@link #mapAsync(Function)} to avoid thread starvation.
     */
    public <R> PhaseTaskIterator<K, R> map(Function<K, R> fn) {
        Objects.requireNonNull(fn, "fn must not be null");
        return new PhaseTaskIterator<>(
                phases,
                skipPhases,
                phase -> CompletableFuture.supplyAsync(() -> fn.apply(phase)),
                v -> true,
                earlyStop);
    }

    /**
     * Maps each phase key to a synchronous task using the provided executor.
     * Use this for I/O-bound tasks (DB, HTTP, file) to avoid starving the common fork-join pool.
     */
    public <R> PhaseTaskIterator<K, R> map(Function<K, R> fn, Executor executor) {
        Objects.requireNonNull(fn, "fn must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        return new PhaseTaskIterator<>(
                phases,
                skipPhases,
                phase -> CompletableFuture.supplyAsync(() -> fn.apply(phase), executor),
                v -> true,
                earlyStop);
    }

    /**
     * For callers that manage their own async execution — custom executor,
     * virtual threads, reactive pipelines, etc.
     */
    public <R> PhaseTaskIterator<K, R> mapAsync(Function<K, CompletableFuture<R>> fn) {
        Objects.requireNonNull(fn, "fn must not be null");
        return new PhaseTaskIterator<>(phases, skipPhases, fn, v -> true, earlyStop);
    }

    // ── Sequential composition (requires prior map / mapAsync) ───────────────

    /**
     * Adds a synchronous step after the current task: the prior output feeds into {@code fn},
     * equivalent to {@link CompletableFuture#thenApply}. Runs on the completing thread.
     *
     * <pre>{@code
     * PhaseTaskIterator.over(phases)
     *     .map(phase -> deploy(phase))     // K → DeployResult
     *     .thenMap(r -> runTests(r))       // DeployResult → TestResult
     *     .execute();
     * }</pre>
     *
     * @throws IllegalStateException if called before {@link #map} or {@link #mapAsync}
     */
    public <R> PhaseTaskIterator<K, R> thenMap(Function<V, R> fn) {
        Objects.requireNonNull(fn, "fn must not be null");
        Function<K, CompletableFuture<V>> current = requireMapper();
        return new PhaseTaskIterator<>(
                phases, skipPhases, phase -> current.apply(phase).thenApply(fn), v -> true, earlyStop);
    }

    /**
     * Same as {@link #thenMap(Function)} but runs the continuation on {@code executor},
     * equivalent to {@link CompletableFuture#thenApplyAsync(Function, Executor)}.
     *
     * @throws IllegalStateException if called before {@link #map} or {@link #mapAsync}
     */
    public <R> PhaseTaskIterator<K, R> thenMap(Function<V, R> fn, Executor executor) {
        Objects.requireNonNull(fn, "fn must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Function<K, CompletableFuture<V>> current = requireMapper();
        return new PhaseTaskIterator<>(
                phases,
                skipPhases,
                phase -> current.apply(phase).thenApplyAsync(fn, executor),
                v -> true,
                earlyStop);
    }

    /**
     * Adds an async step after the current task via {@link CompletableFuture#thenCompose}.
     * Use when the continuation itself returns a {@code CompletableFuture} (e.g. a downstream
     * HTTP call or DB query).
     *
     * @throws IllegalStateException if called before {@link #map} or {@link #mapAsync}
     */
    public <R> PhaseTaskIterator<K, R> thenMapAsync(Function<V, CompletableFuture<R>> fn) {
        Objects.requireNonNull(fn, "fn must not be null");
        Function<K, CompletableFuture<V>> current = requireMapper();
        return new PhaseTaskIterator<>(
                phases, skipPhases, phase -> current.apply(phase).thenCompose(fn), v -> true, earlyStop);
    }

    // ── Parallel composition (requires prior map / mapAsync) ─────────────────

    /**
     * Runs an independent task from the same phase key <em>in parallel</em> with the current
     * task, then combines both results via {@code combiner} —
     * equivalent to {@link CompletableFuture#thenCombine}. Uses the common fork-join pool.
     *
     * <pre>{@code
     * PhaseTaskIterator.over(phases)
     *     .map(phase -> deployA(phase))
     *     .andMap(phase -> deployB(phase), (a, b) -> new Combined(a, b))
     *     .isSuccessCriteria(c -> c.bothOk())
     *     .executeParallel();
     * }</pre>
     *
     * @throws IllegalStateException if called before {@link #map} or {@link #mapAsync}
     */
    public <W, R> PhaseTaskIterator<K, R> andMap(Function<K, W> parallelFn, BiFunction<V, W, R> combiner) {
        Objects.requireNonNull(parallelFn, "parallelFn must not be null");
        Objects.requireNonNull(combiner, "combiner must not be null");
        Function<K, CompletableFuture<V>> current = requireMapper();
        return new PhaseTaskIterator<>(
                phases,
                skipPhases,
                phase -> current.apply(phase)
                        .thenCombine(CompletableFuture.supplyAsync(() -> parallelFn.apply(phase)), combiner),
                v -> true,
                earlyStop);
    }

    /**
     * Same as {@link #andMap(Function, BiFunction)} but runs the parallel branch on {@code executor}.
     *
     * @throws IllegalStateException if called before {@link #map} or {@link #mapAsync}
     */
    public <W, R> PhaseTaskIterator<K, R> andMap(
            Function<K, W> parallelFn, Executor executor, BiFunction<V, W, R> combiner) {
        Objects.requireNonNull(parallelFn, "parallelFn must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        Objects.requireNonNull(combiner, "combiner must not be null");
        Function<K, CompletableFuture<V>> current = requireMapper();
        return new PhaseTaskIterator<>(
                phases,
                skipPhases,
                phase -> current.apply(phase)
                        .thenCombine(
                                CompletableFuture.supplyAsync(() -> parallelFn.apply(phase), executor),
                                combiner),
                v -> true,
                earlyStop);
    }

    /**
     * Same as {@link #andMap(Function, BiFunction)} but the parallel branch is a
     * {@code CompletableFuture} — use when the parallel step manages its own async lifecycle
     * (virtual threads, reactor, custom executor, etc.).
     *
     * @throws IllegalStateException if called before {@link #map} or {@link #mapAsync}
     */
    public <W, R> PhaseTaskIterator<K, R> andMapAsync(
            Function<K, CompletableFuture<W>> parallelFn, BiFunction<V, W, R> combiner) {
        Objects.requireNonNull(parallelFn, "parallelFn must not be null");
        Objects.requireNonNull(combiner, "combiner must not be null");
        Function<K, CompletableFuture<V>> current = requireMapper();
        return new PhaseTaskIterator<>(
                phases,
                skipPhases,
                phase -> current.apply(phase).thenCombine(parallelFn.apply(phase), combiner),
                v -> true,
                earlyStop);
    }

    // ── Post-map configuration ────────────────────────────────────────────────

    /**
     * Predicate applied to each task's output. A phase is SUCCEEDED only when the
     * task completes without exception AND this predicate returns {@code true}.
     * Defaults to always-true if not set.
     */
    public PhaseTaskIterator<K, V> isSuccessCriteria(Predicate<V> criteria) {
        return new PhaseTaskIterator<>(phases, skipPhases, taskMapper, criteria, earlyStop);
    }

    // ── Terminal operations ───────────────────────────────────────────────────

    /** Runs phases one at a time, in order. Waits for each to complete before starting the next. */
    public List<PhaseTaskOutput<K, V>> execute() {
        Function<K, CompletableFuture<V>> mapper = requireMapper();
        Set<K> skip = skipPhases.get();
        List<PhaseTaskOutput<K, V>> results = new ArrayList<>();
        boolean stopped = false;

        for (K phase : phases) {
            if (stopped || skip.contains(phase)) {
                results.add(PhaseTaskOutput.skipped(phase));
                continue;
            }
            PhaseTaskOutput<K, V> output = runPhase(phase, mapper);
            results.add(output);
            if (earlyStop && output.isFailed()) stopped = true;
        }

        return List.copyOf(results);
    }

    /**
     * Fires all phase futures immediately, then collects results in phase order.
     * Results preserve the original phase ordering regardless of completion order.
     */
    public List<PhaseTaskOutput<K, V>> executeParallel() {
        Function<K, CompletableFuture<V>> mapper = requireMapper();
        Set<K> skip = skipPhases.get();
        List<PhaseEntry<K, V>> entries = new ArrayList<>();
        for (K phase : phases) {
            CompletableFuture<V> future = skip.contains(phase) ? null : mapper.apply(phase);
            entries.add(new PhaseEntry<>(phase, future, Instant.now()));
        }

        List<PhaseTaskOutput<K, V>> results = new ArrayList<>();
        boolean stopped = false;

        for (PhaseEntry<K, V> entry : entries) {
            if (stopped || entry.future() == null) {
                results.add(PhaseTaskOutput.skipped(entry.phase()));
                continue;
            }
            PhaseTaskOutput<K, V> output = awaitFuture(entry.phase(), entry.future(), entry.startedAt());
            results.add(output);
            if (earlyStop && output.isFailed()) stopped = true;
        }

        return List.copyOf(results);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private Function<K, CompletableFuture<V>> requireMapper() {
        if (taskMapper == null) throw new IllegalStateException("call map() or mapAsync() before execute()");
        return taskMapper;
    }

    private record PhaseEntry<K, V>(K phase, @Nullable CompletableFuture<V> future, Instant startedAt) {}

    private PhaseTaskOutput<K, V> runPhase(K phase, Function<K, CompletableFuture<V>> mapper) {
        Instant start = Instant.now();
        return awaitFuture(phase, mapper.apply(phase), start);
    }

    private PhaseTaskOutput<K, V> awaitFuture(K phase, CompletableFuture<V> future, Instant startedAt) {
        try {
            V value = future.join();
            Duration duration = Duration.between(startedAt, Instant.now());
            boolean passed = successCriteria.test(value);
            PhaseTaskStatus status = passed ? PhaseTaskStatus.SUCCEEDED : PhaseTaskStatus.FAILED_BY_CRITERIA;
            return new PhaseTaskOutput<>(phase, status, value, duration);
        } catch (CompletionException e) {
            Duration duration = Duration.between(startedAt, Instant.now());
            return new PhaseTaskOutput<>(phase, PhaseTaskStatus.FAILED_BY_EXCEPTION, null, duration);
        }
    }
}
