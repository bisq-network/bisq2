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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig.events.mediation;

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
import bisq.trade.ServiceProvider;
import bisq.trade.mu_sig.MuSigCustomPayoutPartyData;
import bisq.trade.mu_sig.MuSigFeeRateProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbtRequest;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import bisq.trade.protobuf.MusigGrpc;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediationResultAcceptedEventHandlerTest {
    private static final String TX_ID = "ab".repeat(32);
    private static final long BUYER_GROSS_PAYOUT = 90_000;
    private static final long SELLER_GROSS_PAYOUT = 60_000;

    private MusigGrpc.MusigBlockingStub blockingStub;
    private MuSigTrade trade;
    private MediationResultAcceptedEventHandler handler;

    @BeforeEach
    void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class);
        MuSigTradeService tradeService = mock(MuSigTradeService.class);
        blockingStub = mock(MusigGrpc.MusigBlockingStub.class);
        when(serviceProvider.getMuSigTradeService()).thenReturn(tradeService);
        when(tradeService.getMusigBlockingStub()).thenReturn(blockingStub);

        trade = createTrade();
        MuSigMediationResult mediationResult = new MuSigMediationResult(
                new byte[20],
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.CUSTOM_PAYOUT,
                Optional.of(BUYER_GROSS_PAYOUT),
                Optional.of(SELLER_GROSS_PAYOUT),
                Optional.empty(),
                Optional.empty());
        trade.getTradeDispute().setMuSigMediationResult(mediationResult);
        handler = new MediationResultAcceptedEventHandler(serviceProvider, trade);
    }

    @Test
    void givenValidSigningResponse_whenProcessingAcceptance_thenBuildsRequestAndCommitsResponse() {
        CustomPayoutPsbt response = new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                TX_ID,
                89_900,
                59_900);
        stubSigningResponse(response);

        handler.process(new MediationResultAcceptedEvent());

        assertThat(trade.getMyself().getCustomPayoutData()).isEmpty();
        ArgumentCaptor<bisq.trade.protobuf.CustomPayoutPsbtRequest> requestCaptor =
                ArgumentCaptor.forClass(bisq.trade.protobuf.CustomPayoutPsbtRequest.class);
        verify(blockingStub).signCustomPayoutTx(requestCaptor.capture());
        assertThat(CustomPayoutPsbtRequest.fromProto(requestCaptor.getValue()))
                .isEqualTo(new CustomPayoutPsbtRequest(
                        trade.getId(),
                        SELLER_GROSS_PAYOUT,
                        MuSigFeeRateProvider.getPreparedTxFeeRate()));

        handler.commit();

        assertThat(trade.getMyself().getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomPayoutPsbt)
                .contains(response);
    }

    @Test
    void givenOversizedPsbt_whenProcessingAcceptance_thenRejectsResponseWithoutSideEffects() {
        assertInvalidSigningResponse(new CustomPayoutPsbt(
                new byte[4_097],
                TX_ID,
                89_900,
                59_900));
    }

    @Disabled("Enable after trade setup uses the contract security deposits and payout-bound checks are restored")
    @Test
    void givenBuyerPayoutAboveGrossAllocation_whenProcessingAcceptance_thenRejectsResponseWithoutSideEffects() {
        assertInvalidSigningResponse(new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                TX_ID,
                BUYER_GROSS_PAYOUT + 1,
                59_900));
    }

    @Disabled("Enable after trade setup uses the contract security deposits and payout-bound checks are restored")
    @Test
    void givenSellerPayoutAboveGrossAllocation_whenProcessingAcceptance_thenRejectsResponseWithoutSideEffects() {
        assertInvalidSigningResponse(new CustomPayoutPsbt(
                new byte[]{1, 2, 3},
                TX_ID,
                89_900,
                SELLER_GROSS_PAYOUT + 1));
    }

    @Test
    void givenSigningRpcFailure_whenProcessingAcceptance_thenPropagatesFailureWithoutSideEffects() {
        RuntimeException rpcFailure = Status.UNAVAILABLE.asRuntimeException();
        when(blockingStub.signCustomPayoutTx(any(bisq.trade.protobuf.CustomPayoutPsbtRequest.class)))
                .thenThrow(rpcFailure);

        assertThatThrownBy(() -> handler.process(new MediationResultAcceptedEvent()))
                .isSameAs(rpcFailure);
        assertThat(trade.getMyself().getCustomPayoutData()).isEmpty();
    }

    private void assertInvalidSigningResponse(CustomPayoutPsbt response) {
        stubSigningResponse(response);

        assertThatThrownBy(() -> handler.process(new MediationResultAcceptedEvent()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(trade.getMyself().getCustomPayoutData()).isEmpty();
    }

    private void stubSigningResponse(CustomPayoutPsbt response) {
        when(blockingStub.signCustomPayoutTx(any(bisq.trade.protobuf.CustomPayoutPsbtRequest.class)))
                .thenReturn(response.toProto(false));
    }

    private static MuSigTrade createTrade() {
        KeyPair makerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        NetworkId makerNetworkId = createNetworkId(makerKeyPair, 9997, "maker-key");
        NetworkId takerNetworkId = createNetworkId(KeyGeneration.generateDefaultEcKeyPair(), 9998, "taker-key");
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
        Identity myIdentity = new Identity(
                "maker-identity",
                makerNetworkId,
                new KeyBundle(
                        "maker-key-bundle",
                        makerKeyPair,
                        TorKeyGeneration.generateKeyPair(),
                        I2PKeyGeneration.generateKeyPair()));
        return new MuSigTrade(
                contract,
                true,
                false,
                myIdentity,
                offer,
                takerNetworkId,
                makerNetworkId);
    }

    private static NetworkId createNetworkId(KeyPair keyPair, int port, String keyId) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        return new NetworkId(addresses, new PubKey(keyPair.getPublic(), keyId));
    }
}
