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

package bisq.desktop.main.content.user.reputation.burn;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.user.reputation.burn.tab1.BurnBsqTab1Controller;
import bisq.desktop.main.content.user.reputation.burn.tab2.BurnBsqTab2Controller;
import bisq.desktop.main.content.user.reputation.burn.tab3.BurnBsqTab3Controller;
import bisq.desktop.overlay.OverlayController;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqController extends TabController<BurnBsqModel> {
    @Getter
    private final BurnBsqView view;
    private final ServiceProvider serviceProvider;

    public BurnBsqController(ServiceProvider serviceProvider) {
        super(new BurnBsqModel(), NavigationTarget.BURN_BSQ);

        this.serviceProvider = serviceProvider;
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
                return Optional.of(new BurnBsqTab1Controller(serviceProvider));
            }
            case BURN_BSQ_TAB_2: {
                return Optional.of(new BurnBsqTab2Controller(serviceProvider));
            }
            case BURN_BSQ_TAB_3: {
                return Optional.of(new BurnBsqTab3Controller(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onClose() {
        OverlayController.hide();
    }

    //Not caching as the view needs to validate window boundaries during rendering
    @Override
    public boolean useCaching() {
        return false;
    }
}
