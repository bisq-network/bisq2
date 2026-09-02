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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer;

import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.MuSigCreateOfferAmountAndPriceController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.direction_and_market.MuSigCreateOfferDirectionAndMarketController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.payment.MuSigCreateOfferPaymentController;
import bisq.desktop.main.content.mu_sig.offer.draft.create_offer.review.MuSigCreateOfferReviewController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final CreateOfferUseCase createOfferUseCase;
    private final MarketSelection marketSelection;
    private final OverlayController overlayController;
    @Getter
    private final MuSigCreateOfferModel model;
    @Getter
    private final MuSigCreateOfferView view;
    private final MuSigCreateOfferDirectionAndMarketController muSigCreateOfferDirectionAndMarketController;
    private final MuSigCreateOfferPaymentController muSigCreateOfferPaymentController;
    private final MuSigCreateOfferAmountAndPriceController muSigCreateOfferAmountAndPriceController;
    private final MuSigCreateOfferReviewController muSigCreateOfferReviewController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private final Set<Pin> pins = new HashSet<>();

    public MuSigCreateOfferController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_CREATE_OFFER);

        this.serviceProvider = serviceProvider;
        createOfferUseCase = new CreateOfferUseCase(
                serviceProvider.getBondedRolesService().getMarketPriceService(),
                serviceProvider.getSettingsService(),
                serviceProvider.getAccountService());
        marketSelection = createOfferUseCase.getMarketSelection();
        PaymentMethodSelection paymentMethodSelection = createOfferUseCase.getPaymentMethodSelection();

        overlayController = OverlayController.getInstance();

        List<NavigationTarget> targets = new ArrayList<>(List.of(NavigationTarget.MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET));
        targets.add(NavigationTarget.MU_SIG_CREATE_OFFER_PAYMENT_METHODS);
        targets.add(NavigationTarget.MU_SIG_CREATE_OFFER_AMOUNT_AND_PRICE);
        targets.add(NavigationTarget.MU_SIG_CREATE_OFFER_REVIEW_OFFER);

        model = new MuSigCreateOfferModel(targets);
        view = new MuSigCreateOfferView(model, this);

        muSigCreateOfferDirectionAndMarketController = new MuSigCreateOfferDirectionAndMarketController(serviceProvider, createOfferUseCase, this::onNext);
        muSigCreateOfferPaymentController = new MuSigCreateOfferPaymentController(serviceProvider,
                createOfferUseCase,
                view.getRoot(),
                this::setMainButtonsVisibleState);
        muSigCreateOfferAmountAndPriceController = new MuSigCreateOfferAmountAndPriceController(serviceProvider,
                createOfferUseCase,
                view.getRoot(),
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo);
        muSigCreateOfferReviewController = new MuSigCreateOfferReviewController(serviceProvider,
                createOfferUseCase,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo);
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void initWithData(InitData data) {
        marketSelection.onSetMarket(data.getMarket());
    }

    @Override
    public void onActivate() {
        createOfferUseCase.initialize();

        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getNextButtonDisabled().set(false);

        model.getSelectedChildTarget().set(NavigationTarget.MU_SIG_CREATE_OFFER_DIRECTION_AND_MARKET);

        pins.add(marketSelection.marketObservable().addObserver(market -> {
            UIThread.run(() -> {
                updatePaymentMethodProgressLabel(market);
                updateNextButtonDisabledState();
            });
        }));
    }

    @Override
    public void onDeactivate() {
        createOfferUseCase.dispose();
        pins.forEach(Pin::unbind);
        pins.clear();
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        reset();
    }

    @Override
    protected void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        if (navigationTarget == NavigationTarget.MU_SIG_CREATE_OFFER_REVIEW_OFFER) {
            muSigCreateOfferReviewController.initialize();
            model.getNextButtonText().set(Res.get("muSig.offer.create.review.nextButton.createOffer"));
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
            performNavigation(nextIndex, false);
        }
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEventWithTextInputFocusCheck(keyEvent, getView().getRoot(), this::onNext);
    }

    void onBack() {
        int prevIndex = model.getCurrentIndex().get() - 1;
        if (prevIndex >= 0) {
            if (!validate(false)) {
                return;
            }
            performNavigation(prevIndex, true);
        }
    }

    private void performNavigation(int index, boolean animateRight) {
        model.setAnimateRightOut(animateRight);
        model.getCurrentIndex().set(index);
        NavigationTarget finalTarget = model.getChildTargets().get(index);
        model.getSelectedChildTarget().set(finalTarget);
        Navigation.navigateTo(finalTarget);
        updateNextButtonDisabledState();
    }

    private boolean validate(boolean calledFromNext) {
        NavigationTarget current = model.getSelectedChildTarget().get();
        if (current == NavigationTarget.MU_SIG_CREATE_OFFER_AMOUNT_AND_PRICE) {
            return muSigCreateOfferAmountAndPriceController.validate();
        }
        if (calledFromNext && current == NavigationTarget.MU_SIG_CREATE_OFFER_PAYMENT_METHODS) {
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

        muSigCreateOfferAmountAndPriceController.reset();
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
        model.getNextButtonVisible().set(value);
        model.getCloseButtonVisible().set(value);
    }

    private void updatePaymentMethodProgressLabel(Market market) {
        boolean isBaseCurrencyBitcoin = market != null && market.isBaseCurrencyBitcoin();
        model.setPaymentMethodProgressLabel(isBaseCurrencyBitcoin
                ? Res.get("muSig.offer.wizard.progress.account.fiat")
                : Res.get("muSig.offer.wizard.progress.account.crypto"));
    }
}
