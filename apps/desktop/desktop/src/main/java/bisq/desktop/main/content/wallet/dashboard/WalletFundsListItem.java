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
import bisq.presentation.formatters.AmountFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WalletFundsListItem {
    @EqualsAndHashCode.Include
    private final String address;

    private final String usageAsString, amountAsString, numConfirmationsAsString;
    private final Coin amount;
    private final int numUsage, numConfirmations;

    public WalletFundsListItem(String address, long amount, int numUsage, int numConfirmations) {
        this.address = address;
        this.numUsage = numUsage;
        this.numConfirmations = numConfirmations;
        this.amount = Coin.asBtcFromValue(amount);
        usageAsString = Res.get("wallet.funds.usage.description." + (numUsage == 1 ? "single" : "plural"), numUsage);
        amountAsString = AmountFormatter.formatBaseAmount(this.amount);
        numConfirmationsAsString = String.valueOf(numConfirmations);
    }
}
