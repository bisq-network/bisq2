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
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodSpec;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.contract.Role;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
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

import static org.assertj.core.api.Assertions.assertThat;
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
                100_000L,
                5_000_000L,
                baseSideSpec,
                quoteSideSpec,
                Optional.of(mediator),
                priceSpec,
                6_000_000L
        );

        assertTrue(contract.getTakeOfferDate() > 0);
        assertSame(offer, contract.getOffer());
        assertEquals(TradeProtocolType.BISQ_EASY, contract.getProtocolType());
        assertEquals(100_000L, contract.getBaseSideAmount());
        assertEquals(5_000_000L, contract.getQuoteSideAmount());
        assertEquals(baseSideSpec, contract.getBaseSidePaymentMethodSpec());
        assertEquals(quoteSideSpec, contract.getQuoteSidePaymentMethodSpec());
        assertTrue(contract.getMediator().isPresent());
        assertEquals(mediator, contract.getMediator().get());
        assertEquals(priceSpec, contract.getPriceSpec());
        assertEquals(6_000_000L, contract.getMarketPrice());
        assertEquals(Role.TAKER, contract.getTaker().getRole());
        assertEquals(takerNetworkId, contract.getTaker().getNetworkId());
    }

    @Test
    @DisplayName("toProto and fromProto round trip preserves all fields")
    void to_proto_and_from_proto_round_trip_preserves_all_fields() {
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
                200_000L,
                10_000_000L,
                baseSideSpec,
                quoteSideSpec,
                Optional.of(mediator),
                priceSpec,
                6_500_000L
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
    }

    @Test
    @DisplayName("toProto and fromProto round trip preserves contract without mediator")
    void to_proto_and_from_proto_round_trip_preserves_contract_without_mediator() {
        BisqEasyOffer offer = createOffer();
        NetworkId takerNetworkId = createNetworkId(2003);
        BitcoinPaymentMethodSpec baseSideSpec = createBtcPaymentMethodSpec();
        FiatPaymentMethodSpec quoteSideSpec = createFiatPaymentMethodSpec();

        BisqEasyContract original = new BisqEasyContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                300_000L,
                15_000_000L,
                baseSideSpec,
                quoteSideSpec,
                Optional.empty(),
                new MarketPriceSpec(),
                7_000_000L
        );

        bisq.contract.protobuf.Contract proto = original.toProto(false);
        BisqEasyContract deserialized = BisqEasyContract.fromProto(proto);

        assertFalse(deserialized.getMediator().isPresent());
        assertEquals(original.getBaseSideAmount(), deserialized.getBaseSideAmount());
        assertEquals(original.getQuoteSideAmount(), deserialized.getQuoteSideAmount());
    }

    @Test
    @DisplayName("verify does not throw for valid contract")
    void verify_does_not_throw_for_valid_contract() {
        BisqEasyOffer offer = createOffer();
        NetworkId takerNetworkId = createNetworkId(2004);

        assertDoesNotThrow(() -> new BisqEasyContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                100_000L,
                5_000_000L,
                createBtcPaymentMethodSpec(),
                createFiatPaymentMethodSpec(),
                Optional.empty(),
                new MarketPriceSpec(),
                6_000_000L
        ));
    }

    @Test
    @DisplayName("getPriceQuote derived from amounts is consistent with FixPriceSpec")
    void get_price_quote_consistent_with_fix_price_spec() {
        // At $60,000/BTC: 0.001 BTC (100_000 sat) = $60 (600_000 fiat units at precision 4)
        PriceQuote fixedPrice = PriceQuote.fromFiatPrice(60_000, "USD");
        long baseSideAmount = 100_000L;
        long quoteSideAmount = 600_000L;

        BisqEasyOffer offer = createOfferWithMarket(
                new Market("BTC", "USD", "Bitcoin", "US Dollar"),
                new FixPriceSpec(fixedPrice));
        BisqEasyContract contract = new BisqEasyContract(
                System.currentTimeMillis(), offer, createNetworkId(4001),
                baseSideAmount, quoteSideAmount,
                createBtcPaymentMethodSpec(), createFiatPaymentMethodSpec(),
                Optional.empty(), new FixPriceSpec(fixedPrice), fixedPrice.getValue());

        // Derive the implied price from the contract amounts
        Coin baseMoney = Coin.fromValue(baseSideAmount, "BTC");
        Fiat quoteMoney = Fiat.fromValue(quoteSideAmount, "USD");
        PriceQuote impliedPrice = PriceQuote.from(baseMoney, quoteMoney);

        assertThat(impliedPrice.getValue()).isEqualTo(fixedPrice.getValue());
    }

    @Test
    @DisplayName("inconsistent amounts can be constructed — no validation in constructor")
    void inconsistent_amounts_accepted_by_constructor() {
        // Amounts that don't match the price: $60k price but with amounts implying $120k
        PriceQuote fixedPrice = PriceQuote.fromFiatPrice(60_000, "USD");
        long baseSideAmount = 100_000L;
        long inconsistentQuote = 1_200_000L; // double the correct value

        BisqEasyOffer offer = createOfferWithMarket(
                new Market("BTC", "USD", "Bitcoin", "US Dollar"),
                new FixPriceSpec(fixedPrice));

        // INTENTIONAL CANARY: This test documents that BisqEasyContract's constructor currently
        // does NOT validate consistency between amounts and priceSpec. If a future change adds
        // such validation (which would be a good thing), this test will fail — signaling the
        // developer to update dependent code paths that rely on the current lenient behavior.
        assertDoesNotThrow(() -> new BisqEasyContract(
                System.currentTimeMillis(), offer, createNetworkId(4002),
                baseSideAmount, inconsistentQuote,
                createBtcPaymentMethodSpec(), createFiatPaymentMethodSpec(),
                Optional.empty(), new FixPriceSpec(fixedPrice), fixedPrice.getValue()));
    }

    @Test
    @DisplayName("implied price deviation between amounts and priceSpec is measurable")
    void implied_price_deviation_measurable() {
        PriceQuote fixedPrice = PriceQuote.fromFiatPrice(60_000, "USD");
        long baseSideAmount = 100_000L;
        long slightlyOffQuote = 630_000L; // implies $63k instead of $60k

        Coin baseMoney = Coin.fromValue(baseSideAmount, "BTC");
        Fiat quoteMoney = Fiat.fromValue(slightlyOffQuote, "USD");
        PriceQuote impliedPrice = PriceQuote.from(baseMoney, quoteMoney);

        double deviation = Math.abs(impliedPrice.getValue() - fixedPrice.getValue())
                / (double) fixedPrice.getValue();
        assertThat(deviation)
                .as("5%% deviation between implied ($63k) and fixed ($60k) price")
                .isGreaterThan(0.04)
                .isLessThan(0.06);
    }

    private BisqEasyOffer createOfferWithMarket(Market market, PriceSpec priceSpec) {
        NetworkId makerNetworkId = createNetworkId(1001);
        return new BisqEasyOffer(
                "offer-test-consistency",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                priceSpec,
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

    private StableCoinPaymentMethodSpec createStableCoinPaymentMethodSpec() {
        return new StableCoinPaymentMethodSpec(StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));
    }

    private BisqEasyOffer createStableCoinOffer() {
        NetworkId makerNetworkId = createNetworkId(1010);
        Market market = new Market("BTC", "USDC", "Bitcoin", "USD Coin");
        return new BisqEasyOffer(
                "offer-test-stablecoin",
                System.currentTimeMillis(),
                makerNetworkId,
                Direction.BUY,
                market,
                new BaseSideFixedAmountSpec(100_000L),
                new MarketPriceSpec(),
                List.of(TradeProtocolType.BISQ_EASY),
                List.of(createBtcPaymentMethodSpec()),
                List.of(createStableCoinPaymentMethodSpec()),
                List.of(),
                List.of("en"),
                0,
                "1.0.0",
                "1.0.0"
        );
    }

    @Test
    @DisplayName("toProto and fromProto round trip preserves StableCoinPaymentMethodSpec on quote side")
    void to_proto_round_trip_preserves_stablecoin_payment_method_spec() {
        BisqEasyOffer offer = createStableCoinOffer();
        NetworkId takerNetworkId = createNetworkId(2010);
        BitcoinPaymentMethodSpec baseSideSpec = createBtcPaymentMethodSpec();
        StableCoinPaymentMethodSpec quoteSideSpec = createStableCoinPaymentMethodSpec();

        BisqEasyContract original = new BisqEasyContract(
                System.currentTimeMillis(),
                offer,
                takerNetworkId,
                100_000L,
                111_359_000_000L,
                baseSideSpec,
                quoteSideSpec,
                Optional.empty(),
                new MarketPriceSpec(),
                8_980L
        );

        bisq.contract.protobuf.Contract proto = original.toProto(false);
        BisqEasyContract deserialized = BisqEasyContract.fromProto(proto);

        assertThat(deserialized.getQuoteSidePaymentMethodSpec()).isInstanceOf(StableCoinPaymentMethodSpec.class);
        StableCoinPaymentMethodSpec deserializedSpec = (StableCoinPaymentMethodSpec) deserialized.getQuoteSidePaymentMethodSpec();
        assertEquals(quoteSideSpec.getPaymentMethod().getName(), deserializedSpec.getPaymentMethod().getName());
        assertEquals(original.getBaseSideAmount(), deserialized.getBaseSideAmount());
        assertEquals(original.getQuoteSideAmount(), deserialized.getQuoteSideAmount());
        assertEquals("BTC", deserialized.getOffer().getMarket().getBaseCurrencyCode());
        assertEquals("USDC", deserialized.getOffer().getMarket().getQuoteCurrencyCode());
    }
}
