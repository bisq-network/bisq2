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

package network.misq.desktop.main.content.offerbook;

import javafx.geometry.Bounds;
import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.NavigationTarget;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.offerbook.details.OfferDetailsController;
import network.misq.desktop.main.left.NavigationController;
import network.misq.desktop.overlay.OverlayController;

public class OfferbookController implements Controller {
    private final OfferbookModel model;
    @Getter
    private final OfferbookView view;
    @Getter
    private final DefaultServiceProvider serviceProvider;
    private final NavigationController navigationController;
    private final OverlayController overlayController;

    public OfferbookController(DefaultServiceProvider serviceProvider, NavigationController navigationController, OverlayController overlayController) {
        this.serviceProvider = serviceProvider;
        this.navigationController = navigationController;
        this.overlayController = overlayController;
        model = new OfferbookModel(serviceProvider);
        view = new OfferbookView(model, this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // View events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onSelectAskCurrency(String currency) {
        if (currency != null) {
            model.setSelectAskCurrency(currency);
        }
    }

    public void onSelectBidCurrency(String currency) {
        if (currency != null) {
            model.setSelectBidCurrency(currency);
        }
    }

    public void onFlipCurrencies() {
        model.resetFilter();
    }

    public void onCreateOffer() {
        navigationController.navigateTo(NavigationTarget.CREATE_OFFER);
    }

    public void onTakeOffer(OfferListItem item) {
    }

    public void onShowMakerDetails(OfferListItem item, Bounds boundsInParent) {
        overlayController.show(new OfferDetailsController(item, boundsInParent));
    }
}
