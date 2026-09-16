/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig;

import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.messages.network.handler.maker.MuSigTakeOfferRequestValidator;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MuSigTradeServiceAdmissionTest {
    private final MuSigMakerAdmissionFixtures fixtures = new MuSigMakerAdmissionFixtures();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private MockedStatic<MuSigTakeOfferRequestValidator> validator;
    private MuSigTradeService service;
    private boolean shutDown;

    @BeforeEach
    void setUp() {
        validator = neutralizeEconomicsValidation();
        service = fixtures.createInitializedService();
    }

    @AfterEach
    void tearDown() {
        validator.close();
        worker.shutdownNow();
        if (!shutDown) {
            shutDownService();
        }
    }

    @Test
    void secondSetupRequestForTheSameOfferAndTakerCreatesNoSecondTrade() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A second = fixtures.createRequest(fixtures.offer.getDate() + 1_000);
        assertThat(second.getTradeId()).isNotEqualTo(first.getTradeId());

        service.onMessage(first);
        service.onMessage(second);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(service.findProtocol(first.getTradeId())).isPresent();
        assertThat(service.findProtocol(second.getTradeId())).isEmpty();
    }

    @Test
    void secondSetupRequestIsRejectedAgainstItsOwnTradeIdOnly() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A second = fixtures.createRequest(fixtures.offer.getDate() + 1_000);

        service.onMessage(first);
        service.onMessage(second);

        List<MuSigReportErrorMessage> reports = sentReports();
        assertThat(reports).hasSize(1);
        MuSigReportErrorMessage report = reports.get(0);
        assertThat(report.getTradeId()).isEqualTo(second.getTradeId());
        assertThat(report.getTradeProtocolFailure()).isEqualTo(TradeProtocolFailure.SETUP_ALREADY_PENDING);
        assertThat(report.getReceiver()).isEqualTo(fixtures.takerNetworkId);
        assertThat(report.getSender()).isEqualTo(fixtures.makerNetworkId);
        assertThatCode(() -> report.toProto(false)).doesNotThrowAnyException();
    }

    @Test
    void secondSetupRequestIsRejectedWhileTheMakerWaitsForTheTakersNextMessage() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A second = fixtures.createRequest(fixtures.offer.getDate() + 1_000);

        service.onMessage(first);
        fixtures.executor.drain();
        assertThat(stateOf(first)).isEqualTo(MuSigTradeState.MAKER_INITIALIZED_TRADE_AND_CREATED_NONCE_SHARES);

        service.onMessage(second);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(sentReports()).extracting(MuSigReportErrorMessage::getTradeProtocolFailure)
                .containsExactly(TradeProtocolFailure.SETUP_ALREADY_PENDING);
    }

    @Test
    void rejectedRequestResentAfterThePendingSetupEndedIsAdmitted() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A second = fixtures.createRequest(fixtures.offer.getDate() + 1_000);
        when(fixtures.daemon.initTrade(any())).thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        service.onMessage(first);
        service.onMessage(second);
        assertThat(setupAlreadyPendingReports()).hasSize(1);
        fixtures.executor.drain();
        assertThat(stateOf(first)).isEqualTo(MuSigTradeState.FAILED);

        // Rejections are not remembered: the resend is judged against the current trades.
        service.onMessage(second);

        assertThat(service.getTrades()).hasSize(2);
        assertThat(service.findProtocol(second.getTradeId())).isPresent();
        assertThat(setupAlreadyPendingReports()).hasSize(1);
    }

    @Test
    void anotherTakerCanTakeTheOfferWhileASetupIsPending() throws Exception {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId otherTaker = fixtures.registerOtherTaker(otherKeyPair);
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A other = fixtures.createRequest(fixtures.offer, otherTaker, otherKeyPair, fixtures.offer.getDate());

        service.onMessage(first);
        service.onMessage(other);

        assertThat(service.getTrades()).hasSize(2);
        assertThat(sentReports()).isEmpty();
    }

    @Test
    void takerCanTakeAnotherOfferWhileASetupIsPending() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A otherOffer = fixtures.createRequest(fixtures.otherOffer, fixtures.otherOffer.getDate());

        service.onMessage(first);
        service.onMessage(otherOffer);

        assertThat(service.getTrades()).hasSize(2);
        assertThat(sentReports()).isEmpty();
    }

    @Test
    void resentAcceptedRequestIsIgnoredWithoutAReport() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());

        service.onMessage(first);
        service.onMessage(first);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(sentReports()).isEmpty();
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(1);
    }

    @Test
    void tamperedCopyOfAnAcceptedRequestIsDroppedWithoutAReport() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A tampered = tamperedCopyOf(first);

        service.onMessage(first);
        service.onMessage(tampered);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(sentReports()).isEmpty();
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(1);
    }

    @Test
    void copyFailingValidationWhileTheRequestIsAdmittedIsDroppedWithoutAReport() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A tampered = tamperedCopyOf(first);
        CountDownLatch validating = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> copy = worker.submit(() -> {
            try (MockedStatic<MuSigTakeOfferRequestValidator> ignored = neutralizeEconomicsValidation()) {
                // The copy is unknown when it enters validation; the original is admitted meanwhile.
                ignored.when(() -> MuSigTakeOfferRequestValidator.validateIdentity(any(), any()))
                        .thenAnswer(invocation -> {
                            validating.countDown();
                            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                            throw new TradeProtocolException("stale copy", TradeProtocolFailure.OFFER_NOT_AVAILABLE);
                        });
                service.onMessage(tampered);
            }
        });

        try {
            assertThat(validating.await(5, TimeUnit.SECONDS)).isTrue();
            service.onMessage(first);
        } finally {
            release.countDown();
        }
        copy.get(5, TimeUnit.SECONDS);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(service.findProtocol(first.getTradeId())).isPresent();
        assertThat(sentReports()).isEmpty();
    }

    @Test
    void takerCanTakeTheOfferAgainAfterTheFirstSetupFailed() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A second = fixtures.createRequest(fixtures.offer.getDate() + 1_000);
        when(fixtures.daemon.initTrade(any())).thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

        service.onMessage(first);
        fixtures.executor.drain();
        assertThat(stateOf(first)).isEqualTo(MuSigTradeState.FAILED);
        assertThat(service.findTrade(first.getTradeId()).orElseThrow().getErrorMessage()).contains("UNAVAILABLE");

        service.onMessage(second);

        assertThat(service.getTrades()).hasSize(2);
        assertThat(sentReports()).extracting(MuSigReportErrorMessage::getTradeProtocolFailure)
                .doesNotContain(TradeProtocolFailure.SETUP_ALREADY_PENDING);
    }

    @Test
    void requestWhoseHandlingCannotBeScheduledIsRolledBack() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A retry = fixtures.createRequest(fixtures.offer.getDate() + 1_000);

        fixtures.executor.rejectSubmissions(true);
        service.onMessage(first);
        assertThat(service.getTrades()).isEmpty();
        assertThat(service.findProtocol(first.getTradeId())).isEmpty();
        assertThat(sentReports()).isEmpty();
        // The creation write plus the rollback write; the rate limiter would have dropped a
        // second ordinary write this close to the first. The snapshot written last must not
        // resurrect the trade on restart.
        assertThat(fixtures.persistedSnapshots).hasSize(2);
        assertThat(fixtures.persistedSnapshots.get(0).tradeExists(first.getTradeId())).isTrue();
        assertThat(fixtures.persistedSnapshots.get(1).tradeExists(first.getTradeId())).isFalse();

        fixtures.executor.rejectSubmissions(false);
        service.onMessage(retry);
        assertThat(service.getTrades()).hasSize(1);
        assertThat(service.findProtocol(retry.getTradeId())).isPresent();
    }

    @Test
    void rollbackWritesAreCoalescedWhileOneIsInFlight() throws Exception {
        List<CompletableFuture<Void>> writes = new CopyOnWriteArrayList<>();
        when(fixtures.persistence.persistAsync(any())).thenAnswer(invocation -> {
            fixtures.persistedSnapshots.add(invocation.getArgument(0));
            CompletableFuture<Void> write = new CompletableFuture<>();
            writes.add(write);
            return write;
        });
        fixtures.executor.rejectSubmissions(true);
        int creationWrites = 1;

        for (int i = 0; i < 3; i++) {
            service.onMessage(fixtures.createRequest(fixtures.offer.getDate() + i * 1_000));
        }
        // The first admission's ordinary write plus one rollback write; the later rollbacks wait.
        assertThat(writes).hasSize(creationWrites + 1);
        writes.get(creationWrites).complete(null);
        // Exactly one more write covers all rollbacks that happened meanwhile.
        assertThat(writes).hasSize(creationWrites + 2);
        writes.get(creationWrites + 1).complete(null);
        assertThat(writes).hasSize(creationWrites + 2);

        assertThat(service.getTrades()).isEmpty();
        assertThat(fixtures.persistedSnapshots.get(fixtures.persistedSnapshots.size() - 1).getTrades()).isEmpty();
    }

    @Test
    void queuedSetupIsKeptAtShutdown() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());

        service.onMessage(first);
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(1);

        shutDownService();

        // Left for the startup reconciliation; nothing was sent to the daemon for it.
        assertThat(service.getTrades()).hasSize(1);
        assertThat(stateOf(first)).isEqualTo(MuSigTradeState.INIT);
    }

    @Test
    void shutdownWaitsForAnAdmittedRequestToBeDispatched() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        CountDownLatch submissionGate = new CountDownLatch(1);
        fixtures.executor.holdSubmissions(submissionGate);
        Future<?> admission = worker.submit(() -> {
            try (MockedStatic<MuSigTakeOfferRequestValidator> ignored = neutralizeEconomicsValidation()) {
                service.onMessage(first);
            }
        });
        ExecutorService shutdownRunner = Executors.newSingleThreadExecutor();
        try {
            awaitTradeCreated(first);
            CompletableFuture<Boolean> shutdown = CompletableFuture.supplyAsync(this::shutDownServiceAndReport, shutdownRunner);
            // The request is created and its submission is in flight: shutdown must not get past
            // closing admission, or it would take the executor away underneath the dispatch.
            assertThat(shutdown.isDone()).isFalse();
            Thread.sleep(200);
            assertThat(shutdown.isDone()).isFalse();
            submissionGate.countDown();
            admission.get(5, TimeUnit.SECONDS);
            assertThat(shutdown.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            submissionGate.countDown();
            shutdownRunner.shutdownNow();
        }

        assertThat(service.getTrades()).hasSize(1);
        assertThat(stateOf(first)).isEqualTo(MuSigTradeState.INIT);
    }

    @Test
    void requestArrivingAfterShutdownBeganIsDropped() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());

        shutDownService();
        assertThatCode(() -> service.onMessage(first)).doesNotThrowAnyException();

        assertThat(service.getTrades()).isEmpty();
        assertThat(sentReports()).isEmpty();
    }

    @Test
    void concurrentSecondSetupRequestWaitsForTheFirstAndIsRejected() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A second = fixtures.createRequest(fixtures.offer.getDate() + 1_000);
        CountDownLatch secondStarted = new CountDownLatch(1);
        Thread[] secondThread = new Thread[1];
        Future<?> secondRequest;

        synchronized (service.tradeCreationLock) {
            secondRequest = worker.submit(() -> {
                // Static mocks are thread-local, so the request thread registers its own.
                try (MockedStatic<MuSigTakeOfferRequestValidator> ignored = neutralizeEconomicsValidation()) {
                    secondThread[0] = Thread.currentThread();
                    secondStarted.countDown();
                    service.onMessage(second);
                }
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            awaitBlocked(secondThread[0]);
            // The first request enters through the reentrant lock the test holds.
            service.onMessage(first);
            assertThat(service.getTrades()).hasSize(1);
        }
        secondRequest.get(5, TimeUnit.SECONDS);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(service.findProtocol(first.getTradeId())).isPresent();
        List<MuSigReportErrorMessage> reports = sentReports();
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getTradeId()).isEqualTo(second.getTradeId());
        assertThat(reports.get(0).getTradeProtocolFailure()).isEqualTo(TradeProtocolFailure.SETUP_ALREADY_PENDING);
    }

    @Test
    void takerSideTradeCreationWaitsForTheCreationLock() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Thread[] creationThread = new Thread[1];
        Future<?> takerCreation;

        synchronized (service.tradeCreationLock) {
            takerCreation = worker.submit(() -> {
                creationThread[0] = Thread.currentThread();
                started.countDown();
                service.takerCreatesProtocol(fixtures.makerIdentity,
                        fixtures.otherOffer,
                        Coin.asBtcFromValue(111L),
                        Fiat.fromValue(222L, "USD"),
                        fixtures.paymentMethodSpec,
                        fixtures.makerAccountPayload,
                        Optional.empty(),
                        Optional.empty(),
                        fixtures.otherOffer.getPriceSpec(),
                        0);
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            awaitBlocked(creationThread[0]);
            assertThat(service.getTrades()).isEmpty();
        }
        takerCreation.get(5, TimeUnit.SECONDS);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(service.getTrades().iterator().next().isTaker()).isTrue();
    }

    private void shutDownService() {
        shutDownServiceAndReport();
    }

    private boolean shutDownServiceAndReport() {
        shutDown = true;
        return service.shutdown().join();
    }

    private void awaitTradeCreated(SetupTradeMessage_A request) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (service.findTrade(request.getTradeId()).isEmpty()) {
            assertThat(System.currentTimeMillis()).isLessThan(deadline);
            Thread.sleep(5);
        }
    }

    private SetupTradeMessage_A tamperedCopyOf(SetupTradeMessage_A accepted) throws Exception {
        SetupTradeMessage_A other = fixtures.createRequest(accepted.getContract().getTakeOfferDate() + 1_000);
        // The accepted trade id carrying another request's contract and signature fails identity
        // validation; reporting that failure would fail the taker's accepted attempt.
        return fixtures.createRequest(other.getContract(), other.getContractSignatureData(), accepted.getTradeId());
    }

    private MuSigTradeState stateOf(SetupTradeMessage_A request) {
        return service.findTrade(request.getTradeId()).orElseThrow().getTradeState();
    }

    private List<MuSigReportErrorMessage> setupAlreadyPendingReports() {
        return sentReports().stream()
                .filter(report -> report.getTradeProtocolFailure() == TradeProtocolFailure.SETUP_ALREADY_PENDING)
                .toList();
    }

    private List<MuSigReportErrorMessage> sentReports() {
        ArgumentCaptor<MuSigReportErrorMessage> captor = ArgumentCaptor.forClass(MuSigReportErrorMessage.class);
        verify(fixtures.serviceProvider.getNetworkService(), atLeast(0)).confidentialSend(captor.capture(), any(), any());
        return captor.getAllValues();
    }

    private static MockedStatic<MuSigTakeOfferRequestValidator> neutralizeEconomicsValidation() {
        // The economics checks have their own validator tests and need live price/dispute-agent
        // data; the admission tests exercise identity, offer and creation for real.
        MockedStatic<MuSigTakeOfferRequestValidator> validator =
                mockStatic(MuSigTakeOfferRequestValidator.class, CALLS_REAL_METHODS);
        validator.when(() -> MuSigTakeOfferRequestValidator.validateEconomics(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> null);
        return validator;
    }

    private static void awaitBlocked(Thread thread) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (thread.getState() != Thread.State.BLOCKED) {
            assertThat(thread.isAlive()).as("thread ended before reaching the lock").isTrue();
            assertThat(System.currentTimeMillis()).isLessThan(deadline);
            Thread.sleep(5);
        }
    }
}
