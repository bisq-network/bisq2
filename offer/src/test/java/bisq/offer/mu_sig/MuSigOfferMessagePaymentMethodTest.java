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

package bisq.offer.mu_sig;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.options.CollateralOption;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An offer in the offerbook offers only payment methods which support the currencies of its market (rule 1 of
 * docs/specifications/offer/payment-methods.md).
 */
class MuSigOfferMessagePaymentMethodTest {
    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");

    @Test
    void offerWithPaymentMethodsOfTheCurrencyOfItsMarketCanBePublished() {
        assertDoesNotThrow(() -> new MuSigOfferMessage(offer(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE))));
        assertDoesNotThrow(() -> new MuSigOfferMessage(offer(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE))));
    }

    @Test
    void offerWithAPaymentMethodOfAnotherCurrencyCannotBePublished() {
        assertThrows(IllegalArgumentException.class,
                () -> new MuSigOfferMessage(offer(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))));
    }

    @Test
    void receivedOfferMessageWithAPaymentMethodOfAnotherCurrencyFailsAtParsing() {
        MuSigOfferMessage message = new MuSigOfferMessage(offer(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE)));
        bisq.offer.protobuf.MuSigOfferMessage.Builder builder = message.toProto(false).toBuilder();
        builder.getOfferBuilder()
                .clearQuoteSidePaymentSpecs()
                .addQuoteSidePaymentSpecs(new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA))
                        .toProto(false));
        bisq.offer.protobuf.MuSigOfferMessage received = builder.build();

        assertDoesNotThrow(() -> MuSigOfferMessage.fromProto(message.toProto(false)));
        assertThrows(UnresolvableProtobufException.class, () -> MuSigOfferMessage.fromProto(received));
    }

    private static MuSigOffer offer(PaymentMethod<?> paymentMethod) {
        NetworkId makerNetworkId = new NetworkId(new AddressByTransportTypeMap(Map.of(TransportType.CLEAR,
                Address.from("127.0.0.1", 9999))),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "maker"));
        return new MuSigOffer("offerId",
                makerNetworkId,
                Direction.SELL,
                BTC_USD,
                new BaseSideFixedAmountSpec(1_000_000),
                new MarketPriceSpec(),
                List.of(paymentMethod),
                List.of(new CollateralOption(MuSigOffer.DEFAULT_BUYER_SECURITY_DEPOSIT,
                        MuSigOffer.DEFAULT_SELLER_SECURITY_DEPOSIT)),
                "1");
    }
}
