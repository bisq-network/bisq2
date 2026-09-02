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

package bisq.offer.mu_sig.use_case.create_offer.market;

import bisq.common.application.LifecycleScope;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Pin;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkNotNull;
@Slf4j
public class MarketSelection extends LifecycleScope {
    @Delegate
    private final CreateOfferMarketModel model;
    private final Set<Consumer<Market>> listeners = new CopyOnWriteArraySet<>();

    public MarketSelection() {
        this.model = new CreateOfferMarketModel();
    }

    @Override
    public void initialize() {
        if (getMarket() == null) {
            Market market = MarketRepository.getDefaultBtcFiatMarket();
            applyMarket(market, false);
        }
    }


    /* --------------------------------------------------------------------- */
    // User input
    /* --------------------------------------------------------------------- */

    public void onSetMarket(Market market) {
        checkNotNull(market, "market must not be null");
        applyMarket(market, true);
    }


    private void applyMarket(Market market, boolean notifyListeners) {
        if (!market.equals(model.getMarket())) {
            model.setMarket(market);
            if (notifyListeners) {
                listeners.forEach(listener -> listener.accept(market));
            }
        }
    }

    public Pin addMarketListener(Consumer<Market> listener) {
        checkNotNull(listener, "listener must not be null");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }
}
