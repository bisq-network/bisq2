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

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.pow.ProofOfWork;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResultService;
import bisq.trade.MuSigDisputeState;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import bisq.trade.mu_sig.messages.network.MuSigMediationResultRejectionMessage;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.mu_sig.protocol.MuSigTradeState;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MuSigMediationCustomPayoutServiceTest {
    private static final String TX_ID = "ab".repeat(32);
    private static final long TRADE_AMOUNT = 100_000;
    private static final long BUYER_SECURITY_DEPOSIT = 25_000;
    private static final long SELLER_SECURITY_DEPOSIT = 25_000;
    private static final Market MARKET = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private static final Set<MuSigTradeState> ALLOWED_TRADE_STATES = EnumSet.of(
            MuSigTradeState.DEPOSIT_TX_CONFIRMED,
            MuSigTradeState.BUYER_INITIATED_PAYMENT,
            MuSigTradeState.SELLER_RECEIVED_INITIATED_PAYMENT_MESSAGE);
    private static final ResultDefinition VALID_CUSTOM_PAYOUT = new ResultDefinition(
            MediationPayoutDistributionType.CUSTOM_PAYOUT,
            Optional.of(90_000L),
            Optional.of(60_000L),
            Optional.empty());

    private MuSigMediationCustomPayoutService service;

    @BeforeEach
    void setUp() {
        service = new MuSigMediationCustomPayoutService();
    }

    @Test
    void givenAllTradeStates_whenCheckingSigningEligibility_thenOnlyAllowsSigningWindow()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();

        for (MuSigTradeState state : MuSigTradeState.values()) {
            MuSigTrade trade = copyTradeWithState(fixture.trade(), state);

            assertThat(service.canSignCustomPayout(trade))
                    .as("custom payout signing in state %s", state)
                    .isEqualTo(ALLOWED_TRADE_STATES.contains(state));
        }
    }

    @Test
    void givenMediationIsNotClosed_whenCheckingSigningEligibility_thenRejectsSigning()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();
        fixture.tradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_RE_OPENED);

        assertThat(service.canSignCustomPayout(fixture.trade())).isFalse();
    }

    @Test
    void givenMissingResultOrSignature_whenCheckingSigningEligibility_thenRejectsSigning()
            throws GeneralSecurityException {
        SigningFixture missingResult = createSigningFixture(Optional.empty(), SignatureState.MISSING, true);
        SigningFixture missingSignature = createSigningFixture(
                Optional.of(VALID_CUSTOM_PAYOUT), SignatureState.MISSING, true);

        assertThat(service.canSignCustomPayout(missingResult.trade())).isFalse();
        assertThat(service.canSignCustomPayout(missingSignature.trade())).isFalse();
    }

    @Test
    void givenUnverifiableSignedResult_whenCheckingSigningEligibility_thenRejectsSigning()
            throws GeneralSecurityException {
        SigningFixture invalidSignature = createSigningFixture(
                Optional.of(VALID_CUSTOM_PAYOUT), SignatureState.INVALID, true);
        SigningFixture mismatchingContractHash = createSigningFixture(
                Optional.of(VALID_CUSTOM_PAYOUT), SignatureState.VALID, false);
        SigningFixture missingMediator = createSigningFixtureWithoutMediator();

        assertThat(service.canSignCustomPayout(invalidSignature.trade())).isFalse();
        assertThat(service.canSignCustomPayout(mismatchingContractHash.trade())).isFalse();
        assertThat(service.canSignCustomPayout(missingMediator.trade())).isFalse();
    }

    @Test
    void givenNoPayoutResult_whenCheckingSigningEligibility_thenRejectsSigning()
            throws GeneralSecurityException {
        SigningFixture fixture = createSigningFixture(
                Optional.of(new ResultDefinition(
                        MediationPayoutDistributionType.NO_PAYOUT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty())),
                SignatureState.VALID,
                true);

        assertThat(service.canSignCustomPayout(fixture.trade())).isFalse();
    }

    @Test
    void givenValidPredefinedPayoutResult_whenCheckingSigningEligibility_thenAllowsSigning()
            throws GeneralSecurityException {
        SigningFixture fixture = createSigningFixture(
                Optional.of(new ResultDefinition(
                        MediationPayoutDistributionType.BUYER_GETS_TRADE_AMOUNT,
                        Optional.of(TRADE_AMOUNT + BUYER_SECURITY_DEPOSIT),
                        Optional.of(SELLER_SECURITY_DEPOSIT),
                        Optional.empty())),
                SignatureState.VALID,
                true);

        assertThat(service.canSignCustomPayout(fixture.trade())).isTrue();
    }

    @Test
    void givenMissingPayoutContextOrInvalidPayoutAmounts_whenCheckingSigningEligibility_thenRejectsSigning()
            throws GeneralSecurityException {
        SigningFixture missingPayoutContext = createSigningFixtureWithoutPayoutContext();
        SigningFixture invalidPayoutAmounts = createSigningFixture(
                Optional.of(new ResultDefinition(
                        MediationPayoutDistributionType.CUSTOM_PAYOUT,
                        Optional.of(90_000L),
                        Optional.of(59_000L),
                        Optional.empty())),
                SignatureState.VALID,
                true);

        assertThat(service.canSignCustomPayout(missingPayoutContext.trade())).isFalse();
        assertThat(service.canSignCustomPayout(invalidPayoutAmounts.trade())).isFalse();
    }

    @Test
    void givenEitherPartyRejectedResult_whenCheckingSigningEligibility_thenRejectsSigning()
            throws GeneralSecurityException {
        SigningFixture localRejection = createValidSigningFixture();
        localRejection.myself().setMediationResultRejected();
        SigningFixture peerRejection = createValidSigningFixture();
        peerRejection.peer().setMediationResultRejected();

        assertThat(service.canSignCustomPayout(localRejection.trade())).isFalse();
        assertThat(service.canSignCustomPayout(peerRejection.trade())).isFalse();
    }

    @Test
    void givenLocalCustomPayoutAlreadyExists_whenCheckingSigningEligibility_thenRejectsSigning()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();
        fixture.myself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                TX_ID,
                90_000,
                60_000));

        assertThat(service.canSignCustomPayout(fixture.trade())).isFalse();
    }

    @Test
    void givenPeerCustomPayoutAlreadyExists_whenCheckingSigningEligibility_thenStillAllowsSigning()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();
        fixture.peer().setPeersCustomPayoutPsbt(new PeerCustomPayoutPsbt(TX_ID, new byte[]{1, 2, 3}));

        assertThat(service.canSignCustomPayout(fixture.trade())).isTrue();
    }

    @Test
    void givenPeerPsbtArrivesFirst_whenLocalPsbtIsStored_thenCustomPayoutCanBeFinalized()
            throws GeneralSecurityException {
        MuSigTrade trade = createCustomPayoutSignedTrade();
        trade.getPeer().setPeersCustomPayoutPsbt(
                new PeerCustomPayoutPsbt(TX_ID, new byte[]{1, 2, 3}));

        assertThat(service.canFinalizeCustomPayout(trade)).isFalse();

        trade.getMyself().setMyCustomPayoutPsbt(createLocalCustomPayoutPsbt(TX_ID));

        assertThat(service.canFinalizeCustomPayout(trade)).isTrue();
    }

    @Test
    void givenLocalPsbtExistsFirst_whenPeerPsbtIsStored_thenCustomPayoutCanBeFinalized()
            throws GeneralSecurityException {
        MuSigTrade trade = createCustomPayoutSignedTrade();
        trade.getMyself().setMyCustomPayoutPsbt(createLocalCustomPayoutPsbt(TX_ID));

        assertThat(service.canFinalizeCustomPayout(trade)).isFalse();

        trade.getPeer().setPeersCustomPayoutPsbt(
                new PeerCustomPayoutPsbt(TX_ID, new byte[]{1, 2, 3}));

        assertThat(service.canFinalizeCustomPayout(trade)).isTrue();
    }

    @Test
    void givenMismatchingTransactionIds_whenCheckingFinalizationReadiness_thenRejectsFinalization()
            throws GeneralSecurityException {
        MuSigTrade trade = createCustomPayoutSignedTrade();
        trade.getMyself().setMyCustomPayoutPsbt(createLocalCustomPayoutPsbt(TX_ID));
        trade.getPeer().setPeersCustomPayoutPsbt(
                new PeerCustomPayoutPsbt("cd".repeat(32), new byte[]{1, 2, 3}));

        assertThat(service.canFinalizeCustomPayout(trade)).isFalse();
    }

    @Test
    void givenMatchingPsbtsOutsideCustomPayoutSignedState_whenCheckingFinalizationReadiness_thenRejectsFinalization()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();
        fixture.myself().setMyCustomPayoutPsbt(createLocalCustomPayoutPsbt(TX_ID));
        fixture.peer().setPeersCustomPayoutPsbt(
                new PeerCustomPayoutPsbt(TX_ID, new byte[]{1, 2, 3}));

        assertThat(service.canFinalizeCustomPayout(fixture.trade())).isFalse();
    }

    @Test
    void givenSignedMediationResultAndProtocolVersion_whenCheckingMessageContext_thenContextIsAvailable()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();

        assertThat(service.hasRequiredMessageContext(fixture.trade())).isTrue();
    }

    @Test
    void givenIncompleteTradeData_whenCheckingMessageContext_thenContextIsUnavailable()
            throws GeneralSecurityException {
        SigningFixture missingResult = createSigningFixture(Optional.empty(), SignatureState.MISSING, true);
        SigningFixture missingSignature = createSigningFixture(
                Optional.of(VALID_CUSTOM_PAYOUT), SignatureState.MISSING, true);
        SigningFixture missingProtocolVersion = createValidSigningFixture();
        missingProtocolVersion.trade().setProtocolVersion(null);

        assertThat(service.hasRequiredMessageContext(missingResult.trade())).isFalse();
        assertThat(service.hasRequiredMessageContext(missingSignature.trade())).isFalse();
        assertThat(service.hasRequiredMessageContext(missingProtocolVersion.trade())).isFalse();
    }

    @Test
    void givenPendingMessages_whenRemovingAndClearingTrade_thenUpdatesPendingMessages()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();
        MuSigCustomPayoutPsbtMessage customPayoutMessage = createMessage(fixture);
        MuSigMediationResultRejectionMessage rejectionMessage = createRejectionMessage(fixture);

        service.addPendingMessage(customPayoutMessage);
        service.addPendingMessage(customPayoutMessage);
        service.addPendingMessage(rejectionMessage);

        assertThat(service.getPendingMessagesInReplayOrder(fixture.trade().getId()))
                .containsExactly(rejectionMessage, customPayoutMessage);

        service.removePendingMessage(customPayoutMessage);

        assertThat(service.getPendingMessagesInReplayOrder(fixture.trade().getId()))
                .containsExactly(rejectionMessage);

        service.clearTrade(fixture.trade().getId());

        assertThat(service.getPendingMessagesInReplayOrder(fixture.trade().getId())).isEmpty();
    }

    @Test
    void givenConflictingPendingPsbts_whenReadingPendingMessages_thenPreservesArrivalOrder()
            throws GeneralSecurityException {
        SigningFixture fixture = createValidSigningFixture();
        MuSigCustomPayoutPsbtMessage first = createMessage(fixture);
        MuSigCustomPayoutPsbtMessage second = new MuSigCustomPayoutPsbtMessage(
                "second-message-id",
                fixture.trade().getId(),
                fixture.trade().getProtocolVersion(),
                fixture.peer().getNetworkId(),
                fixture.myself().getNetworkId(),
                first.getMediationResultHash(),
                TX_ID,
                new byte[]{4, 5, 6});

        service.addPendingMessage(first);
        service.addPendingMessage(second);

        assertThat(service.getPendingMessagesInReplayOrder(fixture.trade().getId()))
                .containsExactly(first, second);
    }

    private SigningFixture createValidSigningFixture() throws GeneralSecurityException {
        return createSigningFixture(Optional.of(VALID_CUSTOM_PAYOUT), SignatureState.VALID, true);
    }

    private MuSigTrade createCustomPayoutSignedTrade() throws GeneralSecurityException {
        return copyTradeWithState(createValidSigningFixture().trade(), MuSigTradeState.CUSTOM_PAYOUT_SIGNED);
    }

    private static CustomPayoutPsbt createLocalCustomPayoutPsbt(String txId) {
        return new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                txId,
                89_900,
                59_900);
    }

    private static MuSigCustomPayoutPsbtMessage createMessage(SigningFixture fixture) {
        MuSigMediationResult mediationResult = fixture.tradeDispute()
                .getMuSigMediationResult()
                .orElseThrow();
        return new MuSigCustomPayoutPsbtMessage(
                "message-id",
                fixture.trade().getId(),
                fixture.trade().getProtocolVersion(),
                fixture.peer().getNetworkId(),
                fixture.myself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult),
                TX_ID,
                new byte[]{1, 2, 3});
    }

    private static MuSigMediationResultRejectionMessage createRejectionMessage(SigningFixture fixture) {
        MuSigMediationResult mediationResult = fixture.tradeDispute()
                .getMuSigMediationResult()
                .orElseThrow();
        return new MuSigMediationResultRejectionMessage(
                "rejection-message-id",
                fixture.trade().getId(),
                fixture.trade().getProtocolVersion(),
                fixture.peer().getNetworkId(),
                fixture.myself().getNetworkId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult));
    }

    private SigningFixture createSigningFixtureWithoutMediator() throws GeneralSecurityException {
        return createSigningFixture(
                Optional.of(VALID_CUSTOM_PAYOUT), SignatureState.VALID, true, false, true);
    }

    private SigningFixture createSigningFixtureWithoutPayoutContext() throws GeneralSecurityException {
        return createSigningFixture(
                Optional.of(VALID_CUSTOM_PAYOUT), SignatureState.VALID, true, true, false);
    }

    private SigningFixture createSigningFixture(Optional<ResultDefinition> optionalResultDefinition,
                                                SignatureState signatureState,
                                                boolean matchingContractHash)
            throws GeneralSecurityException {
        return createSigningFixture(optionalResultDefinition, signatureState, matchingContractHash, true, true);
    }

    private SigningFixture createSigningFixture(Optional<ResultDefinition> optionalResultDefinition,
                                                SignatureState signatureState,
                                                boolean matchingContractHash,
                                                boolean includeMediator,
                                                boolean includePayoutContext)
            throws GeneralSecurityException {
        KeyPair mediatorKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        UserProfile mediator = createUserProfile(mediatorKeyPair, 9996, "mediator-key");
        Identity myIdentity = createIdentity(9997, "my-key");
        NetworkId peerNetworkId = createNetworkId(
                KeyGeneration.generateDefaultEcKeyPair(), 9998, "peer-key");
        MuSigContract contract = createContract(
                myIdentity.getNetworkId(),
                peerNetworkId,
                includeMediator ? Optional.of(mediator) : Optional.empty(),
                includePayoutContext);
        MuSigTrade trade = createTrade(
                contract, myIdentity, peerNetworkId, MuSigTradeState.DEPOSIT_TX_CONFIRMED);
        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        tradeDispute.setDisputeState(MuSigDisputeState.MEDIATION_CLOSED);
        if (optionalResultDefinition.isPresent()) {
            byte[] contractHash = ContractService.getContractHash(trade.getContract());
            if (!matchingContractHash) {
                contractHash = Arrays.copyOf(contractHash, contractHash.length);
                contractHash[0] ^= 1;
            }
            MuSigMediationResult mediationResult = createMediationResult(
                    contractHash,
                    optionalResultDefinition.orElseThrow());
            tradeDispute.setMuSigMediationResult(mediationResult);
            if (signatureState != SignatureState.MISSING) {
                KeyPair signingKeyPair = signatureState == SignatureState.VALID
                        ? mediatorKeyPair
                        : KeyGeneration.generateDefaultEcKeyPair();
                tradeDispute.setMediationResultSignature(
                        MuSigMediationResultService.signMediationResult(mediationResult, signingKeyPair));
            }
        }

        return new SigningFixture(trade, tradeDispute, trade.getMyself(), trade.getPeer());
    }

    private static MuSigMediationResult createMediationResult(byte[] contractHash,
                                                               ResultDefinition definition) {
        return new MuSigMediationResult(
                contractHash,
                MediationResultReason.OTHER,
                definition.payoutDistributionType(),
                definition.buyerPayoutAmount(),
                definition.sellerPayoutAmount(),
                definition.payoutAdjustmentPercentage(),
                Optional.empty());
    }

    private static MuSigContract createContract(NetworkId makerNetworkId,
                                                NetworkId takerNetworkId,
                                                Optional<UserProfile> mediator,
                                                boolean includePayoutContext) {
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        List<CollateralOption> offerOptions = includePayoutContext
                ? List.of(new CollateralOption(0.25, 0.25))
                : List.of();
        PriceSpec priceSpec = new MarketPriceSpec();
        MuSigOffer offer = new MuSigOffer(
                "custom-payout-offer",
                makerNetworkId,
                Direction.BUY,
                MARKET,
                new BaseSideFixedAmountSpec(TRADE_AMOUNT),
                priceSpec,
                List.of(paymentMethod),
                offerOptions,
                MuSigProtocol.VERSION);
        PaymentMethodSpec<?> paymentMethodSpec =
                PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, MARKET.getQuoteCurrencyCode());
        return new MuSigContract(
                1_700_000_000_000L,
                offer,
                takerNetworkId,
                TRADE_AMOUNT,
                3_500_000,
                paymentMethodSpec,
                new byte[20],
                mediator,
                Optional.empty(),
                priceSpec,
                0);
    }

    private static MuSigTrade createTrade(MuSigContract contract,
                                          Identity myIdentity,
                                          NetworkId peerNetworkId,
                                          MuSigTradeState state) {
        MuSigTrade initialTrade = new MuSigTrade(
                contract,
                true,
                false,
                myIdentity,
                contract.getOffer(),
                peerNetworkId,
                myIdentity.getNetworkId());
        initialTrade.setProtocolVersion(MuSigProtocol.VERSION);
        return copyTradeWithState(initialTrade, state);
    }

    private static MuSigTrade copyTradeWithState(MuSigTrade trade, MuSigTradeState state) {
        MuSigTrade copy = MuSigTrade.fromProto(trade.toProto(false).toBuilder()
                .setState(state.name())
                .build());
        copy.setProtocolVersion(trade.getProtocolVersion());
        return copy;
    }

    private static Identity createIdentity(int port, String keyId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId networkId = createNetworkId(keyPair, port, keyId);
        KeyBundle keyBundle = new KeyBundle(
                "key-bundle-" + keyId,
                keyPair,
                TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair());
        return new Identity("identity-" + keyId, networkId, keyBundle);
    }

    private static UserProfile createUserProfile(KeyPair keyPair, int port, String keyId) {
        NetworkId networkId = createNetworkId(keyPair, port, keyId);
        PubKey pubKey = networkId.getPubKey();
        ProofOfWork proofOfWork = new ProofOfWork(pubKey.getHash(), 0, null, 1.0, new byte[72], 0);
        return new UserProfile(1, "mediator", proofOfWork, 0, networkId, "", "", "1.0.0");
    }

    private static NetworkId createNetworkId(KeyPair keyPair, int port, String keyId) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        return new NetworkId(addresses, pubKey);
    }

    private enum SignatureState {
        MISSING,
        VALID,
        INVALID
    }

    private record ResultDefinition(MediationPayoutDistributionType payoutDistributionType,
                                    Optional<Long> buyerPayoutAmount,
                                    Optional<Long> sellerPayoutAmount,
                                    Optional<Double> payoutAdjustmentPercentage) {
    }

    private record SigningFixture(MuSigTrade trade,
                                  MuSigTradeDispute tradeDispute,
                                  MuSigTradeParty myself,
                                  MuSigTradeParty peer) {
    }

}
