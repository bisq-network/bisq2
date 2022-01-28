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

package bisq.desktop.primary.main.content.trade.take;

import bisq.application.DefaultApplicationService;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.main.content.trade.components.AmountPriceGroup;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.take.components.TakersSettlementSelection;
import bisq.offer.Offer;
import bisq.oracle.marketprice.MarketPriceService;
import javafx.beans.property.BooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TakeOfferController implements InitWithDataController<TakeOfferController.InitData> {
    public static record InitData(Offer offer, BooleanProperty showTakeOfferTab) {
    }

    private final TakeOfferModel model;
    @Getter
    private final TakeOfferView view;

    private final AmountPriceGroup amountPriceGroup;
    private final TakersSettlementSelection settlementSelection;
    private final MarketPriceService marketPriceService;
    private final DirectionSelection directionSelection;


    public TakeOfferController(DefaultApplicationService applicationService) {
        marketPriceService = applicationService.getMarketPriceService();
        model = new TakeOfferModel();

        // will prob use custom design/component not reuse DirectionSelection
        directionSelection = new DirectionSelection(model.selectedMarketProperty);
        directionSelection.setIsTakeOffer();
        model.setDirectionProperty(directionSelection.directionProperty());

        amountPriceGroup = new AmountPriceGroup(model.selectedMarketProperty,
                model.directionProperty,
                marketPriceService);
        amountPriceGroup.setIsTakeOffer();

        settlementSelection = new TakersSettlementSelection(model.selectedMarketProperty,
                model.directionProperty,
                model.selectedProtocolTypeProperty,
                applicationService.getAccountService());

        model.setSelectedBaseSideAccounts(settlementSelection.getSelectedBaseSideAccounts());
        model.setSelectedQuoteSideAccounts(settlementSelection.getSelectedQuoteSideAccounts());
        model.setSelectedBaseSideSettlementMethods(settlementSelection.getSelectedBaseSideSettlementMethods());
        model.setSelectedQuoteSideSettlementMethods(settlementSelection.getSelectedQuoteSideSettlementMethods());


        view = new TakeOfferView(model, this,
                directionSelection.getView(),
                amountPriceGroup.getView(),
                settlementSelection.getView());
    }

    @Override
    public void initWithData(InitData initData) {
        Offer offer = initData.offer();
        model.offer = offer;
        model.selectedMarketProperty.set(offer.getMarket());
        directionSelection.setDirection(offer.getDirection().mirror());
        model.selectedProtocolTypeProperty.set(offer.findProtocolType().orElseThrow());

        model.baseSideAmount = offer.getBaseAmountAsMonetary();
        model.quoteSideAmount = offer.getQuoteAmountAsMonetary(marketPriceService);
        model.fixPrice = offer.getQuote(marketPriceService);

        amountPriceGroup.setBaseSideAmount(model.baseSideAmount);
        amountPriceGroup.setQuoteSideAmount(model.quoteSideAmount);
        amountPriceGroup.setFixPrice(model.fixPrice);

        settlementSelection.setOffer(offer);

        model.showTakeOfferTab = initData.showTakeOfferTab;
    }

    @Override
    public void onViewAttached() {
    }

    @Override
    public void onViewDetached() {
    }


    public void onTakeOffer() {
        model.showTakeOfferTab.set(false);
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }

    public void onCancel() {
        model.showTakeOfferTab.set(false);
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }
}
