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
import network.misq.api.DefaultApi;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.ContentViewController;
import network.misq.desktop.main.content.createoffer.CreateOfferController;
import network.misq.desktop.main.content.offerbook.details.OfferDetailsController;
import network.misq.desktop.overlay.OverlayController;

// As all controllers in a view hierarchy are created at startup we do not do anything in the constructors beside assigning fields.
// The initialize() method starts the MVC group. onViewAdded() is called when the view got added to the stage.
// onViewRemoved() when the view got removed from the stage.
public class OfferbookController implements Controller {
    private OfferbookModel model;
    @Getter
    private OfferbookView view;
    @Getter
    private final DefaultApi api;
    private final ContentViewController contentViewController;
    private final OverlayController overlayController;

    public OfferbookController(DefaultApi api, ContentViewController contentViewController, OverlayController overlayController) {
        this.api = api;
        this.contentViewController = contentViewController;
        this.overlayController = overlayController;
    }

    @Override
    public void initialize() {
        model = new OfferbookModel(api);
        model.initialize();
        view = new OfferbookView(model, this);
    }

    @Override
    public void onViewAdded() {
        model.activate();

        // Platform.runLater(() -> onCreateOffer());
    }

    @Override
    public void onViewRemoved() {
        model.deactivate();
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
        overlayController.show(new CreateOfferController(api));
    }

    public void onTakeOffer(OfferListItem item) {
    }

    public void onShowMakerDetails(OfferListItem item, Bounds boundsInParent) {
        overlayController.show(new OfferDetailsController(item, boundsInParent));
    }
}
