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

import bisq.common.currency.Market;
import bisq.common.observable.collection.ObservableSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FavouriteMarketsService {
    private static final int MAX_ALLOWED_FAVOURITES = 10;

    public static boolean isFavourite(Market market) {
        return SettingsService.getInstance().getFavouriteMarkets().contains(market);
    }

    public static void addFavourite(Market market) {
        ObservableSet<Market> favouriteMarkets = SettingsService.getInstance().getFavouriteMarkets();
        log.info("Current number of favourite markets: {}", favouriteMarkets.size());

        if (favouriteMarkets.size() == MAX_ALLOWED_FAVOURITES) {
            log.info("Cannot add more favourites. Max number of favourites ({}) reached.", MAX_ALLOWED_FAVOURITES);
            return;
        }

        if (!favouriteMarkets.contains(market)) {
            favouriteMarkets.add(market);
            persist();
            log.info("Market added to favourites. Total favourites now: {}", favouriteMarkets.size());
        } else {
            log.info("Market is already in favourites.");
        }
    }

    public static void removeFavourite(Market market) {
        ObservableSet<Market> favouriteMarkets = SettingsService.getInstance().getFavouriteMarkets();

        if (favouriteMarkets.contains(market)) {
            favouriteMarkets.remove(market);
            persist();
            log.info("Market removed from favourites. Total favourites now: {}", favouriteMarkets.size());
        } else {
            log.info("Attempted to remove a market that is not in favourites.");
        }
    }

    private static void persist() {
        SettingsService.getInstance().persist();
    }
}
