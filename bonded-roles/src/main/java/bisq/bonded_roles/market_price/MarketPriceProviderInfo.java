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

package bisq.bonded_roles.market_price;

import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@EqualsAndHashCode
@ToString
public class MarketPriceProviderInfo {
    @Getter
    private final MarketPriceProvider marketPriceProvider;
    // The name which a price node reported for a provider that MarketPriceProvider does not know. MarketPrice does not
    // serialize it, so a market price from the network or from the persisted store does not have it.
    private final Optional<String> unknownProviderName;


    public MarketPriceProviderInfo(MarketPriceProvider marketPriceProvider) {
        this.marketPriceProvider = marketPriceProvider;
        this.unknownProviderName = Optional.empty();
    }

    public MarketPriceProviderInfo(MarketPriceProvider marketPriceProvider, String reportedProviderName) {
        this.marketPriceProvider = marketPriceProvider;
        this.unknownProviderName = marketPriceProvider == MarketPriceProvider.OTHER
                ? Optional.of(reportedProviderName)
                : Optional.empty();
    }

    public String getDisplayName() {
        return marketPriceProvider.getDisplayName()
                .or(() -> unknownProviderName)
                .orElseGet(() -> Res.get("data.na"));
    }
}
