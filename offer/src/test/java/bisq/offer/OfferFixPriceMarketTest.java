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

package bisq.offer;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.CollateralOption;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The fixed price of an offer is a price of the market of the offer (see docs/specifications/offer/fixed-price.md).
 * The amount conversions take the price from the price spec and the currency codes from the market.
 */
class OfferFixPriceMarketTest {
    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final PaymentMethod<?> WISE = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
    private static final NetworkId MAKER = createNetworkId();

    // Each of these quotes is a valid fixed price of its own market, so only the market rule can reject the offer
    static Stream<FixPriceSpec> fixedPricesOfOtherMarkets() {
        return Stream.of(
                // Other quote currency. The amounts are of the same kinds, so a conversion would use a wrong rate
                new FixPriceSpec(PriceQuote.fromFiatPrice(55_000, "EUR")),
                // Other base currency
                new FixPriceSpec(PriceQuote.from(Coin.asXmrFromFaceValue(1), Fiat.fromFaceValue(150, "USD"))),
                // Both currencies differ. The quote side is a coin, so a conversion of a USD amount throws
                new FixPriceSpec(PriceQuote.from(Coin.asXmrFromFaceValue(1), Coin.asBtcFromFaceValue(0.005))),
                // The currencies of the market, but on the other sides
                new FixPriceSpec(PriceQuote.from(Fiat.fromFaceValue(60_000, "USD"), Coin.asBtcFromFaceValue(1))));
    }

    @Test
    void bisqEasyOfferWithAFixedPriceOfItsMarketIsCreatedAndParsed() throws Exception {
        BisqEasyOffer offer = bisqEasyOffer(fixedPriceOfTheMarket());

        assertThat(BisqEasyOffer.fromProto(serializeAndParse(offer.toProto(false)))).isEqualTo(offer);
    }

    @Test
    void muSigOfferWithAFixedPriceOfItsMarketIsCreatedAndParsed() throws Exception {
        MuSigOffer offer = muSigOffer(fixedPriceOfTheMarket());

        assertThat(Offer.fromProto(serializeAndParse(offer.toProto(false)))).isEqualTo(offer);
    }

    @Test
    void offersWithoutAFixedPriceAreNotBoundToAQuote() {
        assertThatCode(() -> bisqEasyOffer(new MarketPriceSpec())).doesNotThrowAnyException();
        assertThatCode(() -> bisqEasyOffer(new FloatPriceSpec(0.05))).doesNotThrowAnyException();
        assertThatCode(() -> muSigOffer(new MarketPriceSpec())).doesNotThrowAnyException();
        assertThatCode(() -> muSigOffer(new FloatPriceSpec(0.05))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("fixedPricesOfOtherMarkets")
    void bisqEasyOfferWithAFixedPriceOfAnotherMarketCannotBeCreated(FixPriceSpec fixedPriceOfAnotherMarket) {
        assertThatThrownBy(() -> bisqEasyOffer(fixedPriceOfAnotherMarket))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("fixedPricesOfOtherMarkets")
    void muSigOfferWithAFixedPriceOfAnotherMarketCannotBeCreated(FixPriceSpec fixedPriceOfAnotherMarket) {
        assertThatThrownBy(() -> muSigOffer(fixedPriceOfAnotherMarket))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("fixedPricesOfOtherMarkets")
    void receivedBisqEasyOfferWithAFixedPriceOfAnotherMarketFailsAtParsing(FixPriceSpec fixedPriceOfAnotherMarket)
            throws Exception {
        bisq.offer.protobuf.Offer received = serializeAndParse(bisqEasyOffer(fixedPriceOfTheMarket()).toProto(false)
                .toBuilder()
                .setPriceSpec(fixedPriceOfAnotherMarket.toProto(false))
                .build());

        assertThatThrownBy(() -> BisqEasyOffer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Offer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class);
    }

    @ParameterizedTest
    @MethodSource("fixedPricesOfOtherMarkets")
    void receivedMuSigOfferWithAFixedPriceOfAnotherMarketFailsAtParsing(FixPriceSpec fixedPriceOfAnotherMarket)
            throws Exception {
        bisq.offer.protobuf.Offer received = serializeAndParse(muSigOffer(fixedPriceOfTheMarket()).toProto(false)
                .toBuilder()
                .setPriceSpec(fixedPriceOfAnotherMarket.toProto(false))
                .build());

        assertThatThrownBy(() -> Offer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void receivedOfferWhoseMarketWasReplacedFailsAtParsing() throws Exception {
        Market btcEur = new Market("BTC", "EUR", "Bitcoin", "Euro");
        bisq.offer.protobuf.Offer received = serializeAndParse(bisqEasyOffer(fixedPriceOfTheMarket()).toProto(false)
                .toBuilder()
                .setMarket(btcEur.toProto(false))
                .build());

        assertThatThrownBy(() -> BisqEasyOffer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static FixPriceSpec fixedPriceOfTheMarket() {
        return new FixPriceSpec(PriceQuote.fromFiatPrice(60_000, "USD"));
    }

    private static BisqEasyOffer bisqEasyOffer(PriceSpec priceSpec) {
        return new BisqEasyOffer(MAKER,
                Direction.BUY,
                BTC_USD,
                new QuoteSideFixedAmountSpec(1_000_000),
                priceSpec,
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                "",
                List.of("en"),
                "1.0.0");
    }

    private static MuSigOffer muSigOffer(PriceSpec priceSpec) {
        return new MuSigOffer(StringUtils.createUid(),
                MAKER,
                Direction.SELL,
                BTC_USD,
                new QuoteSideFixedAmountSpec(1_000_000),
                priceSpec,
                List.of(WISE),
                List.of(new CollateralOption(0.25, 0.25),
                        new AccountOption(WISE,
                                "a1b2c3d4e5f6a7b8c9d0a1b2c3d4e5f6a7b8c9d0",
                                Optional.empty(),
                                List.of(),
                                Optional.empty(),
                                List.of(),
                                new byte[20])),
                "1.0.0");
    }

    private static bisq.offer.protobuf.Offer serializeAndParse(bisq.offer.protobuf.Offer proto)
            throws InvalidProtocolBufferException {
        return bisq.offer.protobuf.Offer.parseFrom(proto.toByteArray());
    }

    private static NetworkId createNetworkId() {
        Address address = Address.from("127.0.0.1", 1001);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key-1001"));
    }
}
