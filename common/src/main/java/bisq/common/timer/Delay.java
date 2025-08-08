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

package bisq.common.timer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A fluent API for scheduling delayed execution of asynchronous tasks that return {@code CompletableFuture<T>}.
 * <p>
 * This utility wraps {@link CompletableFuture#delayedExecutor(long, TimeUnit, Executor)} and composes it with
 * task execution and future flattening, allowing for clean, readable scheduling code such as:
 * </p>
 * <pre>{@code
 * Delay.run(() -> someAsyncOperation())
 *                 .withExecutor(myExecutor)
 *                 .after(1500);
 * }</pre>
 *
 * <h3>Purpose</h3>
 * <ul>
 *   <li>Encapsulates delay + execution + composition in a single readable chain.</li>
 *   <li>Allows reuse of delayed execution patterns without boilerplate.</li>
 *   <li>Improves readability of retry logic, exponential backoff, or deferred execution.</li>
 * </ul>
 *
 * <h3>Benefits</h3>
 * <ul>
 *   <li>Fluent, composable syntax for delayed async tasks.</li>
 *   <li>Efficient scheduling using non-blocking {@code delayedExecutor} API.</li>
 *   <li>Separates concerns: delay logic, execution context, and task definition.</li>
 * </ul>
 *
 * @param <T> the type of result produced by the delayed async task
 * @see CompletableFuture#delayedExecutor(long, TimeUnit, Executor)
 * @see CompletableFuture#thenCompose(Function)
 */
public class Delay<T> {
    private final Supplier<CompletableFuture<T>> taskSupplier;
    private Executor taskExecutor;

    /**
     * Constructs a new {@code Delay} for a given asynchronous task supplier.
     *
     * @param taskSupplier a {@code Supplier} that returns a {@code CompletableFuture<T>} task
     */
    private Delay(Supplier<CompletableFuture<T>> taskSupplier) {
        this.taskSupplier = taskSupplier;
    }

    /**
     * Factory method to create a new delayed execution instance.
     *
     * @param taskSupplier a {@code Supplier} that returns a {@code CompletableFuture<T>} task
     * @param <T>          the type of result produced by the task
     * @return a new {@code Delay<T>} instance
     */
    public static <T> Delay<T> run(Supplier<CompletableFuture<T>> taskSupplier) {
        return new Delay<>(taskSupplier);
    }

    /**
     * Specifies the executor that will execute the task after the delay.
     * <p>
     * This executor is used for actual task execution, not for the delay mechanism itself.
     * The delay is handled non-blockingly by {@code CompletableFuture.delayedExecutor}.
     * </p>
     *
     * @param taskExecutor the {@code Executor} to use for executing the task
     * @return the same {@code Delay} instance for fluent chaining
     */
    public Delay<T> withExecutor(Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    /**
     * Executes the task after a given delay in milliseconds.
     *
     * @param delay the delay in milliseconds before task execution
     * @return a {@code CompletableFuture<T>} representing the delayed async execution
     */
    public CompletableFuture<T> after(long delay) {
        return after(delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes the task after a given delay using the specified time unit.
     * <p>
     * The task is submitted to a delayed executor and then its result is flattened into a single
     * {@code CompletableFuture<T>} using {@code thenCompose}, assuming the supplier returns a nested future.
     * </p>
     *
     * @param delay    the amount of time to delay execution
     * @param timeUnit the unit of time for the delay
     * @return a {@code CompletableFuture<T>} representing the delayed and composed task
     * @throws IllegalStateException if the task executor has not been specified
     */
    public CompletableFuture<T> after(long delay, TimeUnit timeUnit) {
        if (taskExecutor == null) {
            throw new IllegalStateException("Task executor must be specified using withExecutor(...) before calling after(...)");
        }
        return CompletableFuture
                .supplyAsync(taskSupplier, CompletableFuture.delayedExecutor(delay, timeUnit, taskExecutor))
                .thenCompose(Function.identity());
    }
}
