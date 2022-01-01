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

package network.misq.desktop.main.content.createoffer;

import lombok.Getter;
import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.main.content.createoffer.assetswap.amounts.SetAmountsController;
import network.misq.desktop.main.content.createoffer.assetswap.review.ReviewOfferController;
import network.misq.desktop.main.content.createoffer.assetswap.review.ReviewOfferView;

public class CreateOfferController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final ReviewOfferController reviewOfferController;
    private final SetAmountsController setAmountsController;

    public CreateOfferController(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        model = new CreateOfferModel();
        view = new CreateOfferView(model, this);

        reviewOfferController = new ReviewOfferController(serviceProvider);
        setAmountsController = new SetAmountsController();
    }

    @Override
    public void initialize() {
        reviewOfferController.initialize();
        setAmountsController.initialize();

        ReviewOfferView view = reviewOfferController.getView();
        model.selectView(view);
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
    }

    public void onNavigateBack() {
    }

    public void onNavigateNext() {
    }
}
