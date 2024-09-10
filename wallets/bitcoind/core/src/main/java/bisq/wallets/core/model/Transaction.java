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

package bisq.wallets.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Getter
@EqualsAndHashCode
public class Transaction {
    private final String txId;
    private final List<TransactionInput> inputs;
    private final List<TransactionOutput> outputs;
    private final int lockTime;
    private final int height;
    private final Optional<Date> date;
    private final int confirmations;
    private final long amount;
    private final boolean incoming;

    public Transaction(String txId,
                       List<TransactionInput> inputs,
                       List<TransactionOutput> outputs,
                       int lockTime,
                       int height,
                       Optional<Date> date,
                       int confirmations,
                       long amount,
                       boolean incoming) {

        this.txId = txId;
        this.inputs = inputs;
        this.outputs = outputs;
        this.lockTime = lockTime;
        this.height = height;
        this.date = date;
        this.confirmations = confirmations;
        this.amount = amount;
        this.incoming = incoming;
    }
}