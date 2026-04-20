package com.example.demo.util.multiphaseterator;

import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent iterator that runs an async task over a list of phases and collects typed results.
 *
 * <pre>{@code
 * List<PhaseTaskOutput<String, DeployResult>> results = PhaseTaskIterator
 *     .over(List.of("dev", "staging", "prod"))
 *     .map(phase -> deploy(phase))
 *     .isSuccessCriteria(r -> r.exitCode() == 0)
 *     .stopEarly()       // optional — skip remaining phases on first failure
 *     .execute();        // sequential; use executeParallel() to fire all at once
 * }</pre>
 *
 * @param <K> phase key type
 * @param <V> task output type
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
            Function<K, CompletableFuture<V>> taskMapper,
            Predicate<V> successCriteria,
            boolean earlyStop) {
        this.phases = List.copyOf(phases);
        this.skipPhases = skipPhases;
        this.taskMapper = taskMapper;
        this.successCriteria = successCriteria;
        this.earlyStop = earlyStop;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static <K> PhaseStage<K> over(List<K> phases) {
        Objects.requireNonNull(phases, "phases must not be null");
        return new PhaseStage<>(phases);
    }

    // ── Builder steps (each returns a new immutable instance) ────────────────

    /**
     * Predicate applied to each task's output. A phase is SUCCEEDED only when the
     * task completes without exception AND this predicate returns {@code true}.
     * Defaults to always-true if not set.
     */
    public PhaseTaskIterator<K, V> isSuccessCriteria(Predicate<V> criteria) {
        return new PhaseTaskIterator<>(phases, skipPhases, taskMapper, criteria, earlyStop);
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

    // ── Terminal operations ───────────────────────────────────────────────────

    /** Runs phases one at a time, in order. Waits for each to complete before starting the next. */
    public List<PhaseTaskOutput<K, V>> execute() {
        Set<K> skip = skipPhases.get();
        List<PhaseTaskOutput<K, V>> results = new ArrayList<>();
        boolean stopped = false;

        for (K phase : phases) {
            if (stopped || skip.contains(phase)) {
                results.add(PhaseTaskOutput.skipped(phase));
                continue;
            }
            PhaseTaskOutput<K, V> output = runPhase(phase);
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
        Set<K> skip = skipPhases.get();
        List<PhaseEntry<K, V>> entries = new ArrayList<>();
        for (K phase : phases) {
            CompletableFuture<V> future = skip.contains(phase)
                    ? null
                    : taskMapper.apply(phase);
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

    private record PhaseEntry<K, V>(K phase, @Nullable CompletableFuture<V> future, Instant startedAt) {}

    // ── Internal helpers ──────────────────────────────────────────────────────

    private PhaseTaskOutput<K, V> runPhase(K phase) {
        Instant start = Instant.now();
        return awaitFuture(phase, taskMapper.apply(phase), start);
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

    // ── Inner builder: untyped until .map() introduces V ─────────────────────

    public static final class PhaseStage<K> {

        private final List<K> phases;
        private final Supplier<Set<K>> skipPhases;

        private PhaseStage(List<K> phases) {
            this.phases = phases;
            this.skipPhases = Set::of;
        }

        private PhaseStage(List<K> phases, Supplier<Set<K>> skipPhases) {
            this.phases = phases;
            this.skipPhases = skipPhases;
        }

        /** Phases in this list are marked SKIPPED and never executed (snapshot at call time). */
        public PhaseStage<K> skipPhases(List<K> toSkip) {
            Objects.requireNonNull(toSkip, "toSkip must not be null");
            Set<K> snapshot = Set.copyOf(toSkip);
            return new PhaseStage<>(phases, () -> snapshot);
        }

        /**
         * Live overload: the supplier is called fresh on every {@link #execute()} /
         * {@link #executeParallel()}, so changes to the source are reflected without
         * rebuilding the iterator. Accepts any {@code Collection<? extends K>} supplier,
         * e.g. {@code apmConfigHolder::getApmPhaseList} when {@code K=String}.
         */
        public PhaseStage<K> skipPhases(Supplier<? extends Collection<? extends K>> skipPhasesSupplier) {
            Objects.requireNonNull(skipPhasesSupplier, "skipPhasesSupplier must not be null");
            return new PhaseStage<>(phases, () -> Set.copyOf(skipPhasesSupplier.get()));
        }

        /**
         * Maps each phase key to a synchronous task using the common fork-join pool.
         * Suitable for CPU-bound work. For I/O-bound tasks use {@link #map(Function, Executor)}
         * or {@link #mapAsync(Function)} to avoid thread starvation.
         */
        public <V> PhaseTaskIterator<K, V> map(Function<K, V> taskMapper) {
            return new PhaseTaskIterator<>(
                    phases,
                    skipPhases,
                    phase -> CompletableFuture.supplyAsync(() -> taskMapper.apply(phase)),
                    v -> true,
                    false);
        }

        /**
         * Maps each phase key to a synchronous task using the provided executor.
         * Use this for I/O-bound tasks (DB, HTTP, file) to avoid starving the common fork-join pool.
         */
        public <V> PhaseTaskIterator<K, V> map(Function<K, V> taskMapper, Executor executor) {
            return new PhaseTaskIterator<>(
                    phases,
                    skipPhases,
                    phase -> CompletableFuture.supplyAsync(() -> taskMapper.apply(phase), executor),
                    v -> true,
                    false);
        }

        /**
         * For callers that manage their own async execution — custom executor,
         * virtual threads, reactive pipelines, etc.
         */
        public <V> PhaseTaskIterator<K, V> mapAsync(Function<K, CompletableFuture<V>> taskMapper) {
            return new PhaseTaskIterator<>(phases, skipPhases, taskMapper, v -> true, false);
        }
    }
}
