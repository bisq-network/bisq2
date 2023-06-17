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

package bisq.desktop.primary.main.content.trade_apps.poc.old.takeOffer;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.trade_apps.poc.components_poc.AmountPriceGroup;
import bisq.desktop.primary.main.content.trade_apps.poc.components_poc.DirectionSelection;
import bisq.desktop.primary.main.content.trade_apps.poc.old.pendingTrades.PendingTradesController;
import bisq.desktop.primary.main.content.trade_apps.poc.old.takeOffer.components.TakersPaymentSelection;
import bisq.offer.Direction;
import bisq.offer.poc.PocOffer;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.protocol.poc.PocProtocolService;
import javafx.beans.property.BooleanProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TakeOfferController implements InitWithDataController<TakeOfferController.InitData> {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class InitData {
        private final PocOffer offer;
        private final BooleanProperty showTakeOfferTab;

        public InitData(PocOffer offer, BooleanProperty showTakeOfferTab) {
            this.offer = offer;
            this.showTakeOfferTab = showTakeOfferTab;
        }
    }

    private final MarketPriceService marketPriceService;
    private final PocProtocolService pocProtocolService;

    private final TakeOfferModel model;
    @Getter
    private final TakeOfferView view;

    private final AmountPriceGroup amountPriceGroup;
    private final TakersPaymentSelection paymentMethodSelection;
    private final DirectionSelection directionSelection;
    private Subscription selectedBaseSideAccountSubscription, selectedQuoteSideAccountSubscription,
            selectedBaseSidePaymentMethodSubscription, selectedQuoteSidePaymentMethodSubscription;

    public TakeOfferController(DefaultApplicationService applicationService) {
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        pocProtocolService = new PocProtocolService(applicationService.getNetworkService(),
                applicationService.getIdentityService(),
                applicationService.getPersistenceService(), null
                /*applicationService.getOfferService().getBisqEasyOfferService()*/);
        model = new TakeOfferModel();

        // will prob use custom design/component not reuse DirectionSelection
        directionSelection = new DirectionSelection();
        directionSelection.setIsTakeOffer();
        amountPriceGroup = new AmountPriceGroup(marketPriceService);
        amountPriceGroup.setIsTakeOffer();
        paymentMethodSelection = new TakersPaymentSelection(applicationService.getAccountService());
        view = new TakeOfferView(model, this,
                directionSelection.getRoot(),
                amountPriceGroup.getRoot(),
                paymentMethodSelection.getRoot());
    }

    @Override
    public void initWithData(InitData initData) {
        PocOffer offer = initData.getOffer();
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
        amountPriceGroup.setQuote(model.fixPrice);

        paymentMethodSelection.setSelectedMarket(market);
        paymentMethodSelection.setDirection(direction);
        paymentMethodSelection.setSelectedProtocolType(model.getSelectedProtocolType());
        paymentMethodSelection.setOffer(offer);

        model.showTakeOfferTab = initData.showTakeOfferTab;
    }

    @Override
    public void onActivate() {
        selectedBaseSideAccountSubscription = EasyBind.subscribe(paymentMethodSelection.getSelectedBaseSideAccount(),
                model::setSelectedBaseSideAccount);
        selectedQuoteSideAccountSubscription = EasyBind.subscribe(paymentMethodSelection.getSelectedQuoteSideAccount(),
                model::setSelectedQuoteSideAccount);
        selectedBaseSidePaymentMethodSubscription = EasyBind.subscribe(paymentMethodSelection.getSelectedBaseSidePaymentMethod(),
                model::setSelectedBaseSidePaymentPaymentRail);
        selectedQuoteSidePaymentMethodSubscription = EasyBind.subscribe(paymentMethodSelection.getSelectedQuoteSidePaymentMethod(),
                model::setSelectedQuoteSidePaymentPaymentRail);
    }

    @Override
    public void onDeactivate() {
        selectedBaseSideAccountSubscription.unsubscribe();
        selectedQuoteSideAccountSubscription.unsubscribe();
        selectedBaseSidePaymentMethodSubscription.unsubscribe();
        selectedQuoteSidePaymentMethodSubscription.unsubscribe();
    }

    public void onTakeOffer() {
        String baseSidePaymentMethod = model.getSelectedBaseSidePaymentPaymentRail().name();
        String quoteSidePaymentMethod = model.getSelectedQuoteSidePaymentPaymentRail().name();
        pocProtocolService.takeOffer(model.getSelectedProtocolType(),
                        model.offer,
                        model.baseSideAmount,
                        model.quoteSideAmount,
                        baseSidePaymentMethod,
                        quoteSidePaymentMethod)
                .whenComplete((protocol, throwable) -> {
                    model.showTakeOfferTab.set(false);
                    Navigation.navigateTo(NavigationTarget.MULTI_SIG_PENDING_TRADES, new PendingTradesController.InitData(protocol));
                });
    }

    public void onCancel() {
        model.showTakeOfferTab.set(false);
        Navigation.navigateTo(NavigationTarget.MULTI_SIG_OFFER_BOOK);
    }
}
