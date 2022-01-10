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
import bisq.common.data.Pair;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import javafx.geometry.Bounds;
import lombok.Getter;

import java.util.Optional;

import static bisq.desktop.NavigationTarget.CREATE_OFFER;
import static bisq.desktop.NavigationTarget.OFFER_DETAILS;

public class OfferbookController extends NavigationController {
    @Getter
    private final OfferbookModel model;
    @Getter
    private final OfferbookView view;
    @Getter
    private final DefaultServiceProvider serviceProvider;

    public OfferbookController(DefaultServiceProvider serviceProvider) {
        super(NavigationTarget.OFFERBOOK);
        
        this.serviceProvider = serviceProvider;
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
        Navigation.navigateTo(CREATE_OFFER);
    }

    public void onTakeOffer(OfferListItem item) {
    }

    public void onShowMakerDetails(OfferListItem item, Bounds boundsInParent) {
        Navigation.navigateTo(OFFER_DETAILS, new Pair<>(item, boundsInParent));
    }

    @Override
    protected Optional<Controller> createController(NavigationTarget navigationTarget, Optional<Object> data) {
        switch (navigationTarget) {
          /*  case CREATE_OFFER -> {
                return Optional.of(new CreateOfferController(serviceProvider));
            }*/
           /* case OFFER_DETAILS -> {
                Pair<OfferListItem, Bounds> pair = (Pair) data.get();
                OfferListItem item = pair.first();
                Bounds boundsInParent = pair.second();
                return Optional.of(new OfferDetailsController(item, boundsInParent));
            }*/
            default -> {
                return Optional.empty();
            }
        }
    }
}
