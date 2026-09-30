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

package bisq.common.facades.android;

import bisq.common.facades.JdkFacade;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;

public class AndroidJdkFacade implements JdkFacade {
    private final String myPid;

    public AndroidJdkFacade(int myPid) {
        this.myPid = String.valueOf(myPid);
    }

    @Override
    public String getMyPid() {
        return myPid;
    }

    @Override
    public Stream<String> getProcessCommandStream() {
        // TODO
        throw new UnsupportedOperationException("Not supported yet.");
        // return ProcessHandle.allProcesses().map(processHandle -> processHandle.info().commandLine().orElse(""));
    }

    @Override
    public void redirectError(ProcessBuilder processBuilder) {
        // ProcessBuilder.Redirect.DISCARD not supported on Android
        processBuilder.redirectError(new File("/dev/null"));
    }

    @Override
    public void redirectOutput(ProcessBuilder processBuilder) {
        // ProcessBuilder.Redirect.DISCARD not supported on Android
        processBuilder.redirectError(new File("/dev/null"));
    }

    @Override
    public <T> T removeFirst(List<T> list) {
        // List.removeFirst/addFirst crash Android below API 35
        return list.remove(0);
    }

    @Override
    public <T> void addFirst(List<T> list, T element) {
        // List.removeFirst/addFirst crash Android below API 35
        list.add(0, element);
    }

    @Override
    public <T> T getFirst(List<T> list) {
        return list.get(0);
    }

    @Override
    public <T> CompletableFuture<T> exceptionallyCompose(CompletableFuture<T> future,
                                                         Function<Throwable, ? extends CompletionStage<T>> fn) {
        // CompletableFuture.exceptionallyCompose crashes Android below API 34.
        // An explicit result future rather than handle().thenCompose(): cancelling the returned
        // future must suppress the recovery function, as the native method does. With the chained
        // form only the last stage gets cancelled, so fn would still run when the source fails
        // later — in HttpRequestService that would continue the provider failover of a cancelled
        // request. Like the native method, cancelling the result does not cancel the source, and a
        // recovery already in flight is not interrupted.
        CompletableFuture<T> result = new CompletableFuture<>();
        future.whenComplete((value, throwable) -> {
            if (result.isDone()) {
                return;
            }
            if (throwable == null) {
                result.complete(value);
            } else {
                try {
                    fn.apply(throwable).whenComplete((recovered, recoveryThrowable) -> {
                        if (recoveryThrowable == null) {
                            result.complete(recovered);
                        } else {
                            // Wrapped like the native relay: observers see CompletionException, and a
                            // cancelled recovery stage does not report the result as cancelled.
                            result.completeExceptionally(asCompletionException(recoveryThrowable));
                        }
                    });
                } catch (Throwable t) {
                    result.completeExceptionally(asCompletionException(t));
                }
            }
        });
        return result;
    }

    private static CompletionException asCompletionException(Throwable throwable) {
        return throwable instanceof CompletionException completionException
                ? completionException
                : new CompletionException(throwable);
    }
}
