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

package bisq.desktop.primary.main.content.multiSig;

import bisq.desktop.common.view.*;
import bisq.desktop.overlay.OverlayWindow;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

public class MultiSigView extends TabView<MultiSigModel, MultiSigController> {
    protected final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private OverlayWindow overlayWindow;

    public MultiSigView(MultiSigModel model, MultiSigController controller) {
        super(model, controller);

        addTab(Res.get("offerbook"), NavigationTarget.MULTI_SIG_OFFERBOOK);
        addTab(Res.get("openOffers"), NavigationTarget.MULTI_SIG_OPEN_OFFERS);
        addTab(Res.get("pendingTrades"), NavigationTarget.MULTI_SIG_PENDING_TRADES);
        addTab(Res.get("closedTrades"), NavigationTarget.MULTI_SIG_CLOSED_TRADES);

        headlineLabel.setText(Res.get("multiSig"));

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                if (overlayWindow != null) {
                    overlayWindow.close();
                    overlayWindow = null;
                }
                if (model.showCreateOffer.get()) {
                    overlayWindow = new OverlayWindow(getRoot(), childRoot, controller::onCloseCreateOffer);
                    overlayWindow.show();
                } else if (model.showTakeOffer.get()) {
                    overlayWindow = new OverlayWindow(getRoot(), childRoot, controller::onCloseTakeOffer);
                    overlayWindow.show();
                } else {
                    childRoot.getStyleClass().add("bisq-content-bg");
                }
            }
        };
    }

    @Override
    protected void onViewAttached() {
        model.getView().addListener(viewChangeListener);
    }

    @Override
    protected void onViewDetached() {
        model.getView().removeListener(viewChangeListener);
    }
}
