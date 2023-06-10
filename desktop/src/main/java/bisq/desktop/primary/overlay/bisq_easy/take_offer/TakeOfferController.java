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
import bisq.desktop.primary.overlay.bisq_easy.take_offer.amount.TakeOfferAmountController;
import bisq.desktop.primary.overlay.bisq_easy.take_offer.review.TakeOfferReviewController;
import bisq.desktop.primary.overlay.bisq_easy.take_offer.settlement.TakeOfferSettlementController;
import bisq.i18n.Res;
import bisq.offer.amount.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.settlement.SettlementUtil;
import javafx.application.Platform;
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
        private final List<String> takersSettlementMethodNames;

        public InitData(BisqEasyOffer bisqEasyOffer) {
            this(bisqEasyOffer, Optional.empty(), new ArrayList<>());
        }

        public InitData(BisqEasyOffer bisqEasyOffer, Optional<AmountSpec> takersAmountSpec, List<String> takersSettlementMethodNames) {
            this.bisqEasyOffer = bisqEasyOffer;
            this.takersAmountSpec = takersAmountSpec;
            this.takersSettlementMethodNames = takersSettlementMethodNames;
        }
    }

    private final DefaultApplicationService applicationService;
    @Getter
    private final TakeOfferModel model;
    @Getter
    private final TakeOfferView view;
    private final TakeOfferAmountController takeOfferAmountController;
    private final TakeOfferSettlementController takeOfferSettlementController;
    private final TakeOfferReviewController takeOfferReviewController;
    private Subscription takersAmountSpecPin, methodNamePin;

    public TakeOfferController(DefaultApplicationService applicationService) {
        super(NavigationTarget.TAKE_OFFER);

        this.applicationService = applicationService;

        model = new TakeOfferModel();
        view = new TakeOfferView(model, this);

        takeOfferAmountController = new TakeOfferAmountController(applicationService);
        takeOfferSettlementController = new TakeOfferSettlementController(applicationService);
        takeOfferReviewController = new TakeOfferReviewController(applicationService, this::setMainButtonsVisibleState);
    }

    @Override
    public boolean useCaching() {
        return false;
    }

    @Override
    public void initWithData(InitData initData) {
        BisqEasyOffer bisqEasyOffer = initData.getBisqEasyOffer();
        takeOfferAmountController.init(bisqEasyOffer, initData.getTakersAmountSpec());
        takeOfferSettlementController.init(bisqEasyOffer, initData.getTakersSettlementMethodNames());
        takeOfferReviewController.init(bisqEasyOffer);


        model.setAmountVisible(bisqEasyOffer.hasAmountRange());
        model.setSettlementVisible(bisqEasyOffer.getQuoteSideSettlementSpecs().size() > 1);

        model.getChildTargets().clear();
        if (model.isAmountVisible()) {
            model.getChildTargets().add(NavigationTarget.TAKE_OFFER_AMOUNT);
        }
        if (model.isSettlementVisible()) {
            model.getChildTargets().add(NavigationTarget.TAKE_OFFER_SETTLEMENT);
        } else {
            List<String> methodNames = SettlementUtil.getQuoteSideSettlementMethodNames(bisqEasyOffer);
            checkArgument(methodNames.size() == 1);
            takeOfferReviewController.setSettlementMethodName(methodNames.get(0));
        }
        model.getChildTargets().add(NavigationTarget.TAKE_OFFER_REVIEW);
    }

    @Override
    public void onActivate() {
        model.getBackButtonText().set(Res.get("back"));
        model.getNextButtonVisible().set(true);
        takersAmountSpecPin = EasyBind.subscribe(takeOfferAmountController.getTakersAmountSpec(), takeOfferReviewController::setTradeAmountSpec);
        methodNamePin = EasyBind.subscribe(takeOfferSettlementController.getSelectedMethodName(), methodName -> {
            takeOfferReviewController.setSettlementMethodName(methodName);
            updateNextButtonDisabledState(methodName);
        });
    }

    @Override
    public void onDeactivate() {
        takersAmountSpecPin.unsubscribe();
        methodNamePin.unsubscribe();
    }

    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        model.getCloseButtonVisible().set(true);
        model.getNextButtonText().set(navigationTarget == NavigationTarget.TAKE_OFFER_REVIEW ?
                Res.get("bisqEasy.takeOffer.review.takeOffer") :
                Res.get("next"));
        model.getShowProgressBox().set(navigationTarget != NavigationTarget.TAKE_OFFER_REVIEW);
        model.getSelectedChildTarget().set(navigationTarget);
        setMainButtonsVisibleState(true);
        updateNextButtonDisabledState(takeOfferSettlementController.getSelectedMethodName().get());
        model.getTakeOfferButtonVisible().set(navigationTarget == NavigationTarget.TAKE_OFFER_REVIEW);
        model.getNextButtonVisible().set(navigationTarget != NavigationTarget.TAKE_OFFER_REVIEW);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        if (navigationTarget == NavigationTarget.TAKE_OFFER_AMOUNT && !model.isAmountVisible()) {
            if (model.isSettlementVisible()) {
                Navigation.navigateTo(NavigationTarget.TAKE_OFFER_SETTLEMENT);
                return Optional.empty();
            } else {
                Navigation.navigateTo(NavigationTarget.TAKE_OFFER_REVIEW);
                return Optional.empty();
            }
        } else if (navigationTarget == NavigationTarget.TAKE_OFFER_SETTLEMENT && !model.isSettlementVisible()) {
            Navigation.navigateTo(NavigationTarget.TAKE_OFFER_REVIEW);
            return Optional.empty();
        }

        switch (navigationTarget) {
            case TAKE_OFFER_AMOUNT: {
                return Optional.of(takeOfferAmountController);
            }
            case TAKE_OFFER_SETTLEMENT: {
                return Optional.of(takeOfferSettlementController);
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
            model.getSelectedChildTarget().set(nextTarget);
            Navigation.navigateTo(nextTarget);
            updateNextButtonDisabledState(takeOfferSettlementController.getSelectedMethodName().get());
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
            updateNextButtonDisabledState(takeOfferSettlementController.getSelectedMethodName().get());
        }
    }

    void onClose() {
        Navigation.navigateTo(NavigationTarget.MAIN);
        OverlayController.hide();
    }

    void onTakeOffer() {
        takeOfferReviewController.doTakeOffer();
        model.getTakeOfferButtonVisible().set(false);
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    private void updateNextButtonDisabledState(String methodName) {
        if (NavigationTarget.TAKE_OFFER_SETTLEMENT == model.getSelectedChildTarget().get()) {
            model.getNextButtonDisabled().set(methodName == null);
        } else {
            model.getNextButtonDisabled().set(false);
        }
    }

    private void setMainButtonsVisibleState(boolean value) {
        NavigationTarget navigationTarget = model.getSelectedChildTarget().get();
        boolean isAmount = navigationTarget == NavigationTarget.TAKE_OFFER_AMOUNT;
        boolean isSettlementWithNoAmount = navigationTarget == NavigationTarget.TAKE_OFFER_SETTLEMENT && !model.isAmountVisible();
        boolean isReviewWithNoSettlementAndNoAmount = navigationTarget == NavigationTarget.TAKE_OFFER_REVIEW && !model.isAmountVisible() && !model.isSettlementVisible();
        boolean hasPreviousTab = !(isAmount || isSettlementWithNoAmount || isReviewWithNoSettlementAndNoAmount);
        model.getBackButtonVisible().set(value && hasPreviousTab);
        model.getCloseButtonVisible().set(value);
    }
}
