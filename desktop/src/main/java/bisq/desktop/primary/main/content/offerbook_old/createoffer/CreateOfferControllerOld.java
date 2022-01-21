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

package bisq.desktop.primary.main.content.offerbook_old.createoffer;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.offerbook_old.createoffer.assetswap.amounts.SetAmountsController;
import bisq.desktop.primary.main.content.offerbook_old.createoffer.assetswap.review.ReviewOfferController;
import bisq.desktop.primary.main.content.offerbook_old.createoffer.assetswap.review.ReviewOfferView;
import lombok.Getter;

public class CreateOfferControllerOld implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final ReviewOfferController reviewOfferController;
    private final SetAmountsController setAmountsController;

    public CreateOfferControllerOld(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        model = new CreateOfferModel();
        view = new CreateOfferView(model, this);

        reviewOfferController = new ReviewOfferController(serviceProvider);
        setAmountsController = new SetAmountsController();

        ReviewOfferView view = reviewOfferController.getView();
        model.selectView(view);
    }

    public void onNavigateBack() {
    }

    public void onNavigateNext() {
    }
}
