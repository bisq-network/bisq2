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

package bisq.desktop.primary.overlay.bisq_easy.take_offer;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.*;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.bisq_easy.take_offer.amount.TakerSelectAmountController;
import bisq.desktop.primary.overlay.bisq_easy.take_offer.method.TakerSelectPaymentMethodController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
public class TakeOfferController extends NavigationController implements InitWithDataController<TakeOfferController.InitData> {

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final BisqEasyOffer bisqEasyOffer;

        public InitData(BisqEasyOffer bisqEasyOffer) {
            this.bisqEasyOffer = bisqEasyOffer;
        }
    }

    private final DefaultApplicationService applicationService;
    @Getter
    private final TakeOfferModel model;
    @Getter
    private final TakeOfferView view;
    private final TakerSelectAmountController takerSelectAmountController;
    private final TakerSelectPaymentMethodController takerSelectPaymentMethodController;
    private final ListChangeListener<String> paymentMethodsListener;

    public TakeOfferController(DefaultApplicationService applicationService) {
        super(NavigationTarget.TAKE_OFFER);

        this.applicationService = applicationService;

        model = new TakeOfferModel();
        view = new TakeOfferView(model, this);

        model.getChildTargets().addAll(List.of(
                NavigationTarget.TAKE_OFFER_AMOUNT,
                NavigationTarget.TAKE_OFFER_PAYMENT_METHOD,
                NavigationTarget.TAKE_OFFER_REVIEW
        ));

        takerSelectAmountController = new TakerSelectAmountController(applicationService);
        takerSelectPaymentMethodController = new TakerSelectPaymentMethodController(applicationService);

        paymentMethodsListener = c -> {
            c.next();
            handlePaymentMethodsUpdate();
        };
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void initWithData(InitData initData) {
        takerSelectAmountController.setBisqEasyOffer(initData.getBisqEasyOffer());
        takerSelectPaymentMethodController.setBisqEasyOffer(initData.getBisqEasyOffer());
    }

    @Override
    public void onActivate() {
        model.getNextButtonDisabled().set(false);

        handlePaymentMethodsUpdate();
        takerSelectPaymentMethodController.getPaymentMethodNames().addListener(paymentMethodsListener);
    }

    @Override
    public void onDeactivate() {
        takerSelectPaymentMethodController.getPaymentMethodNames().removeListener(paymentMethodsListener);
    }

    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        model.getNextButtonText().set(Res.get("next"));
        model.getBackButtonText().set(Res.get("back"));
        model.getBackButtonVisible().set(navigationTarget != NavigationTarget.TAKE_OFFER_AMOUNT);
        model.getNextButtonVisible().set(navigationTarget != NavigationTarget.TAKE_OFFER_REVIEW);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TAKE_OFFER_AMOUNT: {
                return Optional.of(takerSelectAmountController);
            }
            case TAKE_OFFER_PAYMENT_METHOD: {
                return Optional.of(takerSelectPaymentMethodController);
            }
            case TAKE_OFFER_REVIEW: {
                return Optional.empty();
                // return Optional.of(reviewOfferController);
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            model.setAnimateRightOut(false);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateNextButtonDisabledState();
        }
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            model.setAnimateRightOut(true);
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateNextButtonDisabledState();
        }
    }

    void onClose() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        OverlayController.hide();
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    private void updateNextButtonDisabledState() {
        if (NavigationTarget.TAKE_OFFER_PAYMENT_METHOD.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(takerSelectPaymentMethodController.getPaymentMethodNames().isEmpty());
        } else {
            model.getNextButtonDisabled().set(false);
        }
    }

    private void setMainButtonsVisibleState(boolean value) {
        model.getBackButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.TAKE_OFFER_AMOUNT);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.TAKE_OFFER_REVIEW);
        model.getCloseButtonVisible().set(value);
    }

    private void handlePaymentMethodsUpdate() {
        // reviewOfferController.setPaymentMethodNames(takerSelectPaymentMethodController.getPaymentMethodNames());
        updateNextButtonDisabledState();
    }
}
