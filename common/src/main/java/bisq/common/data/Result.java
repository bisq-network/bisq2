/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.common.data;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents the outcome of an operation that can either succeed with a value
 * or fail with an exception.
 * <p>
 * This class is a minimal Java counterpart to Kotlin’s {@link kotlin.Result Result},
 * providing explicit success/failure handling without using exceptions for
 * control flow.
 * </p>
 *
 * <h3>Examples</h3>
 *
 * <pre>{@code
 * Result<String> r1 = Result.success("ok");
 *
 * Result<Integer> r2 = Result.of(() -> Integer.parseInt("123"));
 *
 * int value = r2.getOrElse(e -> 0);
 * }</pre>
 *
 * @param <T> the success value type
 * @see kotlin.Result
 */

public final class Result<T> {
    @Nullable
    private final T value;
    @Nullable
    private final Throwable error;

    private Result(T value, Throwable error) {
        this.value = value;
        this.error = error;

    }

    /* ---------- Factory methods ---------- */

    public static <T> Result<T> success(T value) {
        return new Result<>(Objects.requireNonNull(value, "value"), null);
    }

    public static <T> Result<T> failure(Throwable error) {
        return new Result<>(null, Objects.requireNonNull(error, "error"));
    }

    public static <T> Result<T> of(ThrowingSupplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception t) {
            return failure(t);
        }
    }

    /* ---------- State ---------- */

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    /* ---------- Access ---------- */

    public T getOrThrow() {
        if (error != null) {
            sneakyThrow(error);
        }
        return value;
    }

    public T getOrElse(Function<Throwable, T> fallback) {
        return isSuccess() ? value : fallback.apply(error);
    }

    public Throwable exceptionOrNull() {
        return error;
    }

    /* ---------- Transform ---------- */

    public <R> Result<R> map(Function<T, R> mapper) {
        if (isFailure()) {
            return failure(error);
        }
        try {
            return success(mapper.apply(value));
        } catch (Exception t) {
            return failure(t);
        }
    }

    public <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        if (isFailure()) {
            return failure(error);
        }
        try {
            return Objects.requireNonNull(mapper.apply(value));
        } catch (Exception t) {
            return failure(t);
        }
    }

    /* ---------- Utilities ---------- */

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
