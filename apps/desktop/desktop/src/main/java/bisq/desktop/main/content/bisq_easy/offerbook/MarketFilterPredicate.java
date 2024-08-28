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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.bisq_easy.BisqEasyMarketFilter;
import lombok.Getter;

import java.util.function.Predicate;

@Getter
public class MarketFilterPredicate {
    public static Predicate<MarketChannelItem> getPredicate(BisqEasyMarketFilter bisqEasyMarketFilter) {
        switch (bisqEasyMarketFilter) {
            case ALL:
                return item -> true;
            case FAVOURITES:
                return item -> item.getIsFavourite().get();
            case WITH_OFFERS:
                return item -> item.getNumOffers().get() > 0;
            default:
                throw new IllegalArgumentException("Unexpected enum " + bisqEasyMarketFilter);
        }
    }
}

