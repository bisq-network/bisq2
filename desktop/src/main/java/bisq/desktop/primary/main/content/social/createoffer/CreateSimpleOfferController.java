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

package bisq.desktop.primary.main.content.social.createoffer;

import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Market;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.main.content.trade.components.AmountPriceGroup;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.components.MarketSelection;
import bisq.desktop.primary.main.content.trade.components.SettlementSelection;
import bisq.offer.OpenOfferService;
import bisq.offer.spec.Direction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class CreateSimpleOfferController implements InitWithDataController<CreateSimpleOfferController.InitData> {

    public static record InitData(Market market, Direction direction, boolean showCreateOfferTab) {
    }

    private final CreateSimpleOfferModel model;
    @Getter
    private final CreateSimpleOfferView view;
    private final OpenOfferService openOfferService;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final AmountPriceGroup amountPriceGroup;
    private final SettlementSelection settlementSelection;
    private Subscription selectedMarketSubscription, directionSubscription,
            baseSideAmountSubscription, quoteSideAmountSubscription, fixPriceSubscription;

    public CreateSimpleOfferController(DefaultApplicationService applicationService) {
        openOfferService = applicationService.getOpenOfferService();
        model = new CreateSimpleOfferModel();

        directionSelection = new DirectionSelection();
        marketSelection = new MarketSelection(applicationService.getSettingsService());
        amountPriceGroup = new AmountPriceGroup(applicationService.getMarketPriceService());
        settlementSelection = new SettlementSelection(applicationService.getAccountService());

        view = new CreateSimpleOfferView(model, this,
                marketSelection.getRoot(),
                directionSelection.getRoot(),
                amountPriceGroup.getRoot(),
                settlementSelection.getRoot());
    }

    @Override
    public void initWithData(InitData data) {
        marketSelection.setSelectedMarket(data.market());
        directionSelection.setDirection(data.direction());
    }

    @Override
    public void onActivate() {
        selectedMarketSubscription = EasyBind.subscribe(marketSelection.selectedMarketProperty(),
                selectedMarket -> {
                    model.setSelectedMarket(selectedMarket);
                    directionSelection.setSelectedMarket(selectedMarket);
                    amountPriceGroup.setSelectedMarket(selectedMarket);
                    settlementSelection.setSelectedMarket(selectedMarket);
                });
        directionSubscription = EasyBind.subscribe(directionSelection.directionProperty(),
                direction -> {
                    model.setDirection(direction);
                    settlementSelection.setDirection(direction);
                });
        baseSideAmountSubscription = EasyBind.subscribe(amountPriceGroup.baseSideAmountProperty(),
                model::setBaseSideAmount);
        quoteSideAmountSubscription = EasyBind.subscribe(amountPriceGroup.quoteSideAmountProperty(),
                model::setQuoteSideAmount);
        fixPriceSubscription = EasyBind.subscribe(amountPriceGroup.fixPriceProperty(),
                model::setFixPrice);


        model.setAllSelectedQuoteSideSettlementMethods(settlementSelection.getSelectedQuoteSideSettlementMethods());
    }

    @Override
    public void onDeactivate() {
        selectedMarketSubscription.unsubscribe();
        directionSubscription.unsubscribe();
        baseSideAmountSubscription.unsubscribe();
        quoteSideAmountSubscription.unsubscribe();
        fixPriceSubscription.unsubscribe();
    }

    public void onCreateOffer() {
      /*  openOfferService.createOffer(model.getSelectedMarket(),
                        model.getDirection(),
                        model.getBaseSideAmount(),
                        model.getFixPrice(),
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
                });*/
    }

    public void onPublishOffer() {
        openOfferService.publishOffer(model.getOffer());
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }

    public void onCancel() {
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }
}
