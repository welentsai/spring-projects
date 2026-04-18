package com.example.demo.util.multiphaseterator;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent iterator that runs an async task over a list of phases and collects typed results.
 *
 * <pre>{@code
 * List<PhaseTaskOutput<String, DeployResult>> results = PhaseTaskIterator
 *     .over(List.of("dev", "staging", "prod"))
 *     .map(phase -> CompletableFuture.supplyAsync(() -> deploy(phase)))
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
    private final Function<K, CompletableFuture<V>> taskMapper;
    private final Predicate<V> successCriteria;
    private final boolean earlyStop;

    private PhaseTaskIterator(
            List<K> phases,
            Function<K, CompletableFuture<V>> taskMapper,
            Predicate<V> successCriteria,
            boolean earlyStop) {
        this.phases = List.copyOf(phases);
        this.taskMapper = taskMapper;
        this.successCriteria = successCriteria;
        this.earlyStop = earlyStop;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static <K> PhaseStage<K> over(List<K> phases) {
        return new PhaseStage<>(phases);
    }

    // ── Builder steps (each returns a new immutable instance) ────────────────

    /**
     * Predicate applied to each task's output. A phase is SUCCEEDED only when the
     * task completes without exception AND this predicate returns {@code true}.
     * Defaults to always-true if not set.
     */
    public PhaseTaskIterator<K, V> isSuccessCriteria(Predicate<V> criteria) {
        return new PhaseTaskIterator<>(phases, taskMapper, criteria, earlyStop);
    }

    /**
     * When set, the first phase whose status is FAILED_BY_EXCEPTION or FAILED_BY_CRITERIA
     * causes all subsequent phases to be marked SKIPPED instead of executed.
     *
     * <p>In {@link #executeParallel()} mode all futures are still fired upfront — stopEarly
     * stops <em>collecting</em> results, not background task execution.
     */
    public PhaseTaskIterator<K, V> stopEarly() {
        return new PhaseTaskIterator<>(phases, taskMapper, successCriteria, true);
    }

    // ── Terminal operations ───────────────────────────────────────────────────

    /** Runs phases one at a time, in order. Waits for each to complete before starting the next. */
    public List<PhaseTaskOutput<K, V>> execute() {
        List<PhaseTaskOutput<K, V>> results = new ArrayList<>();
        boolean stopped = false;

        for (K phase : phases) {
            if (stopped) {
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
        // Fire all futures upfront, recording each task's logical start time
        List<Instant> startTimes = new ArrayList<>();
        List<Map.Entry<K, CompletableFuture<V>>> futures = new ArrayList<>();

        for (K phase : phases) {
            startTimes.add(Instant.now());
            futures.add(Map.entry(phase, taskMapper.apply(phase)));
        }

        List<PhaseTaskOutput<K, V>> results = new ArrayList<>();
        boolean stopped = false;

        for (int i = 0; i < futures.size(); i++) {
            K phase = futures.get(i).getKey();
            if (stopped) {
                results.add(PhaseTaskOutput.skipped(phase));
                continue;
            }
            PhaseTaskOutput<K, V> output = awaitFuture(phase, futures.get(i).getValue(), startTimes.get(i));
            results.add(output);
            if (earlyStop && output.isFailed()) stopped = true;
        }

        return List.copyOf(results);
    }

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
            return new PhaseTaskOutput<>(phase, status, Try.success(value), duration);
        } catch (CompletionException e) {
            Duration duration = Duration.between(startedAt, Instant.now());
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return new PhaseTaskOutput<>(phase, PhaseTaskStatus.FAILED_BY_EXCEPTION, Try.failure(cause), duration);
        }
    }

    // ── Inner builder: untyped until .map() introduces V ─────────────────────

    public static final class PhaseStage<K> {

        private final List<K> phases;

        private PhaseStage(List<K> phases) {
            this.phases = phases;
        }

        /**
         * Maps each phase key to a synchronous task. The function is wrapped in
         * {@link CompletableFuture#supplyAsync} using the common fork-join pool,
         * so {@link PhaseTaskIterator#executeParallel()} runs tasks truly in parallel.
         *
         * <p>Any exception thrown inside the function is captured as
         * {@link PhaseTaskStatus#FAILED_BY_EXCEPTION} — callers do not need to handle it.
         */
        public <V> PhaseTaskIterator<K, V> map(Function<K, V> taskMapper) {
            return new PhaseTaskIterator<>(
                    phases,
                    phase -> CompletableFuture.supplyAsync(() -> taskMapper.apply(phase)),
                    v -> true,
                    false);
        }

        /**
         * For callers that manage their own async execution — custom executor,
         * virtual threads, reactive pipelines, etc.
         */
        public <V> PhaseTaskIterator<K, V> mapAsync(Function<K, CompletableFuture<V>> taskMapper) {
            return new PhaseTaskIterator<>(phases, taskMapper, v -> true, false);
        }
    }
}
