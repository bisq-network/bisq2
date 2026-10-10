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
import bisq.offer.options.FeeOption;
import bisq.offer.options.OfferOption;
import bisq.offer.options.ReputationOption;
import bisq.offer.options.TradeTermsOption;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rules 1, 2 and 4 of docs/specifications/offer/offer-options.md.
 */
class OfferOptionTypesTest {
    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final PaymentMethod<?> WISE = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
    private static final NetworkId MAKER = createNetworkId();
    private static final AccountOption ACCOUNT_OPTION = new AccountOption(WISE,
            "a1b2c3d4e5f6a7b8c9d0a1b2c3d4e5f6a7b8c9d0",
            Optional.empty(),
            List.of(),
            Optional.empty(),
            List.of(),
            new byte[20]);
    private static final FeeOption FEE_OPTION = new FeeOption(FeeOption.FeeType.BSQ, 800_000, "a".repeat(64));

    @Test
    void bisqEasyOfferWithTradeTermsAndReputationOptionsIsCreatedAndParsed() throws Exception {
        BisqEasyOffer offer = bisqEasyOffer();

        assertThat(offer.getOfferOptions())
                .hasAtLeastOneElementOfType(TradeTermsOption.class)
                .hasAtLeastOneElementOfType(ReputationOption.class);
        assertThat(BisqEasyOffer.fromProto(serializeAndParse(offer.toProto(false)))).isEqualTo(offer);
    }

    @Test
    void receivedBisqEasyOfferWithAnAccountOptionFailsAtParsing() throws Exception {
        assertBisqEasyOfferFailsAtParsing(withOption(bisqEasyOffer().toProto(false), ACCOUNT_OPTION));
    }

    @Test
    void receivedBisqEasyOfferWithAFeeOptionFailsAtParsing() throws Exception {
        assertBisqEasyOfferFailsAtParsing(withOption(bisqEasyOffer().toProto(false), FEE_OPTION));
    }

    @Test
    void muSigOfferWithAccountAndCollateralOptionsIsCreatedAndParsed() throws Exception {
        MuSigOffer offer = muSigOffer();

        assertThat(Offer.fromProto(serializeAndParse(offer.toProto(false)))).isEqualTo(offer);
    }

    @Test
    void receivedMuSigOfferWithAFeeOptionFailsAtParsing() throws Exception {
        bisq.offer.protobuf.Offer received = serializeAndParse(withOption(muSigOffer().toProto(false), FEE_OPTION));

        assertThatThrownBy(() -> Offer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void receivedMuSigOfferWithATradeTermsOptionFailsAtParsing() throws Exception {
        bisq.offer.protobuf.Offer received = serializeAndParse(withOption(muSigOffer().toProto(false),
                new TradeTermsOption("terms")));

        assertThatThrownBy(() -> Offer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static void assertBisqEasyOfferFailsAtParsing(bisq.offer.protobuf.Offer offer) throws Exception {
        bisq.offer.protobuf.Offer received = serializeAndParse(offer);

        assertThatThrownBy(() -> BisqEasyOffer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Offer.fromProto(received))
                .isInstanceOf(UnresolvableProtobufException.class);
    }

    private static bisq.offer.protobuf.Offer withOption(bisq.offer.protobuf.Offer offer, OfferOption option) {
        return offer.toBuilder().addOfferOptions(option.toProto(false)).build();
    }

    private static BisqEasyOffer bisqEasyOffer() {
        return new BisqEasyOffer(MAKER,
                Direction.BUY,
                BTC_USD,
                new QuoteSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                "terms",
                List.of("en"),
                "1.0.0");
    }

    private static MuSigOffer muSigOffer() {
        return new MuSigOffer(StringUtils.createUid(),
                MAKER,
                Direction.SELL,
                BTC_USD,
                new QuoteSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(WISE),
                List.of(new CollateralOption(0.25, 0.25), ACCOUNT_OPTION),
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
