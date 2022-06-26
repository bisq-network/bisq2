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

package bisq.desktop.primary.main.content.settings.reputation.earnReputation;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.settings.reputation.earnReputation.bond.BsqBondController;
import bisq.desktop.primary.main.content.settings.reputation.earnReputation.burn.BurnBsqController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class EarnReputationController extends TabController<EarnReputationModel> {
    @Getter
    private final EarnReputationView view;
    private final DefaultApplicationService applicationService;

    public EarnReputationController(DefaultApplicationService applicationService) {
        super(new EarnReputationModel(), NavigationTarget.EARN_REPUTATION);

        this.applicationService = applicationService;
        view = new EarnReputationView(model, this);
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
            case BURN_BSQ -> {
                return Optional.of(new BurnBsqController(applicationService));
            }
            case BSQ_BOND -> {
                return Optional.of(new BsqBondController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
