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

package bisq.trade.mu_sig.messages.network.handler;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
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
import bisq.trade.mu_sig.MuSigTradeDispute;
import bisq.trade.mu_sig.MuSigTradeParty;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.trade.mu_sig.PeerCustomPayoutPsbt;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import bisq.trade.mu_sig.messages.network.MuSigCustomPayoutPsbtMessage;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MuSigCustomPayoutPsbtMessageHandlerTest {
    private static final String TX_ID = "ab".repeat(32);
    private static final byte[] PSBT = {1, 2, 3};

    private ServiceProvider serviceProvider;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class);
        when(serviceProvider.getMuSigTradeService()).thenReturn(mock(MuSigTradeService.class));
    }

    @Test
    void givenValidPeerPsbt_whenProcessed_thenStoresPeerDataOnlyDuringCommit() {
        TradeFixture fixture = createTradeFixture(createMediationResult());
        MuSigCustomPayoutPsbtMessage message = createMessage(fixture);
        MuSigCustomPayoutPsbtMessageHandler handler = createHandler(fixture);

        handler.verifyInternal(message);
        handler.verify(message);
        handler.process(message);

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();

        handler.commit();

        assertThat(fixture.peer().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .contains(new PeerCustomPayoutPsbt(TX_ID, PSBT));
        assertThat(fixture.myself().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenMissingSignedResult_whenMessageReceived_thenIgnoresMessage() {
        TradeFixture fixture = createTradeFixtureWithoutResult();
        MuSigCustomPayoutPsbtMessage message = createMessage(fixture, createMediationResult(), TX_ID, PSBT);

        handle(fixture, message);

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenMismatchingResultHash_whenMessageReceived_thenIgnoresMessage() {
        MuSigMediationResult mediationResult = createMediationResult();
        TradeFixture fixture = createTradeFixture(mediationResult);
        byte[] mismatchingHash = MuSigMediationResultService.getMediationResultHash(mediationResult);
        mismatchingHash[0] ^= 1;

        handle(fixture, createMessage(
                fixture,
                fixture.trade().getId(),
                mismatchingHash,
                TX_ID,
                PSBT,
                fixture.peer().getNetworkId(),
                fixture.myself().getNetworkId(),
                MuSigProtocol.VERSION));

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenUnexpectedEnvelopeContext_whenMessageReceived_thenIgnoresMessages() {
        MuSigMediationResult mediationResult = createMediationResult();
        TradeFixture fixture = createTradeFixture(mediationResult);
        byte[] resultHash = MuSigMediationResultService.getMediationResultHash(mediationResult);
        NetworkId unexpectedNetworkId = createNetworkId(9999, "unexpected");

        handle(fixture, createMessage(
                fixture, "unexpected-trade", resultHash, TX_ID, PSBT,
                fixture.peer().getNetworkId(), fixture.myself().getNetworkId(), MuSigProtocol.VERSION));
        handle(fixture, createMessage(
                fixture, fixture.trade().getId(), resultHash, TX_ID, PSBT,
                unexpectedNetworkId, fixture.myself().getNetworkId(), MuSigProtocol.VERSION));
        handle(fixture, createMessage(
                fixture, fixture.trade().getId(), resultHash, TX_ID, PSBT,
                fixture.peer().getNetworkId(), unexpectedNetworkId, MuSigProtocol.VERSION));
        handle(fixture, createMessage(
                fixture, fixture.trade().getId(), resultHash, TX_ID, PSBT,
                fixture.peer().getNetworkId(), fixture.myself().getNetworkId(), "2.0.0"));

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenPeerAlreadyRejectedResult_whenMessageReceived_thenIgnoresMessage() {
        TradeFixture fixture = createTradeFixture(createMediationResult());
        fixture.peer().setMediationResultRejected();

        handle(fixture, createMessage(fixture));

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenNoPayoutResult_whenMessageReceived_thenIgnoresMessage() {
        TradeFixture fixture = createTradeFixture(createNoPayoutMediationResult());

        handle(fixture, createMessage(fixture));

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenMediationIsNotClosed_whenMessageReceived_thenIgnoresMessage() {
        TradeFixture fixture = createTradeFixture(createMediationResult());
        fixture.tradeDispute().setDisputeState(MuSigDisputeState.MEDIATION_RE_OPENED);

        handle(fixture, createMessage(fixture));

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenLocalPsbtWithDifferentTxId_whenMessageReceived_thenIgnoresMessage() {
        TradeFixture fixture = createTradeFixture(createMediationResult());
        fixture.myself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                "cd".repeat(32),
                59_900,
                39_900));

        handle(fixture, createMessage(fixture));

        assertThat(fixture.peer().getCustomPayoutData()).isEmpty();
    }

    @Test
    void givenLocalPsbtWithSameTxId_whenMessageReceived_thenStoresPeerPsbt() {
        TradeFixture fixture = createTradeFixture(createMediationResult());
        fixture.myself().setMyCustomPayoutPsbt(new CustomPayoutPsbt(
                new byte[]{4, 5, 6},
                TX_ID,
                59_900,
                39_900));

        handle(fixture, createMessage(fixture));

        assertThat(fixture.peer().getCustomPayoutData()).isPresent();
    }

    @Test
    void givenDuplicateAndConflictingMessages_whenReceived_thenKeepsFirstPeerPsbt() {
        TradeFixture fixture = createTradeFixture(createMediationResult());
        MuSigCustomPayoutPsbtMessage first = createMessage(fixture);

        handle(fixture, first);
        handle(fixture, first);
        handle(fixture, createMessage(
                fixture,
                fixture.tradeDispute().getMuSigMediationResult().orElseThrow(),
                TX_ID,
                new byte[]{4, 5, 6}));
        handle(fixture, createMessage(
                fixture,
                fixture.tradeDispute().getMuSigMediationResult().orElseThrow(),
                "cd".repeat(32),
                new byte[]{4, 5, 6}));

        assertThat(fixture.peer().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getPeersCustomPayoutPsbt)
                .contains(new PeerCustomPayoutPsbt(TX_ID, PSBT));
    }

    private MuSigCustomPayoutPsbtMessageHandler createHandler(TradeFixture fixture) {
        return new MuSigCustomPayoutPsbtMessageHandler(serviceProvider, fixture.trade());
    }

    private void handle(TradeFixture fixture, MuSigCustomPayoutPsbtMessage message) {
        createHandler(fixture).handle(message);
    }

    private static TradeFixture createTradeFixture(MuSigMediationResult mediationResult) {
        TradeFixture fixture = createTradeFixtureWithoutResult();
        fixture.tradeDispute().setMuSigMediationResult(mediationResult);
        fixture.tradeDispute().setMediationResultSignature(new byte[]{1});
        return fixture;
    }

    private static TradeFixture createTradeFixtureWithoutResult() {
        Identity myIdentity = createIdentity(9997, "my-key");
        NetworkId peerNetworkId = createNetworkId(9998, "peer-key");
        MuSigContract contract = createContract(myIdentity.getNetworkId(), peerNetworkId);
        MuSigTrade trade = new MuSigTrade(
                contract,
                true,
                false,
                myIdentity,
                contract.getOffer(),
                peerNetworkId,
                myIdentity.getNetworkId());
        trade.setProtocolVersion(MuSigProtocol.VERSION);
        MuSigTradeDispute tradeDispute = trade.getTradeDispute();
        tradeDispute.setDisputeState(MuSigDisputeState.MEDIATION_CLOSED);
        return new TradeFixture(trade, tradeDispute, trade.getMyself(), trade.getPeer());
    }

    private static MuSigContract createContract(NetworkId makerNetworkId, NetworkId takerNetworkId) {
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        MarketPriceSpec priceSpec = new MarketPriceSpec();
        MuSigOffer offer = new MuSigOffer(
                "custom-payout-offer",
                makerNetworkId,
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000),
                priceSpec,
                List.of(paymentMethod),
                List.of(),
                MuSigProtocol.VERSION);
        PaymentMethodSpec<?> paymentMethodSpec =
                PaymentMethodSpecUtil.createPaymentMethodSpec(paymentMethod, market.getQuoteCurrencyCode());
        return new MuSigContract(
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
    }

    private static Identity createIdentity(int port, String keyId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId networkId = createNetworkId(keyPair, port, keyId);
        return new Identity(
                "identity-" + keyId,
                networkId,
                new KeyBundle(
                        "key-bundle-" + keyId,
                        keyPair,
                        TorKeyGeneration.generateKeyPair(),
                        I2PKeyGeneration.generateKeyPair()));
    }

    private static NetworkId createNetworkId(int port, String keyId) {
        return createNetworkId(KeyGeneration.generateDefaultEcKeyPair(), port, keyId);
    }

    private static NetworkId createNetworkId(KeyPair keyPair, int port, String keyId) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        return new NetworkId(addresses, new PubKey(keyPair.getPublic(), keyId));
    }

    private static MuSigCustomPayoutPsbtMessage createMessage(TradeFixture fixture) {
        return createMessage(
                fixture,
                fixture.tradeDispute().getMuSigMediationResult().orElseThrow(),
                TX_ID,
                PSBT);
    }

    private static MuSigCustomPayoutPsbtMessage createMessage(TradeFixture fixture,
                                                              MuSigMediationResult mediationResult,
                                                              String txId,
                                                              byte[] psbt) {
        return createMessage(
                fixture,
                fixture.trade().getId(),
                MuSigMediationResultService.getMediationResultHash(mediationResult),
                txId,
                psbt,
                fixture.peer().getNetworkId(),
                fixture.myself().getNetworkId(),
                MuSigProtocol.VERSION);
    }

    private static MuSigCustomPayoutPsbtMessage createMessage(TradeFixture fixture,
                                                              String tradeId,
                                                              byte[] mediationResultHash,
                                                              String txId,
                                                              byte[] psbt,
                                                              NetworkId sender,
                                                              NetworkId receiver,
                                                              String protocolVersion) {
        return new MuSigCustomPayoutPsbtMessage(
                "message-id",
                tradeId,
                protocolVersion,
                sender,
                receiver,
                mediationResultHash,
                txId,
                psbt);
    }

    private static MuSigMediationResult createMediationResult() {
        return new MuSigMediationResult(
                new byte[20],
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                Optional.of(60_000L),
                Optional.of(40_000L),
                Optional.empty(),
                Optional.empty());
    }

    private static MuSigMediationResult createNoPayoutMediationResult() {
        return new MuSigMediationResult(
                new byte[20],
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.NO_PAYOUT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private record TradeFixture(MuSigTrade trade,
                                MuSigTradeDispute tradeDispute,
                                MuSigTradeParty myself,
                                MuSigTradeParty peer) {
    }
}
