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

package bisq.desktop.primary.main.content.trade;

import bisq.desktop.common.view.FxNavigationTargetTab;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class TradeView extends TabView<TradeModel, TradeController> {

    private FxNavigationTargetTab offerbook, createOffer, takeOffer;
    private Subscription createOfferTabVisibleSubscription, takeOfferTabVisibleSubscription;

    public TradeView(TradeModel model, TradeController controller) {
        super(model, controller);
        addTab(Res.get("trade.offerbook"), NavigationTarget.OFFERBOOK);

        headlineLabel.setText(Res.get("trade"));
    }

    @Override
    protected void onViewAttached() {
        createOfferTabVisibleSubscription = EasyBind.subscribe(model.createOfferTabVisible, this::onCreateOfferTabVisibleChange);
        takeOfferTabVisibleSubscription = EasyBind.subscribe(model.takeOfferTabVisible, this::onTakeOfferTabVisibleChange);
    }

    @Override
    protected void onViewDetached() {
        createOfferTabVisibleSubscription.unsubscribe();
        takeOfferTabVisibleSubscription.unsubscribe();
    }

    private void onCreateOfferTabVisibleChange(boolean value) {
        if (value) {
            addTab(Res.get("trade.createOffer"), NavigationTarget.CREATE_OFFER);
        } else {
            removeTab(NavigationTarget.CREATE_OFFER);
        }
    }

    private void onTakeOfferTabVisibleChange(boolean value) {
        if (value) {
            addTab(Res.get("trade.takeOffer"), NavigationTarget.TAKE_OFFER);
        } else {
            removeTab(NavigationTarget.TAKE_OFFER);
        }
    }
}
