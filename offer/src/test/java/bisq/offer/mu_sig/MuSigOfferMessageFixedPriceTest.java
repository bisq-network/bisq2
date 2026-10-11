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

import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.options.CollateralOption;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An offer in the offerbook needs a canonical fixed price (rule 5 of docs/specifications/offer/fixed-price.md).
 */
class MuSigOfferMessageFixedPriceTest {
    @Test
    void offerWithAFixedPriceWhichIsNotCanonicalCannotBePublished() {
        PriceSpec canonical = new FixPriceSpec(PriceQuote.from(Coin.asBtcFromFaceValue(1.0),
                Fiat.fromValue(643_211_234L, "USD")));
        // The base side id BTC and a value rounded to 8 significant digits, as Bisq Connect builds a fixed price
        PriceSpec notCanonical = new FixPriceSpec(new PriceQuote(643_211_230L,
                new Coin("BTC", 100_000_000L, "BTC", 8, 4),
                new Fiat("USD", 643_211_234L, "USD", 4, 2)));

        assertDoesNotThrow(() -> new MuSigOfferMessage(offer(canonical)));
        assertThrows(IllegalArgumentException.class, () -> new MuSigOfferMessage(offer(notCanonical)));
    }

    private static MuSigOffer offer(PriceSpec priceSpec) {
        NetworkId makerNetworkId = new NetworkId(new AddressByTransportTypeMap(Map.of(TransportType.CLEAR,
                Address.from("127.0.0.1", 9999))),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "maker"));
        return new MuSigOffer("offerId",
                makerNetworkId,
                Direction.SELL,
                new Market("BTC", "USD", "Bitcoin", "US Dollar"),
                new BaseSideFixedAmountSpec(1_000_000),
                priceSpec,
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA)),
                List.of(new CollateralOption(MuSigOffer.DEFAULT_BUYER_SECURITY_DEPOSIT,
                        MuSigOffer.DEFAULT_SELLER_SECURITY_DEPOSIT)),
                "1");
    }
}
