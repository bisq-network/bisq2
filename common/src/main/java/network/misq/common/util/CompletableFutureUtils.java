package network.misq.common.util;/*
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

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompletableFutureUtils {
    /**
     * @param collection Collection of futures
     * @param <T>        The generic type of the future
     * @return Returns a CompletableFuture with a list of the futures we got as parameter once all futures
     * are completed (incl. exceptionally completed).
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
        return CompletableFuture.allOf(list).thenApply(v ->
                Stream.of(list)
                        .map(CompletableFuture::join)
                        .collect(Collectors.<T>toList())
        );
    }

    public static <T> CompletableFuture<List<T>> anyOf(Collection<CompletableFuture<T>> collection) {
        //noinspection unchecked
        return anyOf(collection.toArray(new CompletableFuture[0]));
    }

    @SafeVarargs
    public static <T> CompletableFuture<T> anyOf(CompletableFuture<T>... list) {
        return CompletableFuture.anyOf(list).thenCompose(res ->
                Stream.of(list)
                        .filter(CompletableFuture::isDone)
                        .findAny()
                        .orElseThrow());
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

    public static CompletableFuture<Void> wait(int delayMs) {
        return wait(delayMs, TimeUnit.MILLISECONDS);
    }

    public static CompletableFuture<Void> wait(int minDelayMs, int maxDelayMs) {
        return wait(minDelayMs, maxDelayMs, TimeUnit.MILLISECONDS);
    }

    public static CompletableFuture<Void> wait(int minDelayMs, int maxDelayMs, TimeUnit timeUnit) {
        return delayRandom(CompletableFuture.completedFuture(null), minDelayMs, maxDelayMs, timeUnit);
    }

    public static CompletableFuture<Void> wait(int delay, TimeUnit timeUnit) {
        return delay(CompletableFuture.completedFuture(null), delay, timeUnit);
    }

    public static <T> CompletableFuture<T> delay(CompletableFuture<T> future, int delayMs) {
        return delay(future, delayMs, TimeUnit.MILLISECONDS);
    }

    public static <T> CompletableFuture<T> delay(CompletableFuture<T> future, int delay, TimeUnit timeUnit) {
        return future.thenApplyAsync(e -> e, CompletableFuture.delayedExecutor(delay, timeUnit));
    }

    public static <T> CompletableFuture<T> delayRandom(CompletableFuture<T> future, int minDelayMs, int maxDelayMs) {
        return delayRandom(future, minDelayMs, maxDelayMs, TimeUnit.MILLISECONDS);
    }

    public static <T> CompletableFuture<T> delayRandom(CompletableFuture<T> future, int minDelay, int maxDelay, TimeUnit timeUnit) {
        return future.thenApplyAsync(e -> e, CompletableFuture.delayedExecutor(minDelay + new Random().nextInt(maxDelay - minDelay), timeUnit));
    }
}