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
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.CollateralOption;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The payment methods of an offer support the currencies of the market of the offer (rule 1 of
 * docs/specifications/offer/payment-methods.md). The offerbook messages check the rule. The offer itself does not
 * check it at parsing, so that stored trades and mediation cases keep loading.
 */
class OfferPaymentMethodCurrencyTest {
    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final Market BTC_EUR = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private static final Market XMR_BTC = new Market("XMR", "BTC", "Monero", "Bitcoin");
    private static final NetworkId MAKER = createNetworkId();

    // Payment rails which support several, but not all, currencies
    static Stream<Arguments> multiCurrencyPaymentRailsAndTheirCurrencies() {
        return Stream.of(FiatPaymentRail.REVOLUT, FiatPaymentRail.WISE, FiatPaymentRail.UPHOLD,
                        FiatPaymentRail.ADVANCED_CASH, FiatPaymentRail.PAYSERA, FiatPaymentRail.MONEY_GRAM,
                        FiatPaymentRail.AMAZON_GIFT_CARD, FiatPaymentRail.MONESE, FiatPaymentRail.PERFECT_MONEY)
                .flatMap(paymentRail -> paymentRail.getSupportedCurrencyCodes().stream()
                        .map(code -> Arguments.of(paymentRail, code)));
    }

    @Test
    void bisqEasyOfferWithAPaymentMethodOfAnotherCurrencyBreaksTheRule() {
        BisqEasyOffer sepaInUsd = bisqEasyOffer(BTC_USD, fiat(FiatPaymentRail.SEPA));

        assertThatThrownBy(sepaInUsd::verifyPaymentMethodsSupportMarket)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quote side")
                .hasMessageContaining("quote currency");
    }

