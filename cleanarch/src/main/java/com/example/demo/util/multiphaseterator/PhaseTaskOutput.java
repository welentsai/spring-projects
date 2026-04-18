package com.example.demo.util.multiphaseterator;

import jakarta.annotation.Nullable;
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
        @Nullable Try<V> result, // null when status is SKIPPED
        Duration duration // Duration.ZERO when SKIPPED
        ) {

    static <K, V> PhaseTaskOutput<K, V> skipped(K phase) {
        return new PhaseTaskOutput<>(phase, PhaseTaskStatus.SKIPPED, null, Duration.ZERO);
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
        return result != null ? result.toOptional() : Optional.empty();
    }
}
