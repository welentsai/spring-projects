package com.example.demo.util.multiphaseterator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                    .<String>map(phase -> { throw new RuntimeException("task error"); })
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
                    .<String>map(phase -> { throw new RuntimeException(); })
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
                    .<String>map(phase -> { throw new RuntimeException(); })
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
                    .mapAsync(phase -> CompletableFuture.<String>failedFuture(new RuntimeException("async fail")))
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
                    .map(phase -> { executed.add(phase); return "done"; })
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
                    .map(phase -> { executed.add(phase); return "done"; })
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

            assertThat(results.get(0).isSkipped()).isTrue();   // alpha — explicitly skipped
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA); // beta
            assertThat(results.get(2).isSkipped()).isTrue();   // gamma — stopped early
        }
    }

    @Nested
    class SkipPhasesSupplier {

        @Test
        void supplier_isEvaluatedFreshOnEachExecute() {
            ApmConfigHolder holder = new ApmConfigHolder();
            holder.setApmPhaseList(List.of("alpha"));

            PhaseTaskIterator<String, String> iterator = PhaseTaskIterator
                    .over(PHASES)
                    .skipPhases(holder::getApmPhaseList)
                    .map(phase -> "done");

            List<PhaseTaskOutput<String, String>> first = iterator.execute();
            assertThat(first.get(0).isSkipped()).isTrue();  // alpha skipped
            assertThat(first.get(1).isSucceeded()).isTrue();
            assertThat(first.get(2).isSucceeded()).isTrue();

            holder.setApmPhaseList(List.of("gamma"));

            List<PhaseTaskOutput<String, String>> second = iterator.execute();
            assertThat(second.get(0).isSucceeded()).isTrue(); // alpha now runs
            assertThat(second.get(2).isSkipped()).isTrue();   // gamma now skipped
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
                    .map(phase -> { deployed.add(phase); return "deployed"; })
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
                    .map(phase -> { deployed.add(phase); return "deployed"; })
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

            assertThat(results.get(0).isSkipped()).isTrue();             // alpha — holder skip
            assertThat(results.get(1).status()).isEqualTo(PhaseTaskStatus.FAILED_BY_CRITERIA); // beta
            assertThat(results.get(2).isSkipped()).isTrue();             // gamma — stopEarly
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
}
