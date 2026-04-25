package com.example.demo.util.multiphaseterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

class PhaseTaskIteratorTest {

    private static final List<String> PHASES = List.of("alpha", "beta", "gamma");

    @Nested
    class Execute {

        @Test
        void returnsOneOutputPerPhase_inOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-done")
                    .execute();

            assertThat(results).hasSize(3);
            assertThat(results.get(0).phase()).isEqualTo("alpha");
            assertThat(results.get(1).phase()).isEqualTo("beta");
            assertThat(results.get(2).phase()).isEqualTo("gamma");
        }

        @Test
        void returnsEmptyList_whenNoPhasesGiven() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.<String>of())
                    .map(phase -> "unreachable")
                    .execute();

            assertThat(results).isEmpty();
        }

        @Test
        void allSucceeded_whenCriteriaPasses() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> "ok")
                    .isSuccessCriteria(v -> v.equals("ok"))
                    .execute();

            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void failedByCriteria_whenCriteriaFails() {
            List<PhaseTaskOutput<String, Integer>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> 0)
                    .isSuccessCriteria(v -> v > 0)
                    .execute();

            assertThat(results).allMatch(r -> r.status() == PhaseTaskStatus.FAILED_BY_CRITERIA);
        }

        @Test
        void failedByException_whenTaskThrows() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .<String>map(phase -> {
                        throw new RuntimeException("task error");
                    })
                    .execute();

            assertThat(results).allMatch(r -> r.status() == PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }

        @Test
        void valueAccessible_onSuccess() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "payload")
                    .execute();

            assertThat(results.get(0).value()).contains("payload");
        }

        @Test
        void valueEmpty_onFailure() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .<String>map(phase -> {
                        throw new RuntimeException();
                    })
                    .execute();

            assertThat(results.get(0).value()).isEmpty();
        }

        @Test
        void noCriteria_defaultsToAlwaysSucceeded() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> "anything")
                    .execute();

            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void recordsNonNegativeDuration_forCompletedPhase() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "done")
                    .execute();

            assertThat(results.get(0).duration().toNanos()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    class StopEarly {

        @Test
        void skipsRemainingAfterCriteriaFailure() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.equals("alpha") ? "fail" : "ok")
                    .isSuccessCriteria(v -> v.equals("ok"))
                    .stopEarly()
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA);
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(2).isSkipped()).isTrue();
        }

        @Test
        void skipsRemainingAfterException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> {
                        if (phase.equals("alpha")) throw new RuntimeException("boom");
                        return "ok";
                    })
                    .stopEarly()
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(2).isSkipped()).isTrue();
        }

        @Test
        void doesNotSkip_whenAllSucceed() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> "ok")
                    .isSuccessCriteria(v -> v.equals("ok"))
                    .stopEarly()
                    .execute();

            assertThat(results).noneMatch(PhaseTaskOutput::isSkipped);
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void collectsResultUpToAndIncludingFailure() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.equals("beta") ? "bad" : "ok")
                    .isSuccessCriteria(v -> v.equals("ok"))
                    .stopEarly()
                    .execute();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA);
            assertThat(results.get(2).isSkipped()).isTrue();
        }

        @Test
        void skippedOutput_hasEmptyResult_andZeroDuration() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .<String>map(phase -> {
                        throw new RuntimeException();
                    })
                    .stopEarly()
                    .execute();

            PhaseTaskOutput<String, String> skipped = results.get(1);
            assertThat(skipped.isSkipped()).isTrue();
            assertThat(skipped.value()).isEmpty();
            assertThat(skipped.duration().isZero()).isTrue();
        }
    }

    @Nested
    class ExecuteParallel {

        @Test
        void returnsAllResultsInPhaseOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-parallel")
                    .executeParallel();

            assertThat(results).hasSize(3);
            assertThat(results.get(0).phase()).isEqualTo("alpha");
            assertThat(results.get(1).phase()).isEqualTo("beta");
            assertThat(results.get(2).phase()).isEqualTo("gamma");
        }

        @Test
        void returnsEmptyList_whenNoPhasesGiven() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.<String>of())
                    .map(phase -> "unreachable")
                    .executeParallel();

            assertThat(results).isEmpty();
        }

        @Test
        void allSucceeded_whenCriteriaPasses() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> "ok")
                    .isSuccessCriteria(v -> v.equals("ok"))
                    .executeParallel();

            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void collectsAll_withoutStopEarly() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .mapAsync(phase -> phase.equals("beta")
                            ? CompletableFuture.failedFuture(new RuntimeException("boom"))
                            : CompletableFuture.completedFuture("ok"))
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        @Test
        void stopEarly_skipsAfterFirstFailure() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .mapAsync(phase -> phase.equals("alpha")
                            ? CompletableFuture.failedFuture(new RuntimeException("boom"))
                            : CompletableFuture.completedFuture("ok"))
                    .stopEarly()
                    .executeParallel();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(2).isSkipped()).isTrue();
        }

        // ── thenMap chaining ──────────────────────────────────────────────────

        @Test
        void thenMap_pipelinesResults_inPhaseOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.length()) // String → Integer
                    .thenMap(n -> "len=" + n) // Integer → String
                    .executeParallel();

            assertThat(results).hasSize(3);
            assertThat(results.get(0).phase()).isEqualTo("alpha");
            assertThat(results.get(0).value()).contains("len=5");
            assertThat(results.get(1).value()).contains("len=4"); // "beta"
            assertThat(results.get(2).value()).contains("len=5"); // "gamma"
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void thenMap_multipleSteps_preservesPhaseOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.toUpperCase())
                    .thenMap(s -> s + "!")
                    .thenMap(s -> "[" + s + "]")
                    .executeParallel();

            assertThat(results.get(0).value()).contains("[ALPHA!]");
            assertThat(results.get(1).value()).contains("[BETA!]");
            assertThat(results.get(2).value()).contains("[GAMMA!]");
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void thenMap_exceptionMidPipeline_recordsFailedByException_forThatPhase() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase)
                    .thenMap(s -> {
                        if (s.equals("beta")) throw new RuntimeException("beta thenMap fail");
                        return s + "-ok";
                    })
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        @Test
        void thenMap_stopEarly_skipsAfterFirstPipelineFailure() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .mapAsync(phase -> phase.equals("alpha")
                            ? CompletableFuture.<String>failedFuture(new RuntimeException("boom"))
                            : CompletableFuture.completedFuture(phase))
                    .thenMap(s -> s + "-then")
                    .stopEarly()
                    .executeParallel();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(2).isSkipped()).isTrue();
        }

        @Test
        void thenMap_collectsAll_withoutStopEarly() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase)
                    .thenMap(s -> {
                        if (s.equals("beta")) throw new RuntimeException("beta fail");
                        return s + "-ok";
                    })
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        // ── thenMapAsync chaining ─────────────────────────────────────────────

        @Test
        void thenMapAsync_composesAndPreservesPhaseOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-sync")
                    .thenMapAsync(s -> CompletableFuture.completedFuture(s + "-async"))
                    .executeParallel();

            assertThat(results.get(0).value()).contains("alpha-sync-async");
            assertThat(results.get(1).value()).contains("beta-sync-async");
            assertThat(results.get(2).value()).contains("gamma-sync-async");
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void thenMapAsync_failedContinuation_recordsFailedByException_forThatPhase() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase)
                    .thenMapAsync(s -> s.equals("beta")
                            ? CompletableFuture.<String>failedFuture(new RuntimeException("async fail"))
                            : CompletableFuture.completedFuture(s + "-ok"))
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        // ── andMap chaining ───────────────────────────────────────────────────

        @Test
        void andMap_fanOut_combinesResultsInPhaseOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-A")
                    .andMap(phase -> phase + "-B", (a, b) -> a + "|" + b)
                    .executeParallel();

            assertThat(results.get(0).value()).contains("alpha-A|alpha-B");
            assertThat(results.get(1).value()).contains("beta-A|beta-B");
            assertThat(results.get(2).value()).contains("gamma-A|gamma-B");
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void andMap_exceptionInParallelBranch_recordsFailedByException_forThatPhase() {
            Function<String, String> failForBeta = phase -> {
                if (phase.equals("beta")) throw new RuntimeException("beta parallel fail");
                return phase + "-B";
            };

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-A")
                    .andMap(failForBeta, (a, b) -> a + "|" + b)
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        @Test
        void andMap_stopEarly_skipsAfterFirstFailure() {
            Function<String, String> failForAlpha = phase -> {
                if (phase.equals("alpha")) throw new RuntimeException("alpha parallel fail");
                return phase + "-B";
            };

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-A")
                    .andMap(failForAlpha, (a, b) -> a + "|" + b)
                    .stopEarly()
                    .executeParallel();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(2).isSkipped()).isTrue();
        }

        @Test
        void andMap_isSuccessCriteria_appliesTo_combinedResult() {
            List<PhaseTaskOutput<String, Integer>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.length())
                    .andMap(phase -> 10, Integer::sum)
                    .isSuccessCriteria(n -> n > 10)
                    .executeParallel();

            // alpha(5)+10=15, beta(4)+10=14, gamma(5)+10=15 — all > 10
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        // ── andMapAsync chaining ──────────────────────────────────────────────

        @Test
        void andMapAsync_combinesAsyncBranches_inPhaseOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .mapAsync(phase -> CompletableFuture.completedFuture(phase + "-async1"))
                    .andMapAsync(
                            phase -> CompletableFuture.completedFuture(phase + "-async2"),
                            (a, b) -> a + "|" + b)
                    .executeParallel();

            assertThat(results.get(0).value()).contains("alpha-async1|alpha-async2");
            assertThat(results.get(1).value()).contains("beta-async1|beta-async2");
            assertThat(results.get(2).value()).contains("gamma-async1|gamma-async2");
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void andMapAsync_failedParallelFuture_recordsFailedByException_forThatPhase() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-primary")
                    .andMapAsync(
                            phase -> phase.equals("beta")
                                    ? CompletableFuture.<String>failedFuture(
                                            new RuntimeException("beta async fail"))
                                    : CompletableFuture.completedFuture(phase + "-parallel"),
                            (a, b) -> a + "|" + b)
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        // ── Mixed chaining ────────────────────────────────────────────────────

        @Test
        void andMap_thenThenMap_preservesPhaseOrder() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-A")
                    .andMap(phase -> phase + "-B", (a, b) -> a + "|" + b) // "alpha-A|alpha-B"
                    .thenMap(String::toUpperCase) // "ALPHA-A|ALPHA-B"
                    .executeParallel();

            assertThat(results.get(0).value()).contains("ALPHA-A|ALPHA-B");
            assertThat(results.get(1).value()).contains("BETA-A|BETA-B");
            assertThat(results.get(2).value()).contains("GAMMA-A|GAMMA-B");
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void fullChain_thenMap_andMap_stopEarly_executeParallel() {
            // alpha(5) → "long-a", beta(4) → "short-b" [fails criteria], gamma → skipped
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.length())
                    .thenMap(n -> n > 4 ? "long" : "short")
                    .andMap(phase -> "-" + phase.charAt(0), (label, suffix) -> label + suffix)
                    .isSuccessCriteria(s -> s.startsWith("long"))
                    .stopEarly()
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue(); // "long-a"
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA); // "short-b"
            assertThat(results.get(2).isSkipped()).isTrue();
        }
    }

    @Nested
    class MapAsync {

        @Test
        void acceptsExplicitCompletableFuture() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .mapAsync(phase -> CompletableFuture.completedFuture("async-result"))
                    .execute();

            assertThat(results.get(0).value()).contains("async-result");
            assertThat(results.get(0).isSucceeded()).isTrue();
        }

        @Test
        void capturesFailedFuture_asException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .mapAsync(phase ->
                            CompletableFuture.<String>failedFuture(new RuntimeException("async fail")))
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }
    }

    @Nested
    class SkipPhases {

        @Test
        void skippedPhases_areMarkedSkipped_andNotExecuted() {
            List<String> executed = new ArrayList<>();
            PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(List.of("alpha"))
                    .map(phase -> {
                        executed.add(phase);
                        return "done";
                    })
                    .execute();

            assertThat(executed).containsExactly("beta", "gamma");
        }

        @Test
        void skippedPhases_stillAppearInResults_withSkippedStatus() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(List.of("alpha"))
                    .map(phase -> "done")
                    .execute();

            assertThat(results).hasSize(3);
            assertThat(results.get(0).phase()).isEqualTo("alpha");
            assertThat(results.get(0).isSkipped()).isTrue();
            assertThat(results.get(1).isSucceeded()).isTrue();
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        @Test
        void skippedOutput_hasEmptyValue_andZeroDuration() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(List.of("beta"))
                    .map(phase -> "done")
                    .execute();

            PhaseTaskOutput<String, String> skipped = results.get(1);
            assertThat(skipped.value()).isEmpty();
            assertThat(skipped.duration().isZero()).isTrue();
        }

        @Test
        void skipAllPhases_returnsAllSkipped() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(PHASES)
                    .map(phase -> "unreachable")
                    .execute();

            assertThat(results).allMatch(PhaseTaskOutput::isSkipped);
        }

        @Test
        void emptySkipList_executesAllPhases() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(List.of())
                    .map(phase -> "done")
                    .execute();

            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void skipPhases_worksWithExecuteParallel() {
            List<String> executed = new ArrayList<>();
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(List.of("gamma"))
                    .map(phase -> {
                        executed.add(phase);
                        return "done";
                    })
                    .executeParallel();

            assertThat(executed).doesNotContain("gamma");
            assertThat(results.get(2).isSkipped()).isTrue();
            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).isSucceeded()).isTrue();
        }

        @Test
        void skipPhases_combinesWithStopEarly() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(List.of("alpha"))
                    .map(phase -> phase.equals("beta") ? "fail" : "ok")
                    .isSuccessCriteria(v -> v.equals("ok"))
                    .stopEarly()
                    .execute();

            assertThat(results.get(0).isSkipped()).isTrue(); // alpha — explicitly skipped
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA); // beta
            assertThat(results.get(2).isSkipped()).isTrue(); // gamma — stopped early
        }
    }

    @Nested
    class SkipPhasesSupplier {

        @Test
        void supplier_isEvaluatedFreshOnEachExecute() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(List.of("alpha"));

            PhaseTaskIterator<String, ?> iterator = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList);

            List<PhaseTaskOutput<String, String>> first = iterator
                    .map(phase -> "done")
                    .isSuccessCriteria(v -> v.equals("done"))
                    .execute();

            assertThat(first.get(0).isSkipped()).isTrue(); // alpha skipped
            assertThat(first.get(1).isSucceeded()).isTrue();
            assertThat(first.get(2).isSucceeded()).isTrue();

            holder.setApmPhaseList(List.of("gamma"));

            List<PhaseTaskOutput<String, String>> second = iterator.map(phase -> "done").execute();
            assertThat(second.get(0).isSucceeded()).isTrue(); // alpha now runs
            assertThat(second.get(2).isSkipped()).isTrue(); // gamma now skipped
        }

        @Test
        void supplier_emptyList_executesAllPhases() {
            ApmConfigHolder holder = new ApmConfigHolder();

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> "done")
                    .execute();

            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void supplier_skippedPhases_areNotDeployed() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(List.of("beta"));

            List<String> deployed = new ArrayList<>();
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> {
                        deployed.add(phase);
                        return "deployed";
                    })
                    .execute();

            assertThat(deployed).containsExactly("alpha", "gamma");
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(1).phase()).isEqualTo("beta");
        }

        @Test
        void supplier_multipleSkippedPhases_allMarkedSkipped() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(List.of("alpha", "gamma"));

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> "deployed")
                    .execute();

            assertThat(results.get(0).isSkipped()).isTrue();
            assertThat(results.get(1).isSucceeded()).isTrue();
            assertThat(results.get(2).isSkipped()).isTrue();
        }

        @Test
        void supplier_allPhasesSkipped_nothingDeployed() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(PHASES);

            List<String> deployed = new ArrayList<>();
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> {
                        deployed.add(phase);
                        return "deployed";
                    })
                    .execute();

            assertThat(deployed).isEmpty();
            assertThat(results).allMatch(PhaseTaskOutput::isSkipped);
        }

        @Test
        void supplier_combinesWithStopEarly() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(List.of("alpha"));

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> phase.equals("beta") ? "fail" : "deployed")
                    .isSuccessCriteria(v -> v.equals("deployed"))
                    .stopEarly()
                    .execute();

            assertThat(results.get(0).isSkipped()).isTrue(); // alpha — holder skip
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA); // beta
            assertThat(results.get(2).isSkipped()).isTrue(); // gamma — stopEarly
        }

        @Test
        void supplier_worksWithExecuteParallel() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(List.of("beta"));

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> "deployed")
                    .executeParallel();

            assertThat(results.get(0).isSucceeded()).isTrue();
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(2).isSucceeded()).isTrue();
        }

        @Test
        void supplier_skippedOutput_hasEmptyValue_andZeroDuration() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(List.of("alpha"));

            PhaseTaskOutput<String, String> skipped = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> "deployed")
                    .execute()
                    .get(0);

            assertThat(skipped.phase()).isEqualTo("alpha");
            assertThat(skipped.value()).isEmpty();
            assertThat(skipped.duration().isZero()).isTrue();
        }
    }

    @Nested
    class GenericKeyType {

        @Test
        void acceptsNonStringKeyType() {
            List<PhaseTaskOutput<Integer, String>> results = PhaseTaskIterator
                    .over(List.of(1, 2, 3))
                    .map(n -> "step-" + n)
                    .execute();

            assertThat(results).hasSize(3);
            assertThat(results.get(0).phase()).isEqualTo(1);
            assertThat(results.get(2).value()).contains("step-3");
        }
    }

    // ── Sequential composition ────────────────────────────────────────────────

    @Nested
    class ThenMap {

        @Test
        void pipelinesOutput_fromPreviousStep() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.length()) // String → Integer
                    .thenMap(n -> "len=" + n) // Integer → String
                    .execute();

            assertThat(results.get(0).value()).contains("len=5"); // "alpha"
            assertThat(results.get(1).value()).contains("len=4"); // "beta"
            assertThat(results.get(2).value()).contains("len=5"); // "gamma"
        }

        @Test
        void multipleSteps_chainedSequentially() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> 1)
                    .thenMap(n -> n * 10)
                    .thenMap(n -> "result=" + n)
                    .execute();

            assertThat(results.get(0).value()).contains("result=10");
            assertThat(results.get(0).isSucceeded()).isTrue();
        }

        @Test
        void exceptionInUpstreamMap_recordsFailedByException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .<Integer>map(phase -> {
                        throw new RuntimeException("map failed");
                    })
                    .<String>thenMap(n -> "never")
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }

        @Test
        void exceptionInThenMap_recordsFailedByException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> 42)
                    .<String>thenMap(n -> {
                        throw new RuntimeException("then failed");
                    })
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }

        @Test
        void isSuccessCriteria_appliesTo_finalOutput() {
            List<PhaseTaskOutput<String, Integer>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(String::toUpperCase)
                    .thenMap(String::length)
                    .isSuccessCriteria(n -> n > 3)
                    .execute();

            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded); // all names > 3 chars
        }

        @Test
        void withExecutor_invokesProvidedExecutor() {
            var invoked = new AtomicBoolean(false);
            // Inline executor records invocation, then runs the task on the calling thread
            var trackingExecutor = (java.util.concurrent.Executor) r -> {
                invoked.set(true);
                r.run();
            };

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "mapped")
                    .thenMap(v -> v + "-then", trackingExecutor)
                    .execute();

            assertThat(invoked).isTrue();
            assertThat(results.get(0).value()).contains("mapped-then");
        }

        @Test
        void throwsIllegalState_whenCalledBeforeMap() {
            assertThatThrownBy(() -> PhaseTaskIterator
                    .over(PHASES)
                    .thenMap(v -> v)
                    .execute())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class ThenMapAsync {

        @Test
        void composesAsyncContinuation() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "step1")
                    .thenMapAsync(v -> CompletableFuture.completedFuture(v + "-async"))
                    .execute();

            assertThat(results.get(0).value()).contains("step1-async");
            assertThat(results.get(0).isSucceeded()).isTrue();
        }

        @Test
        void failedFuture_inContinuation_recordsFailedByException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "step1")
                    .thenMapAsync(v -> CompletableFuture.<String>failedFuture(
                            new RuntimeException("async fail")))
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }

        @Test
        void chainedAfterThenMap_composesCorrectly() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> 1)
                    .thenMap(n -> n + "-sync")
                    .thenMapAsync(s -> CompletableFuture.completedFuture(s + "-async"))
                    .execute();

            assertThat(results.get(0).value()).contains("1-sync-async");
        }
    }

    // ── Parallel composition ──────────────────────────────────────────────────

    @Nested
    class AndMap {

        @Test
        void runsBothTasks_andCombinesResults() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase + "-A")
                    .andMap(phase -> phase + "-B", (a, b) -> a + "|" + b)
                    .execute();

            assertThat(results.get(0).value()).contains("alpha-A|alpha-B");
            assertThat(results.get(1).value()).contains("beta-A|beta-B");
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void exceptionInPrimaryTask_recordsFailedByException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .<String>map(phase -> {
                        throw new RuntimeException("primary failed");
                    })
                    .andMap(phase -> "parallel-ok", (a, b) -> a + b)
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }

        @Test
        void exceptionInParallelTask_recordsFailedByException() {
            Function<String, String> failingFn = phase -> {
                throw new RuntimeException("parallel failed");
            };

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "primary-ok")
                    .andMap(failingFn, (a, b) -> a + b)
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }

        @Test
        void withExecutor_invokesProvidedExecutor() {
            var invoked = new AtomicBoolean(false);
            var trackingExecutor = (java.util.concurrent.Executor) r -> {
                invoked.set(true);
                r.run();
            };

            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "A")
                    .andMap(phase -> "B", trackingExecutor, (a, b) -> a + b)
                    .execute();

            assertThat(invoked).isTrue();
            assertThat(results.get(0).value()).contains("AB");
        }

        @Test
        void isSuccessCriteria_appliesTo_combinedResult() {
            List<PhaseTaskOutput<String, Integer>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.length())
                    .andMap(phase -> 10, Integer::sum)
                    .isSuccessCriteria(n -> n > 10)
                    .execute();

            // alpha(5)+10=15>10 ✓, beta(4)+10=14>10 ✓, gamma(5)+10=15>10 ✓
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void throwsIllegalState_whenCalledBeforeMap() {
            assertThatThrownBy(() -> PhaseTaskIterator
                    .over(PHASES)
                    .andMap(phase -> "x", (a, b) -> b)
                    .execute())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class AndMapAsync {

        @Test
        void combinesAsyncFutures() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .mapAsync(phase -> CompletableFuture.completedFuture(phase + "-async1"))
                    .andMapAsync(
                            phase -> CompletableFuture.completedFuture(phase + "-async2"),
                            (a, b) -> a + "|" + b)
                    .execute();

            assertThat(results.get(0).value()).contains("alpha-async1|alpha-async2");
            assertThat(results).allMatch(PhaseTaskOutput::isSucceeded);
        }

        @Test
        void failedParallelFuture_recordsFailedByException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "primary")
                    .andMapAsync(
                            phase -> CompletableFuture.<String>failedFuture(
                                    new RuntimeException("parallel async fail")),
                            (a, b) -> a + b)
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }

        @Test
        void failedPrimaryFuture_recordsFailedByException() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .mapAsync(phase -> CompletableFuture.<String>failedFuture(
                            new RuntimeException("primary failed")))
                    .andMapAsync(
                            phase -> CompletableFuture.completedFuture("parallel-ok"),
                            (a, b) -> a + b)
                    .execute();

            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
        }
    }

    // ── Mixed chaining ────────────────────────────────────────────────────────

    @Nested
    class Chaining {

        @Test
        void thenMap_afterAndMap_pipelinesTheCombinedResult() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "A")
                    .andMap(phase -> "B", (a, b) -> a + b) // → "AB"
                    .thenMap(String::toLowerCase) // → "ab"
                    .execute();

            assertThat(results.get(0).value()).contains("ab");
        }

        @Test
        void andMap_afterThenMap_addsParallelBranch_toTransformedResult() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "step1")
                    .thenMap(String::toUpperCase) // → "STEP1"
                    .andMap(phase -> "-extra", (a, b) -> a + b) // → "STEP1-extra"
                    .execute();

            assertThat(results.get(0).value()).contains("STEP1-extra");
        }

        @Test
        void fullChain_sequential_and_parallel_withStopEarly() {
            // alpha(5) → "long",  beta(4) → "short" [fails],  gamma → skipped
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> phase.length()) // String → Integer
                    .thenMap(n -> n > 4 ? "long" : "short") // Integer → label
                    .andMap(
                            phase -> "-" + phase.charAt(0), // parallel: first char suffix
                            (label, suffix) -> label + suffix) // combine → "long-a" etc.
                    .isSuccessCriteria(s -> s.startsWith("long"))
                    .stopEarly()
                    .execute();

            assertThat(results.get(0).isSucceeded()).isTrue(); // "long-a"
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA); // "short-b"
            assertThat(results.get(2).isSkipped()).isTrue(); // stopped early
        }

        @Test
        void thenMapAsync_afterAndMap_composesCorrectly() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(List.of("only"))
                    .map(phase -> "A")
                    .andMap(phase -> "B", (a, b) -> a + b) // → "AB"
                    .thenMapAsync(s -> CompletableFuture.completedFuture(s + "-done")) // → "AB-done"
                    .execute();

            assertThat(results.get(0).value()).contains("AB-done");
            assertThat(results.get(0).isSucceeded()).isTrue();
        }

        @Test
        void exceptionMidChain_stopsAt_failedStep_remainingPhasesSkipped_withStopEarly() {
            List<PhaseTaskOutput<String, String>> results = PhaseTaskIterator
                    .over(PHASES)
                    .map(phase -> "ok")
                    .thenMap(v -> {
                        if ("ok".equals(v)) throw new RuntimeException("fail mid-chain");
                        return v;
                    })
                    .andMap(phase -> "-suffix", (a, b) -> a + b)
                    .stopEarly()
                    .execute();

            assertThat(results).allMatch(r -> r.status() == PhaseTaskStatus.FAILED_BY_EXCEPTION
                    || r.isSkipped());
            assertThat(results.get(0).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_EXCEPTION);
            assertThat(results.get(1).isSkipped()).isTrue();
            assertThat(results.get(2).isSkipped()).isTrue();
        }
    }
}
