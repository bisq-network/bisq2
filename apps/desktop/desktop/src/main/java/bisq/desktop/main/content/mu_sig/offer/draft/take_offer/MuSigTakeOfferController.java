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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.MuSigTakeOfferAmountController;
import bisq.desktop.main.content.mu_sig.offer.draft.take_offer.payment.MuSigTakeOfferPaymentController;
import bisq.desktop.main.content.mu_sig.offer.draft.take_offer.review.MuSigTakeOfferReviewController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.components.overlay.Popup;
import bisq.common.observable.Pin;
import bisq.i18n.Res;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.settings.SettingsService;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferValidationException;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferUseCase;
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
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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

    private final AccountService accountService;
    private final TakeOfferUseCase takeOfferService;
    private final SettingsService settingsService;
    private Pin priceDeviationPin;
    private Pin amountLimitsPin;
    private boolean warnedAboutPriceDeviation;
    private long activationGeneration;
    private final OverlayController overlayController;
    @Getter
    private final MuSigTakeOfferModel model;
    @Getter
    private final MuSigTakeOfferView view;
    private final MuSigTakeOfferAmountController muSigTakeOfferAmountController;
    private final MuSigTakeOfferPaymentController muSigTakeOfferPaymentController;
    private final MuSigTakeOfferReviewController muSigTakeOfferReviewController;
    private final EventHandler<KeyEvent> onKeyPressedHandler = this::onKeyPressed;
    private Subscription selectedAccountPin, paymentMethodSpecPin;

    public MuSigTakeOfferController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_TAKE_OFFER);

        accountService = serviceProvider.getAccountService();
        settingsService = serviceProvider.getSettingsService();
        overlayController = OverlayController.getInstance();

        takeOfferService = new TakeOfferUseCase(serviceProvider.getBondedRolesService().getMarketPriceService(),
                serviceProvider.getIdentityService(),
                serviceProvider.getSettingsService(),
                serviceProvider.getAccountService());

        model = new MuSigTakeOfferModel();
        view = new MuSigTakeOfferView(model, this);

        muSigTakeOfferAmountController = new MuSigTakeOfferAmountController(takeOfferService,
                this::setMainButtonsVisibleState);
        muSigTakeOfferPaymentController = new MuSigTakeOfferPaymentController(serviceProvider,
                takeOfferService,
                this::setMainButtonsVisibleState);
        muSigTakeOfferReviewController = new MuSigTakeOfferReviewController(serviceProvider,
                takeOfferService,
                this::setMainButtonsVisibleState,
                this::closeAndNavigateTo);
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void initWithData(InitData initData) {
        MuSigOffer muSigOffer = initData.getMuSigOffer();

        try {
            takeOfferService.initialize(muSigOffer);
        } catch (TakeOfferValidationException e) {
            log.warn("Offer {} failed take-offer validation: {}", muSigOffer.getId(), e.getMessage());
            // The overlay is still being constructed at this point; the rejection is surfaced
            // in onActivate once the view is attached.
            model.setTakeOfferValidationFailure(e.getReason());
            model.suppressChildNavigation();
            return;
        }

        muSigTakeOfferAmountController.init(muSigOffer);
        muSigTakeOfferPaymentController.init(muSigOffer);
        muSigTakeOfferReviewController.init(muSigOffer);

        boolean isBaseCurrencyBitcoin = muSigOffer.getMarket().isBaseCurrencyBitcoin();
        model.setPaymentMethodProgressLabel(isBaseCurrencyBitcoin
                ? Res.get("muSig.offer.wizard.progress.account.fiat")
                : Res.get("muSig.offer.wizard.progress.account.crypto"));

        model.setAmountVisible(takeOfferService.shouldShowAmountStep());
        model.setPaymentMethodVisible(takeOfferService.shouldShowPaymentStep());

        model.getChildTargets().clear();
        if (model.isPaymentMethodVisible()) {
            model.getChildTargets().add(NavigationTarget.MU_SIG_TAKE_OFFER_PAYMENT);
        } else {
            muSigTakeOfferReviewController.setTakersAccount(takeOfferService.getSelectedAccount().orElseThrow());
            muSigTakeOfferReviewController.setTakersPaymentMethodSpec(takeOfferService.getSelectedPaymentMethodSpec().orElseThrow());
        }
        if (model.isAmountVisible()) {
            model.getChildTargets().add(NavigationTarget.MU_SIG_TAKE_OFFER_AMOUNT);
        }
        model.getChildTargets().add(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
    }

    @Override
    public void onActivate() {
        overlayController.setUseEscapeKeyHandler(false);
        overlayController.setEnterKeyHandler(null);
        overlayController.getApplicationRoot().addEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);

        // The controller instance is cached across wizard sessions: the latch must start fresh,
        // and bumping the generation invalidates any deferred warning a previous session left in
        // OverlayController.runOnShown.
        warnedAboutPriceDeviation = false;
        activationGeneration++;

        TakeOfferValidationException.Reason validationFailure = model.getTakeOfferValidationFailure();
        if (validationFailure != null) {
            new Popup().warning(getValidationWarning(validationFailure)).show();
            onClose();
            return;
        }

        priceDeviationPin = takeOfferService.getPriceService().priceDeviationObservable().addObserver(deviation ->
                UIThread.run(this::maybeShowPriceDeviationWarning));

        NavigationTarget first = model.getChildTargets().getFirst();
        model.getSelectedChildTarget().set(first);
        model.getBackButtonText().set(Res.get("action.back"));
        model.getNextButtonVisible().set(true);

        // The review step reads the taker's amounts from the domain amount observables directly.
        selectedAccountPin = EasyBind.subscribe(muSigTakeOfferPaymentController.getSelectedAccount(),
                muSigTakeOfferReviewController::setTakersAccount);
        paymentMethodSpecPin = EasyBind.subscribe(muSigTakeOfferPaymentController.getPaymentMethodSpec(),
                muSigTakeOfferReviewController::setTakersPaymentMethodSpec);
        // The domain publishes the limits AFTER updating the collapse state, so observing the
        // limits (not the UI selection properties) reads a consistent shouldShowAmountStep.
        amountLimitsPin = takeOfferService.getAmountService().tradeAmountLimitsObservable().addObserver(limits ->
                UIThread.run(this::updateAmountStepVisibility));
    }

    // A user-initiated recomputation can collapse or un-collapse the effective amount range
    // (take-offer.md, "Amount", collapse rule). It can originate from the payment step (method
    // selection) or from the amount step itself (input-side switch), so the rebuilt target list
    // must be reconciled with the step the user is currently on.
    private void updateAmountStepVisibility() {
        if (model.getTakeOfferValidationFailure() != null) {
            return;
        }
        // The limits observer queues this through UIThread.run; an already queued call can run
        // after onDeactivate disposed the domain, where shouldShowAmountStep must not be asked.
        if (takeOfferService.getAmountService().getTradeAmountLimits() == null) {
            return;
        }
        boolean amountVisible = takeOfferService.shouldShowAmountStep();
        if (amountVisible == model.isAmountVisible()) {
            return;
        }
        model.setAmountVisible(amountVisible);
        if (amountVisible) {
            int reviewIndex = model.getChildTargets().indexOf(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
            if (reviewIndex >= 0 && !model.getChildTargets().contains(NavigationTarget.MU_SIG_TAKE_OFFER_AMOUNT)) {
                model.getChildTargets().add(reviewIndex, NavigationTarget.MU_SIG_TAKE_OFFER_AMOUNT);
            }
        } else {
            model.getChildTargets().remove(NavigationTarget.MU_SIG_TAKE_OFFER_AMOUNT);
        }
        reconcileCurrentStep();
    }

    // The child target list just changed. The current index must follow the selected target's
    // new position, else Next and Back would address neighbours of a stale index. An input-side
    // switch happens ON the amount step, so the recomputation it triggers can remove the very
    // step the user is standing on; the flow then moves to the review step (the collapsed
    // amount has already been published as fixed by the domain).
    private void reconcileCurrentStep() {
        NavigationTarget selected = model.getSelectedChildTarget().get();
        if (selected == null) {
            return;
        }
        int index = model.getChildTargets().indexOf(selected);
        if (index >= 0) {
            model.getCurrentIndex().set(index);
            return;
        }
        int reviewIndex = model.getChildTargets().indexOf(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
        if (reviewIndex < 0) {
            return;
        }
        model.setAnimateRightOut(false);
        model.getCurrentIndex().set(reviewIndex);
        model.getSelectedChildTarget().set(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
        Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
    }

    @Override
    public void onDeactivate() {
        if (priceDeviationPin != null) {
            priceDeviationPin.unbind();
            priceDeviationPin = null;
        }
        if (amountLimitsPin != null) {
            amountLimitsPin.unbind();
            amountLimitsPin = null;
        }
        takeOfferService.dispose();
        overlayController.setUseEscapeKeyHandler(true);
        overlayController.getApplicationRoot().removeEventHandler(KeyEvent.KEY_PRESSED, onKeyPressedHandler);
        if (selectedAccountPin != null) {
            selectedAccountPin.unsubscribe();
            selectedAccountPin = null;
        }
        if (paymentMethodSpecPin != null) {
            paymentMethodSpecPin.unsubscribe();
            paymentMethodSpecPin = null;
        }
        reset();
    }

    @Override
    protected void onNavigationTargetApplied(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        boolean isTakeOfferReview = navigationTarget == NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW;
        model.getNextButtonText().set(isTakeOfferReview ?
                Res.get("muSig.offer.taker.review.takeOffer") :
                Res.get("action.next"));
        setMainButtonsVisibleState(true);
        model.getTakeOfferButtonVisible().set(isTakeOfferReview);
        model.getNextButtonVisible().set(!isTakeOfferReview);
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_TAKE_OFFER_PAYMENT -> {
                if (!model.isPaymentMethodVisible()) {
                    Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER_AMOUNT);
                    yield Optional.empty();
                }
                yield Optional.of(muSigTakeOfferPaymentController);
            }
            case MU_SIG_TAKE_OFFER_AMOUNT -> {
                if (!model.isAmountVisible()) {
                    Navigation.navigateTo(NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW);
                    yield Optional.empty();
                }
                yield Optional.of(muSigTakeOfferAmountController);
            }
            case MU_SIG_TAKE_OFFER_REVIEW -> Optional.of(muSigTakeOfferReviewController);
            default -> Optional.empty();
        };
    }

    void onNext() {
        int nextIndex = model.getCurrentIndex().get() + 1;
        if (nextIndex < model.getChildTargets().size()) {
            if (model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_TAKE_OFFER_PAYMENT) {
                if (!muSigTakeOfferPaymentController.validate()) {
                    return;
                }
            }
            if (model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_TAKE_OFFER_AMOUNT
                    && !takeOfferService.getAmountService().isAmountValid()) {
                // A cleared or unapplicable amount input blocks advancing, not only final
                // confirmation; this also covers Enter, which the parent key handler routes
                // here regardless of which control owns the focus.
                new Popup().warning(Res.get("muSig.takeOffer.validation.invalidAmountInput"))
                        .owner(view.getRoot())
                        .show();
                return;
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

    // Re-reads deviation and threshold at display time: the popup is deferred until the overlay
    // display animation completed (else the overlay stage ends up above it), and the deviation may
    // have changed meanwhile.
    private void maybeShowPriceDeviationWarning() {
        // The observer queues this through UIThread.run; unbinding does not cancel an already
        // queued call, which can therefore run after onDeactivate disposed the domain.
        if (priceDeviationPin == null) {
            return;
        }
        Double deviation = takeOfferService.getPriceService().getPriceDeviation();
        if (deviation == null) {
            return;
        }
        double threshold = settingsService.getPriceDeviationWarningThreshold().get();
        if (Math.abs(deviation) > threshold) {
            if (!warnedAboutPriceDeviation) {
                warnedAboutPriceDeviation = true;
                long generation = activationGeneration;
                overlayController.runOnShown(() -> {
                    // A deferred handler can outlive the wizard session that stored it; only the
                    // registering activation may show its warning.
                    if (generation != activationGeneration || priceDeviationPin == null) {
                        return;
                    }
                    Double currentDeviation = takeOfferService.getPriceService().getPriceDeviation();
                    double currentThreshold = settingsService.getPriceDeviationWarningThreshold().get();
                    if (currentDeviation == null || Math.abs(currentDeviation) <= currentThreshold) {
                        warnedAboutPriceDeviation = false;
                        return;
                    }
                    new Popup().warning(Res.get("muSig.takeOffer.priceDeviationWarning",
                                    PercentageFormatter.formatToPercentWithSymbol(Math.abs(currentDeviation))))
                            .owner(view.getRoot())
                            .show();
                });
            }
        } else {
            warnedAboutPriceDeviation = false;
        }
    }

    private static String getValidationWarning(TakeOfferValidationException.Reason reason) {
        return switch (reason) {
            case OWN_OFFER -> Res.get("muSig.takeOffer.validation.ownOffer");
            case NO_MARKET_PRICE -> Res.get("muSig.takeOffer.validation.noMarketPrice");
            case AMOUNT_OUTSIDE_LIMITS -> Res.get("muSig.takeOffer.validation.amountOutsideLimits");
            default -> Res.get("muSig.takeOffer.validation.invalidOffer");
        };
    }

    void onTakeOffer() {
        muSigTakeOfferReviewController.takeOffer();
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

    private void reset() {
        resetSelectedChildTarget();
        muSigTakeOfferAmountController.reset();
        muSigTakeOfferPaymentController.reset();
        muSigTakeOfferReviewController.reset();

        model.reset();
    }

    private void closeAndNavigateTo(NavigationTarget navigationTarget) {
        OverlayController.hide(() -> Navigation.navigateTo(navigationTarget));
    }

    private void setMainButtonsVisibleState(boolean value) {
        NavigationTarget navigationTarget = model.getNavigationTarget();
        boolean isTakeOfferReview = model.getSelectedChildTarget().get() == NavigationTarget.MU_SIG_TAKE_OFFER_REVIEW;
        model.getBackButtonVisible().set(value && model.getChildTargets().indexOf(navigationTarget) > 0);
        model.getNextButtonVisible().set(value && !isTakeOfferReview);
        model.getTakeOfferButtonVisible().set(value && isTakeOfferReview);
        model.getCloseButtonVisible().set(value);
    }
}
