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
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Only the maker publishes its offer in the offerbook, so that nobody can show the offer of another user
class BisqEasyOfferbookMessageTest {
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

    private static BisqEasyOfferbookMessage message(String authorUserProfileId, Optional<BisqEasyOffer> offer) {
        return new BisqEasyOfferbookMessage("channelId",
                authorUserProfileId,
                offer,
                Optional.of("text"),
                Optional.empty(),
                System.currentTimeMillis(),
                false);
    }

    private static BisqEasyOffer offer(NetworkId maker) {
        return new BisqEasyOffer(maker,
                Direction.BUY,
                new Market("BTC", "EUR", "Bitcoin", "Euro"),
                new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(),
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
