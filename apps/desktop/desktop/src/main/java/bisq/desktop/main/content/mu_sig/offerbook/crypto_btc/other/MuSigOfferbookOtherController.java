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

package bisq.desktop.main.content.mu_sig.offerbook.crypto_btc.other;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.mu_sig.offerbook.crypto_btc.MuSigOfferbookCryptoBtcController;
import bisq.offer.Direction;
import bisq.settings.CookieKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigOfferbookOtherController extends MuSigOfferbookCryptoBtcController<MuSigOfferbookOtherModel, MuSigOfferbookOtherView> {
    public MuSigOfferbookOtherController(ServiceProvider serviceProvider, Direction direction) {
        super(serviceProvider, direction);
    }

    @Override
    protected MuSigOfferbookOtherView createAndGetView() {
        return new MuSigOfferbookOtherView(model, this);
    }

    @Override
    protected MuSigOfferbookOtherModel createAndGetModel(Direction direction) {
        return new MuSigOfferbookOtherModel(direction);
    }

    @Override
    public void onActivate() {
        model.getMarkets().setAll(MarketRepository.getAllNonXMRCryptoCurrencyMarkets());

        super.onActivate();
    }

    @Override
    protected Market getDefaultMarket() {
        return MarketRepository.getDefaultCryptoBtcMarket();
    }

    @Override
    protected boolean isExpectedMarket(Market market) {
        return market.isCrypto() && !market.isXmr();
    }

    @Override
    protected CookieKey getSelectedMarketCookieKey() {
        return CookieKey.MU_SIG_OFFERBOOK_SELECTED_OTHER_MARKET;
    }
}