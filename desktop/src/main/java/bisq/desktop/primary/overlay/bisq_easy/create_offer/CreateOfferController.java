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

package bisq.desktop.primary.overlay.bisq_easy.create_offer;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.*;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.bisq_easy.create_offer.amount.AmountController;
import bisq.desktop.primary.overlay.bisq_easy.create_offer.direction.DirectionController;
import bisq.desktop.primary.overlay.bisq_easy.create_offer.market.MarketController;
import bisq.desktop.primary.overlay.bisq_easy.create_offer.method.PaymentMethodController;
import bisq.desktop.primary.overlay.bisq_easy.create_offer.review.ReviewOfferController;
import bisq.i18n.Res;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;

@Slf4j
public class CreateOfferController extends NavigationController implements InitWithDataController<CreateOfferController.InitData> {

    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final boolean showMatchingOffers;

        public InitData(boolean showMatchingOffers) {
            this.showMatchingOffers = showMatchingOffers;
        }
    }

    private final DefaultApplicationService applicationService;
    @Getter
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final DirectionController directionController;
    private final MarketController marketController;
    private final AmountController amountController;
    private final PaymentMethodController paymentMethodController;
    private final ReviewOfferController reviewOfferController;
    private final ListChangeListener<String> paymentMethodsListener;
    private Subscription directionSubscription, marketSubscription, baseSideMinAmountSubscription,
            baseSideMaxAmountSubscription, quoteSideMinAmountSubscription, quoteSideMaxAmountSubscription,
            isMinAmountEnabledSubscription;

    public CreateOfferController(DefaultApplicationService applicationService) {
        super(NavigationTarget.CREATE_OFFER);

        this.applicationService = applicationService;

        model = new CreateOfferModel();
        view = new CreateOfferView(model, this);

        model.getChildTargets().addAll(List.of(
                NavigationTarget.CREATE_OFFER_DIRECTION,
                NavigationTarget.CREATE_OFFER_MARKET,
                NavigationTarget.CREATE_OFFER_AMOUNT,
                NavigationTarget.CREATE_OFFER_PAYMENT_METHOD,
                NavigationTarget.CREATE_OFFER_REVIEW_OFFER
        ));

        directionController = new DirectionController(applicationService, this::onNext, this::setMainButtonsVisibleState);
        marketController = new MarketController(applicationService, this::onNext);
        amountController = new AmountController(applicationService);
        paymentMethodController = new PaymentMethodController(applicationService);
        reviewOfferController = new ReviewOfferController(applicationService, this::setMainButtonsVisibleState, this::reset);

        paymentMethodsListener = c -> {
            c.next();
            handlePaymentMethodsUpdate();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        reviewOfferController.setShowMatchingOffers(initData.isShowMatchingOffers());
    }

    @Override
    public void onActivate() {
        model.getNextButtonDisabled().set(false);

        directionSubscription = EasyBind.subscribe(directionController.getDirection(), direction -> {
            reviewOfferController.setDirection(direction);
            amountController.setDirection(direction);
        });
        marketSubscription = EasyBind.subscribe(marketController.getMarket(), market -> {
            reviewOfferController.setMarket(market);
            paymentMethodController.setMarket(market);
            amountController.setMarket(market);
            updateNextButtonDisabledState();
        });
        baseSideMinAmountSubscription = EasyBind.subscribe(amountController.getBaseSideMinAmount(), reviewOfferController::setBaseSideMinAmount);
        baseSideMaxAmountSubscription = EasyBind.subscribe(amountController.getBaseSideMaxAmount(), reviewOfferController::setBaseSideMaxAmount);
        quoteSideMinAmountSubscription = EasyBind.subscribe(amountController.getQuoteSideMinAmount(), reviewOfferController::setQuoteSideMinAmount);
        quoteSideMaxAmountSubscription = EasyBind.subscribe(amountController.getQuoteSideMaxAmount(), reviewOfferController::setQuoteSideMaxAmount);
        isMinAmountEnabledSubscription = EasyBind.subscribe(amountController.getIsMinAmountEnabled(), reviewOfferController::setIsMinAmountEnabled);

        handlePaymentMethodsUpdate();
        paymentMethodController.getPaymentMethodNames().addListener(paymentMethodsListener);
    }

    @Override
    public void onDeactivate() {
        directionSubscription.unsubscribe();
        marketSubscription.unsubscribe();
        baseSideMinAmountSubscription.unsubscribe();
        baseSideMaxAmountSubscription.unsubscribe();
        quoteSideMinAmountSubscription.unsubscribe();
        quoteSideMaxAmountSubscription.unsubscribe();
        isMinAmountEnabledSubscription.unsubscribe();

        paymentMethodController.getPaymentMethodNames().removeListener(paymentMethodsListener);
    }

    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        model.getNextButtonText().set(Res.get("next"));
        model.getBackButtonText().set(Res.get("back"));
        model.getBackButtonVisible().set(navigationTarget != NavigationTarget.CREATE_OFFER_DIRECTION);
        model.getNextButtonVisible().set(navigationTarget != NavigationTarget.CREATE_OFFER_REVIEW_OFFER);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CREATE_OFFER_DIRECTION: {
                return Optional.of(directionController);
            }
            case CREATE_OFFER_MARKET: {
                return Optional.of(marketController);
            }
            case CREATE_OFFER_AMOUNT: {
                return Optional.of(amountController);
            }
            case CREATE_OFFER_PAYMENT_METHOD: {
                return Optional.of(paymentMethodController);
            }
            case CREATE_OFFER_REVIEW_OFFER: {
                return Optional.of(reviewOfferController);
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
        reset();
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    private void reset() {
        resetSelectedChildTarget();

        directionController.reset();
        marketController.reset();
        amountController.reset();
        paymentMethodController.reset();
        reviewOfferController.reset();

        model.reset();
    }

    private void updateNextButtonDisabledState() {
        if (NavigationTarget.CREATE_OFFER_MARKET.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(marketController.getMarket().get() == null);
        } else if (NavigationTarget.CREATE_OFFER_PAYMENT_METHOD.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(paymentMethodController.getPaymentMethodNames().isEmpty());
        } else {
            model.getNextButtonDisabled().set(false);
        }
    }

    private void setMainButtonsVisibleState(boolean value) {
        model.getBackButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.CREATE_OFFER_DIRECTION);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.CREATE_OFFER_REVIEW_OFFER);
        model.getCloseButtonVisible().set(value);
    }

    private void handlePaymentMethodsUpdate() {
        reviewOfferController.setPaymentMethodNames(paymentMethodController.getPaymentMethodNames());
        updateNextButtonDisabledState();
    }
}
