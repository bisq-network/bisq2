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

package bisq.user;

import bisq.common.monetary.Market;
import bisq.common.monetary.MarketRepository;
import bisq.persistence.PersistableStore;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class UserStore implements PersistableStore<UserStore> {
    private final Cookie cookie;
    private final List<Market> markets;
    @Setter
    private Market selectedMarket;

    public UserStore() {
        cookie = new Cookie();
        markets = MarketRepository.getMajorMarkets();
        selectedMarket = MarketRepository.getDefault();
    }

    public UserStore(Cookie cookie, List<Market> markets, Market selectedMarket) {
        this.cookie = cookie;
        this.markets = markets;
        this.selectedMarket = selectedMarket;
    }

    @Override
    public UserStore getClone() {
        return new UserStore(cookie, markets, selectedMarket);
    }

    @Override
    public void applyPersisted(UserStore persisted) {
        cookie.putAll(persisted.getCookie());
        markets.clear();
        markets.addAll(persisted.getMarkets());
        selectedMarket = persisted.getSelectedMarket();
    }
}