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

package bisq.trade.mu_sig.protocol;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.fsm.Event;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.NetworkService;
import bisq.network.SendMessageResult;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.trade.MuSigDisputeState;
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigCustomPayoutPartyData;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.trade.mu_sig.PeerCustomPayoutPsbt;
import bisq.trade.mu_sig.events.blockchain.DepositTxConfirmedEvent;
import bisq.trade.mu_sig.events.buyer.PaymentInitiatedEvent;
import bisq.trade.mu_sig.events.mediation.CustomPayoutFinalizationEvent;
import bisq.trade.mu_sig.events.mediation.MediationResultAcceptedEvent;
import bisq.trade.mu_sig.events.mediation.MediationResultRejectedEvent;
import bisq.trade.mu_sig.events.seller.PaymentReceiptConfirmedEvent;
import bisq.trade.mu_sig.messages.grpc.CloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.CustomCloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage;
import bisq.trade.mu_sig.messages.network.PaymentReceivedMessage_F;
import bisq.trade.mu_sig.messages.network.mu_sig_data.SwapTxSignature;
import bisq.trade.protobuf.CloseTradeRequest;
import bisq.trade.protobuf.CustomCloseTradeRequest;
import bisq.trade.protobuf.CustomPayoutPsbtRequest;
import bisq.trade.protobuf.MusigGrpc;
import bisq.trade.protobuf.PartialSignaturesRequest;
import bisq.trade.protobuf.SwapTxSignatureRequest;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.BUYER_INITIATED_PAYMENT;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.CUSTOM_PAYOUT_CLOSED_TRADE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.CUSTOM_PAYOUT_SIGNED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.DEPOSIT_TX_CONFIRMED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.FAILED;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.SELLER_CONFIRMED_PAYMENT_RECEIPT;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_RECEIVED_ACCOUNT_PAYLOAD;
import static bisq.trade.mu_sig.protocol.MuSigTradeState.TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MuSigProtocolTest {
    private MuSigTradeService tradeService;
    private MusigGrpc.MusigBlockingStub blockingStub;
    private ServiceProvider serviceProvider;
    private NetworkService networkService;

    @BeforeEach
    void setUp() {
        tradeService = mock(MuSigTradeService.class);
        blockingStub = mock(MusigGrpc.MusigBlockingStub.class);
        serviceProvider = mock(ServiceProvider.class);
        networkService = mock(NetworkService.class);
        UserService userService = mock(UserService.class);
        BannedUserService bannedUserService = mock(BannedUserService.class);
        ChatService chatService = mock(ChatService.class);
        MuSigOpenTradeChannelService openTradeChannelService = mock(MuSigOpenTradeChannelService.class);
        when(serviceProvider.getMuSigTradeService()).thenReturn(tradeService);
        when(tradeService.getMusigBlockingStub()).thenReturn(blockingStub);
        when(serviceProvider.getNetworkService()).thenReturn(networkService);
        when(serviceProvider.getUserService()).thenReturn(userService);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        when(serviceProvider.getChatService()).thenReturn(chatService);
        when(chatService.getMuSigOpenTradeChannelService()).thenReturn(openTradeChannelService);
        when(openTradeChannelService.findChannelByTradeId(any())).thenReturn(Optional.empty());
        when(networkService.confidentialSend(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendMessageResult()));
    }

    @Test
    void givenQueuedPaymentReceivedMessage_whenStateBecomesEligible_thenClosesNormallyOnReplay() {
        MuSigTrade trade = copyTradeWithState(createTrade(), DEPOSIT_TX_CONFIRMED);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        PaymentReceivedMessage_F paymentReceivedMessage = createPaymentReceivedMessage(trade);
        when(blockingStub.getPartialSignatures(any(PartialSignaturesRequest.class)))
                .thenReturn(bisq.trade.protobuf.PartialSignaturesMessage.newBuilder()
                        .setSwapTxInputPartialSignature(ByteString.copyFrom(new byte[]{1, 2, 3}))
                        .build());
        CloseTradeResponse closeTradeResponse = stubNormalTradeClosure();

        protocol.handle(paymentReceivedMessage);

        assertThat(trade.getTradeState()).isEqualTo(DEPOSIT_TX_CONFIRMED);
        assertThat(trade.getEventQueue()).containsExactly(paymentReceivedMessage);

        protocol.handle(new PaymentInitiatedEvent());

        assertThat(trade.getTradeState()).isEqualTo(BUYER_CLOSED_TRADE);
        assertThat(trade.getEventQueue()).isEmpty();
        assertThat(trade.getMyself().getMyCloseTradeResponse()).contains(closeTradeResponse);
        verify(blockingStub).closeTrade(any(CloseTradeRequest.class));
    }

    @Test
    void givenBuyerAcceptsMediationFirst_whenPaymentReceivedMessageArrives_thenKeepsCustomPayoutPath() {
        MuSigTrade trade = copyTradeWithState(createTrade(true, false), BUYER_INITIATED_PAYMENT);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        PaymentReceivedMessage_F paymentReceivedMessage = createPaymentReceivedMessage(trade);
        stubCustomPayoutSigning(trade);

        protocol.handle(new MediationResultAcceptedEvent());

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);

        protocol.handle(paymentReceivedMessage);

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
        assertThat(trade.getEventQueue()).isEmpty();
        verify(blockingStub, times(1)).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
        verify(blockingStub, never()).closeTrade(any(CloseTradeRequest.class));
    }

    @Test
    void givenPaymentReceivedMessageQueuedAtDepositConfirmation_whenBuyerAcceptsMediation_thenConsumesMessage() {
        MuSigTrade trade = copyTradeWithState(createTrade(true, false), DEPOSIT_TX_CONFIRMED);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        PaymentReceivedMessage_F paymentReceivedMessage = createPaymentReceivedMessage(trade);
        stubCustomPayoutSigning(trade);

        protocol.handle(paymentReceivedMessage);

        assertThat(trade.getTradeState()).isEqualTo(DEPOSIT_TX_CONFIRMED);
        assertThat(trade.getEventQueue()).containsExactly(paymentReceivedMessage);

        protocol.handle(new MediationResultAcceptedEvent());

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
        assertThat(trade.getEventQueue()).isEmpty();
        verify(blockingStub, times(1)).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
        verify(blockingStub, never()).closeTrade(any(CloseTradeRequest.class));
    }

    @Test
    void givenMismatchingPeerPsbt_whenMediationResultAccepted_thenCommitsAndSendsLocalPsbt() {
        MuSigTrade trade = copyTradeWithState(createTrade(true, false), BUYER_INITIATED_PAYMENT);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        trade.getPeer().setPeersCustomPayoutPsbt(
                new PeerCustomPayoutPsbt("cd".repeat(32), new byte[]{4, 5, 6}));
        CustomPayoutPsbt customPayoutPsbt = stubCustomPayoutSigning(trade);
        AtomicBoolean messageSent = new AtomicBoolean();
        AtomicBoolean persistenceRequested = new AtomicBoolean();
        when(networkService.confidentialSend(any(), any(), any()))
                .thenAnswer(invocation -> {
                    assertThat(trade.getMyself().getCustomPayoutData())
                            .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                            .contains(customPayoutPsbt);
                    assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
                    messageSent.set(true);
                    return CompletableFuture.completedFuture(new SendMessageResult());
                });
        doAnswer(invocation -> {
                    assertThat(messageSent).isTrue();
                    assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
                    assertThat(trade.getMyself().getCustomPayoutData())
                            .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                            .contains(customPayoutPsbt);
                    persistenceRequested.set(true);
                    return null;
                })
                .when(tradeService).persist();

        protocol.handle(new MediationResultAcceptedEvent());

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
        assertThat(messageSent).isTrue();
        assertThat(persistenceRequested).isTrue();
        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                .contains(customPayoutPsbt);
        ArgumentCaptor<MuSigCustomPayoutPsbtMessage> messageCaptor =
                ArgumentCaptor.forClass(MuSigCustomPayoutPsbtMessage.class);
        ArgumentCaptor<NetworkId> receiverCaptor = ArgumentCaptor.forClass(NetworkId.class);
        ArgumentCaptor<NetworkIdWithKeyPair> senderCaptor =
                ArgumentCaptor.forClass(NetworkIdWithKeyPair.class);
        verify(networkService).confidentialSend(
                messageCaptor.capture(), receiverCaptor.capture(), senderCaptor.capture());
        MuSigCustomPayoutPsbtMessage message = messageCaptor.getValue();
        assertThat(message.getTradeId()).isEqualTo(trade.getId());
        assertThat(message.getProtocolVersion()).isEqualTo(trade.getProtocolVersion());
        assertThat(message.getSender()).isEqualTo(trade.getMyself().getNetworkId());
        assertThat(message.getReceiver()).isEqualTo(trade.getPeer().getNetworkId());
        assertThat(message.getMediationResultHash()).containsExactly(
                MuSigMediationResultService.getMediationResultHash(
                        trade.getTradeDispute().getMuSigMediationResult().orElseThrow()));
        assertThat(message.getTxId()).isEqualTo(customPayoutPsbt.getTxId());
        assertThat(message.getPsbt()).containsExactly(customPayoutPsbt.getPsbt());
        assertThat(receiverCaptor.getValue()).isEqualTo(trade.getPeer().getNetworkId());
        assertThat(senderCaptor.getValue()).isEqualTo(trade.getMyIdentity().getNetworkIdWithKeyPair());
        verify(tradeService, times(1)).persist();
    }

    @Test
    void givenBuyerClosesNormallyFirst_whenMediationResultAccepted_thenKeepsNormalClosePath() {
        MuSigTrade trade = copyTradeWithState(createTrade(true, false), BUYER_INITIATED_PAYMENT);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        PaymentReceivedMessage_F paymentReceivedMessage = createPaymentReceivedMessage(trade);
        setCustomPayoutMediationResult(trade);
        CloseTradeResponse closeTradeResponse = stubNormalTradeClosure();

        protocol.handle(paymentReceivedMessage);

        assertThat(trade.getTradeState()).isEqualTo(BUYER_CLOSED_TRADE);

        protocol.handle(new MediationResultAcceptedEvent());

        assertThat(trade.getTradeState()).isEqualTo(BUYER_CLOSED_TRADE);
        assertThat(trade.getEventQueue()).isEmpty();
        assertThat(trade.getMyself().getMyCloseTradeResponse()).contains(closeTradeResponse);
        verify(blockingStub).closeTrade(any(CloseTradeRequest.class));
        verify(blockingStub, never()).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
    }

    @Test
    void givenSellerAcceptsMediationFirst_whenPaymentReceiptConfirmed_thenKeepsCustomPayoutPath() {
        MuSigTrade sellerTrade = copyTradeWithState(
                createTrade(false, false),
                SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE);
        MuSigProtocol sellerProtocol = new MuSigSellerAsMakerProtocol(serviceProvider, sellerTrade);
        stubCustomPayoutSigning(sellerTrade);

        sellerProtocol.handle(new MediationResultAcceptedEvent());
        sellerProtocol.handle(new PaymentReceiptConfirmedEvent());

        assertThat(sellerTrade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
        assertThat(sellerTrade.getEventQueue()).isEmpty();
        verify(blockingStub, times(1)).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
        verify(blockingStub, never()).signSwapTx(any(SwapTxSignatureRequest.class));
    }

    @Test
    void givenConcurrentBuyerSettlementEvents_whenHandled_thenExactlyOnePathWins() throws Exception {
        MuSigTrade trade = copyTradeWithState(createTrade(true, false), BUYER_INITIATED_PAYMENT);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        PaymentReceivedMessage_F paymentReceivedMessage = createPaymentReceivedMessage(trade);
        stubCustomPayoutSigning(trade);
        stubNormalTradeClosure();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> customPayoutResult = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, SECONDS)).isTrue();
                protocol.handle(new MediationResultAcceptedEvent());
                return null;
            });
            Future<?> normalCloseResult = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, SECONDS)).isTrue();
                protocol.handle(paymentReceivedMessage);
                return null;
            });

            assertThat(ready.await(5, SECONDS)).isTrue();
            start.countDown();
            customPayoutResult.get(5, SECONDS);
            normalCloseResult.get(5, SECONDS);

            assertThat(trade.getTradeState()).isIn(CUSTOM_PAYOUT_SIGNED, BUYER_CLOSED_TRADE);
            assertThat(trade.getEventQueue()).isEmpty();
            if (trade.getTradeState() == CUSTOM_PAYOUT_SIGNED) {
                verify(blockingStub, times(1)).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
                verify(blockingStub, never()).closeTrade(any(CloseTradeRequest.class));
            } else {
                verify(blockingStub, times(1)).closeTrade(any(CloseTradeRequest.class));
                verify(blockingStub, never()).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("customPayoutAcceptanceTransitionCases")
    void givenEligibleTradeState_whenMediationResultAccepted_thenTransitionsToCustomPayoutSigned(
            CustomPayoutAcceptanceTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                testCase.sourceState());
        CustomPayoutPsbt customPayoutPsbt = stubCustomPayoutSigning(trade);
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);

        protocol.handle(new MediationResultAcceptedEvent());

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                .contains(customPayoutPsbt);
        verify(blockingStub, times(1)).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
    }

    @Test
    void givenStoredLocalPsbt_whenMediationResultAccepted_thenDoesNotCallRpcOrReplacePsbt() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        setCustomPayoutMediationResult(trade);
        CustomPayoutPsbt customPayoutPsbt = new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                "ab".repeat(32),
                59_900,
                39_900);
        trade.getMyself().setMyCustomPayoutPsbt(customPayoutPsbt);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new MediationResultAcceptedEvent());

        assertThat(trade.getTradeState()).isEqualTo(FAILED);
        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                .contains(customPayoutPsbt);
        verify(blockingStub, never()).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
        verify(networkService, never()).confidentialSend(any(MuSigCustomPayoutPsbtMessage.class), any(), any());
    }

    @Test
    void givenLocalRejection_whenMediationResultAccepted_thenDoesNotCallRpcOrStorePsbt() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        setCustomPayoutMediationResult(trade);
        trade.getMyself().setMediationResultRejected();
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new MediationResultAcceptedEvent());

        assertThat(trade.getTradeState()).isEqualTo(FAILED);
        assertThat(trade.getMyself().isMediationResultRejected()).isTrue();
        assertThat(trade.getMyself().getCustomPayoutData()).isEmpty();
        verify(blockingStub, never()).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
        verify(networkService, never()).confidentialSend(any(MuSigCustomPayoutPsbtMessage.class), any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("customPayoutReceiveTransitionCases")
    void givenPeerCustomPayoutPsbt_whenHandled_thenProcessesWithoutChangingTradeState(
            CustomPayoutAcceptanceTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                testCase.sourceState());
        setCustomPayoutMediationResult(trade);
        if (testCase.sourceState() == CUSTOM_PAYOUT_SIGNED) {
            trade.getMyself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(
                    new byte[]{4, 5, 6},
                    "ab".repeat(32),
                    59_900,
                    39_900));
        }
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);
        MuSigCustomPayoutPsbtMessage message = createCustomPayoutPsbtMessage(trade);

        protocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(testCase.sourceState());
        assertThat(trade.getEventQueue()).isEmpty();
        assertThat(trade.getPeer().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .contains(new PeerCustomPayoutPsbt("ab".repeat(32), new byte[]{1, 2, 3}));
        verify(tradeService).persist();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("customPayoutPreConfirmationTransitionCases")
    void givenPeerCustomPayoutPsbtBeforeDepositConfirmation_whenDepositConfirms_thenProcessesQueuedMessage(
            CustomPayoutAcceptanceTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                testCase.sourceState());
        setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);
        MuSigCustomPayoutPsbtMessage message = createCustomPayoutPsbtMessage(trade);

        protocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(testCase.sourceState());
        assertThat(trade.getEventQueue()).containsExactly(message);
        assertThat(trade.getPeer().getCustomPayoutData()).isEmpty();

        protocol.handle(new DepositTxConfirmedEvent());

        assertThat(trade.getTradeState()).isEqualTo(DEPOSIT_TX_CONFIRMED);
        assertThat(trade.getEventQueue()).isEmpty();
        assertThat(trade.getPeer().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .contains(new PeerCustomPayoutPsbt("ab".repeat(32), new byte[]{1, 2, 3}));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sellerPostFTransitionCases")
    void givenPeerCustomPayoutPsbtAfterSellerCommittedMessageF_whenHandled_thenConsumesWithoutProcessing(
            CustomPayoutAcceptanceTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                testCase.sourceState());
        setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);
        MuSigCustomPayoutPsbtMessage message = createCustomPayoutPsbtMessage(trade);

        protocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(SELLER_CONFIRMED_PAYMENT_RECEIPT);
        assertThat(trade.getEventQueue()).isEmpty();
        assertThat(trade.getPeer().getCustomPayoutData()).isEmpty();
        verify(tradeService).persist();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("customPayoutSignedTransitionCases")
    void givenCustomPayoutSigned_whenNormalClosureInputArrives_thenConsumesWithoutChangingState(
            CustomPayoutAcceptanceTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                CUSTOM_PAYOUT_SIGNED);
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);
        Event normalClosureInput = testCase.isBuyer()
                ? createPaymentReceivedMessage(trade)
                : new PaymentReceiptConfirmedEvent();

        protocol.handle(normalClosureInput);

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
        assertThat(trade.getEventQueue()).isEmpty();
        verify(blockingStub, never()).signCustomPayoutTx(any(CustomPayoutPsbtRequest.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("customPayoutSignedTransitionCases")
    void givenMatchingLocalAndPeerPsbt_whenFinalizingCustomPayout_thenClosesTrade(
            CustomPayoutAcceptanceTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                CUSTOM_PAYOUT_SIGNED);
        setCustomPayoutMediationResult(trade);
        CustomPayoutPsbt myCustomPayoutPsbt = new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                "ab".repeat(32),
                59_900,
                39_900);
        PeerCustomPayoutPsbt peersCustomPayoutPsbt = new PeerCustomPayoutPsbt(
                "ab".repeat(32),
                new byte[]{1, 2, 3});
        trade.getMyself().setMyCustomPayoutPsbt(myCustomPayoutPsbt);
        trade.getPeer().setPeersCustomPayoutPsbt(peersCustomPayoutPsbt);
        CustomCloseTradeResponse response = new CustomCloseTradeResponse(new byte[]{7, 8, 9});
        when(blockingStub.customCloseTrade(any(CustomCloseTradeRequest.class)))
                .thenReturn(response.toProto(false));
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);

        protocol.handle(new CustomPayoutFinalizationEvent());

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_CLOSED_TRADE);
        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomCloseTradeResponse)
                .contains(response);
        ArgumentCaptor<CustomCloseTradeRequest> requestCaptor =
                ArgumentCaptor.forClass(CustomCloseTradeRequest.class);
        verify(blockingStub).customCloseTrade(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getTradeId()).isEqualTo(trade.getId());
        assertThat(requestCaptor.getValue().getPeersCustomPayoutPsbt())
                .isEqualTo(ByteString.copyFrom(peersCustomPayoutPsbt.getPsbt()));
        verify(tradeService).stopCloseTradeTimeout(trade);
    }

    @Test
    void givenStoredCustomCloseResponse_whenFinalizingCustomPayout_thenDoesNotCallRpcOrReplaceResponse() {
        MuSigTrade trade = copyTradeWithState(createTrade(), CUSTOM_PAYOUT_SIGNED);
        setCustomPayoutMediationResult(trade);
        trade.getMyself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                "ab".repeat(32),
                59_900,
                39_900));
        trade.getPeer().setPeersCustomPayoutPsbt(new PeerCustomPayoutPsbt(
                "ab".repeat(32),
                new byte[]{1, 2, 3}));
        CustomCloseTradeResponse response = new CustomCloseTradeResponse(new byte[]{7, 8, 9});
        trade.getMyself().setMyCustomCloseTradeResponse(response);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new CustomPayoutFinalizationEvent());

        assertThat(trade.getTradeState()).isEqualTo(FAILED);
        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomCloseTradeResponse)
                .contains(response);
        verify(blockingStub, never()).customCloseTrade(any(CustomCloseTradeRequest.class));
        verify(tradeService, never()).stopCloseTradeTimeout(trade);
    }

    @Test
    void givenMismatchingTransactionIds_whenFinalizingCustomPayout_thenDoesNotCallRpc() {
        MuSigTrade trade = copyTradeWithState(createTrade(), CUSTOM_PAYOUT_SIGNED);
        setCustomPayoutMediationResult(trade);
        trade.getMyself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                "ab".repeat(32),
                59_900,
                39_900));
        trade.getPeer().setPeersCustomPayoutPsbt(new PeerCustomPayoutPsbt(
                "cd".repeat(32),
                new byte[]{1, 2, 3}));
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new CustomPayoutFinalizationEvent());

        assertThat(trade.getTradeState()).isEqualTo(FAILED);
        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomCloseTradeResponse)
                .isEmpty();
        verify(blockingStub, never()).customCloseTrade(any(CustomCloseTradeRequest.class));
        verify(tradeService, never()).stopCloseTradeTimeout(trade);
    }

    @Test
    void givenCustomPayoutSigned_whenPaymentReceivedMessageHasUnexpectedSender_thenFailsTrade() {
        MuSigTrade trade = copyTradeWithState(createTrade(true, false), CUSTOM_PAYOUT_SIGNED);
        MuSigProtocol buyerProtocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        NetworkId unexpectedSender = trade.getMyself().getNetworkId();
        PaymentReceivedMessage_F message = createPaymentReceivedMessage(
                trade,
                "unexpected-payment-received-message",
                unexpectedSender,
                trade.getMyself().getNetworkId(),
                trade.getProtocolVersion());

        buyerProtocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(FAILED);
        assertThat(trade.getEventQueue()).isEmpty();
        verify(blockingStub, never()).closeTrade(any(CloseTradeRequest.class));
    }

    @Test
    void givenCustomPayoutSigned_whenPaymentReceivedMessageHasUnexpectedTradeId_thenFailsTrade() {
        MuSigTrade trade = copyTradeWithState(createTrade(true, false), CUSTOM_PAYOUT_SIGNED);
        MuSigProtocol buyerProtocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        PaymentReceivedMessage_F message = new PaymentReceivedMessage_F(
                "unexpected-trade-payment-received-message",
                "unexpected-trade-id",
                trade.getProtocolVersion(),
                trade.getPeer().getNetworkId(),
                trade.getMyself().getNetworkId(),
                createPaymentReceivedMessage(trade).getSwapTxSignature());

        buyerProtocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(FAILED);
        assertThat(trade.getEventQueue()).isEmpty();
        verify(blockingStub, never()).closeTrade(any(CloseTradeRequest.class));
    }

    @Test
    void givenPeerCustomPayoutPsbtWithUnexpectedSender_whenHandled_thenIgnoresWithoutFailingTrade() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        MuSigMediationResult mediationResult = setCustomPayoutMediationResult(trade);
        MuSigProtocol buyerProtocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        NetworkId unexpectedSender = createNetworkId(
                KeyGeneration.generateDefaultEcKeyPair(),
                9999,
                "unexpected-key");
        MuSigCustomPayoutPsbtMessage message = new MuSigCustomPayoutPsbtMessage(
                "custom-payout-message",
                trade.getId(),
                MuSigProtocol.VERSION,
                unexpectedSender,
                trade.getMyself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult),
                "ab".repeat(32),
                new byte[]{1, 2, 3});

        buyerProtocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getEventQueue()).isEmpty();
        assertThat(trade.getPeer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenValidMediationResult_whenLocalTraderRejectsTwice_thenStoresAndSendsRejectionOnce() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        MuSigMediationResult mediationResult = setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new MediationResultRejectedEvent());
        protocol.handle(new MediationResultRejectedEvent());

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getMyself().isMediationResultRejected()).isTrue();
        ArgumentCaptor<MuSigMediationResultRejectionMessage> messageCaptor =
                ArgumentCaptor.forClass(MuSigMediationResultRejectionMessage.class);
        verify(networkService).confidentialSend(messageCaptor.capture(),
                eq(trade.getPeer().getNetworkId()),
                eq(trade.getMyIdentity().getNetworkIdWithKeyPair()));
        MuSigMediationResultRejectionMessage message = messageCaptor.getValue();
        assertThat(message.getTradeId()).isEqualTo(trade.getId());
        assertThat(message.getProtocolVersion()).isEqualTo(trade.getProtocolVersion());
        assertThat(message.getSender()).isEqualTo(trade.getMyself().getNetworkId());
        assertThat(message.getReceiver()).isEqualTo(trade.getPeer().getNetworkId());
        assertThat(message.getMediationResultHash()).containsExactly(
                MuSigMediationResultService.getMediationResultHash(mediationResult));
        verify(serviceProvider.getChatService().getMuSigOpenTradeChannelService())
                .findChannelByTradeId(trade.getId());
    }

    @Test
    void givenMissingMediationResult_whenLocalTraderRejects_thenIgnoresRejection() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        trade.getTradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_CLOSED);
        trade.getTradeDispute().setMediationResultSignature(new byte[]{1});
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new MediationResultRejectedEvent());

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getMyself().isMediationResultRejected()).isFalse();
        assertThat(trade.getEventQueue()).isEmpty();
        verify(networkService, never()).confidentialSend(any(), any(), any());
        verify(serviceProvider.getChatService().getMuSigOpenTradeChannelService(), never())
                .findChannelByTradeId(any());
    }

    @Test
    void givenNoPayoutMediationResult_whenLocalTraderRejects_thenIgnoresRejection() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        MuSigMediationResult mediationResult = new MuSigMediationResult(
                new byte[20],
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.NO_PAYOUT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        trade.getTradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_CLOSED);
        trade.getTradeDispute().setMuSigMediationResult(mediationResult);
        trade.getTradeDispute().setMediationResultSignature(new byte[]{1});
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new MediationResultRejectedEvent());

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getMyself().isMediationResultRejected()).isFalse();
        assertThat(trade.getEventQueue()).isEmpty();
        verify(networkService, never()).confidentialSend(any(), any(), any());
        verify(serviceProvider.getChatService().getMuSigOpenTradeChannelService(), never())
                .findChannelByTradeId(any());
    }

    @Test
    void givenLocalCustomPayoutData_whenLocalTraderRejects_thenIgnoresRejection() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        setCustomPayoutMediationResult(trade);
        CustomPayoutPsbt customPayoutPsbt = new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                "ab".repeat(32),
                59_900,
                39_900);
        trade.getMyself().setMyCustomPayoutPsbt(customPayoutPsbt);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(new MediationResultRejectedEvent());

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getMyself().isMediationResultRejected()).isFalse();
        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                .contains(customPayoutPsbt);
        assertThat(trade.getEventQueue()).isEmpty();
        verify(networkService, never()).confidentialSend(any(), any(), any());
        verify(serviceProvider.getChatService().getMuSigOpenTradeChannelService(), never())
                .findChannelByTradeId(any());
    }

    @Test
    void givenRepeatedMatchingPeerRejection_whenHandled_thenStoresItWithoutChangingTradeState() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        protocol.handle(createRejectionMessage(trade));
        protocol.handle(createRejectionMessage(trade));

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getPeer().isMediationResultRejected()).isTrue();
        assertThat(trade.getEventQueue()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mediationRejectionBoundaryTransitionCases")
    void givenTradeStateAllowsRejection_whenLocalTraderRejects_thenStoresWithoutChangingState(
            MediationRejectionTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                testCase.sourceState());
        setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);

        protocol.handle(new MediationResultRejectedEvent());

        assertThat(trade.getTradeState()).isEqualTo(testCase.sourceState());
        assertThat(trade.getMyself().isMediationResultRejected()).isTrue();
        assertThat(trade.getEventQueue()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mediationRejectionBoundaryTransitionCases")
    void givenTradeStateAllowsRejection_whenPeerRejects_thenStoresWithoutChangingState(
            MediationRejectionTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                testCase.sourceState());
        setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);

        protocol.handle(createRejectionMessage(trade));

        assertThat(trade.getTradeState()).isEqualTo(testCase.sourceState());
        assertThat(trade.getPeer().isMediationResultRejected()).isTrue();
        assertThat(trade.getEventQueue()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("customPayoutSignedTransitionCases")
    void givenCustomPayoutSigned_whenPeerRejects_thenStoresWithoutChangingState(
            CustomPayoutAcceptanceTransitionCase testCase) {
        MuSigTrade trade = copyTradeWithState(
                createTrade(testCase.isBuyer(), testCase.isTaker()),
                CUSTOM_PAYOUT_SIGNED);
        setCustomPayoutMediationResult(trade);
        trade.getMyself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                "ab".repeat(32),
                59_900,
                39_900));
        MuSigProtocol protocol = testCase.protocolFactory().apply(serviceProvider, trade);

        protocol.handle(createRejectionMessage(trade));

        assertThat(trade.getTradeState()).isEqualTo(CUSTOM_PAYOUT_SIGNED);
        assertThat(trade.getPeer().isMediationResultRejected()).isTrue();
        assertThat(trade.getEventQueue()).isEmpty();
    }

    @Test
    void givenMismatchingPeerRejection_whenHandled_thenIgnoresIt() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        byte[] mismatchingHash = createRejectionMessage(trade).getMediationResultHash();
        mismatchingHash[0] ^= 1;
        MuSigMediationResultRejectionMessage message = new MuSigMediationResultRejectionMessage(
                "rejection-message",
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getPeer().getNetworkId(),
                trade.getMyself().getNetworkId(),
                mismatchingHash);

        protocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getPeer().isMediationResultRejected()).isFalse();
        assertThat(trade.getEventQueue()).isEmpty();
    }

    @Test
    void givenPeerRejectionWithUnexpectedSender_whenHandled_thenIgnoresWithoutFailingTrade() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        MuSigMediationResult mediationResult = setCustomPayoutMediationResult(trade);
        MuSigProtocol protocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);
        NetworkId unexpectedSender = createNetworkId(
                KeyGeneration.generateDefaultEcKeyPair(),
                9999,
                "unexpected-rejection-key");
        MuSigMediationResultRejectionMessage message = new MuSigMediationResultRejectionMessage(
                "unexpected-rejection-message",
                trade.getId(),
                trade.getProtocolVersion(),
                unexpectedSender,
                trade.getMyself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult));

        protocol.handle(message);

        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
        assertThat(trade.getPeer().isMediationResultRejected()).isFalse();
        assertThat(trade.getEventQueue()).isEmpty();
    }

    @Test
    void givenPeerRejectionBeforePeerPsbt_whenHandled_thenRejectionWins() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        setCustomPayoutMediationResult(trade);
        MuSigProtocol buyerProtocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        buyerProtocol.handle(createRejectionMessage(trade));
        buyerProtocol.handle(createCustomPayoutPsbtMessage(trade));

        assertThat(trade.getPeer().isMediationResultRejected()).isTrue();
        assertThat(trade.getPeer().getCustomPayoutData()).isEmpty();
        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
    }

    @Test
    void givenPeerPsbtBeforePeerRejection_whenHandled_thenPsbtWins() {
        MuSigTrade trade = copyTradeWithState(createTrade(), BUYER_INITIATED_PAYMENT);
        setCustomPayoutMediationResult(trade);
        MuSigProtocol buyerProtocol = new MuSigBuyerAsMakerProtocol(serviceProvider, trade);

        buyerProtocol.handle(createCustomPayoutPsbtMessage(trade));
        buyerProtocol.handle(createRejectionMessage(trade));

        assertThat(trade.getPeer().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .contains(new PeerCustomPayoutPsbt("ab".repeat(32), new byte[]{1, 2, 3}));
        assertThat(trade.getPeer().isMediationResultRejected()).isFalse();
        assertThat(trade.getTradeState()).isEqualTo(BUYER_INITIATED_PAYMENT);
    }

    @Test
    void customPayoutSignedIsNonFinalAndCustomPayoutClosedIsFinal() {
        assertThat(CUSTOM_PAYOUT_SIGNED.isFinalState()).isFalse();
        assertThat(CUSTOM_PAYOUT_CLOSED_TRADE.isFinalState()).isTrue();
    }

    private static Stream<CustomPayoutAcceptanceTransitionCase> customPayoutAcceptanceTransitionCases() {
        return Stream.of(
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as maker after deposit confirmation",
                        true,
                        false,
                        DEPOSIT_TX_CONFIRMED,
                        MuSigBuyerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as maker after initiating payment",
                        true,
                        false,
                        BUYER_INITIATED_PAYMENT,
                        MuSigBuyerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as taker after deposit confirmation",
                        true,
                        true,
                        DEPOSIT_TX_CONFIRMED,
                        MuSigBuyerAsTakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as taker after initiating payment",
                        true,
                        true,
                        BUYER_INITIATED_PAYMENT,
                        MuSigBuyerAsTakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as maker after deposit confirmation",
                        false,
                        false,
                        DEPOSIT_TX_CONFIRMED,
                        MuSigSellerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as maker after receiving payment initiation",
                        false,
                        false,
                        SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE,
                        MuSigSellerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as taker after deposit confirmation",
                        false,
                        true,
                        DEPOSIT_TX_CONFIRMED,
                        MuSigSellerAsTakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as taker after receiving payment initiation",
                        false,
                        true,
                        SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE,
                        MuSigSellerAsTakerProtocol::new));
    }

    private static Stream<CustomPayoutAcceptanceTransitionCase> customPayoutReceiveTransitionCases() {
        return Stream.concat(
                customPayoutAcceptanceTransitionCases(),
                customPayoutSignedTransitionCases());
    }

    private static Stream<CustomPayoutAcceptanceTransitionCase> customPayoutPreConfirmationTransitionCases() {
        return Stream.of(
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as maker before deposit confirmation",
                        true,
                        false,
                        MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX,
                        MuSigBuyerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as taker before deposit confirmation",
                        true,
                        true,
                        TAKER_RECEIVED_ACCOUNT_PAYLOAD,
                        MuSigBuyerAsTakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as maker before deposit confirmation",
                        false,
                        false,
                        MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX,
                        MuSigSellerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as taker before deposit confirmation",
                        false,
                        true,
                        TAKER_RECEIVED_ACCOUNT_PAYLOAD,
                        MuSigSellerAsTakerProtocol::new));
    }

    private static Stream<CustomPayoutAcceptanceTransitionCase> sellerPostFTransitionCases() {
        return Stream.of(
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as maker after committing to normal closure",
                        false,
                        false,
                        SELLER_CONFIRMED_PAYMENT_RECEIPT,
                        MuSigSellerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as taker after committing to normal closure",
                        false,
                        true,
                        SELLER_CONFIRMED_PAYMENT_RECEIPT,
                        MuSigSellerAsTakerProtocol::new));
    }

    private static Stream<CustomPayoutAcceptanceTransitionCase> customPayoutSignedTransitionCases() {
        return Stream.of(
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as maker after signing custom payout",
                        true,
                        false,
                        CUSTOM_PAYOUT_SIGNED,
                        MuSigBuyerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "buyer as taker after signing custom payout",
                        true,
                        true,
                        CUSTOM_PAYOUT_SIGNED,
                        MuSigBuyerAsTakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as maker after signing custom payout",
                        false,
                        false,
                        CUSTOM_PAYOUT_SIGNED,
                        MuSigSellerAsMakerProtocol::new),
                new CustomPayoutAcceptanceTransitionCase(
                        "seller as taker after signing custom payout",
                        false,
                        true,
                        CUSTOM_PAYOUT_SIGNED,
                        MuSigSellerAsTakerProtocol::new));
    }

    private static Stream<MediationRejectionTransitionCase> mediationRejectionBoundaryTransitionCases() {
        return Stream.of(
                new MediationRejectionTransitionCase(
                        "buyer as maker once the deposit transaction is known",
                        true,
                        false,
                        MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX,
                        MuSigBuyerAsMakerProtocol::new),
                new MediationRejectionTransitionCase(
                        "buyer as taker once the deposit transaction is published",
                        true,
                        true,
                        TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX,
                        MuSigBuyerAsTakerProtocol::new),
                new MediationRejectionTransitionCase(
                        "seller as maker once the deposit transaction is known",
                        false,
                        false,
                        MAKER_RECEIVED_ACCOUNT_PAYLOAD_AND_DEPOSIT_TX,
                        MuSigSellerAsMakerProtocol::new),
                new MediationRejectionTransitionCase(
                        "seller as taker once the deposit transaction is published",
                        false,
                        true,
                        TAKER_SIGNED_AND_PUBLISHED_DEPOSIT_TX,
                        MuSigSellerAsTakerProtocol::new),
                new MediationRejectionTransitionCase(
                        "seller as maker after confirming payment receipt",
                        false,
                        false,
                        SELLER_CONFIRMED_PAYMENT_RECEIPT,
                        MuSigSellerAsMakerProtocol::new),
                new MediationRejectionTransitionCase(
                        "seller as taker after confirming payment receipt",
                        false,
                        true,
                        SELLER_CONFIRMED_PAYMENT_RECEIPT,
                        MuSigSellerAsTakerProtocol::new));
    }

    private CloseTradeResponse stubNormalTradeClosure() {
        CloseTradeResponse response = new CloseTradeResponse(new byte[]{7, 8, 9});
        when(blockingStub.closeTrade(any(CloseTradeRequest.class)))
                .thenReturn(response.toProto(false));
        return response;
    }

    private CustomPayoutPsbt stubCustomPayoutSigning(MuSigTrade trade) {
        setCustomPayoutMediationResult(trade);
        CustomPayoutPsbt customPayoutPsbt = new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                "ab".repeat(32),
                59_900,
                39_900);
        when(blockingStub.signCustomPayoutTx(any(CustomPayoutPsbtRequest.class)))
                .thenReturn(customPayoutPsbt.toProto(false));
        return customPayoutPsbt;
    }

    private static MuSigMediationResult setCustomPayoutMediationResult(MuSigTrade trade) {
        MuSigMediationResult mediationResult = new MuSigMediationResult(
                new byte[20],
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                Optional.of(60_000L),
                Optional.of(40_000L),
                Optional.empty(),
                Optional.empty());
        trade.getTradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_CLOSED);
        trade.getTradeDispute().setMuSigMediationResult(mediationResult);
        trade.getTradeDispute().setMediationResultSignature(new byte[]{1});
        return mediationResult;
    }

    private static MuSigTrade createTrade() {
        return createTrade(true, false);
    }

    private static MuSigTrade createTrade(boolean isBuyer, boolean isTaker) {
        KeyPair makerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId makerNetworkId = createNetworkId(makerKeyPair, 9997, "maker-key");
        KeyPair takerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId takerNetworkId = createNetworkId(takerKeyPair, 9998, "taker-key");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        MarketPriceSpec priceSpec = new MarketPriceSpec();
        boolean makerIsBuyer = isBuyer != isTaker;
        MuSigOffer offer = new MuSigOffer(
                "protocol-test-offer",
                makerNetworkId,
                makerIsBuyer ? Direction.BUY : Direction.SELL,
                market,
                new BaseSideFixedAmountSpec(100_000),
                priceSpec,
                List.of(paymentMethod),
                List.of(),
                MuSigProtocol.VERSION);
        PaymentMethodSpec<?> paymentMethodSpec =
                PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, market.getQuoteCurrencyCode());
        MuSigContract contract = new MuSigContract(
                1_700_000_000_000L,
                offer,
                takerNetworkId,
                100_000,
                3_500_000,
                paymentMethodSpec,
                new byte[20],
                Optional.empty(),
                Optional.empty(),
                priceSpec,
                0);
        NetworkId myNetworkId = isTaker ? takerNetworkId : makerNetworkId;
        KeyPair myKeyPair = isTaker ? takerKeyPair : makerKeyPair;
        Identity myIdentity = new Identity(
                isTaker ? "taker-identity" : "maker-identity",
                myNetworkId,
                new KeyBundle(
                        isTaker ? "taker-key-bundle" : "maker-key-bundle",
                        myKeyPair,
                        TorKeyGeneration.generateKeyPair(),
                        I2PKeyGeneration.generateKeyPair()));
        return new MuSigTrade(
                contract,
                isBuyer,
                isTaker,
                myIdentity,
                offer,
                takerNetworkId,
                makerNetworkId);
    }

    private static MuSigTrade copyTradeWithState(MuSigTrade trade, MuSigTradeState state) {
        MuSigTrade copy = MuSigTrade.fromProto(trade.toProto(false).toBuilder()
                .setState(state.name())
                .build());
        copy.setProtocolVersion(MuSigProtocol.VERSION);
        return copy;
    }

    private static PaymentReceivedMessage_F createPaymentReceivedMessage(MuSigTrade trade) {
        return createPaymentReceivedMessage(
                trade,
                "message-f",
                trade.getPeer().getNetworkId(),
                trade.getMyself().getNetworkId(),
                MuSigProtocol.VERSION);
    }

    private static MuSigCustomPayoutPsbtMessage createCustomPayoutPsbtMessage(MuSigTrade trade) {
        MuSigMediationResult mediationResult = trade.getTradeDispute()
                .getMuSigMediationResult()
                .orElseThrow();
        return new MuSigCustomPayoutPsbtMessage(
                "custom-payout-message",
                trade.getId(),
                MuSigProtocol.VERSION,
                trade.getPeer().getNetworkId(),
                trade.getMyself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult),
                "ab".repeat(32),
                new byte[]{1, 2, 3});
    }

    private static MuSigMediationResultRejectionMessage createRejectionMessage(MuSigTrade trade) {
        MuSigMediationResult mediationResult = trade.getTradeDispute()
                .getMuSigMediationResult()
                .orElseThrow();
        return new MuSigMediationResultRejectionMessage(
                "rejection-message",
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getPeer().getNetworkId(),
                trade.getMyself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult));
    }

    private static PaymentReceivedMessage_F createPaymentReceivedMessage(MuSigTrade trade,
                                                                          String messageId,
                                                                          NetworkId sender,
                                                                          NetworkId receiver,
                                                                          String protocolVersion) {
        SwapTxSignature swapTxSignature = SwapTxSignature.fromProto(
                bisq.trade.protobuf.SwapTxSignature.newBuilder()
                        .setSwapTx(ByteString.copyFrom(new byte[]{1, 2, 3}))
                        .setPeerOutputPrvKeyShare(ByteString.copyFrom(new byte[]{4, 5, 6}))
                        .build());
        return new PaymentReceivedMessage_F(
                messageId,
                trade.getId(),
                protocolVersion,
                sender,
                receiver,
                swapTxSignature);
    }

    private static NetworkId createNetworkId(KeyPair keyPair, int port, String keyId) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        return new NetworkId(addresses, new PubKey(keyPair.getPublic(), keyId));
    }

    private record CustomPayoutAcceptanceTransitionCase(
            String description,
            boolean isBuyer,
            boolean isTaker,
            MuSigTradeState sourceState,
            BiFunction<ServiceProvider, MuSigTrade, MuSigProtocol> protocolFactory) {
        @Override
        public String toString() {
            return description;
        }
    }

    private record MediationRejectionTransitionCase(
            String description,
            boolean isBuyer,
            boolean isTaker,
            MuSigTradeState sourceState,
            BiFunction<ServiceProvider, MuSigTrade, MuSigProtocol> protocolFactory) {
        @Override
        public String toString() {
            return description;
        }
    }
}
