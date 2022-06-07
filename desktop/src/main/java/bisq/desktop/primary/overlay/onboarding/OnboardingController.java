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

package bisq.desktop.primary.overlay.onboarding;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.bisqeasy.BisqEasyOnboardingController;
import bisq.desktop.primary.overlay.onboarding.offer.amount.AmountController;
import bisq.desktop.primary.overlay.onboarding.offer.complete.OfferCompletedController;
import bisq.desktop.primary.overlay.onboarding.offer.direction.DirectionController;
import bisq.desktop.primary.overlay.onboarding.offer.market.MarketController;
import bisq.desktop.primary.overlay.onboarding.offer.method.PaymentMethodController;
import bisq.desktop.primary.overlay.onboarding.offer.published.OfferPublishedController;
import bisq.desktop.primary.overlay.onboarding.profile.CreateProfileController;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OnboardingController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final OnboardingModel model;
    @Getter
    private final OnboardingView view;

    public OnboardingController(DefaultApplicationService applicationService) {
        super(NavigationTarget.ONBOARDING);

        this.applicationService = applicationService;
        model = new OnboardingModel();
        view = new OnboardingView(model, this);
    }

    @Override
    public void onNavigateToChild(NavigationTarget navigationTarget) {
        model.getSkipButtonText().set(Res.get("onboarding.navProgress.skip"));
        switch (navigationTarget) {
            case BISQ_EASY_ONBOARDING -> {
                model.getNavigationProgressVisible().set(false);
                model.getSkipButtonVisible().set(false);
            }
            case CREATE_PROFILE -> {
                model.getNavigationProgressVisible().set(false);
                model.getSkipButtonVisible().set(false);
            }
            case ONBOARDING_DIRECTION -> {
                model.getNavigationProgressVisible().set(true);
                model.getNavigationProgressIndex().set(0);
                model.getSkipButtonVisible().set(true);
            }
            case ONBOARDING_MARKET -> {
                model.getNavigationProgressVisible().set(true);
                model.getNavigationProgressIndex().set(1);
                model.getSkipButtonVisible().set(true);
            }
            case ONBOARDING_AMOUNT -> {
                model.getNavigationProgressVisible().set(true);
                model.getNavigationProgressIndex().set(2);
                model.getSkipButtonVisible().set(true);
            }
            case ONBOARDING_PAYMENT_METHOD -> {
                model.getNavigationProgressVisible().set(true);
                model.getNavigationProgressIndex().set(3);
                model.getSkipButtonVisible().set(true);
            }
            case ONBOARDING_OFFER_COMPLETED -> {
                model.getNavigationProgressVisible().set(true);
                model.getNavigationProgressIndex().set(4);
                model.getSkipButtonVisible().set(true);
            }
            case ONBOARDING_OFFER_PUBLISHED -> {
                model.getNavigationProgressVisible().set(false);
                model.getSkipButtonText().set(Res.get("close"));
                model.getSkipButtonVisible().set(true);
            }
            default -> {
            }
        }
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case BISQ_EASY_ONBOARDING -> {
                return Optional.of(new BisqEasyOnboardingController(applicationService));
            }
            case CREATE_PROFILE -> {
                return Optional.of(new CreateProfileController(applicationService));
            }
            case ONBOARDING_DIRECTION -> {
                return Optional.of(new DirectionController(applicationService));
            }
            case ONBOARDING_MARKET -> {
                return Optional.of(new MarketController(applicationService));
            }
            case ONBOARDING_AMOUNT -> {
                return Optional.of(new AmountController(applicationService));
            }
            case ONBOARDING_PAYMENT_METHOD -> {
                return Optional.of(new PaymentMethodController(applicationService));
            }
            case ONBOARDING_OFFER_COMPLETED -> {
                return Optional.of(new OfferCompletedController(applicationService));
            }
            case ONBOARDING_OFFER_PUBLISHED -> {
                return Optional.of(new OfferPublishedController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    public void onSkip() {
        OverlayController.hide();
    }
}
