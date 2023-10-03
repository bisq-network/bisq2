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

package bisq.desktop.main.content.bisq_easy.trade_wizard;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.bisq_easy.trade_wizard.amount.TradeWizardAmountController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.direction.TradeWizardDirectionController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.market.TradeWizardMarketController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.payment_method.TradeWizardPaymentMethodController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.price.TradeWizardPriceController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.review.TradeWizardReviewController;
import bisq.desktop.main.content.bisq_easy.trade_wizard.select_offer.TradeWizardSelectOfferController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.Direction;
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
public class TradeWizardController extends NavigationController implements InitWithDataController<TradeWizardController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final boolean isCreateOfferMode;

        public InitData(boolean isCreateOfferMode) {
            this.isCreateOfferMode = isCreateOfferMode;
        }
    }

    private final ServiceProvider serviceProvider;
    @Getter
    private final TradeWizardModel model;
    @Getter
    private final TradeWizardView view;
    private final TradeWizardDirectionController tradeWizardDirectionController;
    private final TradeWizardMarketController tradeWizardMarketController;
    private final TradeWizardPriceController tradeWizardPriceController;
    private final TradeWizardAmountController tradeWizardAmountController;
    private final TradeWizardPaymentMethodController tradeWizardPaymentMethodController;
    private final TradeWizardSelectOfferController tradeWizardSelectOfferController;
    private final TradeWizardReviewController tradeWizardReviewController;

    private final ListChangeListener<FiatPaymentMethod> paymentMethodsListener;
    private Subscription directionPin, marketPin, amountSpecPin,
            isMinAmountEnabledPin, priceSpecPin,
            selectedBisqEasyOfferPin, isBackButtonHighlightedPin, areAmountsValidPin;

    public TradeWizardController(ServiceProvider serviceProvider) {
        super(NavigationTarget.TRADE_WIZARD);

        this.serviceProvider = serviceProvider;

        model = new TradeWizardModel();
        view = new TradeWizardView(model, this);

        tradeWizardDirectionController = new TradeWizardDirectionController(serviceProvider,
                this::onNext,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo);
        tradeWizardMarketController = new TradeWizardMarketController(serviceProvider, this::onNext);
        tradeWizardPriceController = new TradeWizardPriceController(serviceProvider);
        tradeWizardAmountController = new TradeWizardAmountController(serviceProvider);
        tradeWizardPaymentMethodController = new TradeWizardPaymentMethodController(serviceProvider, this::onNext);
        tradeWizardSelectOfferController = new TradeWizardSelectOfferController(serviceProvider,
                this::onBack,
                this::onNext,
                this::closeAndNavigateTo);
        tradeWizardReviewController = new TradeWizardReviewController(serviceProvider,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo);

        paymentMethodsListener = c -> {
            c.next();
            handlePaymentMethodsUpdate();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        boolean isCreateOfferMode = initData.isCreateOfferMode();
        model.setCreateOfferMode(isCreateOfferMode);
        tradeWizardAmountController.setIsCreateOfferMode(isCreateOfferMode);
    }

    @Override
    public void onActivate() {
        model.getNextButtonDisabled().set(false);
        model.getChildTargets().clear();
        model.getChildTargets().addAll(List.of(
                NavigationTarget.TRADE_WIZARD_DIRECTION,
                NavigationTarget.TRADE_WIZARD_MARKET,
                NavigationTarget.TRADE_WIZARD_PAYMENT_METHOD,
                NavigationTarget.TRADE_WIZARD_AMOUNT,
                NavigationTarget.TRADE_WIZARD_TAKE_OFFER_OFFER,
                NavigationTarget.TRADE_WIZARD_REVIEW_OFFER
        ));

        directionPin = EasyBind.subscribe(tradeWizardDirectionController.getDirection(), direction -> {
            tradeWizardMarketController.setDirection(direction);
            tradeWizardSelectOfferController.setDirection(direction);
            tradeWizardAmountController.setDirection(direction);
            tradeWizardPaymentMethodController.setDirection(direction);
            model.getPriceProgressItemVisible().set(direction == Direction.SELL);
            if (direction == Direction.SELL) {
                model.getChildTargets().add(2, NavigationTarget.TRADE_WIZARD_PRICE);
            } else {
                model.getChildTargets().remove(NavigationTarget.TRADE_WIZARD_PRICE);
            }
        });
        marketPin = EasyBind.subscribe(tradeWizardMarketController.getMarket(), market -> {
            tradeWizardSelectOfferController.setMarket(market);
            tradeWizardPaymentMethodController.setMarket(market);
            tradeWizardPriceController.setMarket(market);
            tradeWizardAmountController.setMarket(market);
            updateNextButtonDisabledState();
        });
        amountSpecPin = EasyBind.subscribe(tradeWizardAmountController.getAmountSpec(),
                amountSpec -> {
                    tradeWizardSelectOfferController.setAmountSpec(amountSpec);
                });
        isMinAmountEnabledPin = EasyBind.subscribe(tradeWizardAmountController.getIsMinAmountEnabled(),
                isMinAmountEnabled -> {
                    tradeWizardSelectOfferController.setIsMinAmountEnabled(isMinAmountEnabled);
                });
        priceSpecPin = EasyBind.subscribe(tradeWizardPriceController.getPriceSpec(),
                priceSpec -> {
                    tradeWizardAmountController.setPriceSpec(priceSpec);
                    tradeWizardSelectOfferController.setPriceSpec(priceSpec);
                });
        selectedBisqEasyOfferPin = EasyBind.subscribe(tradeWizardSelectOfferController.getSelectedBisqEasyOffer(),
                selectedBisqEasyOffer -> {
                    tradeWizardReviewController.setSelectedBisqEasyOffer(selectedBisqEasyOffer);
                    updateNextButtonDisabledState();
                });
        isBackButtonHighlightedPin = EasyBind.subscribe(tradeWizardSelectOfferController.getIsBackButtonHighlighted(),
                isBackButtonHighlighted -> model.getIsBackButtonHighlighted().set(isBackButtonHighlighted));

        handlePaymentMethodsUpdate();
        tradeWizardPaymentMethodController.getFiatPaymentMethods().addListener(paymentMethodsListener);
        areAmountsValidPin = EasyBind.subscribe(
                tradeWizardAmountController.areAmountsValid(),
                areAmountsValid -> model.getNextButtonDisabled().set(!areAmountsValid));
    }

    @Override
    public void onDeactivate() {
        directionPin.unsubscribe();
        marketPin.unsubscribe();
        amountSpecPin.unsubscribe();
        isMinAmountEnabledPin.unsubscribe();
        priceSpecPin.unsubscribe();
        areAmountsValidPin.unsubscribe();
        selectedBisqEasyOfferPin.unsubscribe();
        isBackButtonHighlightedPin.unsubscribe();
        tradeWizardPaymentMethodController.getFiatPaymentMethods().removeListener(paymentMethodsListener);
    }

    @Override
    protected void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        if (navigationTarget == NavigationTarget.TRADE_WIZARD_REVIEW_OFFER) {
            if (model.isCreateOfferMode()) {
                tradeWizardReviewController.setDataForCreateOffer(
                        tradeWizardDirectionController.getDirection().get(),
                        tradeWizardMarketController.getMarket().get(),
                        tradeWizardPaymentMethodController.getFiatPaymentMethods(),
                        tradeWizardAmountController.getAmountSpec().get(),
                        tradeWizardPriceController.getPriceSpec().get()
                );
                model.getNextButtonText().set(Res.get("bisqEasy.tradeWizard.review.nextButton.createOffer"));
            } else {
                tradeWizardReviewController.setDataForTakeOffer(tradeWizardSelectOfferController.getSelectedBisqEasyOffer().get(),
                        tradeWizardAmountController.getAmountSpec().get(),
                        tradeWizardPriceController.getPriceSpec().get(),
                        tradeWizardPaymentMethodController.getFiatPaymentMethods()
                );
                model.getNextButtonText().set(Res.get("bisqEasy.tradeWizard.review.nextButton.takeOffer"));
                updateNextButtonDisabledState();
            }
        } else {
            model.getNextButtonText().set(Res.get("action.next"));
        }
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        // model.getNextButtonText().set(Res.get("action.next"));
        model.getBackButtonText().set(Res.get("action.back"));
        model.getBackButtonVisible().set(navigationTarget != NavigationTarget.TRADE_WIZARD_DIRECTION);
        // model.getNextButtonVisible().set(navigationTarget != NavigationTarget.TRADE_WIZARD_REVIEW_OFFER);
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TRADE_WIZARD_DIRECTION: {
                return Optional.of(tradeWizardDirectionController);
            }
            case TRADE_WIZARD_MARKET: {
                return Optional.of(tradeWizardMarketController);
            }
            case TRADE_WIZARD_PRICE: {
                return Optional.of(tradeWizardPriceController);
            }
            case TRADE_WIZARD_PAYMENT_METHOD: {
                return Optional.of(tradeWizardPaymentMethodController);
            }
            case TRADE_WIZARD_AMOUNT: {
                return Optional.of(tradeWizardAmountController);
            }
            case TRADE_WIZARD_TAKE_OFFER_OFFER: {
                return Optional.of(tradeWizardSelectOfferController);
            }
            case TRADE_WIZARD_REVIEW_OFFER: {
                return Optional.of(tradeWizardReviewController);
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onNext() {
        if (model.getSelectedChildTarget().get() == NavigationTarget.TRADE_WIZARD_REVIEW_OFFER) {
            if (model.isCreateOfferMode()) {
                tradeWizardReviewController.publishOffer();
            } else {
                tradeWizardReviewController.takeOffer();
            }
        } else {
            navigateNext();
        }
    }

    void navigateNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (isTakeOfferItem(nextIndex)) {
            nextIndex++;
        }
        if (nextIndex < model.getChildTargets().size()) {
            if (model.getSelectedChildTarget().get() == NavigationTarget.TRADE_WIZARD_PAYMENT_METHOD &&
                    tradeWizardPaymentMethodController.getCustomFiatPaymentMethodNameNotEmpty()) {
                tradeWizardPaymentMethodController.showCustomMethodNotEmptyWarning();
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
        if (isTakeOfferItem(prevIndex)) {
            prevIndex--;
        }
        if (prevIndex >= 0) {
            model.setAnimateRightOut(true);
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateNextButtonDisabledState();
        }
    }

    private boolean isTakeOfferItem(int index) {
        return model.isCreateOfferMode() && !model.getChildTargets().isEmpty() && model.getChildTargets().get(index) == NavigationTarget.TRADE_WIZARD_TAKE_OFFER_OFFER;
    }

    void onClose() {
        OverlayController.hide();
        reset();
    }

    void onQuit() {
        serviceProvider.getShutDownHandler().shutdown();
    }

    private void reset() {
        resetSelectedChildTarget();

        tradeWizardDirectionController.reset();
        tradeWizardMarketController.reset();
        tradeWizardPriceController.reset();
        tradeWizardAmountController.reset();
        tradeWizardPaymentMethodController.reset();
        tradeWizardSelectOfferController.reset();
        tradeWizardReviewController.reset();

        model.reset();
    }

    private void updateNextButtonDisabledState() {
        if (NavigationTarget.TRADE_WIZARD_MARKET.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(tradeWizardMarketController.getMarket().get() == null);
        } else if (NavigationTarget.TRADE_WIZARD_TAKE_OFFER_OFFER.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(tradeWizardSelectOfferController.getSelectedBisqEasyOffer().get() == null);
        } else {
            model.getNextButtonDisabled().set(false);
        }
    }

    private void closeAndNavigateTo(NavigationTarget NavigationTarget) {
        reset();
        OverlayController.hide(() -> Navigation.navigateTo(NavigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        model.getBackButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.TRADE_WIZARD_DIRECTION);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.TRADE_WIZARD_REVIEW_OFFER);
        model.getCloseButtonVisible().set(value);
    }

    private void handlePaymentMethodsUpdate() {
        tradeWizardSelectOfferController.setFiatPaymentMethods(tradeWizardPaymentMethodController.getFiatPaymentMethods());
        tradeWizardAmountController.setFiatPaymentMethods(tradeWizardPaymentMethodController.getFiatPaymentMethods());
        tradeWizardReviewController.setFiatPaymentMethods(tradeWizardPaymentMethodController.getFiatPaymentMethods());
    }
}
