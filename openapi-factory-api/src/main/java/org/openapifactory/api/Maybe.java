package org.openapifactory.api;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Maybe<T> {
    static <T> Maybe<T> missing(String errorMessage) {
        return new Missing<>(errorMessage);
    }

    static <T> Maybe<T> present(T o) {
        return new Present<T>(o);
    }

    static <T> Maybe<T> ofNullable(T o, String errorMessage) {
        return o != null ? present(o) : missing(errorMessage);
    }

    T required();

    T orNull();

    void ifPresent(Consumer<T> consumer);

    boolean isPresent();

    <T2> Maybe<T2> map(Function<T, T2> fn);

    T orElse(T defaultValue);

    <T2 extends T> Maybe<T2> filterType(Class<T2> subtype);

    Maybe<T> filter(Predicate<T> predicate, String errorMessage);

    Maybe<T> filter(Predicate<T> predicate, Function<T, String> errorMessage);


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

        @Override
        public <T2 extends T> Maybe<T2> filterType(Class<T2> subtype) {
            return missing(errorMessage);
        }

        @Override
        public Maybe<T> filter(Predicate<T> predicate, String errorMessage) {
            return missing(this.errorMessage);
        }

        @Override
        public Maybe<T> filter(Predicate<T> predicate, Function<T, String> errorMessage) {
            return missing(this.errorMessage);
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

        @Override
        public <T2 extends T> Maybe<T2> filterType(Class<T2> subtype) {
            if (subtype.isAssignableFrom(o.getClass())) {
                //noinspection unchecked
                return present((T2)o);
            }
            return missing("Not of " + subtype + ": " + o);
        }

        @Override
        public Maybe<T> filter(Predicate<T> predicate, String errorMessage) {
            return predicate.test(o) ? this : missing(errorMessage);
        }

        @Override
        public Maybe<T> filter(Predicate<T> predicate, Function<T, String > errorMessage) {
            return predicate.test(o) ? this : missing(errorMessage.apply(o));
        }
    }
}
