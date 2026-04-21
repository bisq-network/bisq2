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

import bisq.common.data.Range;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TradeAmountRange extends Range<TradeAmount> {
    public TradeAmountRange(TradeAmount min, TradeAmount max) {
        super(min, max);
        checkNotNull(min, "min must not be null");
        checkNotNull(max, "max must not be null");
        checkArgument(min.getBaseSideAmount().getCode().equals(max.getBaseSideAmount().getCode()),
                "min and max base side codes must match. min.base=%s; max.base=%s",
                min.getBaseSideAmount().getCode(), max.getBaseSideAmount().getCode());
        checkArgument(min.getQuoteSideAmount().getCode().equals(max.getQuoteSideAmount().getCode()),
                "min and max quote side codes must match. min.quote=%s; max.quote=%s",
                min.getQuoteSideAmount().getCode(), max.getQuoteSideAmount().getCode());
    }
}
