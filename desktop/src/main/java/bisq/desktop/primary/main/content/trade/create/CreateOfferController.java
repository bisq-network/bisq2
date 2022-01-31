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
import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Market;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.main.content.trade.components.*;
import bisq.offer.Direction;
import bisq.offer.OfferService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class CreateOfferController implements InitWithDataController<CreateOfferController.InitData> {
    public static record InitData(Market market, Direction direction, BooleanProperty showCreateOfferTab) {
    }

    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final OfferService offerService;
    private final ChangeListener<SwapProtocolType> selectedProtocolTypListener;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final AmountPriceGroup amountPriceGroup;
    private final ProtocolSelection protocolSelection;
    private final SettlementSelection settlementSelection;

    public CreateOfferController(DefaultApplicationService applicationService) {
        offerService = applicationService.getOfferService();
        model = new CreateOfferModel();

        marketSelection = new MarketSelection(applicationService.getUserService());
        model.setSelectedMarketProperty(marketSelection.selectedMarketProperty());

        directionSelection = new DirectionSelection(model.selectedMarketProperty());
        model.setDirectionProperty(directionSelection.directionProperty());

        amountPriceGroup = new AmountPriceGroup(model.selectedMarketProperty(),
                model.directionProperty(),
                applicationService.getMarketPriceService());
        model.setBaseSideAmountProperty(amountPriceGroup.baseSideAmountProperty());
        model.setQuoteSideAmountProperty(amountPriceGroup.quoteSideAmountProperty());
        model.setFixPriceProperty(amountPriceGroup.fixPriceProperty());

        protocolSelection = new ProtocolSelection(model.selectedMarketProperty());
        model.setSelectedProtocolTypeProperty(protocolSelection.selectedProtocolType());

        settlementSelection = new SettlementSelection(model.selectedMarketProperty(),
                model.directionProperty(),
                model.selectedProtocolTypeProperty(),
                applicationService.getAccountService());
        model.setSelectedBaseSideAccounts(settlementSelection.getSelectedBaseSideAccounts());
        model.setSelectedQuoteSideAccounts(settlementSelection.getSelectedQuoteSideAccounts());
        model.setSelectedBaseSideSettlementMethods(settlementSelection.getSelectedBaseSideSettlementMethods());
        model.setSelectedQuoteSideSettlementMethods(settlementSelection.getSelectedQuoteSideSettlementMethods());

        view = new CreateOfferView(model, this,
                marketSelection.getView(),
                directionSelection.getView(),
                amountPriceGroup.getView(),
                protocolSelection.getView(),
                settlementSelection.getView());

        selectedProtocolTypListener = (observable, oldValue, newValue) -> model.getCreateOfferButtonVisibleProperty().set(newValue != null);
    }

    @Override
    public void initWithData(InitData data) {
        marketSelection.setSelectedMarket(data.market());
        directionSelection.setDirection(data.direction());
        model.showCreateOfferTab = data.showCreateOfferTab();
    }

    @Override
    public void onViewAttached() {
        model.selectedProtocolTypeProperty().addListener(selectedProtocolTypListener);
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
        model.showCreateOfferTab.set(false);
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }

    public void onCancel() {
        model.showCreateOfferTab.set(false);
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }
}
