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
import bisq.common.market.MarketRepository;
import bisq.offer.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MuSigOfferTest {

    private MuSigOffer createOffer() {
        Market market = MarketRepository.getDefaultBtcFiatMarket();
        return new MuSigOffer("source-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER)),
                List.of(),
                "1.0.0");
    }

    @Test
    void cloneWithNewIdGivesNewIdAndKeepsAllOtherTerms() {
        MuSigOffer source = createOffer();
        MuSigOffer clone = MuSigOffer.cloneWithNewId(source, "new-id");

        assertEquals("new-id", clone.getId());
        assertNotEquals(source.getId(), clone.getId());

        assertEquals(source.getMakerNetworkId(), clone.getMakerNetworkId());
        assertEquals(source.getDirection(), clone.getDirection());
        assertEquals(source.getMarket(), clone.getMarket());
        assertEquals(source.getAmountSpec(), clone.getAmountSpec());
        assertEquals(source.getPriceSpec(), clone.getPriceSpec());
        assertEquals(source.getProtocolTypes(), clone.getProtocolTypes());
        assertEquals(source.getBaseSidePaymentMethodSpecs(), clone.getBaseSidePaymentMethodSpecs());
        assertEquals(source.getQuoteSidePaymentMethodSpecs(), clone.getQuoteSidePaymentMethodSpecs());
        assertEquals(source.getOfferOptions(), clone.getOfferOptions());
        assertEquals(source.getTradeProtocolVersion(), clone.getTradeProtocolVersion());
    }
}
