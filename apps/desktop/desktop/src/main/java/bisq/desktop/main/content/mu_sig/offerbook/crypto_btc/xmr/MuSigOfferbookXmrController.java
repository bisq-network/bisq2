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

package bisq.desktop.main.content.mu_sig.offerbook.crypto_btc.xmr;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.mu_sig.offerbook.crypto_btc.MuSigOfferbookCryptoBtcController;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigOfferbookXmrController extends MuSigOfferbookCryptoBtcController<MuSigOfferbookXmrModel, MuSigOfferbookXmrView> {
    public MuSigOfferbookXmrController(ServiceProvider serviceProvider, Direction direction) {
        super(serviceProvider, direction);
    }

    @Override
    protected MuSigOfferbookXmrView createAndGetView() {
        return new MuSigOfferbookXmrView(model, this);
    }

    @Override
    protected MuSigOfferbookXmrModel createAndGetModel(Direction direction) {
        return new MuSigOfferbookXmrModel(direction, MarketRepository.getXmrMarket());
    }

    @Override
    public void onActivate() {
        super.onActivate();
    }

    @Override
    protected Market getDefaultMarket() {
        return MarketRepository.getXmrMarket();
    }

    @Override
    protected boolean isExpectedMarket(Market market) {
        return market.isXmr();
    }

    @Override
    protected void applyInitialSelectedMarket() {
        // Do nothing as we don't support multiple markets here
    }

    @Override
    protected CookieKey getSelectedMarketCookieKey() {
        throw new UnsupportedOperationException("getCookieKey is not supported in MuSigOfferbookXmrController");
    }
}