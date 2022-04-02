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

package bisq.desktop.primary.main.content.social.onboarding.onboardNewbie;

import bisq.application.DefaultApplicationService;
import bisq.common.monetary.Market;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.main.content.trade.components.DirectionSelection;
import bisq.desktop.primary.main.content.trade.components.MarketSelection;
import bisq.offer.spec.Direction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class OnboardNewbieController implements InitWithDataController<OnboardNewbieController.InitData> {


    public static record InitData(Market market, Direction direction, boolean showCreateOfferTab) {
    }

    private final OnboardNewbieModel model;
    @Getter
    private final OnboardNewbieView view;
    private final MarketSelection marketSelection;
    private final DirectionSelection directionSelection;
    private final BtcFiatAmountGroup btcFiatAmountGroup;
    private final PaymentMethodsSelection paymentMethodsSelection;

    private Subscription selectedMarketSubscription, directionSubscription, baseSideAmountSubscription;

    public OnboardNewbieController(DefaultApplicationService applicationService) {
        model = new OnboardNewbieModel();

        directionSelection = new DirectionSelection();
        marketSelection = new MarketSelection(applicationService.getSettingsService());
        btcFiatAmountGroup = new BtcFiatAmountGroup(applicationService.getMarketPriceService());
        paymentMethodsSelection = new PaymentMethodsSelection();

        view = new OnboardNewbieView(model, this,
                marketSelection.getRoot(),
                directionSelection.getRoot(),
                btcFiatAmountGroup.getRoot(),
                paymentMethodsSelection.getRoot());
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
                    btcFiatAmountGroup.setSelectedMarket(selectedMarket);
                    paymentMethodsSelection.setSelectedMarket(selectedMarket);
                });
        directionSubscription = EasyBind.subscribe(directionSelection.directionProperty(),
                direction -> {
                    model.setDirection(direction);
                    btcFiatAmountGroup.setDirection(direction);
                    paymentMethodsSelection.setDirection(direction);
                });
        baseSideAmountSubscription = EasyBind.subscribe(btcFiatAmountGroup.baseSideAmountProperty(),
                model::setBaseSideAmount);

        model.getSelectedPaymentMethods().setAll(paymentMethodsSelection.getSelectedPaymentMethods());
    }

    @Override
    public void onDeactivate() {
        selectedMarketSubscription.unsubscribe();
        directionSubscription.unsubscribe();
        baseSideAmountSubscription.unsubscribe();
    }

    public void onCreateOffer() {
      Navigation.navigateTo(NavigationTarget.CHAT); //todo
    }

    public void onPublishOffer() {
       Navigation.navigateTo(NavigationTarget.CHAT);//todo
    }

    public void onCancel() {
      Navigation.navigateTo(NavigationTarget.CHAT);//todo
    }
}
