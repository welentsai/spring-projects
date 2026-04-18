package com.example.demo.util.multiphaseterator;

import java.time.Duration;
import java.util.Optional;

/**
 * Result of a single phase execution.
 *
 * @param <K> phase key type (e.g. String phase name, enum, etc.)
 * @param <V> task output type
 */
public record PhaseTaskOutput<K, V>(
        K phase,
        PhaseTaskStatus status,
        Optional<V> result,
        Duration duration) {

    static <K, V> PhaseTaskOutput<K, V> skipped(K phase) {
        return new PhaseTaskOutput<>(phase, PhaseTaskStatus.SKIPPED, Optional.empty(), Duration.ZERO);
    }

    public boolean isSucceeded() {
        return status == PhaseTaskStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == PhaseTaskStatus.FAILED_BY_EXCEPTION || status == PhaseTaskStatus.FAILED_BY_CRITERIA;
    }

    public boolean isSkipped() {
        return status == PhaseTaskStatus.SKIPPED;
    }

    /** Returns the task output value, or empty if the phase failed or was skipped. */
    public Optional<V> value() {
        return result;
    }
}
