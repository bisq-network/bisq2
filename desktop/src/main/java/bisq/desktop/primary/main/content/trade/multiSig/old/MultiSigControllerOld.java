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

package bisq.desktop.primary.main.content.trade.multiSig.old;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.multiSig.old.closedTrades.ClosedTradesController;
import bisq.desktop.primary.main.content.trade.multiSig.old.createOffer.MultiSigCreateOfferController;
import bisq.desktop.primary.main.content.trade.multiSig.old.offerbook.OfferbookController;
import bisq.desktop.primary.main.content.trade.multiSig.old.openoffers.OpenOffersController;
import bisq.desktop.primary.main.content.trade.multiSig.old.pendingTrades.PendingTradesController;
import bisq.desktop.primary.main.content.trade.multiSig.old.takeOffer.TakeOfferController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MultiSigControllerOld extends TabController<MultiSigModel> implements Controller {
    private final DefaultApplicationService applicationService;
    @Getter
    private final MultiSigView view;
    private final OfferbookController offerbookController;

    public MultiSigControllerOld(DefaultApplicationService applicationService) {
        super(new MultiSigModel(), NavigationTarget.BISQ_MULTISIG);

        this.applicationService = applicationService;
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
        model.showCreateOffer.set(navigationTarget == NavigationTarget.MULTI_SIG_CREATE_OFFER);
        model.showTakeOffer.set(navigationTarget == NavigationTarget.MULTI_SIG_TAKE_OFFER);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        model.showCreateOffer.set(false);
        model.showTakeOffer.set(false);
        switch (navigationTarget) {
            case MULTI_SIG_OFFER_BOOK: {
                return Optional.of(offerbookController);
            }
            case MULTI_SIG_CREATE_OFFER: {
                model.showCreateOffer.set(true);
                return Optional.of(new MultiSigCreateOfferController(applicationService));
            }
            case MULTI_SIG_TAKE_OFFER: {
                model.showTakeOffer.set(true);
                return Optional.of(new TakeOfferController(applicationService));
            }
            case MULTI_SIG_OPEN_OFFERS: {
                return Optional.of(new OpenOffersController(applicationService));
            }
            case MULTI_SIG_PENDING_TRADES: {
                return Optional.of(new PendingTradesController(applicationService));
            }
            case MULTI_SIG_CLOSED_TRADES: {
                return Optional.of(new ClosedTradesController(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    public void onCloseCreateOffer() {
        model.showCreateOffer.set(false);
        Navigation.navigateTo(NavigationTarget.MULTI_SIG_OFFER_BOOK);
    }

    public void onCloseTakeOffer() {
        model.showTakeOffer.set(false);
        Navigation.navigateTo(NavigationTarget.MULTI_SIG_OFFER_BOOK);
    }
}
