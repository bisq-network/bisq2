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

package bisq.common.monetary;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class PriceQuoteTest {
    @Test
    void testToQuoteMonetary() {
        Coin btc = Coin.asBtcFromFaceValue(1.0);
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary quoteSideMonetary = priceQuote.toQuoteSideMonetary(btc);
        assertTrue(quoteSideMonetary instanceof Fiat);
        assertEquals(500000000, quoteSideMonetary.value);

        btc = Coin.asBtcFromFaceValue(2.0);
        priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        quoteSideMonetary = priceQuote.toQuoteSideMonetary(btc);
        assertEquals(1000000000, quoteSideMonetary.value);
    }
}