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

package bisq.common.facades;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Facade for JDK operations that differ between Java SE and Android.
 * <p>
 * All string I/O operations use UTF-8 encoding exclusively for consistency
 * and to prevent data corruption from encoding mismatches.
 * <p>
 * File operations apply restricted permissions (owner-only read/write) on POSIX systems
 * to protect sensitive data like private keys.
 */
public interface JdkFacade {
    String getMyPid();

    Stream<String> getProcessCommandStream();

    void redirectError(ProcessBuilder processBuilder);

    void redirectOutput(ProcessBuilder processBuilder);

    // The JDK 21 SequencedCollection methods List.removeFirst()/addFirst()/getFirst() only exist on
    // Android 15+ (API 35) and crash older devices with NoSuchMethodError.

    <T> T removeFirst(List<T> list);

    <T> void addFirst(List<T> list, T element);

    <T> T getFirst(List<T> list);

    // CompletableFuture.exceptionallyCompose is JDK 12+; on Android it only exists from API 34
    // and crashes older devices with NoSuchMethodError.

    <T> CompletableFuture<T> exceptionallyCompose(CompletableFuture<T> future,
                                                  Function<Throwable, ? extends CompletionStage<T>> fn);
}
