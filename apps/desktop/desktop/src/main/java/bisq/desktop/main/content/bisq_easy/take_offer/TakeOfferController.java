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

package bisq.desktop.main.content.bisq_easy.take_offer;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.bisq_easy.take_offer.amount.TakeOfferAmountController;
import bisq.desktop.main.content.bisq_easy.take_offer.payment_methods.TakeOfferPaymentController;
import bisq.desktop.main.content.bisq_easy.take_offer.review.TakeOfferReviewController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

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

    private final OverlayController overlayController;
    @Getter
    private final TakeOfferModel model;
    @Getter
    private final TakeOfferView view;
    private final TakeOfferAmountController takeOfferAmountController;
    private final TakeOfferPaymentController takeOfferPaymentController;
    private final TakeOfferReviewController takeOfferReviewController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription takersBaseSideAmountPin, takersQuoteSideAmountPin, selectedBitcoinPaymentMethodSpecPin, selectedFiatPaymentMethodSpecPin;

    public TakeOfferController(ServiceProvider serviceProvider) {
        super(NavigationTarget.TAKE_OFFER);

        overlayController = OverlayController.getInstance();

        model = new TakeOfferModel();
        view = new TakeOfferView(model, this);

        takeOfferAmountController = new TakeOfferAmountController(serviceProvider);
        takeOfferPaymentController = new TakeOfferPaymentController(serviceProvider);
        takeOfferReviewController = new TakeOfferReviewController(serviceProvider, this::setMainButtonsVisibleState, this::closeAndNavigateTo);
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void initWithData(InitData initData) {
        BisqEasyOffer bisqEasyOffer = initData.getBisqEasyOffer();
        takeOfferAmountController.init(bisqEasyOffer);
        takeOfferPaymentController.init(bisqEasyOffer);
        takeOfferReviewController.init(bisqEasyOffer);

        model.setAmountVisible(bisqEasyOffer.hasAmountRange());
        List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs = bisqEasyOffer.getBaseSidePaymentMethodSpecs();
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = bisqEasyOffer.getQuoteSidePaymentMethodSpecs();
        model.setPaymentMethodVisible(baseSidePaymentMethodSpecs.size() > 1 || quoteSidePaymentMethodSpecs.size() > 1);

        model.getChildTargets().clear();
        if (model.isAmountVisible()) {
            model.getChildTargets().add(NavigationTarget.TAKE_OFFER_AMOUNT);
        }
        if (model.isPaymentMethodVisible()) {
            model.getChildTargets().add(NavigationTarget.TAKE_OFFER_PAYMENT);
        } else {
            checkArgument(baseSidePaymentMethodSpecs.size() == 1);
            checkArgument(quoteSidePaymentMethodSpecs.size() == 1);
            takeOfferReviewController.setBitcoinPaymentMethodSpec(baseSidePaymentMethodSpecs.get(0));
            takeOfferReviewController.setFiatPaymentMethodSpec(quoteSidePaymentMethodSpecs.get(0));
        }
        model.getChildTargets().add(NavigationTarget.TAKE_OFFER_REVIEW);
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getSelectedChildTarget().set(model.getChildTargets().get(0));
        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);
        takersBaseSideAmountPin = EasyBind.subscribe(takeOfferAmountController.getTakersBaseSideAmount(),
                takeOfferReviewController::setTakersBaseSideAmount);
        takersQuoteSideAmountPin = EasyBind.subscribe(takeOfferAmountController.getTakersQuoteSideAmount(),
                takeOfferReviewController::setTakersQuoteSideAmount);
        selectedBitcoinPaymentMethodSpecPin = EasyBind.subscribe(takeOfferPaymentController.getSelectedBitcoinPaymentMethodSpec(),
                takeOfferReviewController::setBitcoinPaymentMethodSpec);
        selectedFiatPaymentMethodSpecPin = EasyBind.subscribe(takeOfferPaymentController.getSelectedFiatPaymentMethodSpec(),
                takeOfferReviewController::setFiatPaymentMethodSpec);
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        takersQuoteSideAmountPin.unsubscribe();
        takersBaseSideAmountPin.unsubscribe();
        selectedBitcoinPaymentMethodSpecPin.unsubscribe();
        selectedFiatPaymentMethodSpecPin.unsubscribe();
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        boolean isTakeOfferReview = navigationTarget == NavigationTarget.TAKE_OFFER_REVIEW;
        model.getNextButtonText().set(isTakeOfferReview ?
                Res.get("bisqEasy.takeOffer.review.takeOffer") :
                Res.get("action.next"));
        model.getShowProgressBox().set(!isTakeOfferReview);
        setMainButtonsVisibleState(true);
        model.getTakeOfferButtonVisible().set(isTakeOfferReview);
        model.getNextButtonVisible().set(!isTakeOfferReview);
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TAKE_OFFER_AMOUNT: {
                if (!model.isAmountVisible()) {
                    Navigation.navigateTo(NavigationTarget.TAKE_OFFER_PAYMENT);
                    return Optional.empty();
                }
                return Optional.of(takeOfferAmountController);
            }
            case TAKE_OFFER_PAYMENT: {
                if (!model.isPaymentMethodVisible()) {
                    Navigation.navigateTo(NavigationTarget.TAKE_OFFER_REVIEW);
                    return Optional.empty();
                }
                return Optional.of(takeOfferPaymentController);
            }
            case TAKE_OFFER_REVIEW: {
                return Optional.of(takeOfferReviewController);
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            if (model.getSelectedChildTarget().get() == NavigationTarget.TAKE_OFFER_PAYMENT) {
                if (!takeOfferPaymentController.isValid()) {
                    takeOfferPaymentController.handleInvalidInput();
                    return;
                }
            }
            model.setAnimateRightOut(false);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
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
        }
    }

    void onClose() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        OverlayController.hide();
    }

    void onTakeOffer() {
        takeOfferReviewController.takeOffer(() -> {
            model.getBackButtonVisible().set(true);
            model.getTakeOfferButtonVisible().set(true);
        });
        model.getBackButtonVisible().set(false);
        model.getTakeOfferButtonVisible().set(false);
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, this::onNext);
    }

    private void closeAndNavigateTo(NavigationTarget NavigationTarget) {
        OverlayController.hide(() -> Navigation.navigateTo(NavigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        NavigationTarget navigationTarget = model.getNavigationTarget();
        model.getBackButtonVisible().set(value && model.getChildTargets().indexOf(navigationTarget) > 0);
        model.getCloseButtonVisible().set(value);
    }
}
