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

package bisq.desktop.main.left;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTargetController;
import bisq.desktop.main.content.ContentController;
import bisq.desktop.main.content.createoffer.CreateOfferController;
import bisq.desktop.main.content.markets.MarketsController;
import bisq.desktop.main.content.offerbook.OfferbookController;
import bisq.desktop.main.content.settings.SettingsController;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;

public class NavigationController extends NavigationTargetController implements Controller {
    private final DefaultServiceProvider serviceProvider;
    private final NavigationModel model;
    @Getter
    private final NavigationView view;

    public NavigationController(DefaultServiceProvider serviceProvider,
                                ContentController contentController,
                                OverlayController overlayController) {
        super(contentController, overlayController);

        this.serviceProvider = serviceProvider;
        model = new NavigationModel(serviceProvider);
        view = new NavigationView(model, this);
    }

    @Override
    protected Controller getMarketsController(NavigationTarget navigationTarget) {
        return new MarketsController(serviceProvider);
    }

    @Override
    protected Controller getCreateOfferController(NavigationTarget navigationTarget) {
        return new CreateOfferController(serviceProvider);
    }

    @Override
    protected Controller getOfferbookController(NavigationTarget navigationTarget) {
        return new OfferbookController(serviceProvider, this, overlayController);
    }

    @Override
    protected Controller getSettingsController(NavigationTarget navigationTarget) {
        return new SettingsController(serviceProvider, contentController, overlayController, navigationTarget);
    }
}
