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

// The daemon's balance as BDK reports it, in satoshis. Spendable is what the daemon reports as the balance:
// confirmed plus trusted pending, never untrusted pending or immature funds.
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
public final class BalanceBreakdown {
    private final long spendable;
    private final long confirmed;
    private final long trustedPending;
    private final long untrustedPending;
    private final long immature;
}
