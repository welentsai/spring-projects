package com.example.demo.util.multiphaseterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TryTest {

    // ── factories ─────────────────────────────────────────────────────────────

    @Test
    void success_returnsTrue_forIsSuccess() {
        assertThat(Try.success("ok").isSuccess()).isTrue();
    }

    @Test
    void success_returnsFalse_forIsFailure() {
        assertThat(Try.success("ok").isFailure()).isFalse();
    }

    @Test
    void failure_isFailure() {
        Try<String> t = Try.failure(new RuntimeException("boom"));
        assertThat(t.isFailure()).isTrue();
        assertThat(t.isSuccess()).isFalse();
    }

    @Test
    void of_wrapsSupplierResult() {
        assertThat(Try.of(() -> 42).isSuccess()).isTrue();
    }

    @Test
    void of_catchesException_returnsFailure() {
        Try<Integer> t = Try.of(() -> { throw new IllegalArgumentException("bad"); });
        assertThat(t.isFailure()).isTrue();
        assertThat(t).isInstanceOfSatisfying(Try.Failure.class,
                f -> assertThat(f.cause()).isInstanceOf(IllegalArgumentException.class));
    }

    // ── getOrElse ─────────────────────────────────────────────────────────────

    @Test
    void getOrElse_returnsValue_onSuccess() {
        assertThat(Try.success("hello").getOrElse("fallback")).isEqualTo("hello");
    }

    @Test
    void getOrElse_returnsFallback_onFailure() {
        assertThat(Try.<String>failure(new RuntimeException()).getOrElse("fallback")).isEqualTo("fallback");
    }

    // ── getOrThrow ────────────────────────────────────────────────────────────

    @Test
    void getOrThrow_returnsValue_onSuccess() {
        assertThat(Try.success(99).getOrThrow()).isEqualTo(99);
    }

    @Test
    void getOrThrow_throws_onFailure() {
        RuntimeException cause = new RuntimeException("cause");
        assertThatThrownBy(() -> Try.failure(cause).getOrThrow())
                .isInstanceOf(IllegalStateException.class)
                .hasCause(cause);
    }

    // ── map ───────────────────────────────────────────────────────────────────

    @Test
    void map_transformsValue_onSuccess() {
        assertThat(Try.success("hello").map(String::toUpperCase).getOrElse("")).isEqualTo("HELLO");
    }

    @Test
    void map_propagatesFailure_withoutCallingMapper() {
        RuntimeException cause = new RuntimeException("original");
        Try<String> result = Try.<String>failure(cause).map(s -> { throw new AssertionError("should not run"); });
        assertThat(result.isFailure()).isTrue();
        assertThat(result).isInstanceOfSatisfying(Try.Failure.class,
                f -> assertThat(f.cause()).isSameAs(cause));
    }

    @Test
    void map_returnsFailure_whenMapperThrows() {
        Try<String> result = Try.success("ok").map(s -> { throw new RuntimeException("mapper error"); });
        assertThat(result.isFailure()).isTrue();
    }

    // ── flatMap ───────────────────────────────────────────────────────────────

    @Test
    void flatMap_chainsSuccesses() {
        Try<Integer> result = Try.success("42").flatMap(s -> Try.success(Integer.parseInt(s)));
        assertThat(result.getOrElse(-1)).isEqualTo(42);
    }

    @Test
    void flatMap_propagatesFirstFailure() {
        RuntimeException cause = new RuntimeException("first");
        Try<Integer> result = Try.<String>failure(cause)
                .flatMap(s -> Try.success(Integer.parseInt(s)));
        assertThat(result.isFailure()).isTrue();
        assertThat(result).isInstanceOfSatisfying(Try.Failure.class,
                f -> assertThat(f.cause()).isSameAs(cause));
    }

    // ── onSuccess / onFailure ─────────────────────────────────────────────────

    @Test
    void onSuccess_callsAction_onSuccess() {
        var holder = new String[1];
        Try.success("triggered").onSuccess(v -> holder[0] = v);
        assertThat(holder[0]).isEqualTo("triggered");
    }

    @Test
    void onSuccess_doesNotCallAction_onFailure() {
        var holder = new String[1];
        Try.<String>failure(new RuntimeException()).onSuccess(v -> holder[0] = v);
        assertThat(holder[0]).isNull();
    }

    @Test
    void onFailure_callsAction_onFailure() {
        var holder = new Throwable[1];
        RuntimeException cause = new RuntimeException("err");
        Try.<String>failure(cause).onFailure(t -> holder[0] = t);
        assertThat(holder[0]).isSameAs(cause);
    }

    // ── toOptional ────────────────────────────────────────────────────────────

    @Test
    void toOptional_returnsValue_onSuccess() {
        assertThat(Try.success("hi").toOptional()).contains("hi");
    }

    @Test
    void toOptional_returnsEmpty_onFailure() {
        assertThat(Try.<String>failure(new RuntimeException()).toOptional()).isEmpty();
    }

    @Test
    void toOptional_returnsEmpty_whenValueIsNull() {
        assertThat(Try.success(null).toOptional()).isEmpty();
    }
}
