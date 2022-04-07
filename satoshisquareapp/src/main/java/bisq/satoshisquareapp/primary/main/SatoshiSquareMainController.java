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

package bisq.satoshisquareapp.primary.main;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.primary.main.content.social.SocialController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SatoshiSquareMainController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final SatoshiSquareMainModel model;
    @Getter
    private final SatoshiSquareMainView view;

    public SatoshiSquareMainController(DefaultApplicationService applicationService) {
        super(NavigationTarget.MAIN);

        this.applicationService = applicationService;
        model = new SatoshiSquareMainModel();
        view = new SatoshiSquareMainView(model, this);
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
            case SOCIAL -> {
                return Optional.of(new SocialController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
