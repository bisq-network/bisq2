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

import java.util.Arrays;
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
     * have completed successfully. If any future got canceled or completed exceptionally the result also
     * completes exceptionally.
     * This is different to the `CompletableFuture.allOf` behaviour which completes successfully also if any of the futures
     * complete exceptionally.
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
        CompletableFuture<List<T>> result = CompletableFuture.allOf(list).thenApply(v ->
                Stream.of(list)
                        .map(CompletableFuture::join)
                        .collect(Collectors.<T>toList())
        );

        Arrays.stream(list)
                .filter(future -> !CompletableFutureUtils.isCompleted(future))
                .findAny()
                .map(f -> f.handle((__, throwable) -> throwable).join())
                .ifPresent(result::completeExceptionally);

        return result;
    }

    /**
     * @param collection Collection of futures
     * @param <T>        The generic type of the future
     * @return Returns a CompletableFuture with the result once any future has completed successfully.
     * If all futures completed exceptionally the result also completes exceptionally.
     * This is different to the `CompletableFuture.anyOf` behaviour which completes successfully also if any of the futures
     * complete exceptionally.
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
        Stream.of(list).forEach(future -> {
            future.whenComplete((result, throwable) -> {
                if (!resultFuture.isDone()) {
                    if (throwable == null) {
                        resultFuture.complete(result);
                    } else {
                        if (remaining.decrementAndGet() == 0) {
                            resultFuture.completeExceptionally(throwable);
                        }
                    }
                }
            });
        });
        return resultFuture;
    }

    // Strangely the CompletableFuture API do not offer that method
    public static <T> boolean isCompleted(CompletableFuture<T> future) {
        return future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled();
    }

    // CompletableFuture.applyToEither has some undesired error handling behavior (if first fail result fails).
    // This method provides the expected behaviour that if one of the 2 futures completes we complete our
    // result future. If both fail the result fail as well.
    // Borrowed from https://4comprehension.com/be-careful-with-completablefuture-applytoeither/
    public static <T> CompletableFuture<T> either(CompletableFuture<T> f1, CompletableFuture<T> f2) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFuture.allOf(f1, f2).whenComplete((__, throwable) -> {
            if (f1.isCompletedExceptionally() && f2.isCompletedExceptionally()) {
                result.completeExceptionally(throwable);
            }
        });

        f1.thenAccept(result::complete);
        f2.thenAccept(result::complete);
        return result;
    }
}