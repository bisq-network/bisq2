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

package bisq.desktop.primary.main.content.swap.create;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.swap.create.components.*;
import bisq.offer.Direction;
import bisq.offer.OfferService;
import bisq.oracle.marketprice.MarketPriceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferController implements Controller {
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final OfferService offerService;

    public CreateOfferController(DefaultServiceProvider serviceProvider) {
        offerService = serviceProvider.getOfferService();
        MarketPriceService marketPriceService = serviceProvider.getMarketPriceService();
        OfferPreparationModel offerPreparationModel = new OfferPreparationModel();

        var marketSelectionController = new MarketSelection.MarketSelectionController(offerPreparationModel, marketPriceService);
        var directionController = new DirectionSelection.DirectionController(offerPreparationModel);
        var amountPriceController = new AmountPriceGroup.AmountPriceController(offerPreparationModel, marketPriceService);
        var protocolSelectionController = new ProtocolSelection.ProtocolController(offerPreparationModel);
        var accountSelectionController = new AccountSelection.AccountController(offerPreparationModel, serviceProvider.getAccountService());

        model = new CreateOfferModel(offerPreparationModel);
        view = new CreateOfferView(model, this,
                marketSelectionController.getView(),
                directionController.getView(),
                amountPriceController.getView(),
                protocolSelectionController.getView(),
                accountSelectionController.getView());
    }

    @Override
    public void onViewAttached() {
        model.setDirection(Direction.BUY);
    }

    @Override
    public void onViewDetached() {  
    }

    public void onCreateOffer() {
        offerService.createOffer(model.getSelectedMarket(),
                        model.getDirection(),
                        model.getBaseSideAmount(),
                        model.getQuoteSideAmount(),
                        model.getFixPrice(),
                        model.getSelectedProtocolTyp(),
                        null, //todo
                        null)
                .whenComplete((offer, throwable) -> {
                    if (throwable == null) {
                        model.getOffer().set(offer);
                    } else {
                        //todo provide error to UI
                    }
                });
    }

    public void onPublishOffer() {
        offerService.publishOffer(model.getOffer().get());
    }
}
