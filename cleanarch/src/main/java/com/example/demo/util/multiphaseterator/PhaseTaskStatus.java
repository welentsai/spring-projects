package com.example.demo.util.multiphaseterator;

public enum PhaseTaskStatus {
    /** Task completed and passed isSuccessCriteria. */
    SUCCEEDED,

    /** Task threw an exception or the CompletableFuture completed exceptionally. */
    FAILED_BY_EXCEPTION,

    /** Task completed but the output did not satisfy isSuccessCriteria. */
    FAILED_BY_CRITERIA,

    /** Phase was not executed because stopEarly() triggered on a prior failure. */
    SKIPPED
}
