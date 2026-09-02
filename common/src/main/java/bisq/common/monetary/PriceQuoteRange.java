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
import bisq.common.market.Market;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PriceQuoteRange extends Range<PriceQuote> {
    public PriceQuoteRange(PriceQuote min, PriceQuote max) {
        super(min, max);
        checkNotNull(min, "min must not be null");
        checkNotNull(max, "max must not be null");
        checkArgument(min.getBaseSideMonetary().getCode().equals(max.getBaseSideMonetary().getCode()),
                "min and max base side codes must match. min.base=%s; max.base=%s",
                min.getBaseSideMonetary().getCode(), max.getBaseSideMonetary().getCode());
        checkArgument(min.getQuoteSideMonetary().getCode().equals(max.getQuoteSideMonetary().getCode()),
                "min and max quote side codes must match. min.quote=%s; max.quote=%s",
                min.getQuoteSideMonetary().getCode(), max.getQuoteSideMonetary().getCode());
    }

    public String printRelevantStringa(Market market) {
        String min, max;
        if (market.getRelevantCurrencyCode().equals(getMin().getQuoteSideMonetary().getCode())) {
            min = getMin().getQuoteSideMonetary().asDouble() + " " + getMin().getQuoteSideMonetary().getCode();
            max = getMax().getQuoteSideMonetary().asDouble() + " " + getMax().getQuoteSideMonetary().getCode();
        } else {
            min = getMin().getBaseSideMonetary().asDouble() + " " + getMin().getBaseSideMonetary().getCode();
            max = getMax().getBaseSideMonetary().asDouble() + " " + getMax().getBaseSideMonetary().getCode();
        }
        return min + " - " + max;
    }

    public String printRelevantString(Market market) {
        PriceQuote minMonetary = getMin();
        PriceQuote maxMonetary = getMax();
        if (market.getRelevantCurrencyCode().equals(minMonetary.getQuoteSideMonetary().getCode())) {
            return minMonetary.getQuoteSideMonetary().printAsDouble(false) + " - " +
                    maxMonetary.getQuoteSideMonetary().printAsDouble();
        } else {
            return minMonetary.getBaseSideMonetary().printAsDouble(false) + " - " +
                    maxMonetary.getBaseSideMonetary().printAsDouble();
        }
    }
}
