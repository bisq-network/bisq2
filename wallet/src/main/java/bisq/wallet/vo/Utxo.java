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

package bisq.wallet.vo;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public final class Utxo {
    private final String txId;
    private final int vout;
    private final long amount;
    private final String address;
    private final int confirmations;

    public static Utxo fromProto(bisq.wallet.protobuf.Utxo utxo) {
        return new Utxo(
                utxo.getTxId(),
                utxo.getVout(),
                utxo.getAmount(),
                utxo.getAddress(),
                utxo.getConfirmations()
        );
    }
}
