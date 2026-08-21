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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AndroidJdkFacadeTest {
    private final AndroidJdkFacade facade = new AndroidJdkFacade(0);

    // The fallback must mirror CompletableFuture.exceptionallyCompose, which is not available
    // on Android below API 34.

    @Test
    void exceptionallyCompose_successfulFuture_doesNotInvokeHandler() {
        AtomicBoolean handlerInvoked = new AtomicBoolean(false);
        CompletableFuture<String> result = facade.exceptionallyCompose(
                CompletableFuture.completedFuture("value"),
                throwable -> {
                    handlerInvoked.set(true);
                    return CompletableFuture.completedFuture("recovered");
                });

        assertThat(result.join()).isEqualTo("value");
        assertThat(handlerInvoked.get()).isFalse();
    }

    @Test
    void exceptionallyCompose_failedFuture_composesRecovery() {
        CompletableFuture<String> result = facade.exceptionallyCompose(
                CompletableFuture.failedFuture(new IOException("boom")),
                throwable -> CompletableFuture.completedFuture("recovered"));

        assertThat(result.join()).isEqualTo("recovered");
    }

    @Test
    void exceptionallyCompose_handlerReceivesWrappedExceptionLikeJdkImplementation() throws ExecutionException, InterruptedException {
        IOException cause = new IOException("boom");
        CompletableFuture<Throwable> observed = new CompletableFuture<>();

        facade.exceptionallyCompose(
                CompletableFuture.<String>supplyAsync(() -> {
                    throw new CompletionException(cause);
                }),
                throwable -> {
                    observed.complete(throwable);
                    return CompletableFuture.completedFuture("recovered");
                }).join();

        assertThat(observed.get()).isInstanceOf(CompletionException.class).hasCause(cause);
    }

    @Test
    void exceptionallyCompose_cancelledResult_suppressesRecoveryHandler() {
        // Native exceptionallyCompose does not invoke the handler once its dependent future is
        // cancelled; the fallback must match, or a cancelled HttpRequestService request would keep
        // walking the provider failover chain.
        CompletableFuture<String> source = new CompletableFuture<>();
        AtomicBoolean handlerInvoked = new AtomicBoolean(false);
        CompletableFuture<String> result = facade.exceptionallyCompose(source, throwable -> {
            handlerInvoked.set(true);
            return CompletableFuture.completedFuture("recovered");
        });

        result.cancel(true);
        source.completeExceptionally(new IOException("boom"));

        assertThat(handlerInvoked.get()).isFalse();
        assertThat(result.isCancelled()).isTrue();
    }

    @Test
    void exceptionallyCompose_cancelledResult_staysCancelledOnSourceSuccess() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> result =
                facade.exceptionallyCompose(source, throwable -> CompletableFuture.completedFuture("recovered"));

        result.cancel(true);
        source.complete("value");

        assertThat(result.isCancelled()).isTrue();
    }

    @Test
    void exceptionallyCompose_handlerReturningFailedFuture_propagatesFailure() {
        IllegalStateException replacement = new IllegalStateException("replaced");
        CompletableFuture<String> result = facade.exceptionallyCompose(
                CompletableFuture.failedFuture(new IOException("boom")),
                throwable -> CompletableFuture.failedFuture(replacement));

        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .hasCause(replacement);
    }

    // The native relay wraps a recovery failure in CompletionException before completing the
    // dependent, so observers (whenComplete/handle) see the wrapper, not the raw cause, and a
    // cancelled recovery stage surfaces as a failure rather than cancelling the result.

    @Test
    void exceptionallyCompose_failedRecovery_observersSeeCompletionExceptionLikeJdk() throws ExecutionException, InterruptedException {
        IllegalStateException replacement = new IllegalStateException("replaced");
        CompletableFuture<String> result = facade.exceptionallyCompose(
                CompletableFuture.failedFuture(new IOException("boom")),
                throwable -> CompletableFuture.failedFuture(replacement));

        CompletableFuture<Throwable> observed = new CompletableFuture<>();
        result.whenComplete((value, throwable) -> observed.complete(throwable));

        assertThat(observed.get()).isInstanceOf(CompletionException.class).hasCause(replacement);
    }

    @Test
    void exceptionallyCompose_cancelledRecovery_failsResultWithoutCancellingItLikeJdk() throws ExecutionException, InterruptedException {
        CompletableFuture<String> recovery = new CompletableFuture<>();
        CompletableFuture<String> result = facade.exceptionallyCompose(
                CompletableFuture.failedFuture(new IOException("boom")),
                throwable -> recovery);

        recovery.cancel(true);

        CompletableFuture<Throwable> observed = new CompletableFuture<>();
        result.whenComplete((value, throwable) -> observed.complete(throwable));

        assertThat(result.isCancelled()).isFalse();
        assertThat(observed.get())
                .isInstanceOf(CompletionException.class)
                .cause()
                .isInstanceOf(CancellationException.class);
    }

    @Test
    void exceptionallyCompose_throwingHandler_observersSeeCompletionExceptionLikeJdk() throws ExecutionException, InterruptedException {
        RuntimeException handlerFailure = new RuntimeException("handler blew up");
        CompletableFuture<String> result = facade.exceptionallyCompose(
                CompletableFuture.failedFuture(new IOException("boom")),
                throwable -> {
                    throw handlerFailure;
                });

        CompletableFuture<Throwable> observed = new CompletableFuture<>();
        result.whenComplete((value, throwable) -> observed.complete(throwable));

        assertThat(observed.get()).isInstanceOf(CompletionException.class).hasCause(handlerFailure);
    }

    @Test
    void exceptionallyCompose_recoveryFailingWithCompletionException_isNotDoubleWrappedLikeJdk() throws ExecutionException, InterruptedException {
        RuntimeException cause = new RuntimeException("pre-wrapped");
        CompletableFuture<String> result = facade.exceptionallyCompose(
                CompletableFuture.failedFuture(new IOException("boom")),
                throwable -> CompletableFuture.failedFuture(new CompletionException(cause)));

        CompletableFuture<Throwable> observed = new CompletableFuture<>();
        result.whenComplete((value, throwable) -> observed.complete(throwable));

        assertThat(observed.get()).isInstanceOf(CompletionException.class).hasCause(cause);
    }
}
