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

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.NavigationTargetTab;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class TradeView extends TabView<JFXTabPane, TradeModel, TradeController> {

    private NavigationTargetTab createOffer;
    private NavigationTargetTab takeOffer;
    private Subscription createOfferTabVisibleSubscription, takeOfferTabVisibleSubscription;

    public TradeView(TradeModel model, TradeController controller) {
        super(new JFXTabPane(), model, controller);
    }

    @Override
    protected void createAndAddTabs() {
        NavigationTargetTab offerbook = createTab(Res.common.get("trade.offerbook"), NavigationTarget.OFFERBOOK);
        createOffer = createTab(Res.common.get("trade.createOffer"), NavigationTarget.CREATE_OFFER);
        takeOffer = createTab(Res.common.get("trade.takeOffer"), NavigationTarget.TAKE_OFFER);
        root.getTabs().setAll(offerbook);
    }

    @Override
    public void onViewAttached() {
        createOfferTabVisibleSubscription = EasyBind.subscribe(model.createOfferTabVisible, this::onCreateOfferTabVisibleChange);
        takeOfferTabVisibleSubscription = EasyBind.subscribe(model.takeOfferTabVisible, this::onTakeOfferTabVisibleChange);
        super.onViewAttached();
    }

    @Override
    public void onViewDetached() {
        super.onViewDetached();
        createOfferTabVisibleSubscription.unsubscribe();
        takeOfferTabVisibleSubscription.unsubscribe();
    }

    private void onCreateOfferTabVisibleChange(boolean value) {
        if (value) {
            root.getTabs().add(createOffer);
        } else {
            root.getTabs().remove(createOffer);
        }
    }

    private void onTakeOfferTabVisibleChange(boolean value) {
        if (value) {
            root.getTabs().add(takeOffer);
        } else {
            root.getTabs().remove(takeOffer);
        }
    }
}
