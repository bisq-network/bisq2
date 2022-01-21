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

package bisq.desktop.primary.main.content.swap;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.swap.create.CreateOfferController;
import bisq.desktop.primary.main.content.swap.listings.OfferbookController;
import bisq.desktop.primary.main.content.swap.take.TakeOfferController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SwapController extends TabController {
    private final DefaultServiceProvider serviceProvider;
    @Getter
    private final SwapModel model;
    @Getter
    private final SwapView view;

    public SwapController(DefaultServiceProvider serviceProvider) {
        super(NavigationTarget.SWAP);

        this.serviceProvider = serviceProvider;
        model = new SwapModel(serviceProvider);
        view = new SwapView(model, this);
    }

    @Override
    protected Optional<Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case OFFERBOOK -> {
                return Optional.of(new OfferbookController(serviceProvider));
            }
            case CREATE_OFFER -> {
                return Optional.of(new CreateOfferController(serviceProvider));
            }
            case TAKE_OFFER -> {
                return Optional.of(new TakeOfferController(serviceProvider));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
