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

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.mu_sig.open_trades.MuSigDisputeAgentType;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.support.arbitration.ArbitrationCaseState;
import bisq.support.arbitration.mu_sig.MuSigArbitrationStateChangeMessage;
import bisq.support.mediation.MediationCaseState;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.dispute.mu_sig.MuSigDisputeCasePaymentDetailsRequest;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultAcceptanceMessage;
import bisq.support.mediation.mu_sig.MuSigMediationStateChangeMessage;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.arbitration.MuSigTraderArbitrationService;
import bisq.trade.mu_sig.mediation.MuSigTraderMediationService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MuSigTradeDisputeServiceTest {
    private static final AtomicInteger FIXTURE_SEQUENCE = new AtomicInteger();

    private MuSigOpenTradeChannelService muSigOpenTradeChannelService;
    private MuSigTraderMediationService muSigTraderMediationService;
    private MuSigTraderArbitrationService muSigTraderArbitrationService;
    private AtomicInteger persistCalls;
    private Map<String, MuSigTrade> tradeById;
    private MuSigTradeDisputeService service;

    @BeforeEach
    void setUp() {
        BannedUserService bannedUserService = mock(BannedUserService.class);
        muSigOpenTradeChannelService = mock(MuSigOpenTradeChannelService.class);
        muSigTraderMediationService = mock(MuSigTraderMediationService.class);
        muSigTraderArbitrationService = mock(MuSigTraderArbitrationService.class);
        persistCalls = new AtomicInteger();
        tradeById = new HashMap<>();

        service = new MuSigTradeDisputeService(
                bannedUserService,
                muSigOpenTradeChannelService,
                muSigTraderMediationService,
                muSigTraderArbitrationService,
                tradeId -> Optional.ofNullable(tradeById.get(tradeId)),
                persistCalls::incrementAndGet
        );
    }

    @Test
    @DisplayName("given trade with mediator and no dispute when request mediation then sets requested persists and delegates")
    void given_trade_with_mediator_and_no_dispute_when_request_mediation_then_sets_requested_persists_and_delegates() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.NO_DISPUTE, true, false);
        MuSigOpenTradeChannel channel = stubOpenTradeChannel(fixture);

        service.requestMediation(fixture.trade());

        assertThat(fixture.tradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.MEDIATION_REQUESTED);
        assertThat(persistCalls.get()).isEqualTo(1);
        verify(muSigTraderMediationService).requestMediation(
                fixture.tradeId(),
                fixture.identity(),
                fixture.peer(),
                fixture.mediator().orElseThrow(),
                fixture.contract(),
                channel);
    }

    @Test
    @DisplayName("given trade without mediator when request mediation then keeps state and does not delegate")
    void given_trade_without_mediator_when_request_mediation_then_keeps_state_and_does_not_delegate() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.NO_DISPUTE, false, false);
        stubOpenTradeChannel(fixture);

        service.requestMediation(fixture.trade());

        assertThat(fixture.tradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.NO_DISPUTE);
        assertThat(persistCalls.get()).isZero();
        verifyNoInteractions(muSigTraderMediationService);
    }

    @Test
    @DisplayName("given trade already in dispute when request mediation then keeps state and does not delegate")
    void given_trade_already_in_dispute_when_request_mediation_then_keeps_state_and_does_not_delegate() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_OPEN, true, false);
        stubOpenTradeChannel(fixture);

        service.requestMediation(fixture.trade());

        assertThat(fixture.tradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.MEDIATION_OPEN);
        assertThat(persistCalls.get()).isZero();
        verifyNoInteractions(muSigTraderMediationService);
    }

    @Test
    @DisplayName("given trade without mediation result when accept mediation result then throws and does not persist")
    void given_trade_without_mediation_result_when_accept_mediation_result_then_throws_and_does_not_persist() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_OPEN, true, false);
        stubOpenTradeChannel(fixture);

        assertThatThrownBy(() -> service.acceptMediationResult(fixture.trade()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(persistCalls.get()).isZero();
        verifyNoInteractions(muSigTraderMediationService);
    }

    @Test
    @DisplayName("given trade with mediation result when accept mediation result then marks accepted persists and delegates")
    void given_trade_with_mediation_result_when_accept_mediation_result_then_marks_accepted_persists_and_delegates() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_CLOSED, true, false);
        MuSigOpenTradeChannel channel = stubOpenTradeChannel(fixture);
        fixture.tradeDispute().setMuSigMediationResult(createMediationResult());

        service.acceptMediationResult(fixture.trade());

        assertThat(fixture.myself().getMediationResultAccepted()).contains(true);
        assertThat(persistCalls.get()).isEqualTo(1);
        verify(muSigTraderMediationService).sendMediationResultAcceptanceMessage(
                fixture.tradeId(),
                fixture.identity(),
                fixture.peer(),
                true,
                channel);
    }

    @Test
    @DisplayName("given trade with mediation result when reject mediation result then marks rejected persists and delegates")
    void given_trade_with_mediation_result_when_reject_mediation_result_then_marks_rejected_persists_and_delegates() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_CLOSED, true, false);
        MuSigOpenTradeChannel channel = stubOpenTradeChannel(fixture);
        fixture.tradeDispute().setMuSigMediationResult(createMediationResult());

        service.rejectMediationResult(fixture.trade());

        assertThat(fixture.myself().getMediationResultAccepted()).contains(false);
        assertThat(persistCalls.get()).isEqualTo(1);
        verify(muSigTraderMediationService).sendMediationResultAcceptanceMessage(
                fixture.tradeId(),
                fixture.identity(),
                fixture.peer(),
                false,
                channel);
    }

    @Test
    @DisplayName("given trade with arbitrator and closed mediation when request arbitration then sets requested persists and delegates")
    void given_trade_with_arbitrator_and_closed_mediation_when_request_arbitration_then_sets_requested_persists_and_delegates() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_CLOSED, true, true);
        MuSigOpenTradeChannel channel = stubOpenTradeChannel(fixture);
        fixture.tradeDispute().setMuSigMediationResult(createMediationResult());
        byte[] mediationResultSignature = signatureBytes();
        fixture.tradeDispute().setMediationResultSignature(mediationResultSignature);

        service.requestArbitration(fixture.trade());

        assertThat(fixture.tradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.ARBITRATION_REQUESTED);
        assertThat(persistCalls.get()).isEqualTo(1);
        verify(muSigTraderArbitrationService).requestArbitration(
                fixture.tradeId(),
                fixture.identity(),
                fixture.peer(),
                fixture.arbitrator().orElseThrow(),
                fixture.contract(),
                fixture.tradeDispute().getMuSigMediationResult().orElseThrow(),
                mediationResultSignature,
                channel);
    }

    @Test
    @DisplayName("given trade not mediation closed when request arbitration then keeps state and does not delegate")
    void given_trade_not_mediation_closed_when_request_arbitration_then_keeps_state_and_does_not_delegate() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_OPEN, true, true);
        stubOpenTradeChannel(fixture);

        service.requestArbitration(fixture.trade());

        assertThat(fixture.tradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.MEDIATION_OPEN);
        assertThat(persistCalls.get()).isZero();
        verifyNoInteractions(muSigTraderArbitrationService);
    }

    @Test
    @DisplayName("given authorized open mediation message when on dispute message then transitions to mediation open and persists")
    void given_authorized_open_mediation_message_when_on_dispute_message_then_transitions_to_mediation_open_and_persists() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_REQUESTED, true, false);
        tradeById.put(fixture.tradeId(), fixture.trade());
        MuSigOpenTradeChannel channel = stubOpenTradeChannel(fixture);

        MuSigMediationStateChangeMessage message = new MuSigMediationStateChangeMessage(
                "msg-8",
                fixture.tradeId(),
                fixture.mediator().orElseThrow(),
                MediationCaseState.OPEN,
                Optional.empty(),
                Optional.empty()
        );

        service.onDisputeMessage(message);

        assertThat(fixture.tradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.MEDIATION_OPEN);
        assertThat(persistCalls.get()).isEqualTo(1);
        verify(muSigTraderMediationService).applyMediationStateToChannel(
                fixture.tradeId(),
                MuSigDisputeState.MEDIATION_OPEN,
                MuSigDisputeState.MEDIATION_REQUESTED,
                channel);
    }

    @Test
    @DisplayName("given queued payment details request without channel when maybe process pending dispute messages then replays and sends response")
    void given_queued_payment_details_request_without_channel_when_maybe_process_pending_dispute_messages_then_replays_and_sends_response() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_OPEN, true, false);
        fixture.taker().setAccountPayload(createNationalBankPayload("taker-9", "DE901"));
        fixture.maker().setAccountPayload(createNationalBankPayload("maker-9", "DE902"));
        tradeById.put(fixture.tradeId(), fixture.trade());
        when(muSigOpenTradeChannelService.findChannelByTradeId(fixture.tradeId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createOpenTradeChannel(fixture)));

        MuSigDisputeCasePaymentDetailsRequest message =
                new MuSigDisputeCasePaymentDetailsRequest(fixture.tradeId(), fixture.mediator().orElseThrow());

        service.onDisputeMessage(message);
        assertThat(persistCalls.get()).isZero();

        service.maybeProcessPendingDisputeMessages(fixture.tradeId());

        verify(muSigTraderMediationService).sendDisputeCasePaymentDetailsResponse(
                fixture.tradeId(),
                fixture.identity(),
                fixture.mediator().orElseThrow(),
                fixture.taker().getAccountPayload().orElseThrow(),
                fixture.maker().getAccountPayload().orElseThrow());
    }

    @Test
    @DisplayName("given queued acceptance without mediation result when maybe process pending dispute messages then marks peer acceptance")
    void given_queued_acceptance_without_mediation_result_when_maybe_process_pending_dispute_messages_then_marks_peer_acceptance() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_OPEN, true, false);
        tradeById.put(fixture.tradeId(), fixture.trade());
        when(muSigOpenTradeChannelService.findChannelByTradeId(fixture.tradeId())).thenReturn(Optional.of(createOpenTradeChannel(fixture)));

        MuSigMediationResultAcceptanceMessage message =
                new MuSigMediationResultAcceptanceMessage(fixture.tradeId(), fixture.takerProfile(), true);

        service.onDisputeMessage(message);
        assertThat(fixture.peer().getMediationResultAccepted()).isEmpty();
        assertThat(persistCalls.get()).isZero();

        fixture.tradeDispute().setMuSigMediationResult(createMediationResult());
        service.maybeProcessPendingDisputeMessages(fixture.tradeId());

        assertThat(fixture.peer().getMediationResultAccepted()).contains(true);
        assertThat(persistCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("given authorized open arbitration message when on dispute message then transitions to arbitration open and persists")
    void given_authorized_open_arbitration_message_when_on_dispute_message_then_transitions_to_arbitration_open_and_persists() {
        TradeFixture fixture = createTradeFixture(MuSigDisputeState.MEDIATION_CLOSED, true, true);
        tradeById.put(fixture.tradeId(), fixture.trade());
        MuSigOpenTradeChannel channel = stubOpenTradeChannel(fixture);

        MuSigArbitrationStateChangeMessage message = new MuSigArbitrationStateChangeMessage(
                "msg-11",
                fixture.tradeId(),
                fixture.arbitrator().orElseThrow(),
                ArbitrationCaseState.OPEN,
                Optional.empty(),
                Optional.empty()
        );

        service.onDisputeMessage(message);

        assertThat(fixture.tradeDispute().getDisputeState()).isEqualTo(MuSigDisputeState.ARBITRATION_OPEN);
        assertThat(persistCalls.get()).isEqualTo(1);
        verify(muSigTraderArbitrationService).applyArbitrationStateToChannel(
                fixture.tradeId(),
                MuSigDisputeState.ARBITRATION_OPEN,
                MuSigDisputeState.MEDIATION_CLOSED,
                channel);
    }

    private TradeFixture createTradeFixture(MuSigDisputeState disputeState,
                                            boolean includeMediator,
                                            boolean includeArbitrator) {
        int index = FIXTURE_SEQUENCE.incrementAndGet();
        String tradeId = UUID.randomUUID().toString();
        int offset = 100 + index;
        UserProfile maker = createUserProfile(20_000 + offset);
        UserProfile taker = createUserProfile(21_000 + offset);
        Optional<UserProfile> mediator = includeMediator ? Optional.of(createUserProfile(22_000 + offset)) : Optional.empty();
        Optional<UserProfile> arbitrator = includeArbitrator ? Optional.of(createUserProfile(23_000 + offset)) : Optional.empty();

        AccountPayload<?> takerPayload = createNationalBankPayload("taker-" + index, "DE" + offset + "01");
        AccountPayload<?> makerPayload = createNationalBankPayload("maker-" + index, "DE" + offset + "02");
        MuSigContract contract = createContract(maker, taker, mediator, arbitrator, "offer-" + tradeId, takerPayload, makerPayload);

        Identity identity = createIdentity(maker.getNetworkId(), "identity-" + tradeId);
        MuSigTrade trade = new MuSigTrade(
                contract,
                true,
                false,
                identity,
                contract.getOffer(),
                taker.getNetworkId(),
                maker.getNetworkId()
        );
        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        tradeDispute.setDisputeState(disputeState);
        MuSigTradeParty myself = trade.getMyself();
        MuSigTradeParty peer = trade.getPeer();
        MuSigTradeParty takerParty = trade.getTaker();
        MuSigTradeParty makerParty = trade.getMaker();

        return new TradeFixture(trade.getId(), trade, tradeDispute, contract, identity, myself, peer, takerParty, makerParty, maker, taker, mediator, arbitrator);
    }

    private MuSigContract createContract(UserProfile maker,
                                         UserProfile taker,
                                         Optional<UserProfile> mediator,
                                         Optional<UserProfile> arbitrator,
                                         String offerId,
                                         AccountPayload<?> takerPayloadForHash,
                                         AccountPayload<?> makerPayloadForHash) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        List<AccountOption> accountOptions = List.of(new AccountOption(
                paymentMethod,
                "0123456789abcdef0123456789abcdef01234567",
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(),
                OfferOptionUtil.createSaltedAccountPayloadHash(makerPayloadForHash, offerId)
        ));
        MuSigOffer offer = new MuSigOffer(
                offerId,
                maker.getNetworkId(),
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                accountOptions,
                "1.0.0"
        );
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, "EUR");
        return new MuSigContract(
                System.currentTimeMillis(),
                offer,
                taker.getNetworkId(),
                100_000L,
                3_500_000L,
                quoteSidePaymentMethodSpec,
                OfferOptionUtil.createSaltedAccountPayloadHash(takerPayloadForHash, offerId),
                mediator,
                arbitrator,
                createPriceSpec(),
                0
        );
    }

    private PriceSpec createPriceSpec() {
        return new MarketPriceSpec();
    }

    private UserProfile createUserProfile(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        NetworkId networkId = new NetworkId(addresses, pubKey);
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "nick-" + port, proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private Identity createIdentity(NetworkId networkId, String tag) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyBundle keyBundle = new KeyBundle(
                tag,
                keyPair,
                TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair()
        );
        return new Identity(tag, networkId, keyBundle);
    }

    private MuSigOpenTradeChannel createOpenTradeChannel(TradeFixture fixture) {
        return MuSigOpenTradeChannel.create(
                fixture.tradeId(),
                new UserIdentity(fixture.identity(), fixture.makerProfile()),
                java.util.Set.of(fixture.makerProfile(), fixture.takerProfile()),
                fixture.mediator(),
                fixture.arbitrator(),
                MuSigDisputeAgentType.NONE
        );
    }

    private MuSigOpenTradeChannel stubOpenTradeChannel(TradeFixture fixture) {
        MuSigOpenTradeChannel channel = createOpenTradeChannel(fixture);
        when(muSigOpenTradeChannelService.findChannelByTradeId(fixture.tradeId())).thenReturn(Optional.of(channel));
        return channel;
    }

    private AccountPayload<?> createNationalBankPayload(String id, String accountNr) {
        return new NationalBankAccountPayload(
                id,
                "DE",
                "EUR",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                accountNr,
                Optional.empty(),
                Optional.empty()
        );
    }

    private MuSigMediationResult createMediationResult() {
        return new MuSigMediationResult(
                new byte[20],
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                Optional.of(60_000L),
                Optional.of(40_000L),
                Optional.empty(),
                Optional.empty()
        );
    }

    private byte[] signatureBytes() {
        return new byte[70];
    }

    private record TradeFixture(String tradeId,
                                MuSigTrade trade,
                                MuSigTradeDispute tradeDispute,
                                MuSigContract contract,
                                Identity identity,
                                MuSigTradeParty myself,
                                MuSigTradeParty peer,
                                MuSigTradeParty taker,
                                MuSigTradeParty maker,
                                UserProfile makerProfile,
                                UserProfile takerProfile,
                                Optional<UserProfile> mediator,
                                Optional<UserProfile> arbitrator) {
    }
}
