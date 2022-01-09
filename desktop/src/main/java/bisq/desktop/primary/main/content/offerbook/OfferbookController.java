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

package bisq.desktop.primary.main.content.offerbook;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.content.createoffer.CreateOfferController;
import bisq.desktop.primary.main.content.offerbook.details.OfferDetailsController;
import javafx.geometry.Bounds;
import lombok.Getter;

public class OfferbookController extends NavigationController {
    private final OfferbookModel model;
    @Getter
    private final OfferbookView view;
    @Getter
    private final DefaultServiceProvider serviceProvider;
    private final OverlayController overlayController;

    public OfferbookController(DefaultServiceProvider serviceProvider, 
                               ContentController contentController, 
                               OverlayController overlayController) {
        super(contentController, overlayController);
        this.serviceProvider = serviceProvider;
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
        navigateTo(NavigationTarget.CREATE_OFFER);
    }

    public void onTakeOffer(OfferListItem item) {
    }

    public void onShowMakerDetails(OfferListItem item, Bounds boundsInParent) {
        overlayController.show(new OfferDetailsController(item, boundsInParent));
    }

    @Override
    protected Controller getController(NavigationTarget localTarget, NavigationTarget navigationTarget) {
        switch (localTarget) {
            case CREATE_OFFER -> {
                return new CreateOfferController(serviceProvider);
            }
            default -> throw new IllegalArgumentException("Invalid navigationTarget for this host. localTarget=" + localTarget);
        }
    }

    @Override
    protected NavigationTarget resolveLocalTarget(NavigationTarget navigationTarget) {
        return resolveAsRootHost(navigationTarget);
    }
}
