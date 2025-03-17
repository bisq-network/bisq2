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

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
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

    public static <T> CompletableFuture<T> anyOf(Stream<CompletableFuture<T>> collection) {
        return anyOf(collection.collect(Collectors.toList()));
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

    /*public static <T> boolean isCompleted(CompletableFuture<T> future) {
        return future.state() == Future.State.SUCCESS;
    }*/
}