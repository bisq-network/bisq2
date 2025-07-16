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

package bisq.desktop.main.content.mu_sig.create_offer;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.mu_sig.create_offer.amount_and_price.MuSigCreateOfferAmountAndPriceController;
import bisq.desktop.main.content.mu_sig.create_offer.direction_and_market.MuSigCreateOfferDirectionAndMarketController;
import bisq.desktop.main.content.mu_sig.create_offer.payment.MuSigCreateOfferPaymentController;
import bisq.desktop.main.content.mu_sig.create_offer.review.MuSigCreateOfferReviewController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MuSigCreateOfferController extends NavigationController implements InitWithDataController<MuSigCreateOfferController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final Market market;

        public InitData(Market market) {
            this.market = market;
        }
    }

    private final ServiceProvider serviceProvider;
    private final OverlayController overlayController;
    @Getter
    private final MuSigCreateOfferModel model;
    @Getter
    private final MuSigCreateOfferView view;
    private final MuSigCreateOfferDirectionAndMarketController muSigCreateOfferDirectionAndMarketController;
    private final MuSigCreateOfferAmountAndPriceController muSigCreateOfferAmountAndPriceController;
    private final MuSigCreateOfferPaymentController muSigCreateOfferPaymentController;
    private final MuSigCreateOfferReviewController muSigCreateOfferReviewController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription directionPin, marketPin, priceSpecPin;
    private Pin selectedAccountByPaymentMethodPin;

    public MuSigCreateOfferController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_CREATE_OFFER);

        this.serviceProvider = serviceProvider;
        overlayController = OverlayController.getInstance();

        model = new MuSigCreateOfferModel();
        view = new MuSigCreateOfferView(model, this);

        muSigCreateOfferDirectionAndMarketController = new MuSigCreateOfferDirectionAndMarketController(serviceProvider, this::onNext);
        muSigCreateOfferAmountAndPriceController = new MuSigCreateOfferAmountAndPriceController(serviceProvider,
                view.getRoot(),
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo);
        muSigCreateOfferPaymentController = new MuSigCreateOfferPaymentController(serviceProvider, view.getRoot());
        muSigCreateOfferReviewController = new MuSigCreateOfferReviewController(serviceProvider,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo);
    }

    @Override
    public void initWithData(InitData data) {
        Market market = data.getMarket();
        muSigCreateOfferDirectionAndMarketController.setMarket(market);
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getNextButtonDisabled().set(false);
        model.getChildTargets().clear();
        model.getChildTargets().addAll(List.of(
                NavigationTarget.MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET,
                NavigationTarget.MU_SIG_CREATE_OFFER_AMOUNT_AND_PRICE,
                NavigationTarget.MU_SIG_CREATE_OFFER_PAYMENT_METHODS,
                NavigationTarget.MU_SIG_CREATE_OFFER_REVIEW_OFFER
        ));
        model.getSelectedChildTarget().set(NavigationTarget.MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET);

        directionPin = EasyBind.subscribe(muSigCreateOfferDirectionAndMarketController.getDirection(), direction -> {
            muSigCreateOfferAmountAndPriceController.setDirection(direction);
            muSigCreateOfferPaymentController.setDirection(direction);
        });
        marketPin = EasyBind.subscribe(muSigCreateOfferDirectionAndMarketController.getMarket(), market -> {
            muSigCreateOfferPaymentController.setMarket(market);
            muSigCreateOfferAmountAndPriceController.setMarket(market);
            updateNextButtonDisabledState();
        });
        priceSpecPin = EasyBind.subscribe(muSigCreateOfferAmountAndPriceController.getPriceSpec(),
                muSigCreateOfferAmountAndPriceController::updateAmountSpecWithPriceSpec);
        handlePaymentMethodsUpdate();
        selectedAccountByPaymentMethodPin = muSigCreateOfferPaymentController.getSelectedAccountByPaymentMethod().addObserver(() ->
                UIThread.run(this::handlePaymentMethodsUpdate));
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        directionPin.unsubscribe();
        marketPin.unsubscribe();
        priceSpecPin.unsubscribe();
        selectedAccountByPaymentMethodPin.unbind();
        reset();
    }

    @Override
    protected void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        if (navigationTarget == NavigationTarget.MU_SIG_CREATE_OFFER_REVIEW_OFFER) {
            muSigCreateOfferReviewController.setDataForCreateOffer(
                    muSigCreateOfferDirectionAndMarketController.getDirection().get(),
                    muSigCreateOfferDirectionAndMarketController.getMarket().get(),
                    muSigCreateOfferPaymentController.getSelectedAccountByPaymentMethod(),
                    muSigCreateOfferAmountAndPriceController.getBaseSideAmountSpec().get(),
                    muSigCreateOfferAmountAndPriceController.getPriceSpec().get()
            );
            model.getNextButtonText().set(Res.get("bisqEasy.tradeWizard.review.nextButton.createOffer"));
        } else {
            model.getNextButtonText().set(Res.get("action.next"));
        }
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        model.getBackButtonText().set(Res.get("action.back"));
        model.getBackButtonVisible().set(navigationTarget != NavigationTarget.MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET);
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET -> Optional.of(muSigCreateOfferDirectionAndMarketController);
            case MU_SIG_CREATE_OFFER_AMOUNT_AND_PRICE -> Optional.of(muSigCreateOfferAmountAndPriceController);
            case MU_SIG_CREATE_OFFER_PAYMENT_METHODS -> Optional.of(muSigCreateOfferPaymentController);
            case MU_SIG_CREATE_OFFER_REVIEW_OFFER -> Optional.of(muSigCreateOfferReviewController);
            default -> Optional.empty();
        };
    }

    void onNext() {
        if (model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_CREATE_OFFER_REVIEW_OFFER) {
            muSigCreateOfferReviewController.publishOffer();
        } else {
            navigateNext();
        }
    }

    void navigateNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            if (!validate(true)) {
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

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onNext);
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            if (!validate(false)) {
                return;
            }

            model.setAnimateRightOut(true);
            model.getCurrentIndex().set(prevIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(prevIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateNextButtonDisabledState();
        }
    }

    private boolean validate(boolean calledFromNext) {
        if (model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_CREATE_OFFER_AMOUNT_AND_PRICE) {
            return muSigCreateOfferAmountAndPriceController.validate();
        }
        if (calledFromNext && model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_CREATE_OFFER_PAYMENT_METHODS) {
            // For PaymentMethod we tolerate to go back without having one selected
            return muSigCreateOfferPaymentController.validate();
        }
        return true;
    }

    void onClose() {
        OverlayController.hide();
    }

    void onQuit() {
        serviceProvider.getShutDownHandler().shutdown();
    }

    private void reset() {
        resetSelectedChildTarget();

        muSigCreateOfferDirectionAndMarketController.reset();
        muSigCreateOfferAmountAndPriceController.reset();
        muSigCreateOfferPaymentController.reset();
        muSigCreateOfferReviewController.reset();

        model.reset();
    }

    private void updateNextButtonDisabledState() {
        // TODO as UI is WIP we leave the currently not useful code here to be used maybe later for disabling next if
        // ui input is missing.
       /* if (NavigationTarget.BISQ_EASY_TRADE_WIZARD_DIRECTION_AND_MARKET.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(tradeWizardDirectionAndMarketController.getMarket().get() == null);
        } else if (NavigationTarget.BISQ_EASY_TRADE_WIZARD_TAKE_OFFER_OFFER.equals(model.getSelectedChildTarget().get())) {
            model.getNextButtonDisabled().set(tradeWizardSelectOfferController.getSelectedBisqEasyOffer().get() == null);
        } else {*/
        model.getNextButtonDisabled().set(false);
        // }
    }

    private void closeAndNavigateTo(NavigationTarget navigationTarget) {
        OverlayController.hide(() -> Navigation.navigateTo(navigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        model.getBackButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.MU_SIG_CREATE_OFFER_REVIEW_OFFER);
        model.getCloseButtonVisible().set(value);
    }

    private void handlePaymentMethodsUpdate() {
        ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = muSigCreateOfferPaymentController.getSelectedAccountByPaymentMethod();
        List<PaymentMethod<?>> paymentMethods = new ArrayList<>(selectedAccountByPaymentMethod.keySet());
        muSigCreateOfferAmountAndPriceController.setPaymentMethods(paymentMethods);
        muSigCreateOfferReviewController.setPaymentMethods(paymentMethods);
    }
}
