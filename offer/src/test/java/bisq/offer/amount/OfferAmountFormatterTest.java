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

package bisq.offer.amount;

import bisq.common.locale.LocaleRepository;
import bisq.common.monetary.Coin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OfferAmountFormatterTest {

    @BeforeEach
    void setUp() {
        LocaleRepository.setDefaultLocale(Locale.US);
    }

    @Test
    void formatDepositAmountAsBtcFormatsWithCode() {
        String formatted = OfferAmountFormatter.formatDepositAmountAsBTC(Coin.asBtcFromValue(123_456_789L));
        assertEquals("1.23456789 BTC", formatted);
    }

    @Test
    void formatDepositAmountAsBtcRejectsNonBtc() {
        assertThrows(IllegalArgumentException.class,
                () -> OfferAmountFormatter.formatDepositAmountAsBTC(Coin.asXmrFromValue(1)));
    }
}
