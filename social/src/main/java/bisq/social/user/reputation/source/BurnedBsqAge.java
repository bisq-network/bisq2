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

package bisq.social.user.reputation.source;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class BurnedBsqAge {
    private final ReputationSource source= ReputationSource.BURNED_BSQ_AGE;
    private final String txId;
    private final byte[] pubKeyHash;
    private final long amount;
    private final long date;

    public BurnedBsqAge(String txId, byte[] pubKeyHash, long amount, long date) {
        this.txId = txId;
        this.pubKeyHash = pubKeyHash;
        this.amount = amount;
        this.date = date;
    }
}