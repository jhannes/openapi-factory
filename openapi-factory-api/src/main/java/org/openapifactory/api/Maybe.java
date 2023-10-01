package org.openapifactory.api;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Maybe<T> {
    static <T> Maybe<T> missing(String errorMessage) {
        return new Missing<>(errorMessage);
    }

    static <T> Maybe<T> present(T o) {
        return new Present<T>(o);
    }

    T required();

    T orNull();

    void ifPresent(Consumer<T> consumer);

    boolean isPresent();

    <T2> Maybe<T2> map(Function<T, T2> fn);

    T orElse(T defaultValue);

    class Missing<T> implements Maybe<T> {
        private final String errorMessage;

        public Missing(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public T required() {
            throw new RuntimeException(errorMessage);
        }

        @Override
        public T orNull() {
            return null;
        }

        @Override
        public void ifPresent(Consumer<T> o) {
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public <T2> Maybe<T2> map(Function<T, T2> o) {
            return new Missing<>(errorMessage);
        }

        @Override
        public T orElse(T defaultValue) {
            return defaultValue;
        }
    }

    class Present<T> implements Maybe<T> {
        private final T o;

        public Present(T o) {
            this.o = o;
        }

        @Override
        public T required() {
            return o;
        }

        @Override
        public T orNull() {
            return o;
        }

        @Override
        public void ifPresent(Consumer<T> consumer) {
            consumer.accept(o);
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public <T2> Maybe<T2> map(Function<T, T2> fn) {
            return present(fn.apply(o));
        }

        @Override
        public T orElse(T defaultValue) {
            return o;
        }
    }
}
