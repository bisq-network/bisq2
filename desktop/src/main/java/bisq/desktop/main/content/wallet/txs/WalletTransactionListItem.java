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

package bisq.desktop.main.content.wallet.txs;

import bisq.common.monetary.Coin;
import bisq.desktop.components.table.DateTableItem;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.wallets.core.model.Transaction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Getter
@EqualsAndHashCode
public class WalletTransactionListItem implements DateTableItem {
    private final long date;
    private final String dateString, timeString;
    private final String txId;
    private final String amountAsString;
    private final String confirmationsAsString;
    private final Coin amount;
    private final int confirmations;

    public WalletTransactionListItem(Transaction transaction) {
        date = transaction.getDate().orElseGet(Date::new).getTime();
        dateString = DateFormatter.formatDate(date);
        timeString = DateFormatter.formatTime(date);
        txId = transaction.getTxId();
        amount = transaction.getAmount();
        amountAsString = AmountFormatter.formatAmount(amount);
        confirmations = transaction.getConfirmations();
        confirmationsAsString = String.valueOf(confirmations);
    }
}
