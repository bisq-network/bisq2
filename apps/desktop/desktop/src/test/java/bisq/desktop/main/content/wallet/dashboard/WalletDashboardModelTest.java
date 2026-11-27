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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.common.monetary.Coin;
import bisq.i18n.Res;
import bisq.wallet.vo.BalanceBreakdown;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WalletDashboardModelTest {
    @BeforeAll
    static void setUp() {
        Res.setAndApplyLanguageTag("en");
    }

    @Test
    void balancesFormatAsZeroUntilTheDaemonAnswered() {
        WalletDashboardModel model = new WalletDashboardModel();
        String zero = model.getFormattedBtcBalanceProperty().getValue();

        model.getBalanceAsCoinProperty().set(null);
        model.getAvailableBalanceAsCoinProperty().set(null);

        assertEquals(zero, model.getFormattedBtcBalanceProperty().getValue());
        assertEquals(zero, model.getFormattedAvailableBalanceProperty().getValue());
    }

    @Test
    void theTotalCountsEverythingAndAvailableOnlyTheSpendablePart() {
        BalanceBreakdown breakdown = new BalanceBreakdown(150, 100, 50, 30, 20);

        assertEquals(Coin.asBtcFromValue(200), WalletDashboardModel.totalOf(breakdown));
        assertEquals(Coin.asBtcFromValue(150), WalletDashboardModel.availableOf(breakdown));
        assertNull(WalletDashboardModel.totalOf(null));
        assertNull(WalletDashboardModel.availableOf(null));
    }

    @Test
    void balancesFollowTheCoinProperties() {
        WalletDashboardModel model = new WalletDashboardModel();

        model.getBalanceAsCoinProperty().set(Coin.asBtcFromValue(150_000_000));
        model.getAvailableBalanceAsCoinProperty().set(Coin.asBtcFromValue(100_000_000));

        assertEquals(WalletDashboardModel.formatBalance(Coin.asBtcFromValue(150_000_000)), model.getFormattedBtcBalanceProperty().getValue());
        assertEquals(WalletDashboardModel.formatBalance(Coin.asBtcFromValue(100_000_000)), model.getFormattedAvailableBalanceProperty().getValue());
    }
}
