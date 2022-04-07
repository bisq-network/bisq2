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

package bisq.desktop.primary.main.content.trade;

import bisq.application.DefaultApplicationService;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.CachingController;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.create.CreateOfferController;
import bisq.desktop.primary.main.content.trade.offerbook.OfferbookController;
import bisq.desktop.primary.main.content.trade.take.TakeOfferController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeController extends TabController implements /*Navigation.Listener,*/ CachingController {

    private final DefaultApplicationService applicationService;
    @Getter
    private final TradeModel model;
    @Getter
    private final TradeView view;
    private final OfferbookController offerbookController;

    public TradeController(DefaultApplicationService applicationService) {
        super(NavigationTarget.TRADE);

        this.applicationService = applicationService;
        model = new TradeModel();
        view = new TradeView(model, this);

        offerbookController = new OfferbookController(applicationService);
    }

    @Override
    public void onActivate() {
       // Navigation.addListener(host, na);
    }

    @Override
    public void onDeactivate() {
       // Navigation.removeListener(host, this);
    }

    @Override
    public void onNavigate2(NavigationTarget navigationTarget, Optional<Object> data) {
        model.createOfferTabVisible.set(navigationTarget == NavigationTarget.CREATE_OFFER ||
                offerbookController.showCreateOfferTab());
        model.takeOfferTabVisible.set(navigationTarget == NavigationTarget.TAKE_OFFER ||
                offerbookController.getShowTakeOfferTab().get());
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case OFFERBOOK -> {
                return Optional.of(offerbookController);
            }
            case CREATE_OFFER -> {
                return Optional.of(new CreateOfferController(applicationService));
            }
            case TAKE_OFFER -> {
                return Optional.of(new TakeOfferController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
