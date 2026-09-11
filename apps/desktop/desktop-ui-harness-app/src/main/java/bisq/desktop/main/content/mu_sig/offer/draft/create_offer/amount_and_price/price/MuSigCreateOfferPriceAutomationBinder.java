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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.price;

import bisq.desktop_ui_harness_app.AbstractDesktopAutomationViewBinder;

public final class MuSigCreateOfferPriceAutomationBinder extends AbstractDesktopAutomationViewBinder<MuSigCreateOfferPriceView> {
    @Override
    public Class<MuSigCreateOfferPriceView> viewType() {
        return MuSigCreateOfferPriceView.class;
    }

    @Override
    public void bind(MuSigCreateOfferPriceView view) {
        scope(view.getRoot(), "mu-sig-create-offer-price");
        id(view.percentageInput(), "percentage-input");
        id(view.percentagePriceModeAction(), "percentage-mode");
        id(view.fixedPriceModeAction(), "fixed-mode");
        id(view.priceSlider(), "slider");
        id(view.fixedPriceInput(), "fixed-input");
    }
}
