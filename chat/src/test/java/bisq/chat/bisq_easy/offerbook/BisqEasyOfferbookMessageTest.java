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

package bisq.chat.bisq_easy.offerbook;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Only the maker publishes its offer in the offerbook, so that nobody can show the offer of another user.
// An offer is published only in the channel of its market, so that it is not shown in the offerbook of another market.
class BisqEasyOfferbookMessageTest {
    private static final Market BTC_EUR = new Market("BTC", "EUR", "Bitcoin", "Euro");
    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");

    private final NetworkId maker = createNetworkId(1001);
    private final NetworkId otherUser = createNetworkId(1002);

    @Test
    void makerCanPublishItsOffer() {
        assertDoesNotThrow(() -> message(maker.getPubKey().getId(), Optional.of(offer(maker))));
        assertDoesNotThrow(() -> message(otherUser.getPubKey().getId(), Optional.empty()));
    }

    @Test
    void messageWithOfferOfAnotherMakerCannotBeCreated() {
        assertThrows(IllegalArgumentException.class,
                () -> message(otherUser.getPubKey().getId(), Optional.of(offer(maker))));
    }

    @Test
    void receivedMessageWithOfferOfAnotherMakerFailsAtParsing() {
        bisq.chat.protobuf.ChatMessage proto = message(maker.getPubKey().getId(), Optional.of(offer(maker)))
                .toProto(false).toBuilder()
                .setAuthorUserProfileId(otherUser.getPubKey().getId())
                .build();

        assertThrows(UnresolvableProtobufException.class, () -> BisqEasyOfferbookMessage.fromProto(proto));
    }

    // The amount conversions of the offer lists throw for such an offer, so it must not reach them
    // (see docs/specifications/offer/fixed-price.md)
    @Test
    void receivedMessageWithAnOfferWhoseFixedPriceIsOfAnotherMarketFailsAtParsing() {
        PriceSpec fixedPriceOfTheMarket = new FixPriceSpec(PriceQuote.fromFiatPrice(55_000, "EUR"));
        PriceSpec fixedPriceOfAnotherMarket = new FixPriceSpec(
                PriceQuote.from(Coin.asXmrFromFaceValue(1), Coin.asBtcFromFaceValue(0.005)));
        BisqEasyOfferbookMessage message = message(maker.getPubKey().getId(),
                Optional.of(offer(maker, fixedPriceOfTheMarket)));
        bisq.chat.protobuf.ChatMessage.Builder builder = message.toProto(false).toBuilder();
        builder.getBisqEasyOfferbookMessageBuilder().getBisqEasyOfferBuilder()
                .setPriceSpec(fixedPriceOfAnotherMarket.toProto(false));
        bisq.chat.protobuf.ChatMessage proto = builder.build();

        assertDoesNotThrow(() -> BisqEasyOfferbookMessage.fromProto(message.toProto(false)));
        assertThrows(UnresolvableProtobufException.class, () -> BisqEasyOfferbookMessage.fromProto(proto));
    }

    // Bisq Connect shows the price of an offer from the quote side amount, while nodes convert the amounts of a trade
    // with the value. So an offer in the offerbook needs the canonical form (rule 5 of
    // docs/specifications/offer/fixed-price.md). The quote here has the shape which Bisq Connect sent through API nodes
    // of version 2.1.13 (bisq-mobile 7f9cddce7b): the base side id BTC, and a value rounded to 8 significant digits.
    @Test
    void offerWithAFixedPriceWhichIsNotCanonicalIsRejectedFromTheOfferbook() {
        PriceSpec canonical = new FixPriceSpec(PriceQuote.from(Coin.asBtcFromFaceValue(1.0),
                Fiat.fromValue(643_211_234L, "EUR")));
        PriceSpec ofBisqConnect = new FixPriceSpec(new PriceQuote(643_211_230L,
                new Coin("BTC", 100_000_000L, "BTC", 8, 4),
                new Fiat("EUR", 643_211_234L, "EUR", 4, 2)));
        BisqEasyOfferbookMessage message = message(maker.getPubKey().getId(), Optional.of(offer(maker, canonical)));
        bisq.chat.protobuf.ChatMessage.Builder builder = message.toProto(false).toBuilder();
        builder.getBisqEasyOfferbookMessageBuilder().getBisqEasyOfferBuilder()
                .setPriceSpec(ofBisqConnect.toProto(false));
        bisq.chat.protobuf.ChatMessage received = builder.build();

        assertDoesNotThrow(() -> BisqEasyOfferbookMessage.fromProto(message.toProto(false)));
        assertThrows(IllegalArgumentException.class,
                () -> message(maker.getPubKey().getId(), Optional.of(offer(maker, ofBisqConnect))));
        assertThrows(UnresolvableProtobufException.class, () -> BisqEasyOfferbookMessage.fromProto(received));
    }

