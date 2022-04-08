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
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.CachingController;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.main.content.portfolio.pending.PendingTradesController;
import bisq.desktop.primary.main.content.trade.components.AmountPriceGroup;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.take.components.TakersSettlementSelection;
import bisq.offer.Offer;
import bisq.offer.spec.Direction;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.protocol.ProtocolService;
import javafx.beans.property.BooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TakeOfferController implements InitWithDataController<TakeOfferController.InitData>, CachingController {
    public static record InitData(Offer offer, BooleanProperty showTakeOfferTab) {
    }

    private final MarketPriceService marketPriceService;
    private final ProtocolService protocolService;

    private final TakeOfferModel model;
    @Getter
    private final TakeOfferView view;

    private final AmountPriceGroup amountPriceGroup;
    private final TakersSettlementSelection settlementSelection;
    private final DirectionSelection directionSelection;
    private Subscription selectedBaseSideAccountSubscription, selectedQuoteSideAccountSubscription,
            selectedBaseSideSettlementMethodSubscription, selectedQuoteSideSettlementMethodSubscription;

    public TakeOfferController(DefaultApplicationService applicationService) {
        marketPriceService = applicationService.getMarketPriceService();
        protocolService = applicationService.getProtocolService();
        model = new TakeOfferModel();

        // will prob use custom design/component not reuse DirectionSelection
        directionSelection = new DirectionSelection();
        directionSelection.setIsTakeOffer();
        amountPriceGroup = new AmountPriceGroup(marketPriceService);
        amountPriceGroup.setIsTakeOffer();
        settlementSelection = new TakersSettlementSelection(applicationService.getAccountService());
        view = new TakeOfferView(model, this,
                directionSelection.getRoot(),
                amountPriceGroup.getRoot(),
                settlementSelection.getRoot());
    }

    @Override
    public void initWithData(InitData initData) {
        Offer offer = initData.offer();
        model.offer = offer;
        Direction direction = offer.getDirection();
        model.direction = direction;
        model.setSelectedProtocolType(offer.findProtocolType().orElseThrow());
        model.baseSideAmount = offer.getBaseAmountAsMonetary();
        model.quoteSideAmount = offer.getQuoteAmountAsMonetary(marketPriceService);
        model.fixPrice = offer.getQuote(marketPriceService);

        Market market = offer.getMarket();
        directionSelection.setSelectedMarket(market);
        directionSelection.setDirection(direction.mirror());
        directionSelection.hideDirection(direction);

        amountPriceGroup.setSelectedMarket(market);
        amountPriceGroup.setBaseSideAmount(model.baseSideAmount);
        amountPriceGroup.setQuoteSideAmount(model.quoteSideAmount);
        amountPriceGroup.setFixPrice(model.fixPrice);

        settlementSelection.setSelectedMarket(market);
        settlementSelection.setDirection(direction);
        settlementSelection.setSelectedProtocolType(model.getSelectedProtocolType());
        settlementSelection.setOffer(offer);

        model.showTakeOfferTab = initData.showTakeOfferTab;
    }

    @Override
    public void onActivate() {
        selectedBaseSideAccountSubscription = EasyBind.subscribe(settlementSelection.getSelectedBaseSideAccount(),
                model::setSelectedBaseSideAccount);
        selectedQuoteSideAccountSubscription = EasyBind.subscribe(settlementSelection.getSelectedQuoteSideAccount(),
                model::setSelectedQuoteSideAccount);
        selectedBaseSideSettlementMethodSubscription = EasyBind.subscribe(settlementSelection.getSelectedBaseSideSettlementMethod(),
                model::setSelectedBaseSideSettlementMethod);
        selectedQuoteSideSettlementMethodSubscription = EasyBind.subscribe(settlementSelection.getSelectedQuoteSideSettlementMethod(),
                model::setSelectedQuoteSideSettlementMethod);
    }

    @Override
    public void onDeactivate() {
        selectedBaseSideAccountSubscription.unsubscribe();
        selectedQuoteSideAccountSubscription.unsubscribe();
        selectedBaseSideSettlementMethodSubscription.unsubscribe();
        selectedQuoteSideSettlementMethodSubscription.unsubscribe();
    }

    public void onTakeOffer() {
        String baseSideSettlementMethod = model.getSelectedBaseSideSettlementMethod().name();
        String quoteSideSettlementMethod = model.getSelectedQuoteSideSettlementMethod().name();
        protocolService.takeOffer(model.getSelectedProtocolType(),
                        model.offer,
                        model.baseSideAmount,
                        model.quoteSideAmount,
                        baseSideSettlementMethod,
                        quoteSideSettlementMethod)
                .whenComplete((protocol, throwable) -> {
                    model.showTakeOfferTab.set(false);
                    Navigation.navigateTo(NavigationTarget.PENDING_TRADES, new PendingTradesController.InitData(protocol));
                });
    }

    public void onCancel() {
        model.showTakeOfferTab.set(false);
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }
}
