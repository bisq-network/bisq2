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

package bisq.contract.bisq_easy;

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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.Role;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BisqEasyContractTest {

    @Test
    @DisplayName("construction sets all fields correctly")
    void construction_sets_all_fields_correctly() {
        BisqEasyOffer offer = createOffer();
        NetworkId takerNetworkId = createNetworkId(2001);
        BitcoinPaymentMethodSpec baseSideSpec = createBtcPaymentMethodSpec();
        FiatPaymentMethodSpec quoteSideSpec = createFiatPaymentMethodSpec();
        UserProfile mediator = createUserProfile(3001);
        PriceSpec priceSpec = new MarketPriceSpec();

        BisqEasyContract contract = new BisqEasyContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                // 0.001 BTC for 60 EUR at a market price of 60 000 EUR
                100_000L,
                600_000L,
                baseSideSpec,
                quoteSideSpec,
                Optional.of(mediator),
                priceSpec,
                600_000_000L
        );

        assertTrue(contract.getTakeOfferDate() > 0);
        assertSame(offer, contract.getOffer());
        assertEquals(TradeProtocolType.BISQ_EASY, contract.getProtocolType());
        assertEquals(100_000L, contract.getBaseSideAmount());
        assertEquals(600_000L, contract.getQuoteSideAmount());
        assertEquals(baseSideSpec, contract.getBaseSidePaymentMethodSpec());
        assertEquals(quoteSideSpec, contract.getQuoteSidePaymentMethodSpec());
        assertTrue(contract.getMediator().isPresent());
        assertEquals(mediator, contract.getMediator().get());
        assertEquals(priceSpec, contract.getPriceSpec());
        assertEquals(600_000_000L, contract.getMarketPrice());
        assertEquals(Role.TAKER, contract.getTaker().getRole());
        assertEquals(takerNetworkId, contract.getTaker().getNetworkId());
    }

    @Test
    @DisplayName("toProto and fromProto round trip preserves all fields")
    void to_proto_and_from_proto_round_trip_preserves_all_fields() throws UnresolvableProtobufException {
        BisqEasyOffer offer = createOffer();
        NetworkId takerNetworkId = createNetworkId(2002);
        BitcoinPaymentMethodSpec baseSideSpec = createBtcPaymentMethodSpec();
        FiatPaymentMethodSpec quoteSideSpec = createFiatPaymentMethodSpec();
        UserProfile mediator = createUserProfile(3002);
        PriceSpec priceSpec = new MarketPriceSpec();

        BisqEasyContract original = new BisqEasyContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                // 0.002 BTC for 130 EUR at a market price of 65 000 EUR
                200_000L,
                1_300_000L,
                baseSideSpec,
                quoteSideSpec,
                Optional.of(mediator),
                priceSpec,
                650_000_000L
        );

        bisq.contract.protobuf.Contract proto = original.toProto(false);
        BisqEasyContract deserialized = BisqEasyContract.fromProto(proto);

        assertEquals(original.getTakeOfferDate(), deserialized.getTakeOfferDate());
        assertEquals(original.getBaseSideAmount(), deserialized.getBaseSideAmount());
        assertEquals(original.getQuoteSideAmount(), deserialized.getQuoteSideAmount());
        assertEquals(original.getBaseSidePaymentMethodSpec(), deserialized.getBaseSidePaymentMethodSpec());
        assertEquals(original.getQuoteSidePaymentMethodSpec(), deserialized.getQuoteSidePaymentMethodSpec());
        assertEquals(original.getMarketPrice(), deserialized.getMarketPrice());
        assertEquals(original.getProtocolType(), deserialized.getProtocolType());
        assertEquals(original.getPriceSpec(), deserialized.getPriceSpec());
        assertTrue(deserialized.getMediator().isPresent());
        assertEquals(original.getMediator().get().getId(), deserialized.getMediator().get().getId());
        assertEquals(original.getOffer().getId(), deserialized.getOffer().getId());
        // Contract equality does not cover the taker, so the taker's fields are compared one by one
        assertEquals(original.getTaker().getRole(), deserialized.getTaker().getRole());
        assertEquals(original.getTaker().getNetworkId(), deserialized.getTaker().getNetworkId());
        // The serialized form covers every field, also one added later
        assertEquals(original.toProto(false), deserialized.toProto(false));
    }

    private BisqEasyOffer createOffer() {
        NetworkId makerNetworkId = createNetworkId(1001);
        Market market = new Market("BTC", "EUR", "Bitcoin", "Euro");
        return new BisqEasyOffer(
                "offer-test-1",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(createBtcPaymentMethodSpec()),
                List.of(createFiatPaymentMethodSpec()),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );
    }

    private BitcoinPaymentMethodSpec createBtcPaymentMethodSpec() {
        return new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN));
    }

    private FiatPaymentMethodSpec createFiatPaymentMethodSpec() {
        return new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA));
    }

    private NetworkId createNetworkId(int port) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), "key-" + port);
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(
                Map.of(TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port))
        );
        return new NetworkId(addresses, pubKey);
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
