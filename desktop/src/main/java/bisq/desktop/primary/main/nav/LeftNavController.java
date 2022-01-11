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

package bisq.desktop.primary.main.nav;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class LeftNavController implements Controller, Navigation.Listener {
    private final LeftNavModel model;
    @Getter
    private final LeftNavView view;

    public LeftNavController(DefaultServiceProvider serviceProvider) {
        model = new LeftNavModel(serviceProvider);
        view = new LeftNavView(model, this);

        // By using ROOT we listen to all NavigationTargets
        Navigation.addListener(NavigationTarget.ROOT, this);
    }

    void select(NavigationTarget navigationTarget) {
        model.select(navigationTarget);
        Navigation.navigateTo(navigationTarget);
    }

    @Override
    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        model.select(navigationTarget);
    }
}
