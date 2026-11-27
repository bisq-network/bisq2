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
import bisq.identity.Identity;
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

import java.io.IOException;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MuSigTradeServiceAdmissionTest {
    // Registering a protocol queues the drain of buffered dispute and settlement messages, and
    // the admitted request queues its handler: two tasks per admitted setup, none for a copy.
    private static final int TASKS_PER_ADMITTED_SETUP = 2;

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
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(TASKS_PER_ADMITTED_SETUP);
        // The second delivery is dropped before it is verified.
        validator.verify(() -> MuSigTakeOfferRequestValidator.validateIdentity(any(), any()), times(1));
    }

    @Test
    void tamperedCopyOfAnAcceptedRequestIsDroppedWithoutAReport() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A tampered = tamperedCopyOf(first);

        service.onMessage(first);
        service.onMessage(tampered);

        assertThat(service.getTrades()).hasSize(1);
        assertThat(sentReports()).isEmpty();
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(TASKS_PER_ADMITTED_SETUP);
    }

    @Test
    void requestWithTheIdOfAnAcceptedTradeFromAnotherSenderIsAnsweredLikeAnUnknownId() throws Exception {
        KeyPair otherKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId otherSender = fixtures.registerOtherTaker(otherKeyPair);
        SetupTradeMessage_A accepted = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A own = fixtures.createRequest(fixtures.offer, otherSender, otherKeyPair, fixtures.offer.getDate());
        String unknownId = fixtures.createRequest(fixtures.offer, otherSender, otherKeyPair, fixtures.offer.getDate() + 1_000).getTradeId();
        SetupTradeMessage_A withKnownId = fixtures.createRequest(own.getContract(), own.getContractSignatureData(), accepted.getTradeId());
        SetupTradeMessage_A withUnknownId = fixtures.createRequest(own.getContract(), own.getContractSignatureData(), unknownId);

        service.onMessage(accepted);
        service.onMessage(withKnownId);
        service.onMessage(withUnknownId);

        // The answer to a sender who is not the peer of a trade must not show whether the maker
        // holds a trade with the ID. Its report cannot fail the accepted trade at the taker.
        List<MuSigReportErrorMessage> reports = sentReports();
        assertThat(reports).extracting(MuSigReportErrorMessage::getTradeId)
                .containsExactly(accepted.getTradeId(), unknownId);
        assertThat(reports).extracting(MuSigReportErrorMessage::getReceiver).containsOnly(otherSender);
        assertThat(reports.get(0).getTradeProtocolFailure()).isEqualTo(reports.get(1).getTradeProtocolFailure());
        assertThat(reports.get(0).getErrorMessage()).isEqualTo(reports.get(1).getErrorMessage());
        assertThat(service.getTrades()).hasSize(1);
        assertThat(stateOf(accepted)).isEqualTo(MuSigTradeState.INIT);
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(TASKS_PER_ADMITTED_SETUP);
    }

    @Test
    void requestOfThePeerForAnAcceptedTradeToAnotherIdentityIsVerifiedAndReported() throws Exception {
        Identity otherIdentity = fixtures.registerOtherIdentityOfTheNode(KeyGeneration.generateDefaultEcKeyPair());
        SetupTradeMessage_A accepted = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A toOtherIdentity = fixtures.createRequest(accepted.getContract(),
                accepted.getContractSignatureData(), accepted.getTradeId(), otherIdentity.getNetworkId());

        service.onMessage(accepted);
        service.onMessage(toOtherIdentity);

        // A silent drop would tell the peer that the other identity belongs to the node which
        // holds its trade. The report comes from the other identity, so the node of the peer does
        // not take it as a message of its trade peer.
        List<MuSigReportErrorMessage> reports = sentReports();
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getTradeId()).isEqualTo(accepted.getTradeId());
        assertThat(reports.get(0).getSender()).isEqualTo(otherIdentity.getNetworkId());
        assertThat(reports.get(0).getReceiver()).isEqualTo(fixtures.takerNetworkId);
        assertThat(service.getTrades()).hasSize(1);
        assertThat(stateOf(accepted)).isEqualTo(MuSigTradeState.INIT);
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
        assertThat(service.findOpenTrade(first.getTradeId()).orElseThrow().getErrorMessage()).contains("UNAVAILABLE");

        service.onMessage(second);

        assertThat(service.getTrades()).hasSize(2);
        assertThat(sentReports()).extracting(MuSigReportErrorMessage::getTradeProtocolFailure)
                .doesNotContain(TradeProtocolFailure.SETUP_ALREADY_PENDING);
    }

    @Test
    void requestWhoseHandlingCannotBeScheduledIsRolledBack() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        SetupTradeMessage_A retry = fixtures.createRequest(fixtures.offer.getDate() + 1_000);

        fixtures.executor.rejectSubmissionsFrom(1);
        service.onMessage(first);
        assertThat(service.getTrades()).isEmpty();
        assertThat(service.findProtocol(first.getTradeId())).isEmpty();
        assertThat(sentReports()).isEmpty();
        // The creation write plus the rollback write; the rate limiter would have dropped a
        // second ordinary write this close to the first. The snapshot written last must not
        // resurrect the trade on restart.
        assertThat(fixtures.persistedSnapshots).hasSize(2);
        assertThat(fixtures.persistedSnapshots.get(0).findOpenTrade(first.getTradeId())).isPresent();
        assertThat(fixtures.persistedSnapshots.get(1).findOpenTrade(first.getTradeId())).isEmpty();

        fixtures.executor.acceptSubmissions();
        service.onMessage(retry);
        assertThat(service.getTrades()).hasSize(1);
        assertThat(service.findProtocol(retry.getTradeId())).isPresent();
    }

    @Test
    void requestWhoseDispatchIsRejectedAfterTheRegistrationIsRolledBack() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());

        // The registration of the protocol submits first and is accepted; the dispatch of the
        // request is the second submission.
        fixtures.executor.rejectSubmissionsFrom(2);
        service.onMessage(first);

        assertThat(service.getTrades()).isEmpty();
        assertThat(service.findProtocol(first.getTradeId())).isEmpty();
        assertThat(sentReports()).isEmpty();
        assertThat(fixtures.persistedSnapshots).hasSize(2);
        assertThat(fixtures.persistedSnapshots.get(1).findOpenTrade(first.getTradeId())).isEmpty();

        // The task of the registration is still queued. It runs for a protocol which is not
        // registered any more and must not bring the trade back or reach the daemon.
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(1);
        fixtures.executor.drain();
        assertThat(service.getTrades()).isEmpty();
        verify(fixtures.daemon, never()).initTrade(any());
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
        fixtures.executor.rejectSubmissionsFrom(1);
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
    void failedRollbackWriteLeavesTheStateUnsavedForTheStore() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        List<MuSigTradeStore> snapshots = new CopyOnWriteArrayList<>();
        List<CompletableFuture<Void>> writes = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> {
            snapshots.add(invocation.getArgument(0));
            CompletableFuture<Void> write = new CompletableFuture<>();
            writes.add(write);
            return write;
        }).when(fixtures.persistence).persistAsync(any());
        fixtures.executor.rejectSubmissionsFrom(1);

        service.onMessage(first);
        // The creation write, which stays in progress, and the rollback write.
        assertThat(writes).hasSize(2);
        assertThat(service.isDropped()).isFalse();
        writes.get(1).completeExceptionally(new IOException("No space left on device"));

        // The store keeps the state without the trade as unsaved, so its next write or its write
        // at shutdown repeats the failed write.
        assertThat(service.isDropped()).isTrue();

        // The failed write does not hold back the write of a later rollback.
        service.onMessage(fixtures.createRequest(fixtures.offer.getDate() + 1_000));
        assertThat(writes).hasSize(3);
        assertThat(snapshots.get(2).getTrades()).isEmpty();
    }

    @Test
    void ordinaryStoreWriteTakesItsCopyAndSubmitsItUnderTheCreationLock() throws Exception {
        SetupTradeMessage_A request = fixtures.createRequest(fixtures.offer.getDate());
        MuSigTrade addedDuringTheStep = new MuSigTrade(request.getContract(), false, false, fixtures.makerIdentity,
                fixtures.offer, fixtures.takerNetworkId, fixtures.makerNetworkId);
        CountDownLatch started = new CountDownLatch(1);
        Thread[] writerThread = new Thread[1];
        Future<Boolean> write;

        synchronized (service.getTradeCreationLock()) {
            write = worker.submit(() -> {
                writerThread[0] = Thread.currentThread();
                started.countDown();
                return service.persist().join();
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            // A copy taken during a creation step could be submitted after the rollback write of
            // that step and bring the removed trade back. So a write waits for the step before
            // it takes its copy.
            awaitBlocked(writerThread[0]);
            assertThat(fixtures.persistedSnapshots).isEmpty();
            service.getPersistableStore().addTrade(addedDuringTheStep);
        }

        assertThat(write.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(fixtures.persistedSnapshots).hasSize(1);
        assertThat(fixtures.persistedSnapshots.get(0).findOpenTrade(addedDuringTheStep.getId())).isPresent();
    }

    @Test
    void queuedSetupIsKeptAtShutdown() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());

        service.onMessage(first);
        assertThat(fixtures.executor.pendingTasks()).isEqualTo(TASKS_PER_ADMITTED_SETUP);

        shutDownService();

        // The trade stays in its first state (see the limitation in
        // docs/specifications/trade/mu-sig-pending-setups.md); nothing was sent to the daemon.
        assertThat(service.getTrades()).hasSize(1);
        assertThat(stateOf(first)).isEqualTo(MuSigTradeState.INIT);
    }

    @Test
    void shutdownWaitsForAnAdmittedRequestToBeDispatched() throws Exception {
        SetupTradeMessage_A first = fixtures.createRequest(fixtures.offer.getDate());
        CountDownLatch submissionGate = new CountDownLatch(1);
        // The registration of the protocol submits first; the gate holds the second submission,
        // the dispatch of the request.
        CountDownLatch dispatchHeld = fixtures.executor.holdSubmissionsFrom(2, submissionGate);
        Future<?> admission = worker.submit(() -> {
            try (MockedStatic<MuSigTakeOfferRequestValidator> ignored = neutralizeEconomicsValidation()) {
                service.onMessage(first);
            }
        });
        CountDownLatch shutdownStarted = new CountDownLatch(1);
        Thread[] shutdownThread = new Thread[1];
        ExecutorService shutdownRunner = Executors.newSingleThreadExecutor();
        try {
            assertThat(dispatchHeld.await(5, TimeUnit.SECONDS)).isTrue();
            Future<Boolean> shutdown = shutdownRunner.submit(() -> {
                shutdownThread[0] = Thread.currentThread();
                shutdownStarted.countDown();
                return shutDownServiceAndReport();
            });
            assertThat(shutdownStarted.await(5, TimeUnit.SECONDS)).isTrue();
            // The dispatch is in flight under the creation lock. Shutdown waits for this lock
            // before it closes admission, so it cannot take the executor away underneath the
            // dispatch.
            awaitBlocked(shutdownThread[0]);
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
        assertThat(fixtures.executor.isShutdown()).isTrue();
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

        synchronized (service.getTradeCreationLock()) {
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

        synchronized (service.getTradeCreationLock()) {
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

    private SetupTradeMessage_A tamperedCopyOf(SetupTradeMessage_A accepted) throws Exception {
        SetupTradeMessage_A other = fixtures.createRequest(accepted.getContract().getTakeOfferDate() + 1_000);
        // The accepted trade id carrying another request's contract and signature fails identity
        // validation; reporting that failure would fail the taker's accepted attempt.
        return fixtures.createRequest(other.getContract(), other.getContractSignatureData(), accepted.getTradeId());
    }

    private MuSigTradeState stateOf(SetupTradeMessage_A request) {
        return service.findOpenTrade(request.getTradeId()).orElseThrow().getTradeState();
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
