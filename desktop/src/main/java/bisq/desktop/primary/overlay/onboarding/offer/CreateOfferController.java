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

package bisq.desktop.primary.overlay.onboarding.offer;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.desktop.primary.overlay.onboarding.offer.amount.AmountController;
import bisq.desktop.primary.overlay.onboarding.offer.complete.OfferCompletedController;
import bisq.desktop.primary.overlay.onboarding.offer.direction.DirectionController;
import bisq.desktop.primary.overlay.onboarding.offer.market.MarketController;
import bisq.desktop.primary.overlay.onboarding.offer.method.PaymentMethodController;
import bisq.desktop.primary.overlay.onboarding.offer.published.OfferPublishedController;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class CreateOfferController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final CreateOfferModel model;
    @Getter
    private final CreateOfferView view;

    public CreateOfferController(DefaultApplicationService applicationService) {
        super(NavigationTarget.CREATE_OFFER);

        this.applicationService = applicationService;
        model = new CreateOfferModel();
        view = new CreateOfferView(model, this);
    }

    @Override
    public void onActivate() {
        OverlayController.setTransitionsType(Transitions.Type.BLACK);
        model.getSkipButtonText().set(Res.get("onboarding.navProgress.skip"));
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CREATE_OFFER_DIRECTION -> {
                return Optional.of(new DirectionController(applicationService));
            }
            case CREATE_OFFER_MARKET -> {
                return Optional.of(new MarketController(applicationService));
            }
            case CREATE_OFFER_AMOUNT -> {
                return Optional.of(new AmountController(applicationService));
            }
            case CREATE_OFFER_PAYMENT_METHOD -> {
                return Optional.of(new PaymentMethodController(applicationService));
            }
            case CREATE_OFFER_OFFER_COMPLETED -> {
                return Optional.of(new OfferCompletedController(applicationService));
            }
            case CREATE_OFFER_OFFER_PUBLISHED -> {
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
