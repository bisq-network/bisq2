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

package bisq.desktop.main.content.mu_sig.take_offer;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.mu_sig.take_offer.amount.MuSigTakeOfferAmountController;
import bisq.desktop.main.content.mu_sig.take_offer.payment_methods.MuSigTakeOfferPaymentController;
import bisq.desktop.main.content.mu_sig.take_offer.review.MuSigTakeOfferReviewController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.MuSigOffer;
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
public class MuSigTakeOfferController extends NavigationController implements InitWithDataController<MuSigTakeOfferController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigOffer muSigOffer;

        public InitData(MuSigOffer muSigOffer) {
            this.muSigOffer = muSigOffer;
        }
    }

    private final OverlayController overlayController;
    @Getter
    private final MuSigTakeOfferModel model;
    @Getter
    private final MuSigTakeOfferView view;
    private final MuSigTakeOfferAmountController muSigTakeOfferAmountController;
    private final MuSigTakeOfferPaymentController muSigTakeOfferPaymentController;
    private final MuSigTakeOfferReviewController muSigTakeOfferReviewController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription takersBaseSideAmountPin, takersQuoteSideAmountPin, selectedBitcoinPaymentMethodSpecPin, selectedFiatPaymentMethodSpecPin;

    public MuSigTakeOfferController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_TAKE_OFFER);

        overlayController = OverlayController.getInstance();

        model = new MuSigTakeOfferModel();
        view = new MuSigTakeOfferView(model, this);

        muSigTakeOfferAmountController = new MuSigTakeOfferAmountController(serviceProvider, this::setMainButtonsVisibleState);
        muSigTakeOfferPaymentController = new MuSigTakeOfferPaymentController(serviceProvider);
        muSigTakeOfferReviewController = new MuSigTakeOfferReviewController(serviceProvider, this::setMainButtonsVisibleState, this::closeAndNavigateTo);
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void initWithData(InitData initData) {
        MuSigOffer bisqEasyOffer = initData.getMuSigOffer();
        muSigTakeOfferAmountController.init(bisqEasyOffer);
        muSigTakeOfferPaymentController.init(bisqEasyOffer);
        muSigTakeOfferReviewController.init(bisqEasyOffer);

        model.setAmountVisible(bisqEasyOffer.hasAmountRange());
        List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs = bisqEasyOffer.getBaseSidePaymentMethodSpecs();
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = bisqEasyOffer.getQuoteSidePaymentMethodSpecs();
        model.setPaymentMethodVisible(baseSidePaymentMethodSpecs.size() > 1 || quoteSidePaymentMethodSpecs.size() > 1);

        model.getChildTargets().clear();
        if (model.isAmountVisible()) {
            model.getChildTargets().add(NavigationTarget.MU_SIG_TAKE_OFFER_AMOUNT);
        }
        if (model.isPaymentMethodVisible()) {
            model.getChildTargets().add(NavigationTarget.MU_SIG_TAKE_OFFER_PAYMENT);
        } else {
            checkArgument(baseSidePaymentMethodSpecs.size() == 1);
            checkArgument(quoteSidePaymentMethodSpecs.size() == 1);
            muSigTakeOfferReviewController.setBitcoinPaymentMethodSpec(baseSidePaymentMethodSpecs.get(0));
            muSigTakeOfferReviewController.setFiatPaymentMethodSpec(quoteSidePaymentMethodSpecs.get(0));
        }
        model.getChildTargets().add(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        model.getSelectedChildTarget().set(model.getChildTargets().get(0));
        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);
        takersBaseSideAmountPin = EasyBind.subscribe(muSigTakeOfferAmountController.getTakersBaseSideAmount(),
                muSigTakeOfferReviewController::setTakersBaseSideAmount);
        takersQuoteSideAmountPin = EasyBind.subscribe(muSigTakeOfferAmountController.getTakersQuoteSideAmount(),
                muSigTakeOfferReviewController::setTakersQuoteSideAmount);
        selectedBitcoinPaymentMethodSpecPin = EasyBind.subscribe(muSigTakeOfferPaymentController.getSelectedBitcoinPaymentMethodSpec(),
                muSigTakeOfferReviewController::setBitcoinPaymentMethodSpec);
        selectedFiatPaymentMethodSpecPin = EasyBind.subscribe(muSigTakeOfferPaymentController.getSelectedFiatPaymentMethodSpec(),
                muSigTakeOfferReviewController::setFiatPaymentMethodSpec);
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        takersBaseSideAmountPin.unsubscribe();
        takersQuoteSideAmountPin.unsubscribe();
        selectedBitcoinPaymentMethodSpecPin.unsubscribe();
        selectedFiatPaymentMethodSpecPin.unsubscribe();
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        boolean isTakeOfferReview = navigationTarget == NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW;
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
        return switch (navigationTarget) {
            case MU_SIG_TAKE_OFFER_AMOUNT -> {
                if (!model.isAmountVisible()) {
                    Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER_PAYMENT);
                    yield Optional.empty();
                }
                yield Optional.of(muSigTakeOfferAmountController);
            }
            case MU_SIG_TAKE_OFFER_PAYMENT -> {
                if (!model.isPaymentMethodVisible()) {
                    Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
                    yield Optional.empty();
                }
                yield Optional.of(muSigTakeOfferPaymentController);
            }
            case MU_SIG_TAKE_OFFER_REVIEW -> Optional.of(muSigTakeOfferReviewController);
            default -> Optional.empty();
        };
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get();
        if (nextIndex < model.getChildTargets().size()) {
            if (model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_TAKE_OFFER_PAYMENT) {
                if (!muSigTakeOfferPaymentController.isValid()) {
                    muSigTakeOfferPaymentController.handleInvalidInput();
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
        muSigTakeOfferReviewController.takeOffer(() -> {
            model.getBackButtonVisible().set(true);
            model.getTakeOfferButtonVisible().set(true);
        });
        model.getBackButtonVisible().set(false);
        model.getTakeOfferButtonVisible().set(false);
    }

    void onKeyPressed(KeyEvent keyEvent) {
        KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onClose);
        KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
            if (model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW) {
                onTakeOffer();
            } else {
                onNext();
            }
        });
    }

    private void closeAndNavigateTo(NavigationTarget navigationTarget) {
        OverlayController.hide(() -> Navigation.navigateTo(navigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        NavigationTarget navigationTarget = model.getNavigationTarget();
        model.getBackButtonVisible().set(value && model.getChildTargets().indexOf(navigationTarget) > 0);
        model.getNextButtonVisible().set(value && model.getSelectedChildTarget().get() != NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
        model.getCloseButtonVisible().set(value);
    }
}
