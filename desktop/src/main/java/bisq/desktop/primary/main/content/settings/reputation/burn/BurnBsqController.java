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

package bisq.desktop.primary.main.content.settings.reputation.burn;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.settings.reputation.burn.tab1.BurnBsqTab1Controller;
import bisq.desktop.primary.main.content.settings.reputation.burn.tab2.BurnBsqTab2Controller;
import bisq.desktop.primary.main.content.settings.reputation.burn.tab3.BurnBsqTab3Controller;
import bisq.desktop.primary.overlay.OverlayController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BurnBsqController extends TabController<BurnBsqModel> {
    @Getter
    private final BurnBsqView view;
    private final DefaultApplicationService applicationService;

    public BurnBsqController(DefaultApplicationService applicationService) {
        super(new BurnBsqModel(), NavigationTarget.BURN_BSQ);

        this.applicationService = applicationService;
        view = new BurnBsqView(model, this);
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
            case BURN_BSQ_TAB_1: {
                return Optional.of(new BurnBsqTab1Controller(applicationService));
            }
            case BURN_BSQ_TAB_2: {
                return Optional.of(new BurnBsqTab2Controller(applicationService));
            }
            case BURN_BSQ_TAB_3: {
                return Optional.of(new BurnBsqTab3Controller(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onClose() {
        OverlayController.hide();
    }
}
