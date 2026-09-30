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

package bisq.settings;

import bisq.common.application.Service;
import bisq.common.market.Market;
import bisq.common.observable.collection.ObservableSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FavouriteMarketsService implements Service {
    private static final int MAX_ALLOWED_FAVOURITES = 5;

    private final ObservableSet<Market> favouriteMarkets;

    public FavouriteMarketsService(ObservableSet<Market> favouriteMarkets) {
        this.favouriteMarkets = favouriteMarkets;
    }

    public boolean isFavourite(Market market) {
        return favouriteMarkets.contains(market);
    }

    public void addFavourite(Market market) {
        if (!canAddNewFavourite()) {
            return;
        }

        if (!favouriteMarkets.contains(market)) {
            favouriteMarkets.add(market);
            log.info("Market added to favourites. Total favourites now: {}", favouriteMarkets.size());
        } else {
            log.info("Market is already in favourites.");
        }
    }

    public void removeFavourite(Market market) {
        if (favouriteMarkets.contains(market)) {
            favouriteMarkets.remove(market);
            log.info("Market removed from favourites. Total favourites now: {}", favouriteMarkets.size());
        } else {
            log.info("Attempted to remove a market that is not in favourites.");
        }
    }

    public boolean canAddNewFavourite() {
        log.info("Current number of favourite markets: {}", favouriteMarkets.size());

        if (favouriteMarkets.size() == MAX_ALLOWED_FAVOURITES) {
            log.info("Cannot add more favourites. Max number of favourites ({}) reached.", MAX_ALLOWED_FAVOURITES);
            return false;
        }
        return true;
    }
}
