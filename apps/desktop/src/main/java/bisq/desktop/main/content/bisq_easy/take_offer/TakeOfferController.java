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

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.bisq_easy.take_offer.amount.TakeOfferAmountController;
import bisq.desktop.main.content.bisq_easy.take_offer.payment_method.TakeOfferPaymentController;
import bisq.desktop.main.content.bisq_easy.take_offer.price.TakeOfferPriceController;
import bisq.desktop.main.content.bisq_easy.take_offer.review.TakeOfferReviewController;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
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

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TakeOfferController extends NavigationController implements InitWithDataController<TakeOfferController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final BisqEasyOffer bisqEasyOffer;
        private final Optional<AmountSpec> takersAmountSpec;
        private final List<FiatPaymentMethod> takersPaymentMethods;

        public InitData(BisqEasyOffer bisqEasyOffer) {
            this(bisqEasyOffer, Optional.empty(), new ArrayList<>());
        }

        public InitData(BisqEasyOffer bisqEasyOffer, Optional<AmountSpec> takersAmountSpec, List<FiatPaymentMethod> takersPaymentMethods) {
            this.bisqEasyOffer = bisqEasyOffer;
            this.takersAmountSpec = takersAmountSpec;
            this.takersPaymentMethods = takersPaymentMethods;
        }
    }

    private final OverlayController overlayController;
    @Getter
    private final TakeOfferModel model;
    @Getter
    private final TakeOfferView view;
    private final TakeOfferPriceController takeOfferPriceController;
    private final TakeOfferAmountController takeOfferAmountController;
    private final TakeOfferPaymentController takeOfferPaymentController;
    private final TakeOfferReviewController takeOfferReviewController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription tradePriceSpecPin, takersBaseSideAmountPin, takersQuoteSideAmountPin, methodNamePin;

    public TakeOfferController(ServiceProvider serviceProvider) {
        super(NavigationTarget.TAKE_OFFER);

        overlayController = OverlayController.getInstance();

        model = new TakeOfferModel();
        view = new TakeOfferView(model, this);

        takeOfferPriceController = new TakeOfferPriceController(serviceProvider);
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
        takeOfferPriceController.init(bisqEasyOffer);
        takeOfferAmountController.init(bisqEasyOffer, initData.getTakersAmountSpec());
        takeOfferPaymentController.init(bisqEasyOffer, initData.getTakersPaymentMethods());
        takeOfferReviewController.init(bisqEasyOffer);

        model.setPriceVisible(bisqEasyOffer.getDirection().isBuy());
        model.setAmountVisible(bisqEasyOffer.hasAmountRange());
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = bisqEasyOffer.getQuoteSidePaymentMethodSpecs();
        model.setPaymentMethodVisible(quoteSidePaymentMethodSpecs.size() > 1);

        model.getChildTargets().clear();
        if (model.isPriceVisible()) {
            model.getChildTargets().add(NavigationTarget.TAKE_OFFER_PRICE);
        }
        if (model.isAmountVisible()) {
            model.getChildTargets().add(NavigationTarget.TAKE_OFFER_AMOUNT);
        }
        if (model.isPaymentMethodVisible()) {
            model.getChildTargets().add(NavigationTarget.TAKE_OFFER_PAYMENT);
        } else {
            checkArgument(quoteSidePaymentMethodSpecs.size() == 1);
            takeOfferReviewController.setFiatPaymentMethodSpec(quoteSidePaymentMethodSpecs.get(0));
        }
        model.getChildTargets().add(NavigationTarget.TAKE_OFFER_REVIEW);
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);
        tradePriceSpecPin = EasyBind.subscribe(takeOfferPriceController.getPriceSpec(),
                priceSpec -> {
                    takeOfferAmountController.setTradePriceSpec(priceSpec);
                    takeOfferReviewController.setTradePriceSpec(priceSpec);
                });
        takersBaseSideAmountPin = EasyBind.subscribe(takeOfferAmountController.getTakersBaseSideAmount(),
                takeOfferReviewController::setTakersBaseSideAmount);
        takersQuoteSideAmountPin = EasyBind.subscribe(takeOfferAmountController.getTakersQuoteSideAmount(),
                takeOfferReviewController::setTakersQuoteSideAmount);
        methodNamePin = EasyBind.subscribe(takeOfferPaymentController.getSelectedFiatPaymentMethodSpec(),
                spec -> {
                    takeOfferReviewController.setFiatPaymentMethodSpec(spec);
                    updateNextButtonDisabledState();
                });
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        tradePriceSpecPin.unsubscribe();
        takersQuoteSideAmountPin.unsubscribe();
        takersBaseSideAmountPin.unsubscribe();
        methodNamePin.unsubscribe();
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
        updateNextButtonDisabledState();
        model.getTakeOfferButtonVisible().set(isTakeOfferReview);
        model.getNextButtonVisible().set(!isTakeOfferReview);
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TAKE_OFFER_PRICE: {
                if (!model.isPriceVisible()) {
                    Navigation.navigateTo(NavigationTarget.TAKE_OFFER_AMOUNT);
                    return Optional.empty();
                }
                return Optional.of(takeOfferPriceController);
            }
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
            model.setAnimateRightOut(false);
            model.getCurrentIndex().set(nextIndex);
            NavigationTarget nextTarget = model.getChildTargets().get(nextIndex);
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
            Navigation.navigateTo(nextTarget);
            updateNextButtonDisabledState();
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
        //reset();
        OverlayController.hide(() -> Navigation.navigateTo(NavigationTarget));
    }

    private void updateNextButtonDisabledState() {
        if (NavigationTarget.TAKE_OFFER_PAYMENT == model.getNavigationTarget()) {
            model.getNextButtonDisabled().set(takeOfferPaymentController.getSelectedFiatPaymentMethodSpec().get() == null);
        } else {
            model.getNextButtonDisabled().set(false);
        }
    }

    private void setMainButtonsVisibleState(boolean value) {
        NavigationTarget navigationTarget = model.getNavigationTarget();
        model.getBackButtonVisible().set(value && model.getChildTargets().indexOf(navigationTarget) > 0);
        model.getCloseButtonVisible().set(value);
    }
}
