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

package bisq.desktop.overlay.bisq_easy.create_offer;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.*;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.bisq_easy.create_offer.amount.CreateOfferAmountController;
import bisq.desktop.overlay.bisq_easy.create_offer.direction.CreateOfferDirectionController;
import bisq.desktop.overlay.bisq_easy.create_offer.market.CreateOfferMarketController;
import bisq.desktop.overlay.bisq_easy.create_offer.payment_method.CreateOfferPaymentMethodController;
import bisq.desktop.overlay.bisq_easy.create_offer.price.CreateOfferPriceController;
import bisq.desktop.overlay.bisq_easy.create_offer.review.CreateOfferReviewOfferController;
import bisq.i18n.Res;
import bisq.offer.Direction;
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

    private final ServiceProvider serviceProvider;
    @Getter
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;
    private final CreateOfferDirectionController createOfferDirectionController;
    private final CreateOfferMarketController createOfferMarketController;
    private final CreateOfferPriceController createOfferPriceController;
    private final CreateOfferAmountController createOfferAmountController;
    private final CreateOfferPaymentMethodController createOfferPaymentMethodController;
    private final CreateOfferReviewOfferController createOfferReviewOfferController;
    private final ListChangeListener<FiatPaymentMethod> paymentMethodsListener;
    private Subscription directionPin, marketPin, amountSpecPin,
            isMinAmountEnabledPin, priceSpecPin, showCustomMethodNotEmptyWarningPin;

    public CreateOfferController(ServiceProvider serviceProvider) {
        super(NavigationTarget.CREATE_OFFER);

        this.serviceProvider = serviceProvider;

        model = new CreateOfferModel();
        view = new CreateOfferView(model, this);

        model.getChildTargets().addAll(List.of(
                NavigationTarget.CREATE_OFFER_DIRECTION,
                NavigationTarget.CREATE_OFFER_MARKET,
                NavigationTarget.CREATE_OFFER_AMOUNT,
                NavigationTarget.CREATE_OFFER_PAYMENT_METHOD,
                NavigationTarget.CREATE_OFFER_REVIEW_OFFER
        ));

        createOfferDirectionController = new CreateOfferDirectionController(serviceProvider, this::onNext, this::setMainButtonsVisibleState);
        createOfferMarketController = new CreateOfferMarketController(serviceProvider, this::onNext);
        createOfferPriceController = new CreateOfferPriceController(serviceProvider);
        createOfferAmountController = new CreateOfferAmountController(serviceProvider);
        createOfferPaymentMethodController = new CreateOfferPaymentMethodController(serviceProvider);
        createOfferReviewOfferController = new CreateOfferReviewOfferController(serviceProvider, this::setMainButtonsVisibleState, this::reset);

        paymentMethodsListener = c -> {
            c.next();
            handlePaymentMethodsUpdate();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        createOfferReviewOfferController.setShowMatchingOffers(initData.isShowMatchingOffers());
    }

    @Override
    public void onActivate() {
        model.getNextButtonDisabled().set(false);

        directionPin = EasyBind.subscribe(createOfferDirectionController.getDirection(), direction -> {
            createOfferReviewOfferController.setDirection(direction);
            createOfferAmountController.setDirection(direction);
            model.getPriceProgressItemVisible().set(direction == Direction.SELL);
            if (direction == Direction.SELL) {
                model.getChildTargets().add(2, NavigationTarget.CREATE_OFFER_PRICE);
            } else {
                model.getChildTargets().remove(NavigationTarget.CREATE_OFFER_PRICE);
            }
        });
        marketPin = EasyBind.subscribe(createOfferMarketController.getMarket(), market -> {
            createOfferReviewOfferController.setMarket(market);
            createOfferPaymentMethodController.setMarket(market);
            createOfferPriceController.setMarket(market);
            createOfferAmountController.setMarket(market);
            updateNextButtonDisabledState();
        });
        amountSpecPin = EasyBind.subscribe(createOfferAmountController.getAmountSpec(), createOfferReviewOfferController::setAmountSpec);
        isMinAmountEnabledPin = EasyBind.subscribe(createOfferAmountController.getIsMinAmountEnabled(), createOfferReviewOfferController::setIsMinAmountEnabled);
        priceSpecPin = EasyBind.subscribe(createOfferPriceController.getPriceSpec(), priceSpec -> {
            createOfferAmountController.setPriceSpec(priceSpec);
            createOfferReviewOfferController.setPriceSpec(priceSpec);
        });

        showCustomMethodNotEmptyWarningPin = EasyBind.subscribe(createOfferPaymentMethodController.getShowCustomMethodNotEmptyWarning(),
                showCustomMethodNotEmptyWarning -> {
                    if (model.getSelectedChildTarget().get() == NavigationTarget.CREATE_OFFER_PAYMENT_METHOD) {
                        model.getNextButtonVisible().set(!showCustomMethodNotEmptyWarning);
                        model.getBackButtonVisible().set(!showCustomMethodNotEmptyWarning);
                    }
                });

        handlePaymentMethodsUpdate();
        createOfferPaymentMethodController.getFiatPaymentMethods().addListener(paymentMethodsListener);
    }

    @Override
    public void onDeactivate() {
        directionPin.unsubscribe();
        marketPin.unsubscribe();
        amountSpecPin.unsubscribe();
        isMinAmountEnabledPin.unsubscribe();
        priceSpecPin.unsubscribe();
        showCustomMethodNotEmptyWarningPin.unsubscribe();
        createOfferPaymentMethodController.getFiatPaymentMethods().removeListener(paymentMethodsListener);
    }

    public void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        model.getNextButtonText().set(Res.get("action.next"));
        model.getBackButtonText().set(Res.get("action.back"));
        model.getBackButtonVisible().set(navigationTarget != NavigationTarget.CREATE_OFFER_DIRECTION);
        model.getNextButtonVisible().set(navigationTarget != NavigationTarget.CREATE_OFFER_REVIEW_OFFER);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CREATE_OFFER_DIRECTION: {
                return Optional.of(createOfferDirectionController);
            }
            case CREATE_OFFER_MARKET: {
                return Optional.of(createOfferMarketController);
            }
            case CREATE_OFFER_PRICE: {
                return Optional.of(createOfferPriceController);
            }
            case CREATE_OFFER_AMOUNT: {
                return Optional.of(createOfferAmountController);
            }
            case CREATE_OFFER_PAYMENT_METHOD: {
                return Optional.of(createOfferPaymentMethodController);
            }
            case CREATE_OFFER_REVIEW_OFFER: {
                return Optional.of(createOfferReviewOfferController);
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            if (model.getSelectedChildTarget().get() == NavigationTarget.CREATE_OFFER_PAYMENT_METHOD &&
                    createOfferPaymentMethodController.getCustomFiatPaymentMethodNameNotEmpty()) {
                createOfferPaymentMethodController.showCustomMethodNotEmptyWarning();
                return;
            }
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
        serviceProvider.getShotDownHandler().shutdown().thenAccept(result -> Platform.exit());
    }

    private void reset() {
        resetSelectedChildTarget();

        createOfferDirectionController.reset();
        createOfferMarketController.reset();
        createOfferPriceController.reset();
        createOfferAmountController.reset();
        createOfferPaymentMethodController.reset();
        createOfferReviewOfferController.reset();

        model.reset();
    }

    private void updateNextButtonDisabledState() {
        if (NavigationTarget.CREATE_OFFER_MARKET.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(createOfferMarketController.getMarket().get() == null);
        } else if (NavigationTarget.CREATE_OFFER_PAYMENT_METHOD.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(createOfferPaymentMethodController.getFiatPaymentMethods().isEmpty());
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
        createOfferReviewOfferController.setFiatPaymentMethods(createOfferPaymentMethodController.getFiatPaymentMethods());
        updateNextButtonDisabledState();
    }
}