    @Test
    void messageInTheChannelOfTheMarketOfItsOfferCanBeCreatedAndParsed() {
        BisqEasyOfferbookMessage message = message(BisqEasyOfferbookChannel.createId(BTC_EUR),
                maker.getPubKey().getId(),
                Optional.of(offer(maker)));

        BisqEasyOfferbookMessage parsed = assertDoesNotThrow(
                () -> BisqEasyOfferbookMessage.fromProto(message.toProto(false)));
        assertEquals(message, parsed);
    }

    @Test
    void messageWithAnOfferInTheChannelOfAnotherMarketCannotBeCreated() {
        assertThrows(IllegalArgumentException.class,
                () -> message(BisqEasyOfferbookChannel.createId(BTC_USD),
                        maker.getPubKey().getId(),
                        Optional.of(offer(maker))));
    }

    @Test
    void receivedMessageWithAnOfferInTheChannelOfAnotherMarketFailsAtParsing() {
        bisq.chat.protobuf.ChatMessage proto = message(maker.getPubKey().getId(), Optional.of(offer(maker)))
                .toProto(false);
        bisq.chat.protobuf.ChatMessage protoInTheChannelOfAnotherMarket = proto.toBuilder()
                .setChannelId(BisqEasyOfferbookChannel.createId(BTC_USD))
                .build();

        assertDoesNotThrow(() -> BisqEasyOfferbookMessage.fromProto(proto));
        assertThrows(UnresolvableProtobufException.class,
                () -> BisqEasyOfferbookMessage.fromProto(protoInTheChannelOfAnotherMarket));
    }

    @Test
    void messageWithoutOfferCanBeInTheChannelOfAnyMarket() {
        for (Market market : List.of(BTC_EUR, BTC_USD)) {
            BisqEasyOfferbookMessage message = message(BisqEasyOfferbookChannel.createId(market),
                    otherUser.getPubKey().getId(),
                    Optional.empty());

            assertDoesNotThrow(() -> BisqEasyOfferbookMessage.fromProto(message.toProto(false)));
        }
    }

    // The message is in the channel of the market of its offer. A message without offer is in the channel of BTC_EUR.
    private static BisqEasyOfferbookMessage message(String authorUserProfileId, Optional<BisqEasyOffer> offer) {
        Market market = offer.map(BisqEasyOffer::getMarket).orElse(BTC_EUR);
        return message(BisqEasyOfferbookChannel.createId(market), authorUserProfileId, offer);
    }

    private static BisqEasyOfferbookMessage message(String channelId,
                                                    String authorUserProfileId,
                                                    Optional<BisqEasyOffer> offer) {
        return new BisqEasyOfferbookMessage(channelId,
                authorUserProfileId,
                offer,
                Optional.of("text"),
                Optional.empty(),
                System.currentTimeMillis(),
                false);
    }

    private static BisqEasyOffer offer(NetworkId maker) {
        return offer(maker, new MarketPriceSpec());
    }

    private static BisqEasyOffer offer(NetworkId maker, PriceSpec priceSpec) {
        return new BisqEasyOffer(maker,
                Direction.BUY,
                BTC_EUR,
                new BaseSideFixedAmountSpec(100_000),
                priceSpec,
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                "",
                List.of("en"),
                "1.0.0");
    }

    private static NetworkId createNetworkId(int port) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "key-" + port));
    }
}
