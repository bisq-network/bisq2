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

package bisq.bonded_roles.market_price;

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.proto.UnresolvableProtobufException;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The oracle node signs its market price data as a whole, and the proof of work of the message which carries the data
 * commits to its hash. So a node must parse the data without dropping an entry, also if it does not know the market of
 * that entry. The set of known markets depends on the currency data of the runtime.
 * See docs/specifications/bonded-roles/market-prices.md.
 */
class AuthorizedMarketPriceDataTest {
    // No runtime derives a market from this code
    private static final String UNKNOWN_CURRENCY_CODE = "ZZZ";

    @Test
    void parsedDataHasTheHashOfTheSentDataIfTheReceiverDoesNotKnowAMarket() throws Exception {
        AuthorizedMarketPriceData sent = dataWithKnownAndUnknownMarket();
        assertThat(MarketRepository.findAnyMarketByMarketCodes("BTC/" + UNKNOWN_CURRENCY_CODE)).isEmpty();

        AuthorizedMarketPriceData received = AuthorizedMarketPriceData.fromProto(
                bisq.bonded_roles.protobuf.AuthorizedMarketPriceData.parseFrom(sent.serialize()));

        assertThat(received.serializeForHash()).isEqualTo(sent.serializeForHash());
        assertThat(received.getMarketPriceByCurrencyMap()).hasSize(2);
    }

    // "BTC/ZZZ/" must not be read as the market BTC/ZZZ: it would replace the entry of the real key and the parsed data
    // would have the hash of data which the oracle node never signed with that entry.
    @Test
    void keyWhichIsNotExactlyBaseAndQuoteCodeFailsParsing() {
        for (String key : new String[]{"BTC/" + UNKNOWN_CURRENCY_CODE + "/", "BTC/" + UNKNOWN_CURRENCY_CODE + "//",
                "BTC/" + UNKNOWN_CURRENCY_CODE + "/X", "BTC/", "/" + UNKNOWN_CURRENCY_CODE, "BTC"}) {
            bisq.bonded_roles.protobuf.AuthorizedMarketPriceData proto = dataWithKnownAndUnknownMarket().toProto(false)
                    .toBuilder()
                    .putMarketPriceByCurrencyMap(key, anyMarketPrice().toProto(false))
                    .build();

            assertThatThrownBy(() -> AuthorizedMarketPriceData.fromProto(proto))
                    .as("key=%s", key)
                    .isInstanceOf(UnresolvableProtobufException.class);
        }
    }

    // The limit of entries is checked before the entries are parsed, so that an oversized map costs no parsing
    @Test
    void mapAboveTheLimitOfEntriesFailsParsingBeforeItsEntriesAreParsed() {
        bisq.bonded_roles.protobuf.AuthorizedMarketPriceData.Builder builder = dataWithKnownAndUnknownMarket().toProto(false).toBuilder();
        // An entry without a price quote fails if it is parsed. Its exception has a cause, the one of the limit has none.
        for (int i = 0; i < 200; i++) {
            builder.putMarketPriceByCurrencyMap("BTC/Z" + i, bisq.bonded_roles.protobuf.MarketPrice.getDefaultInstance());
        }

        assertThatThrownBy(() -> AuthorizedMarketPriceData.fromProto(builder.build()))
                .isInstanceOf(UnresolvableProtobufException.class)
                .hasMessageContaining("marketPriceByCurrencyMap size");
    }

    private static MarketPrice anyMarketPrice() {
        return new MarketPrice(PriceQuote.fromFiatPrice(60000, UNKNOWN_CURRENCY_CODE),
                System.currentTimeMillis(),
                new MarketPriceProviderInfo(MarketPriceProvider.BISQAGGREGATE));
    }

    private static AuthorizedMarketPriceData dataWithKnownAndUnknownMarket() {
        long now = System.currentTimeMillis();
        TreeMap<Market, MarketPrice> marketPriceByMarket = new TreeMap<>();
        for (String code : new String[]{"USD", UNKNOWN_CURRENCY_CODE}) {
            MarketPrice marketPrice = new MarketPrice(PriceQuote.fromFiatPrice(60000, code),
                    now,
                    new MarketPriceProviderInfo(MarketPriceProvider.BISQAGGREGATE));
            marketPriceByMarket.put(marketPrice.getMarket(), marketPrice);
        }
        return new AuthorizedMarketPriceData(marketPriceByMarket, true);
    }
}
