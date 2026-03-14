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
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import com.google.protobuf.Message;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static bisq.offer.options.OfferOptionUtil.createSaltedAccountPayloadHash;

class MuSigTradeUtilsTest {
    @Test
    void returnsBaseAsBtcSideForBtcFiatMarket() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 111L, 222L);

        Monetary btcSideMonetary = MuSigTradeUtils.getBtcSideMonetary(contract);
        Monetary nonBtcSideMonetary = MuSigTradeUtils.getNonBtcSideMonetary(contract);

        assertEquals(111L, btcSideMonetary.getValue());
        assertEquals("BTC", btcSideMonetary.getCode());
        assertEquals(222L, nonBtcSideMonetary.getValue());
        assertEquals("USD", nonBtcSideMonetary.getCode());
    }

    @Test
    void returnsQuoteAsBtcSideForCryptoBtcMarket() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 111L, 222L);

        Monetary btcSideMonetary = MuSigTradeUtils.getBtcSideMonetary(contract);
        Monetary nonBtcSideMonetary = MuSigTradeUtils.getNonBtcSideMonetary(contract);

        assertEquals(222L, btcSideMonetary.getValue());
        assertEquals("BTC", btcSideMonetary.getCode());
        assertEquals(111L, nonBtcSideMonetary.getValue());
        assertEquals("XMR", nonBtcSideMonetary.getCode());
    }

    @Test
    void matchesMakersSaltedAccountPayloadHashWhenTakerReceivesPeersAccountPayload() {
        TestAccountPayload accountPayload = new TestAccountPayload(new byte[]{1, 2, 3});
        MuSigContract contract = createContractWithPeerHashes(
                createBtcFiatMarket(),
                createSaltedAccountPayloadHash(accountPayload, "test-id"),
                createSaltedAccountPayloadHash(new TestAccountPayload(new byte[]{9, 9, 9}), "test-id"));
        MuSigTrade trade = createTrade(contract, true);

        assertTrue(MuSigTradeUtils.doesPeerAccountPayloadMatchContract(trade, accountPayload));
    }

    @Test
    void matchesTakersSaltedAccountPayloadHashWhenMakerReceivesPeersAccountPayload() {
        TestAccountPayload accountPayload = new TestAccountPayload(new byte[]{4, 5, 6});
        MuSigContract contract = createContractWithPeerHashes(
                createBtcFiatMarket(),
                createSaltedAccountPayloadHash(new TestAccountPayload(new byte[]{9, 9, 9}), "test-id"),
                createSaltedAccountPayloadHash(accountPayload, "test-id"));
        MuSigTrade trade = createTrade(contract, false);

        assertTrue(MuSigTradeUtils.doesPeerAccountPayloadMatchContract(trade, accountPayload));
    }

    @Test
    void returnsFalseWhenPeerAccountPayloadHashDoesNotMatchContract() {
        TestAccountPayload accountPayload = new TestAccountPayload(new byte[]{1, 2, 3});
        MuSigContract contract = createContractWithPeerHashes(
                createBtcFiatMarket(),
                createSaltedAccountPayloadHash(new TestAccountPayload(new byte[]{9, 9, 9}), "test-id"),
                createSaltedAccountPayloadHash(new TestAccountPayload(new byte[]{8, 8, 8}), "test-id"));
        MuSigTrade trade = createTrade(contract, true);

        assertFalse(MuSigTradeUtils.doesPeerAccountPayloadMatchContract(trade, accountPayload));
    }

    @Test
    void returnsEmptyAndFalseWhenPeersContractSaltedAccountPayloadHashIsMissing() {
        TestAccountPayload accountPayload = new TestAccountPayload(new byte[]{1, 2, 3});
        MuSigContract contract = createContractWithPeerHashes(
                createBtcFiatMarket(),
                Optional.empty(),
                Optional.of(createSaltedAccountPayloadHash(new TestAccountPayload(new byte[]{9, 9, 9}), "test-id")));
        MuSigTrade trade = createTrade(contract, true);

        assertFalse(MuSigTradeUtils.findPeersContractSaltedAccountPayloadHash(trade).isPresent());
        assertFalse(MuSigTradeUtils.doesPeerAccountPayloadMatchContract(trade, accountPayload));
    }

    private MuSigContract createContract(Market market, long baseSideAmount, long quoteSideAmount) {
        MuSigOffer offer = new MuSigOffer("test-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                List.of(),
                List.of(),
                "1.0.0");
        PaymentMethodSpec<?> baseSpec = market.isBaseCurrencyBitcoin()
                ? PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0)
                : PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        PaymentMethodSpec<?> quoteSpec = market.isBaseCurrencyBitcoin()
                ? PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD")
                : PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.MAKER, offer.getMakerNetworkId()),
                new Party(Role.TAKER, null),
                baseSideAmount,
                quoteSideAmount,
                baseSpec,
                quoteSpec,
                Optional.empty(),
                null,
                0);
    }

    private MuSigContract createContractWithPeerHashes(Market market, byte[] makerHash, byte[] takerHash) {
        return createContractWithPeerHashes(market, Optional.of(makerHash), Optional.of(takerHash));
    }

    private MuSigContract createContractWithPeerHashes(Market market,
                                                       Optional<byte[]> makerHash,
                                                       Optional<byte[]> takerHash) {
        MuSigOffer offer = new MuSigOffer("test-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                List.of(),
                List.of(),
                "1.0.0");
        PaymentMethodSpec<?> baseSpec = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        PaymentMethodSpec<?> quoteSpec = PaymentMethodSpecUtil.createPaymentMethodSpec(
                FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER),
                "USD");
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.MAKER, createNetworkId("maker", 9998), makerHash),
                new Party(Role.TAKER, createNetworkId("taker", 9999), takerHash),
                111L,
                222L,
                baseSpec,
                quoteSpec,
                Optional.empty(),
                null,
                0);
    }

    private MuSigTrade createTrade(MuSigContract contract, boolean isTaker) {
        Identity myIdentity = createIdentity(isTaker ? contract.getTaker().getNetworkId() : contract.getMaker().getNetworkId());
        return new MuSigTrade(contract,
                true,
                isTaker,
                myIdentity,
                contract.getOffer(),
                contract.getTaker().getNetworkId(),
                contract.getMaker().getNetworkId());
    }

    private Identity createIdentity(NetworkId networkId) {
        KeyBundle keyBundle = new KeyBundle("test-key-bundle",
                KeyGeneration.generateDefaultEcKeyPair(),
                TorKeyGeneration.generateKeyPair(),
                I2PKeyGeneration.generateKeyPair());
        return new Identity("test-id", networkId, keyBundle);
    }

    private NetworkId createNetworkId(String keyIdSuffix, int port) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "test-key-" + keyIdSuffix);
        return new NetworkId(addresses, pubKey);
    }

    private Market createBtcFiatMarket() {
        return new Market("BTC", "USD", "Bitcoin", "US Dollar");
    }

    private Market createCryptoBtcMarket() {
        return new Market("XMR", "BTC", "Monero", "Bitcoin");
    }

    private static final class TestAccountPayload extends AccountPayload<PaymentMethod<?>> {
        private final byte[] serializedForHash;

        private TestAccountPayload(byte[] serializedForHash) {
            super("test-account-id", new byte[32]);
            this.serializedForHash = serializedForHash;
        }

        @Override
        public byte[] serializeForHash() {
            return serializedForHash;
        }

        @Override
        public Message.Builder getBuilder(boolean serializeForHash) {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        public byte[] getBisq1CompatibleFingerprint() {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        protected byte[] getBisq2Fingerprint() {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        public PaymentMethod<?> getPaymentMethod() {
            throw new UnsupportedOperationException("Not required for this test");
        }

        @Override
        public String getAccountDataDisplayString() {
            throw new UnsupportedOperationException("Not required for this test");
        }
    }
}