    @Test
    void oneUnsupportedPaymentMethodBreaksTheRule() {
        BisqEasyOffer offer = bisqEasyOffer(BTC_USD, fiat(FiatPaymentRail.ZELLE), fiat(FiatPaymentRail.SEPA));

        assertThatThrownBy(offer::verifyPaymentMethodsSupportMarket).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bisqEasyOfferWithPaymentMethodsOfItsCurrencyFollowsTheRule() {
        assertThatCode(() -> bisqEasyOffer(BTC_USD, fiat(FiatPaymentRail.ZELLE), fiat(FiatPaymentRail.STRIKE),
                fiat(FiatPaymentRail.ACH_TRANSFER)).verifyPaymentMethodsSupportMarket())
                .doesNotThrowAnyException();
        assertThatCode(() -> bisqEasyOffer(BTC_EUR, fiat(FiatPaymentRail.SEPA), fiat(FiatPaymentRail.SEPA_INSTANT))
                .verifyPaymentMethodsSupportMarket())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("multiCurrencyPaymentRailsAndTheirCurrencies")
    void multiCurrencyPaymentMethodFollowsTheRuleInEachOfItsCurrencies(FiatPaymentRail paymentRail, String code) {
        Market market = new Market("BTC", code, "Bitcoin", code);

        assertThatCode(() -> bisqEasyOffer(market, fiat(paymentRail)).verifyPaymentMethodsSupportMarket())
                .doesNotThrowAnyException();
    }

    @Test
    void multiCurrencyPaymentMethodBreaksTheRuleInACurrencyWhichItDoesNotSupport() {
        Market btcBrl = new Market("BTC", "BRL", "Bitcoin", "Brazilian Real");

        assertThatThrownBy(() -> bisqEasyOffer(btcBrl, fiat(FiatPaymentRail.REVOLUT)).verifyPaymentMethodsSupportMarket())
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Earlier versions offered national bank transfers also in EUR. A payment rail of all currencies and a custom
    // payment method follow the rule in every market.
    @Test
    void paymentMethodsOfAllCurrenciesAndCustomPaymentMethodsFollowTheRuleInEveryMarket() {
        for (Market market : List.of(BTC_USD, BTC_EUR, new Market("BTC", "QQQ", "Bitcoin", "Unknown"))) {
            assertThatCode(() -> bisqEasyOffer(market,
                    fiat(FiatPaymentRail.NATIONAL_BANK),
                    fiat(FiatPaymentRail.F2F),
                    fiat(FiatPaymentRail.SWIFT),
                    FiatPaymentMethod.fromCustomName("My local bank")).verifyPaymentMethodsSupportMarket())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void bitcoinSettlementMethodsFollowTheRule() {
        BisqEasyOffer offer = new BisqEasyOffer(MAKER,
                Direction.BUY,
                BTC_USD,
                new QuoteSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN),
                        BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.LN)),
                List.of(fiat(FiatPaymentRail.ZELLE)),
                "",
                List.of("en"),
                "1.0.0");

        assertThatCode(offer::verifyPaymentMethodsSupportMarket).doesNotThrowAnyException();
    }

    // Stored trades and mediation cases contain the offer. They were agreed before, so the rule must not make them
    // fail at parsing.
    @Test
    void offerWhichBreaksTheRuleIsCreatedAndParsed() throws Exception {
        BisqEasyOffer bisqEasyOffer = bisqEasyOffer(BTC_USD, fiat(FiatPaymentRail.SEPA));
        MuSigOffer muSigOffer = muSigOffer(BTC_USD, fiat(FiatPaymentRail.SEPA));

        assertThat(BisqEasyOffer.fromProto(serializeAndParse(bisqEasyOffer.toProto(false)))).isEqualTo(bisqEasyOffer);
        assertThat(Offer.fromProto(serializeAndParse(muSigOffer.toProto(false)))).isEqualTo(muSigOffer);
    }

    @Test
    void muSigOfferWithAPaymentMethodOfAnotherCurrencyBreaksTheRule() {
        assertThatThrownBy(() -> muSigOffer(BTC_USD, fiat(FiatPaymentRail.SEPA)).verifyPaymentMethodsSupportMarket())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> muSigOffer(BTC_USD, fiat(FiatPaymentRail.WISE)).verifyPaymentMethodsSupportMarket())
                .doesNotThrowAnyException();
    }

    // In a crypto market the base side has the payment method of the crypto asset, and the quote side has the
    // Bitcoin settlement method
    @Test
    void muSigOfferOfACryptoMarketFollowsTheRule() {
        assertThatCode(() -> muSigOffer(XMR_BTC, new CryptoPaymentMethod("XMR")).verifyPaymentMethodsSupportMarket())
                .doesNotThrowAnyException();
    }

    @Test
    void receivedMuSigOfferOfACryptoMarketWithThePaymentMethodOfAnotherAssetBreaksTheRule() throws Exception {
        bisq.offer.protobuf.Offer received = serializeAndParse(muSigOffer(XMR_BTC, new CryptoPaymentMethod("XMR"))
                .toProto(false)
                .toBuilder()
                .clearBaseSidePaymentSpecs()
                .addBaseSidePaymentSpecs(new CryptoPaymentMethodSpec(new CryptoPaymentMethod("LTC")).toProto(false))
                .build());

        Offer<?, ?> offer = Offer.fromProto(received);

        assertThatThrownBy(offer::verifyPaymentMethodsSupportMarket)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base side")
                .hasMessageContaining("base currency");
    }

    @Test
    void receivedBisqEasyOfferWithAPaymentMethodOfAnotherCurrencyBreaksTheRule() throws Exception {
        bisq.offer.protobuf.Offer received = serializeAndParse(bisqEasyOffer(BTC_USD, fiat(FiatPaymentRail.ZELLE))
                .toProto(false)
                .toBuilder()
                .clearQuoteSidePaymentSpecs()
                .addQuoteSidePaymentSpecs(new FiatPaymentMethodSpec(fiat(FiatPaymentRail.SEPA)).toProto(false))
                .build());

        BisqEasyOffer offer = BisqEasyOffer.fromProto(received);

        assertThatThrownBy(offer::verifyPaymentMethodsSupportMarket).isInstanceOf(IllegalArgumentException.class);
    }

    private static FiatPaymentMethod fiat(FiatPaymentRail paymentRail) {
        return FiatPaymentMethod.fromPaymentRail(paymentRail);
    }

    private static BisqEasyOffer bisqEasyOffer(Market market, FiatPaymentMethod... fiatPaymentMethods) {
        return new BisqEasyOffer(MAKER,
                Direction.BUY,
                market,
                new QuoteSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                List.of(fiatPaymentMethods),
                "",
                List.of("en"),
                "1.0.0");
    }

    private static MuSigOffer muSigOffer(Market market, PaymentMethod<?> paymentMethod) {
        return new MuSigOffer(StringUtils.createUid(),
                MAKER,
                Direction.SELL,
                market,
                new QuoteSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                List.of(new CollateralOption(0.25, 0.25)),
                "1.0.0");
    }

    private static bisq.offer.protobuf.Offer serializeAndParse(bisq.offer.protobuf.Offer proto) throws Exception {
        return bisq.offer.protobuf.Offer.parseFrom(proto.toByteArray());
    }

    private static NetworkId createNetworkId() {
        Address address = Address.from("127.0.0.1", 1001);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key-1001"));
    }
}
