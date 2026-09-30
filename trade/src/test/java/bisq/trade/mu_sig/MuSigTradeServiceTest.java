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

package bisq.trade.mu_sig;

import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.settings.SettingsService;
import bisq.support.arbitration.mu_sig.MuSigArbitrationRequest;
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.support.mediation.mu_sig.MuSigMediationStateChangeMessage;
import bisq.trade.MuSigDisputeState;
import bisq.trade.ServiceProvider;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.events.mediation.MediationResultAcceptedEvent;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import bisq.trade.protobuf.CustomCloseTradeRequest;
import bisq.trade.protobuf.CustomCloseTradeResponse;
import bisq.trade.protobuf.CustomPayoutPsbtRequest;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protobuf.SwapTxSignatureRequest;
import bisq.trade.protobuf.SwapTxSignatureResponse;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import com.google.protobuf.ByteString;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.lang.management.ManagementFactory;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MuSigTradeServiceTest {
    private static final String CUSTOM_PAYOUT_TX_ID = "ab".repeat(32);

    @Test
    void mediationCallsReturnWhileCustomSigningIsWaitingForAnRpcResponse() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.enableConcurrentProcessing(4);
            fixture.caller.submit(() -> fixture.service.acceptMediationResult(fixture.trade)).get(5, SECONDS);
            StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt> response = fixture.rpc.signing.get(5, SECONDS);

            fixture.caller.submit(() -> {
                fixture.service.rejectMediationResult(fixture.trade);
                fixture.service.requestMediation(fixture.trade);
                fixture.service.rejectMediationResultAndRequestArbitration(fixture.trade);
            }).get(5, SECONDS);
            awaitProtocolWaiters(fixture, 3);

            assertThat(fixture.trade.getMyself().getCustomPayoutData()).isEmpty();
            assertThat(fixture.caller.submit(() -> "responsive").get(5, SECONDS)).isEqualTo("responsive");

            completeSigning(response);

            fixture.finishProcessing();
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
            assertThat(fixture.trade.getMyself().isMediationResultRejected()).isFalse();
        }
    }

    @Test
    void mediationCallsReturnWhileCustomFinalizationIsWaitingForAnRpcResponse() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.enableConcurrentProcessing(2);
            fixture.trade.getPeer().setPeersCustomPayoutPsbt(
                    new PeerCustomPayoutPsbt(CUSTOM_PAYOUT_TX_ID, new byte[]{4, 5, 6}));
            fixture.service.acceptMediationResult(fixture.trade);
            completeSigning(fixture.rpc.signing.get(5, SECONDS));
            StreamObserver<CustomCloseTradeResponse> response = fixture.rpc.closing.get(5, SECONDS);

            fixture.caller.submit(() -> fixture.service.rejectMediationResult(fixture.trade)).get(5, SECONDS);
            awaitProtocolWaiters(fixture, 1);
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_SIGNED);

            response.onNext(CustomCloseTradeResponse.newBuilder()
                    .setCustomPayoutTx(ByteString.copyFrom(new byte[]{7, 8, 9})).build());
            response.onCompleted();

            fixture.finishProcessing();
            assertThat(fixture.trade.getMyself().isMediationResultRejected()).isFalse();
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_CLOSED_TRADE);
        }
    }

    @Test
    void rejectionReturnsWhileNormalSigningIsWaitingForAnRpcResponse() throws Exception {
        try (MediationFixture fixture = new MediationFixture(false)) {
            fixture.enableConcurrentProcessing(3);
            fixture.service.paymentReceiptConfirmed(fixture.trade);
            StreamObserver<SwapTxSignatureResponse> response = fixture.rpc.swapSigning.get(5, SECONDS);

            fixture.caller.submit(() -> fixture.service.rejectMediationResult(fixture.trade)).get(5, SECONDS);
            fixture.service.acceptMediationResult(fixture.trade);
            awaitProtocolWaiters(fixture, 2);
            assertThat(fixture.trade.getMyself().isMediationResultRejected()).isFalse();

            response.onNext(SwapTxSignatureResponse.newBuilder()
                    .setSwapTx(ByteString.copyFrom(new byte[]{1, 2, 3}))
                    .setPeerOutputPrvKeyShare(ByteString.copyFrom(new byte[]{4, 5, 6})).build());
            response.onCompleted();

            fixture.finishProcessing();
            assertThat(fixture.trade.getMyself().isMediationResultRejected()).isTrue();
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.SELLER_CONFIRMED_PAYMENT_RECEIPT);
            assertThat(fixture.trade.getEventQueue()).noneMatch(MediationResultAcceptedEvent.class::isInstance);
            assertThat(fixture.rpc.signing).isNotDone();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void settlementRpcDoesNotHoldTheLockForAnotherTrade(boolean finalize) throws Exception {
        try (MediationFixture fixture = new MediationFixture(
                createMediatedTrade(true, MuSigTradeState.DEPOSIT_TX_CONFIRMED), false)) {
            MuSigTrade otherTrade = createMediatedTrade(false, MuSigTradeState.DEPOSIT_TX_CONFIRMED);
            fixture.service.getPersistableStore().addTrade(otherTrade);
            fixture.service.initialize().get(5, SECONDS);
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);
            fixture.enableConcurrentProcessing(2);
            if (finalize) {
                fixture.trade.getPeer().setPeersCustomPayoutPsbt(
                        new PeerCustomPayoutPsbt(CUSTOM_PAYOUT_TX_ID, new byte[]{4, 5, 6}));
            }

            fixture.service.acceptMediationResult(fixture.trade);
            StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt> signing = fixture.rpc.signing.get(5, SECONDS);
            if (finalize) {
                completeSigning(signing);
                fixture.rpc.closing.get(5, SECONDS);
            }

            CompletableFuture<Void> rejected = new CompletableFuture<>();
            Pin pin = otherTrade.getMyself().mediationResultRejectedObservable().addObserver(value -> {
                if (value) {
                    rejected.complete(null);
                }
            });
            try {
                fixture.service.rejectMediationResult(otherTrade);
                rejected.get(5, SECONDS);
                assertThat(otherTrade.getMyself().isMediationResultRejected()).isTrue();
                assertThat(fixture.trade.getTradeState()).isEqualTo(finalize
                        ? MuSigTradeState.CUSTOM_PAYOUT_SIGNED : MuSigTradeState.DEPOSIT_TX_CONFIRMED);
            } finally {
                pin.unbind();
            }

            if (finalize) {
                completeClosing(fixture.rpc.closing.get(5, SECONDS));
            } else {
                completeSigning(signing);
            }
            fixture.finishProcessing();
            assertThat(fixture.trade.getTradeState()).isEqualTo(finalize
                    ? MuSigTradeState.CUSTOM_PAYOUT_CLOSED_TRADE : MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
        }
    }

    @Test
    void mediatorUpdateWaitsForSigningWithoutBlockingMessageDelivery() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.addChannel();
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);
            fixture.enableConcurrentProcessing(2);
            fixture.service.acceptMediationResult(fixture.trade);
            StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt> signing = fixture.rpc.signing.get(5, SECONDS);
            CompletableFuture<Void> reopened = new CompletableFuture<>();
            Pin pin = fixture.trade.getTradeDispute().disputeStateObservable().addObserver(state -> {
                if (state == MuSigDisputeState.MEDIATION_RE_OPENED) {
                    reopened.complete(null);
                }
            });
            try {
                fixture.caller.submit(() -> fixture.service.onMessage(
                        createMediationMessage(fixture.trade, MediationCaseState.RE_OPENED))).get(5, SECONDS);
                awaitProtocolWaiters(fixture, 1);

                assertThat(reopened).isNotDone();
                assertThat(fixture.trade.getTradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.MEDIATION_CLOSED);

                completeSigning(signing);
                reopened.get(5, SECONDS);
                fixture.finishProcessing();
                assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
            } finally {
                pin.unbind();
            }
        }
    }

    @Test
    void peerPsbtWaitsForLocalSigningAndDuplicateDeliveryFinalizesOnlyOnce() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.enableConcurrentProcessing(3);
            fixture.service.acceptMediationResult(fixture.trade);
            StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt> signing = fixture.rpc.signing.get(5, SECONDS);
            MuSigCustomPayoutPsbtMessage message = createPsbtMessage(fixture.trade);

            fixture.service.onMessage(message);
            fixture.service.onMessage(message);
            awaitProtocolWaiters(fixture, 2);
            assertThat(fixture.trade.getPeer().getCustomPayoutData()).isEmpty();
            assertThat(fixture.rpc.closing).isNotDone();

            completeSigning(signing);
            StreamObserver<CustomCloseTradeResponse> closing = fixture.rpc.closing.get(5, SECONDS);
            completeClosing(closing);
            fixture.finishProcessing();
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_CLOSED_TRADE);
            assertThat(fixture.rpc.closingCalls).hasValue(1);
            assertThat(fixture.trade.getPeer().getCustomPayoutData()).isPresent();
        }
    }

    @Test
    void earlyDisputeAndSettlementMessagesAreReplayedAfterProtocolRegistration() throws Exception {
        MuSigTrade trade = createMediatedTrade(true, MuSigTradeState.DEPOSIT_TX_CONFIRMED);
        trade.getTradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_RE_OPENED);
        try (MediationFixture fixture = new MediationFixture(trade, false)) {
            fixture.addChannel();
            fixture.service.onMessage(createPsbtMessage(trade));
            fixture.service.onMessage(createRejectionMessage(trade));
            fixture.service.onMessage(createMediationMessage(trade, MediationCaseState.CLOSED));
            assertThat(trade.getPeer().isMediationResultRejected()).isFalse();

            fixture.service.initialize().get(5, SECONDS);
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);

            assertThat(trade.getTradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.MEDIATION_CLOSED);
            assertThat(trade.getPeer().isMediationResultRejected()).isTrue();
            assertThat(trade.getPeer().getCustomPayoutData()).isEmpty();
            assertThat(fixture.service.getMuSigMediationCustomPayoutService()
                    .getPendingMessagesInReplayOrder(trade.getId())).isEmpty();
        }
    }

    @Test
    void messageRacingWithProtocolRegistrationIsNotLost() throws Exception {
        try (MediationFixture fixture = new MediationFixture(
                createMediatedTrade(true, MuSigTradeState.DEPOSIT_TX_CONFIRMED), false)) {
            CompletableFuture<Boolean> initialization;
            Future<?> delivery;
            synchronized (fixture.service.getPendingMessagesLock()) {
                initialization = CompletableFuture.supplyAsync(() -> fixture.service.initialize().join());
                delivery = fixture.caller.submit(() -> fixture.service.onMessage(createRejectionMessage(fixture.trade)));
                awaitMonitorWaiters(fixture.service.getPendingMessagesLock(), 2);
            }
            initialization.get(5, SECONDS);
            delivery.get(5, SECONDS);
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);

            assertThat(fixture.trade.getPeer().isMediationResultRejected()).isTrue();
            assertThat(fixture.service.getMuSigMediationCustomPayoutService()
                    .getPendingMessagesInReplayOrder(fixture.trade.getId())).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void earlierBufferedRejectionIsProcessedBeforeNewSettlementInputs(boolean acceptLocally) throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            synchronized (fixture.service.getTradeProtocolById().get(fixture.trade.getId())) {
                fixture.service.getMuSigMediationCustomPayoutService().addPendingMessage(createRejectionMessage(fixture.trade));
            }

            if (acceptLocally) {
                fixture.service.acceptMediationResult(fixture.trade);
            } else {
                fixture.service.onMessage(createPsbtMessage(fixture.trade));
            }
            fixture.finishProcessing();

            assertThat(fixture.trade.getPeer().isMediationResultRejected()).isTrue();
            assertThat(fixture.trade.getPeer().getCustomPayoutData()).isEmpty();
            assertThat(fixture.trade.getMyself().getCustomPayoutData()).isEmpty();
            assertThat(fixture.rpc.signing).isNotDone();
        }
    }

    @Test
    void channelCreationReplaysTheResultAndFinalizesAPendingPeerPsbt() throws Exception {
        MuSigTrade trade = createMediatedTrade(true, MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
        MuSigMediationStateChangeMessage result = createMediationMessage(trade, MediationCaseState.CLOSED);
        MuSigCustomPayoutPsbtMessage psbt = createPsbtMessage(trade);
        trade.getMyself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(new byte[]{1, 2, 3}, CUSTOM_PAYOUT_TX_ID, 89_900, 59_900));
        bisq.trade.protobuf.Trade.Builder builder = trade.toProto(false).toBuilder();
        builder.getMuSigTradeBuilder().getTradeDisputeBuilder()
                .setDisputeState(MuSigDisputeState.MEDIATION_OPEN.toProtoEnum())
                .clearMuSigMediationResult().clearMediationResultSignature();
        trade = MuSigTrade.fromProto(builder.build());
        try (MediationFixture fixture = new MediationFixture(trade, true)) {
            fixture.service.onMessage(psbt);
            fixture.service.onMessage(result);
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);
            assertThat(fixture.trade.getPeer().getCustomPayoutData()).isEmpty();
            assertThat(fixture.rpc.closing).isNotDone();

            fixture.caller.submit(fixture::addChannel).get(5, SECONDS);
            StreamObserver<CustomCloseTradeResponse> closing = fixture.rpc.closing.get(5, SECONDS);
            assertThat(fixture.caller.submit(() -> "responsive").get(5, SECONDS)).isEqualTo("responsive");
            assertThat(fixture.trade.getPeer().getCustomPayoutData()).isPresent();

            completeClosing(closing);
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_CLOSED_TRADE);
        }
    }

    @Test
    void acceptanceRechecksEligibilityAfterAnEarlierRejection() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.service.rejectMediationResult(fixture.trade);
            fixture.service.acceptMediationResult(fixture.trade);

            fixture.finishProcessing();
            assertThat(fixture.trade.getMyself().isMediationResultRejected()).isTrue();
            assertThat(fixture.trade.getMyself().getCustomPayoutData()).isEmpty();
            assertThat(fixture.rpc.signing).isNotDone();
        }
    }

    @Test
    void repeatedAcceptanceWhileSigningCallsTheRpcOnlyOnce() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.enableConcurrentProcessing(2);
            fixture.service.acceptMediationResult(fixture.trade);
            StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt> response = fixture.rpc.signing.get(5, SECONDS);

            fixture.service.acceptMediationResult(fixture.trade);
            awaitProtocolWaiters(fixture, 1);

            completeSigning(response);
            fixture.finishProcessing();

            assertThat(fixture.rpc.signingCalls).hasValue(1);
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void arbitrationIsRequestedOnlyAfterRejectionIsApplied(boolean alreadySigned) throws Exception {
        UserProfile arbitrator = createUserProfile(createNetworkId("arbitrator", 9995), "arbitrator");
        MuSigTrade trade = createMediatedTrade(true, alreadySigned
                ? MuSigTradeState.CUSTOM_PAYOUT_SIGNED : MuSigTradeState.DEPOSIT_TX_CONFIRMED, Optional.of(arbitrator));
        if (alreadySigned) {
            trade.getMyself().setMyCustomPayoutPsbt(
                    new CustomPayoutPsbt(new byte[]{1, 2, 3}, CUSTOM_PAYOUT_TX_ID, 89_900, 59_900));
        }
        try (MediationFixture fixture = new MediationFixture(trade, true)) {
            fixture.addChannel();
            fixture.service.rejectMediationResultAndRequestArbitration(trade);
            fixture.finishProcessing();

            assertThat(trade.getMyself().isMediationResultRejected()).isEqualTo(!alreadySigned);
            assertThat(trade.getTradeDispute().getDisputeState()).isEqualTo(alreadySigned
                    ? MuSigDisputeState.MEDIATION_CLOSED : MuSigDisputeState.ARBITRATION_REQUESTED);
            if (alreadySigned) {
                verify(fixture.networkService, never()).confidentialSend(isA(MuSigArbitrationRequest.class), any(), any());
            } else {
                InOrder sends = inOrder(fixture.networkService);
                sends.verify(fixture.networkService).confidentialSend(isA(MuSigMediationResultRejectionMessage.class), any(), any());
                sends.verify(fixture.networkService).confidentialSend(isA(MuSigArbitrationRequest.class), any(), any());
            }
        }
    }

    @Test
    void signingFailureIsReportedThroughTheTradeState() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.service.acceptMediationResult(fixture.trade);
            fixture.rpc.signing.get(5, SECONDS).onError(Status.INTERNAL
                    .withDescription("Test signing failure").asRuntimeException());

            fixture.finishProcessing();
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.FAILED);
            assertThat(fixture.trade.getMyself().getCustomPayoutData()).isEmpty();
        }
    }

    @Test
    void rejectedExecutorSubmissionIsReportedToTheCaller() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            fixture.service.getExecutor().shutdown();

            for (Runnable action : List.<Runnable>of(
                    () -> fixture.service.acceptMediationResult(fixture.trade),
                    () -> fixture.service.rejectMediationResult(fixture.trade),
                    () -> fixture.service.requestMediation(fixture.trade),
                    () -> fixture.service.rejectMediationResultAndRequestArbitration(fixture.trade),
                    () -> fixture.service.paymentInitiated(fixture.trade),
                    () -> fixture.service.onMessage(createPsbtMessage(fixture.trade)),
                    () -> fixture.service.onMessage(createMediationMessage(fixture.trade, MediationCaseState.RE_OPENED)))) {
                assertThatThrownBy(action::run).isInstanceOf(RejectedExecutionException.class);
            }
        }
    }

    @Test
    void acceptanceRequiresDepositConfirmation() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true,
                MuSigTradeState.MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX)) {
            fixture.service.acceptMediationResult(fixture.trade);
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);
            assertThat(fixture.rpc.signing).isNotDone();

            fixture.service.skipWaitForConfirmation(fixture.trade);
            fixture.service.acceptMediationResult(fixture.trade);
            completeSigning(fixture.rpc.signing.get(5, SECONDS));

            fixture.finishProcessing();
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
        }
    }

    @Test
    void acceptanceRequiresClosedMediation() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            synchronized (fixture.service.getTradeProtocolById().get(fixture.trade.getId())) {
                fixture.trade.getTradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_RE_OPENED);
            }
            fixture.service.acceptMediationResult(fixture.trade);
            fixture.service.getExecutor().submit(() -> {}).get(5, SECONDS);
            assertThat(fixture.rpc.signing).isNotDone();

            synchronized (fixture.service.getTradeProtocolById().get(fixture.trade.getId())) {
                fixture.trade.getTradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_CLOSED);
            }
            fixture.service.acceptMediationResult(fixture.trade);
            completeSigning(fixture.rpc.signing.get(5, SECONDS));

            fixture.finishProcessing();
            assertThat(fixture.trade.getTradeState()).isEqualTo(MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
        }
    }

    @Test
    void acceptanceIsRefusedAfterPeerRejection() throws Exception {
        try (MediationFixture fixture = new MediationFixture(true)) {
            MuSigMediationResultRejectionMessage rejection = new MuSigMediationResultRejectionMessage(
                    "peer-rejection", fixture.trade.getId(), MuSigProtocol.VERSION,
                    fixture.trade.getPeer().getNetworkId(), fixture.trade.getMyself().getNetworkId(),
                    MuSigMediationResultService.getMediationResultHash(
                            fixture.trade.getTradeDispute().getMuSigMediationResult().orElseThrow()));

            fixture.service.onMessage(rejection);

            fixture.service.acceptMediationResult(fixture.trade);
            fixture.finishProcessing();
            assertThat(fixture.trade.getPeer().isMediationResultRejected()).isTrue();
            assertThat(fixture.rpc.signing).isNotDone();
        }
    }

    @Test
    void takeOfferRejectionIsReportedFromTheReceivingIdentity() {
        NetworkService networkService = mock(NetworkService.class);
        IdentityService identityService = mock(IdentityService.class);
        SetupTradeMessage_A request = mock(SetupTradeMessage_A.class);
        Identity makerIdentity = mock(Identity.class);
        NetworkIdWithKeyPair makerNetworkIdWithKeyPair = mock(NetworkIdWithKeyPair.class);
        NetworkId makerNetworkId = createNetworkId("maker", 9997);
        NetworkId takerNetworkId = createNetworkId("taker", 9999);
        when(request.getTradeId()).thenReturn("trade-id");
        when(request.getReceiver()).thenReturn(makerNetworkId);
        when(request.getSender()).thenReturn(takerNetworkId);
        when(identityService.findAnyIdentityByNetworkId(makerNetworkId)).thenReturn(Optional.of(makerIdentity));
        when(makerIdentity.getNetworkId()).thenReturn(makerNetworkId);
        when(makerIdentity.getNetworkIdWithKeyPair()).thenReturn(makerNetworkIdWithKeyPair);
        when(networkService.confidentialSend(any(), eq(takerNetworkId), same(makerNetworkIdWithKeyPair)))
                .thenReturn(CompletableFuture.completedFuture(null));
        TradeProtocolException rejection = new TradeProtocolException("x".repeat(600),
                TradeProtocolFailure.PRICE_DEVIATION);

        MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request, rejection);

        ArgumentCaptor<MuSigReportErrorMessage> captor = ArgumentCaptor.forClass(MuSigReportErrorMessage.class);
        verify(networkService).confidentialSend(captor.capture(), eq(takerNetworkId), same(makerNetworkIdWithKeyPair));
        MuSigReportErrorMessage report = captor.getValue();
        assertThat(report.getTradeId()).isEqualTo("trade-id");
        assertThat(report.getProtocolVersion()).isEqualTo(MuSigProtocol.VERSION);
        assertThat(report.getSender()).isEqualTo(makerNetworkId);
        assertThat(report.getReceiver()).isEqualTo(takerNetworkId);
        assertThat(report.getTradeProtocolFailure()).isEqualTo(TradeProtocolFailure.PRICE_DEVIATION);
        assertThat(report.getErrorMessage()).hasSize(MuSigReportErrorMessage.MAX_LENGTH_ERROR_MESSAGE);
        assertThat(report.getStackTrace()).isEmpty();
    }

    @Test
    void takeOfferRejectionIsNotSentWithoutTheReceivingIdentity() {
        NetworkService networkService = mock(NetworkService.class);
        IdentityService identityService = mock(IdentityService.class);
        SetupTradeMessage_A request = mock(SetupTradeMessage_A.class);
        NetworkId makerNetworkId = createNetworkId("maker", 9997);
        when(request.getTradeId()).thenReturn("trade-id");
        when(request.getReceiver()).thenReturn(makerNetworkId);
        when(identityService.findAnyIdentityByNetworkId(makerNetworkId)).thenReturn(Optional.empty());

        MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request,
                new TradeProtocolException(TradeProtocolFailure.OFFER_NOT_AVAILABLE));

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    @Test
    void takeOfferRejectionSendFailuresDoNotEscape() {
        NetworkService networkService = mock(NetworkService.class);
        IdentityService identityService = mock(IdentityService.class);
        SetupTradeMessage_A request = mock(SetupTradeMessage_A.class);
        Identity makerIdentity = mock(Identity.class);
        NetworkIdWithKeyPair makerNetworkIdWithKeyPair = mock(NetworkIdWithKeyPair.class);
        NetworkId makerNetworkId = createNetworkId("maker", 9997);
        NetworkId takerNetworkId = createNetworkId("taker", 9999);
        when(request.getTradeId()).thenReturn("trade-id");
        when(request.getReceiver()).thenReturn(makerNetworkId);
        when(request.getSender()).thenReturn(takerNetworkId);
        when(identityService.findAnyIdentityByNetworkId(makerNetworkId)).thenReturn(Optional.of(makerIdentity));
        when(makerIdentity.getNetworkId()).thenReturn(makerNetworkId);
        when(makerIdentity.getNetworkIdWithKeyPair()).thenReturn(makerNetworkIdWithKeyPair);
        when(networkService.confidentialSend(any(), eq(takerNetworkId), same(makerNetworkIdWithKeyPair)))
                .thenThrow(new IllegalStateException("synchronous send failure"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("asynchronous send failure")));

        assertThatCode(() -> MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request,
                new TradeProtocolException(TradeProtocolFailure.OFFER_NOT_AVAILABLE)))
                .doesNotThrowAnyException();
        assertThatCode(() -> MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request,
                new TradeProtocolException(TradeProtocolFailure.OFFER_NOT_AVAILABLE)))
                .doesNotThrowAnyException();
    }

    private static NetworkId createNetworkId(String keyIdSuffix, int port) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        return new NetworkId(addresses, new PubKey(keyPair.getPublic(), "test-key-" + keyIdSuffix));
    }

    private static UserProfile createUserProfile(NetworkId networkId, String nickname) {
        ProofOfWork proofOfWork = new ProofOfWork(networkId.getPubKey().getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, nickname, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static void completeSigning(StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt> response) {
        response.onNext(new CustomPayoutPsbt(new byte[]{1, 2, 3}, CUSTOM_PAYOUT_TX_ID, 89_900, 59_900).toProto(false));
        response.onCompleted();
    }

    private static void completeClosing(StreamObserver<CustomCloseTradeResponse> response) {
        response.onNext(CustomCloseTradeResponse.newBuilder()
                .setCustomPayoutTx(ByteString.copyFrom(new byte[]{7, 8, 9})).build());
        response.onCompleted();
    }

    private static MuSigMediationStateChangeMessage createMediationMessage(MuSigTrade trade, MediationCaseState state) {
        return new MuSigMediationStateChangeMessage("mediation-state", trade.getId(),
                trade.getContract().getMediator().orElseThrow().getNetworkId(), state,
                trade.getTradeDispute().getMuSigMediationResult(), trade.getTradeDispute().getMediationResultSignature());
    }

    private static MuSigCustomPayoutPsbtMessage createPsbtMessage(MuSigTrade trade) {
        return new MuSigCustomPayoutPsbtMessage("peer-psbt", trade.getId(), MuSigProtocol.VERSION,
                trade.getPeer().getNetworkId(), trade.getMyself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(trade.getTradeDispute().getMuSigMediationResult().orElseThrow()),
                CUSTOM_PAYOUT_TX_ID, new byte[]{4, 5, 6});
    }

    private static MuSigMediationResultRejectionMessage createRejectionMessage(MuSigTrade trade) {
        return new MuSigMediationResultRejectionMessage("peer-rejection", trade.getId(), MuSigProtocol.VERSION,
                trade.getPeer().getNetworkId(), trade.getMyself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(trade.getTradeDispute().getMuSigMediationResult().orElseThrow()));
    }

    private static void awaitProtocolWaiters(MediationFixture fixture, int count) throws InterruptedException {
        awaitMonitorWaiters(fixture.service.getTradeProtocolById().get(fixture.trade.getId()), count);
    }

    private static void awaitMonitorWaiters(Object monitor, int count) throws InterruptedException {
        // Observe actual contention; a queued task alone would not exercise same-trade locking.
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            long waiters = Arrays.stream(ManagementFactory.getThreadMXBean().dumpAllThreads(false, false))
                    .filter(info -> info.getThreadState() == Thread.State.BLOCKED && info.getLockInfo() != null &&
                            info.getLockInfo().getIdentityHashCode() == System.identityHashCode(monitor))
                    .count();
            if (waiters >= count) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Expected " + count + " tasks waiting for the monitor");
    }

    private static final class MediationFixture implements AutoCloseable {
        private final PendingRpcService rpc = new PendingRpcService();
        private final Server server;
        private final ExecutorService caller = Executors.newSingleThreadExecutor();
        private final MuSigTradeService service;
        private final MuSigTrade trade;
        private final NetworkService networkService;
        private final MuSigOpenTradeChannelService channelService;

        private MediationFixture(boolean isBuyer) throws Exception {
            this(isBuyer, isBuyer ? MuSigTradeState.DEPOSIT_TX_CONFIRMED
                    : MuSigTradeState.SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE);
        }

        private MediationFixture(boolean isBuyer, MuSigTradeState initialState) throws Exception {
            this(createMediatedTrade(isBuyer, initialState), true);
        }

        private MediationFixture(MuSigTrade trade, boolean initialize) throws Exception {
            this.trade = trade;
            server = Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create()).addService(rpc).build().start();
            networkService = mock(NetworkService.class);
            when(networkService.getConfidentialMessageServices()).thenReturn(Set.of());
            when(networkService.confidentialSend(any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(new SendMessageResult()));
            channelService = mock(MuSigOpenTradeChannelService.class);
            when(channelService.getChannels()).thenReturn(new ObservableSet<>());
            when(channelService.findChannelByTradeId(any())).thenReturn(Optional.empty());
            ChatService chatService = mock(ChatService.class);
            when(chatService.getMuSigOpenTradeChannelService()).thenReturn(channelService);
            UserService userService = mock(UserService.class);
            when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
            UserProfileService userProfileService = mock(UserProfileService.class);
            when(userService.getUserProfileService()).thenReturn(userProfileService);
            when(userProfileService.findUserProfile(trade.getMyIdentity().getId()))
                    .thenReturn(Optional.of(createUserProfile(trade.getMyself().getNetworkId(), "trader")));
            when(userProfileService.findUserProfile(trade.getPeer().getNetworkId().getId()))
                    .thenReturn(Optional.of(createUserProfile(trade.getPeer().getNetworkId(), "peer")));
            AlertService alertService = mock(AlertService.class);
            when(alertService.getAuthorizedAlertDataSet()).thenReturn(new ObservableSet<>());
            BondedRolesService bondedRolesService = mock(BondedRolesService.class);
            when(bondedRolesService.getAlertService()).thenReturn(alertService);
            SettingsService settingsService = mock(SettingsService.class);
            when(settingsService.getNumDaysAfterRedactingTradeData()).thenReturn(new Observable<>(90));
            @SuppressWarnings("unchecked")
            Persistence<MuSigTradeStore> persistence = mock(Persistence.class);
            when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
            PersistenceService persistenceService = mock(PersistenceService.class);
            when(persistenceService.getOrCreatePersistence(any(), any(), any(MuSigTradeStore.class)))
                    .thenReturn(persistence);
            ServiceProvider serviceProvider = mock(ServiceProvider.class);
            when(serviceProvider.getNetworkService()).thenReturn(networkService);
            when(serviceProvider.getChatService()).thenReturn(chatService);
            when(serviceProvider.getUserService()).thenReturn(userService);
            when(serviceProvider.getBondedRolesService()).thenReturn(bondedRolesService);
            when(serviceProvider.getSettingsService()).thenReturn(settingsService);
            when(serviceProvider.getPersistenceService()).thenReturn(persistenceService);

            service = new MuSigTradeService(new MuSigTradeService.Config("127.0.0.1", server.getPort()),
                    serviceProvider, AppType.DESKTOP);
            when(serviceProvider.getMuSigTradeService()).thenReturn(service);
            service.getPersistableStore().addTrade(trade);
            if (initialize) {
                service.initialize().get(5, SECONDS);
                service.getExecutor().submit(() -> {}).get(5, SECONDS);
            }
        }

        private void enableConcurrentProcessing(int workers) {
            // Isolate locking from the production pool's usual single-worker queueing behavior.
            ((ThreadPoolExecutor) service.getExecutor()).setCorePoolSize(workers);
        }

        private void finishProcessing() throws InterruptedException {
            // After the last input/RPC response, drain all workers before checking no-op outcomes.
            service.getExecutor().shutdown();
            assertThat(service.getExecutor().awaitTermination(5, SECONDS)).isTrue();
        }

        private void addChannel() {
            NetworkId networkId = trade.getMyself().getNetworkId();
            UserProfile profile = createUserProfile(networkId, "trader");
            // User-name rendering uses the application's UserProfileService singleton.
            UserIdentity userIdentity = mock(UserIdentity.class);
            when(userIdentity.getId()).thenReturn(profile.getId());
            when(userIdentity.getUserName()).thenReturn("trader");
            MuSigOpenTradeChannel channel = MuSigOpenTradeChannel.create(trade.getId(), userIdentity, Set.of(profile),
                    trade.getContract().getMediator(), trade.getContract().getArbitrator(), MuSigDisputeAgentType.MEDIATOR);
            when(channelService.findChannelByTradeId(trade.getId())).thenReturn(Optional.of(channel));
            channelService.getChannels().add(channel);
        }

        @Override
        public void close() throws Exception {
            server.shutdownNow();
            service.shutdown().get(5, SECONDS);
            caller.shutdownNow();
            server.awaitTermination(5, SECONDS);
        }
    }

    private static MuSigTrade createMediatedTrade(boolean isBuyer, MuSigTradeState state) throws Exception {
        return createMediatedTrade(isBuyer, state, Optional.empty());
    }

    private static MuSigTrade createMediatedTrade(boolean isBuyer,
                                                MuSigTradeState state,
                                                Optional<UserProfile> arbitrator) throws Exception {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId myNetworkId = new NetworkId(new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", 9997))),
                new PubKey(keyPair.getPublic(), "my-key"));
        Identity identity = new Identity("my-identity", myNetworkId, new KeyBundle("my-key-bundle", keyPair,
                TorKeyGeneration.generateKeyPair(), I2PKeyGeneration.generateKeyPair()));
        NetworkId peerNetworkId = createNetworkId("peer", 9998);
        KeyPair mediatorKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId mediatorNetworkId = new NetworkId(new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", 9996))),
                new PubKey(mediatorKeyPair.getPublic(), "mediator-key"));
        ProofOfWork proofOfWork = new ProofOfWork(mediatorNetworkId.getPubKey().getHash(), 0, null, 1.0, new byte[72], 0);
        UserProfile mediator = new UserProfile(1, "mediator", proofOfWork, 0, mediatorNetworkId, "", "", "1.0.0");
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        FiatPaymentMethod paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        MarketPriceSpec priceSpec = new MarketPriceSpec();
        MuSigOffer offer = new MuSigOffer("mediation-service-test-offer", myNetworkId,
                isBuyer ? Direction.BUY : Direction.SELL, market, new BaseSideFixedAmountSpec(100_000), priceSpec,
                List.of(paymentMethod), List.of(new CollateralOption(0.25, 0.25)), MuSigProtocol.VERSION);
        MuSigContract contract = new MuSigContract(System.currentTimeMillis(), offer, peerNetworkId, 100_000, 3_500_000,
                PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, market.getQuoteCurrencyCode()),
                new byte[20], Optional.of(mediator), arbitrator, priceSpec, 0);
        MuSigTrade initialTrade = new MuSigTrade(contract, isBuyer, false, identity, offer, peerNetworkId, myNetworkId);
        MuSigTrade trade = MuSigTrade.fromProto(initialTrade.toProto(false).toBuilder().setState(state.name()).build());
        trade.setProtocolVersion(MuSigProtocol.VERSION);
        MuSigMediationResult result = new MuSigMediationResult(ContractService.getContractHash(trade.getContract()),
                MediationResultReason.OTHER, MediationPayoutDistributionType.CUSTOM_PAYOUT,
                Optional.of(90_000L), Optional.of(60_000L), Optional.empty(), Optional.empty());
        trade.getTradeDispute().setMuSigMediationResult(result);
        trade.getTradeDispute().setMediationResultSignature(
                MuSigMediationResultService.signMediationResult(result, mediatorKeyPair));
        trade.getTradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_CLOSED);
        return trade;
    }

    private static final class PendingRpcService extends MusigGrpc.MusigImplBase {
        private final CompletableFuture<StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt>> signing = new CompletableFuture<>();
        private final CompletableFuture<StreamObserver<CustomCloseTradeResponse>> closing = new CompletableFuture<>();
        private final CompletableFuture<StreamObserver<SwapTxSignatureResponse>> swapSigning = new CompletableFuture<>();
        private final AtomicInteger signingCalls = new AtomicInteger();
        private final AtomicInteger closingCalls = new AtomicInteger();

        @Override
        public void signCustomPayoutTx(CustomPayoutPsbtRequest request,
                                       StreamObserver<bisq.trade.protobuf.CustomPayoutPsbt> responseObserver) {
            signingCalls.incrementAndGet();
            signing.complete(responseObserver);
        }

        @Override
        public void customCloseTrade(CustomCloseTradeRequest request, StreamObserver<CustomCloseTradeResponse> responseObserver) {
            closingCalls.incrementAndGet();
            closing.complete(responseObserver);
        }

        @Override
        public void signSwapTx(SwapTxSignatureRequest request, StreamObserver<SwapTxSignatureResponse> responseObserver) {
            swapSigning.complete(responseObserver);
        }
    }
}
