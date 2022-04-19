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

package bisq.desktop.primary.main.content.trade.multiSig;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.trade.multiSig.create.CreateOfferController;
import bisq.desktop.primary.main.content.trade.multiSig.offerbook.OfferbookController;
import bisq.desktop.primary.main.content.trade.multiSig.take.TakeOfferController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MultiSigController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final MultiSigModel model;
    @Getter
    private final MultiSigView view;
    private final OfferbookController offerbookController;

    public MultiSigController(DefaultApplicationService applicationService) {
        super(NavigationTarget.TRADE);

        this.applicationService = applicationService;
        model = new MultiSigModel();
        view = new MultiSigView(model, this);

        offerbookController = new OfferbookController(applicationService);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        model.showCreateOffer.set(navigationTarget == NavigationTarget.CREATE_OFFER);
        model.showTakeOffer.set(navigationTarget == NavigationTarget.TAKE_OFFER);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        model.showCreateOffer.set(false);
        model.showTakeOffer.set(false);
        switch (navigationTarget) {
            case OFFERBOOK -> {
                return Optional.of(offerbookController);
            }
            case CREATE_OFFER -> {
                model.showCreateOffer.set(true);
                return Optional.of(new CreateOfferController(applicationService));
            }
            case TAKE_OFFER -> {
                model.showTakeOffer.set(true);
                return Optional.of(new TakeOfferController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    public void onOpenCreateOffer() {
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER);
   /*     Navigation.navigateTo(NavigationTarget.CREATE_OFFER,
                new CreateOfferController.InitData(model.selectedMarket,
                        model.direction,
                        model.showCreateOfferTab));*/
    }

    public void onCloseCreateOffer() {
        model.showCreateOffer.set(false);
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }
    public void onCloseTakeOffer() {
        model.showTakeOffer.set(false);
        Navigation.navigateTo(NavigationTarget.OFFERBOOK);
    }
}
