package bisq.common.util;/*
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CompletableFutureUtils {
    /**
     * @param collection Collection of futures
     * @param <T>        The generic type of the future
     * @return Returns a CompletableFuture with a list of the results once all futures
     * have completed successfully.
     * If any future got canceled or completed exceptionally the result also completes exceptionally.
     * The difference to the `CompletableFuture.allOf` method is that we expect that all futures have the same type,
     * and we return a list of all results. Order of result list is same as order of the futures passed (not completion order).
     */
    public static <T> CompletableFuture<List<T>> allOf(Collection<CompletableFuture<T>> collection) {
        //noinspection unchecked
        return allOf(collection.toArray(new CompletableFuture[0]));
    }

    public static <T> CompletableFuture<List<T>> allOf(Stream<CompletableFuture<T>> stream) {
        return allOf(stream.collect(Collectors.toList()));
    }

    @SafeVarargs
    public static <T> CompletableFuture<List<T>> allOf(CompletableFuture<T>... list) {
        return CompletableFuture.allOf(list)
                .thenApply(nil ->
                        // We want to return the results in list, not the futures. Once allOf call is complete
                        // we know that all futures have successfully completed, thus the join call does not block.
                        Stream.of(list)
                                .map(CompletableFuture::join)
                                .collect(Collectors.<T>toList())
                );
    }

    public static <T> CompletableFuture<List<T>> failureTolerantAllOf(Collection<CompletableFuture<T>> collection) {
        //noinspection unchecked
        return failureTolerantAllOf(collection.toArray(new CompletableFuture[0]));
    }

    public static <T> CompletableFuture<List<T>> failureTolerantAllOf(Stream<CompletableFuture<T>> stream) {
        return failureTolerantAllOf(stream.collect(Collectors.toList()));
    }

    /**
     * Variation of allOf which does not fail if one future fails
     */
    @SafeVarargs
    public static <T> CompletableFuture<List<T>> failureTolerantAllOf(CompletableFuture<T>... list) {
        List<CompletableFuture<T>> nonFailing = Stream.of(list)
                .map(future -> future.handle((result, throwable) -> throwable == null ? result : null))
                .toList();
        return allOf(nonFailing)
                .thenApply(v -> nonFailing.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                );
    }

    /**
     * @param collection Collection of futures
     * @param <T>        The generic type of the future
     * @return Returns a CompletableFuture with the result once any future has completed successfully.
     * If all futures completed exceptionally or got cancelled the result also completes exceptionally.
     * This is different to the `CompletableFuture.anyOf` behaviour which completes exceptionally if any of the futures
     * complete exceptionally or got cancelled.
     */
    public static <T> CompletableFuture<T> anyOf(Collection<CompletableFuture<T>> collection) {
        //noinspection unchecked
        return anyOf(collection.toArray(new CompletableFuture[0]));
    }

    public static <T> CompletableFuture<T> anyOf(Stream<CompletableFuture<T>> stream) {
        return anyOf(stream.collect(Collectors.toList()));
    }

    @SafeVarargs
    public static <T> CompletableFuture<T> anyOf(CompletableFuture<T>... list) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        AtomicInteger remaining = new AtomicInteger(list.length);
        Stream.of(list).forEach(future -> future.whenComplete((result, throwable) -> {
            if (!resultFuture.isDone()) {
                if (throwable == null) {
                    resultFuture.complete(result);
                } else {
                    if (remaining.decrementAndGet() == 0) {
                        resultFuture.completeExceptionally(throwable);
                    }
                }
            }
        }));
        return resultFuture;
    }

    public static <T> CompletableFuture<T> logOnFailure(CompletableFuture<T> future) {
        return logOnFailure(future, null);
    }

    public static <T> CompletableFuture<T> logOnFailure(CompletableFuture<T> future, @Nullable String errorMessage) {
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                if (errorMessage == null) {
                    log.error("Executing future failed", throwable);
                } else {
                    log.error(errorMessage, throwable);
                }
            }
        });

        return future;
    }

    public static <T> CompletableFuture<T> toCompletableFuture(ListenableFuture<T> future) {
        var completableFuture = new CompletableFuture<T>();
        // Propagate cancellation from returned future to source future.
        completableFuture.whenComplete((result, throwable) -> {
            if (completableFuture.isCancelled()) {
                future.cancel(true);
            }
        });
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof java.util.concurrent.CancellationException) {
                    completableFuture.cancel(false);
                } else {
                    completableFuture.completeExceptionally(t);
                }
            }
        }, MoreExecutors.directExecutor());
        return completableFuture;
    }
}