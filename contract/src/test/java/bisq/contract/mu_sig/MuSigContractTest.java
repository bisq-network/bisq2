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

package bisq.contract.mu_sig;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.network.identity.NetworkId;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MuSigContractTest {
    @Test
    void usesBaseAsBtcSideWhenBaseCurrencyIsBitcoin() {
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        MuSigContract contract = createContract(createBtcFiatMarket(), 111L, 222L, baseSpec, quoteSpec);

        assertEquals(111L, contract.getBtcSideAmount());
        assertEquals(222L, contract.getNonBtcSideAmount());
        assertSame(baseSpec, contract.getBtcSidePaymentMethodSpec());
        assertSame(quoteSpec, contract.getNonBtcSidePaymentMethodSpec());
    }

    @Test
    void usesQuoteAsBtcSideWhenBaseCurrencyIsNotBitcoin() {
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        MuSigContract contract = createContract(createCryptoBtcMarket(), 111L, 222L, baseSpec, quoteSpec);

        assertEquals(222L, contract.getBtcSideAmount());
        assertEquals(111L, contract.getNonBtcSideAmount());
        assertSame(quoteSpec, contract.getBtcSidePaymentMethodSpec());
        assertSame(baseSpec, contract.getNonBtcSidePaymentMethodSpec());
    }

    @Test
    void keepsTakerSaltedAccountPayloadHashAtProtoRoundTrip() {
        byte[] hash = new byte[20];
        hash[0] = 1;
        hash[19] = 2;
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");
        MuSigContract contract = createContract(createBtcFiatMarket(), 111L, 222L, baseSpec, quoteSpec, hash);

        MuSigContract fromProto = MuSigContract.fromProto(contract.toProto(false));
        assertArrayEquals(hash, fromProto.getTakerSaltedAccountPayloadHash());
    }

    @Test
    void rejectsNon20ByteTakerSaltedAccountPayloadHash() {
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD");

        assertThrows(IllegalArgumentException.class, () ->
                createContract(createBtcFiatMarket(), 111L, 222L, baseSpec, quoteSpec, new byte[19]));
    }

    private MuSigContract createContract(Market market,
                                         long baseSideAmount,
                                         long quoteSideAmount,
                                         PaymentMethodSpec<?> baseSpec,
                                         PaymentMethodSpec<?> quoteSpec) {
        return createContract(market, baseSideAmount, quoteSideAmount, baseSpec, quoteSpec, new byte[20]);
    }

    private MuSigContract createContract(Market market,
                                         long baseSideAmount,
                                         long quoteSideAmount,
                                         PaymentMethodSpec<?> baseSpec,
                                         PaymentMethodSpec<?> quoteSpec,
                                         byte[] takerSaltedAccountPayloadHash) {
        NetworkId makerNetworkId = createNetworkId(18001);
        NetworkId takerNetworkId = createNetworkId(18002);
        MuSigOffer offer = new MuSigOffer("test-id",
                makerNetworkId,
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(baseSideAmount),
                new MarketPriceSpec(),
                List.of(),
                List.of(),
                "1.0.0");
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.TAKER, takerNetworkId),
                baseSideAmount,
                quoteSideAmount,
                baseSpec,
                quoteSpec,
                takerSaltedAccountPayloadHash,
                Optional.empty(),
                new MarketPriceSpec(),
                0);
    }

    private NetworkId createNetworkId(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "mu-sig-contract-test-key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        return new NetworkId(addresses, pubKey);
    }

    private Market createBtcFiatMarket() {
        return new Market("BTC", "USD", "Bitcoin", "US Dollar");
    }

    private Market createCryptoBtcMarket() {
        return new Market("XMR", "BTC", "Monero", "Bitcoin");
    }
}
