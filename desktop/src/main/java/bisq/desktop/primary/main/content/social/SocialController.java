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

package bisq.desktop.primary.main.content.social;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.content.social.hangout.HangoutController;
import bisq.desktop.primary.main.content.social.tradeintent.TradeIntentController;
import bisq.desktop.overlay.OverlayController;
import lombok.Getter;

public class SocialController extends TabController<SocialModel> {
    private final DefaultServiceProvider serviceProvider;
    @Getter
    private final SocialModel model;
    @Getter
    private final SocialView view;

    public SocialController(DefaultServiceProvider serviceProvider,
                            ContentController contentController,
                            OverlayController overlayController) {
        super(contentController, overlayController);

        this.serviceProvider = serviceProvider;
        model = new SocialModel(serviceProvider);
        view = new SocialView(model, this);
    }

    @Override
    protected NavigationTarget resolveLocalTarget(NavigationTarget navigationTarget) {
        return resolveAsLevel1Host(navigationTarget);
    }

    @Override
    protected Controller getController(NavigationTarget localTarget,NavigationTarget navigationTarget) {
        switch (localTarget) {
            case TRADE_INTENT -> {
                return new TradeIntentController(serviceProvider);
            }
            case HANGOUT -> {
                return new HangoutController(serviceProvider);
            }
            default -> throw new IllegalArgumentException("Invalid navigationTarget for this host. localTarget=" + localTarget);
        }
    }
}
