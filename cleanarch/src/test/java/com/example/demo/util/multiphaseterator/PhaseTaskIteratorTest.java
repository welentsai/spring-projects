package com.example.demo.util.multiphaseterator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
