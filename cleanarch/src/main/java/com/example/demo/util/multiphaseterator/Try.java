package com.example.demo.util.multiphaseterator;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Try<T> permits Try.Success, Try.Failure {

    record Success<T>(T value) implements Try<T> {}

    record Failure<T>(Throwable cause) implements Try<T> {}

    static <T> Try<T> of(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Try<T> failure(Throwable cause) {
        return new Failure<>(cause);
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }

    default T getOrElse(T fallback) {
        if (this instanceof Success<T> s) return s.value();
        return fallback;
    }

    /** @throws IllegalStateException wrapping the original cause */
    default T getOrThrow() {
        if (this instanceof Success<T> s) return s.value();
        var f = (Failure<T>) this;
        throw new IllegalStateException(f.cause().getMessage(), f.cause());
    }

    default <U> Try<U> map(Function<T, U> mapper) {
        if (this instanceof Success<T> s) return Try.of(() -> mapper.apply(s.value()));
        return new Failure<>(((Failure<T>) this).cause());
    }

    default <U> Try<U> flatMap(Function<T, Try<U>> mapper) {
        if (this instanceof Success<T> s) return mapper.apply(s.value());
        return new Failure<>(((Failure<T>) this).cause());
    }

    default Try<T> onSuccess(Consumer<T> action) {
        if (this instanceof Success<T> s) action.accept(s.value());
        return this;
    }

    default Try<T> onFailure(Consumer<Throwable> action) {
        if (this instanceof Failure<T> f) action.accept(f.cause());
        return this;
    }

    default Optional<T> toOptional() {
        if (this instanceof Success<T> s) return Optional.ofNullable(s.value());
        return Optional.empty();
    }
}
