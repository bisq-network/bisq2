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

package bisq.trade.bisq_easy.protocol;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.market.Market;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.I2PKeyGeneration;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.keys.TorKeyPair;
import bisq.security.pow.ProofOfWork;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BisqEasyProtocolTest {

    private ServiceProvider serviceProvider;
    private NetworkId makerNetworkId;
    private NetworkId takerNetworkId;
    private Identity makerIdentity;
    private Identity takerIdentity;
    private BisqEasyOffer buyOffer;
    private BisqEasyContract buyContract;

    @BeforeEach
    void set_up() {
        serviceProvider = mock(ServiceProvider.class);
        makerNetworkId = createNetworkId(3001);
        takerNetworkId = createNetworkId(3002);
        makerIdentity = createIdentity("maker", makerNetworkId);
        takerIdentity = createIdentity("taker", takerNetworkId);

        buyOffer = createOffer(makerNetworkId, Direction.BUY);
        buyContract = createContract(buyOffer, takerNetworkId);
    }

    @Test
    @DisplayName("Buyer-as-taker protocol constructs and wires transition table without errors")
    void buyer_as_taker_protocol_constructs_without_errors() {
        BisqEasyTrade trade = new BisqEasyTrade(buyContract, true, true, takerIdentity, buyOffer, takerNetworkId, makerNetworkId);
        BisqEasyBuyerAsTakerProtocol protocol = new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade);

        assertEquals(BisqEasyTradeState.INIT, trade.getState());
        assertEquals("1.0.0", protocol.getVersion());
        assertSame(trade, protocol.getTrade());
    }

    @Test
    @DisplayName("Seller-as-maker protocol constructs and wires transition table without errors")
    void seller_as_maker_protocol_constructs_without_errors() {
        BisqEasyTrade trade = new BisqEasyTrade(buyContract, false, false, makerIdentity, buyOffer, takerNetworkId, makerNetworkId);
        BisqEasySellerAsMakerProtocol protocol = new BisqEasySellerAsMakerProtocol(serviceProvider, trade);

        assertEquals(BisqEasyTradeState.INIT, trade.getState());
        assertEquals("1.0.0", protocol.getVersion());
        assertSame(trade, protocol.getTrade());
    }

    @Test
    @DisplayName("Buyer-as-maker protocol constructs and wires transition table without errors")
    void buyer_as_maker_protocol_constructs_without_errors() {
        BisqEasyOffer sellOffer = createOffer(makerNetworkId, Direction.SELL);
        BisqEasyContract sellContract = createContract(sellOffer, takerNetworkId);
        BisqEasyTrade trade = new BisqEasyTrade(sellContract, true, false, makerIdentity, sellOffer, takerNetworkId, makerNetworkId);
        BisqEasyBuyerAsMakerProtocol protocol = new BisqEasyBuyerAsMakerProtocol(serviceProvider, trade);

        assertEquals(BisqEasyTradeState.INIT, trade.getState());
        assertSame(trade, protocol.getTrade());
    }

    @Test
    @DisplayName("Seller-as-taker protocol constructs and wires transition table without errors")
    void seller_as_taker_protocol_constructs_without_errors() {
        BisqEasyOffer sellOffer = createOffer(makerNetworkId, Direction.SELL);
        BisqEasyContract sellContract = createContract(sellOffer, takerNetworkId);
        BisqEasyTrade trade = new BisqEasyTrade(sellContract, false, true, takerIdentity, sellOffer, takerNetworkId, makerNetworkId);
        BisqEasySellerAsTakerProtocol protocol = new BisqEasySellerAsTakerProtocol(serviceProvider, trade);

        assertEquals(BisqEasyTradeState.INIT, trade.getState());
        assertSame(trade, protocol.getTrade());
    }

    @Test
    @DisplayName("All protocol variants share the same version string")
    void all_protocol_variants_share_same_version() {
        BisqEasyTrade buyerTakerTrade = new BisqEasyTrade(buyContract, true, true, takerIdentity, buyOffer, takerNetworkId, makerNetworkId);
        BisqEasyTrade sellerMakerTrade = new BisqEasyTrade(buyContract, false, false, makerIdentity, buyOffer, takerNetworkId, makerNetworkId);

        BisqEasyBuyerAsTakerProtocol p1 = new BisqEasyBuyerAsTakerProtocol(serviceProvider, buyerTakerTrade);
        BisqEasySellerAsMakerProtocol p2 = new BisqEasySellerAsMakerProtocol(serviceProvider, sellerMakerTrade);

        assertEquals(p1.getVersion(), p2.getVersion());
        assertEquals(BisqEasyProtocol.VERSION, p1.getVersion());
    }

    @Test
    @DisplayName("Trade initial state is not a final state")
    void trade_initial_state_is_not_final() {
        BisqEasyTrade trade = new BisqEasyTrade(buyContract, true, true, takerIdentity, buyOffer, takerNetworkId, makerNetworkId);
        new BisqEasyBuyerAsTakerProtocol(serviceProvider, trade);

        assertFalse(trade.getState().isFinalState());
    }

    private BisqEasyOffer createOffer(NetworkId makerNid, Direction direction) {
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        return new BisqEasyOffer(
                "offer-proto-test",
                System.currentTimeMillis(),
                makerNid,
                direction,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN))),
                List.of(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );
    }

    private BisqEasyContract createContract(BisqEasyOffer offer, NetworkId takerNid) {
        UserProfile mediator = createUserProfile(4001);
        return new BisqEasyContract(
                System.currentTimeMillis(),
                offer,
                takerNid,
                100_000L,
                5_000_000L,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                Optional.of(mediator),
                new MarketPriceSpec(),
                6_000_000L
        );
    }

    private NetworkId createNetworkId(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        return new NetworkId(addresses, pubKey);
    }

    private Identity createIdentity(String tag, NetworkId networkId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        TorKeyPair torKeyPair = TorKeyGeneration.generateKeyPair();
        var i2pKeyPair = I2PKeyGeneration.generateKeyPair();
        KeyBundle keyBundle = new KeyBundle(tag, keyPair, torKeyPair, i2pKeyPair);
        return new Identity(tag, networkId, keyBundle);
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
}
