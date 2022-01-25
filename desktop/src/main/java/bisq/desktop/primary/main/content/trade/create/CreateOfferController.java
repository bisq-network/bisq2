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

package bisq.desktop.primary.main.content.trade.create;

import bisq.account.protocol.SwapProtocolType;
import bisq.application.DefaultServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.trade.components.*;
import bisq.offer.Direction;
import bisq.offer.OfferService;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class CreateOfferController implements Controller {
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final OfferService offerService;
    private final ChangeListener<SwapProtocolType> selectedProtocolTypListener;

    public CreateOfferController(DefaultServiceProvider serviceProvider) {
        offerService = serviceProvider.getOfferService();
        MarketPriceService marketPriceService = serviceProvider.getMarketPriceService();
        model = new CreateOfferModel();

        var marketSelectionController = new MarketSelection.MarketSelectionController(model.getSelectedMarketProperty(),
                marketPriceService);
        var directionController = new DirectionSelection.DirectionController(model.getDirectionProperty(),
                model.selectedMarketProperty());
        var amountPriceController = new AmountPriceGroup.AmountPriceController(model.getBaseSideAmountProperty(),
                model.getQuoteSideAmountProperty(),
                model.getFixPriceProperty(),
                model.selectedMarketProperty(),
                model.directionProperty(),
                marketPriceService);
        var protocolSelectionController = new ProtocolSelection.ProtocolController(model.getSelectedProtocolTypeProperty(),
                model.selectedMarketProperty());
        var accountSelectionController = new AccountSelection.AccountController(
                model.getSelectedBaseSideAccounts(),
                model.getSelectedQuoteSideAccounts(),
                model.getSelectedBaseSideSettlementMethods(),
                model.getSelectedQuoteSideSettlementMethods(),
                model.selectedMarketProperty(),
                model.directionProperty(),
                model.selectedProtocolTypeProperty(),
                serviceProvider.getAccountService());


        view = new CreateOfferView(model, this,
                marketSelectionController.getView(),
                directionController.getView(),
                amountPriceController.getView(),
                protocolSelectionController.getView(),
                accountSelectionController.getView());

        selectedProtocolTypListener = (observable, oldValue, newValue) -> model.getCreateOfferButtonVisibleProperty().set(newValue != null);
    }

    @Override
    public void onViewAttached() {
        model.selectedProtocolTypeProperty().addListener(selectedProtocolTypListener);
        model.getDirectionProperty().set(Direction.BUY);
        model.getCreateOfferButtonVisibleProperty().set(model.getSelectedProtocolType() != null);
    }

    @Override
    public void onViewDetached() {
        model.selectedProtocolTypeProperty().removeListener(selectedProtocolTypListener);
    }

    public void onCreateOffer() {
        offerService.createOffer(model.getSelectedMarket(),
                        model.getDirection(),
                        model.getBaseSideAmount(),
                        model.getFixPrice(),
                        model.getSelectedProtocolType(),
                        new ArrayList<>(model.getSelectedBaseSideAccounts()),
                        new ArrayList<>(model.getSelectedQuoteSideAccounts()),
                        new ArrayList<>(model.getSelectedBaseSideSettlementMethods()),
                        new ArrayList<>(model.getSelectedQuoteSideSettlementMethods()))
                .whenComplete((offer, throwable) -> {
                    if (throwable == null) {
                        model.getOfferProperty().set(offer);
                    } else {
                        //todo provide error to UI
                    }
                });
    }

    public void onPublishOffer() {
        offerService.publishOffer(model.getOffer());
    }
}
