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

package bisq.desktop.primary.main.left;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

@Slf4j
public class LeftNavController implements Controller {
    private final LeftNavModel model;
    @Getter
    private final LeftNavView view;

    public LeftNavController(DefaultApplicationService applicationService) {
        model = new LeftNavModel(applicationService);
        view = new LeftNavView(model, this);
    }

    public void setNavigationTarget(NavigationTarget navigationTarget) {
        Optional<NavigationTarget> supportedNavigationTarget;
        Set<NavigationTarget> navigationTargets = model.getNavigationTargets();
        if (navigationTargets.contains(navigationTarget)) {
            supportedNavigationTarget = Optional.of(navigationTarget);
        } else {
            supportedNavigationTarget = navigationTarget.getPath().stream()
                    .filter(navigationTargets::contains)
                    .findAny();
        }
        supportedNavigationTarget.ifPresent(target->{
            findTabButton(target).ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
            model.getSelectedNavigationTarget().set(target);  
        });
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNavigationTargetSelected(NavigationTarget navigationTarget) {
        findTabButton(navigationTarget).ifPresent(leftNavButton -> model.getSelectedNavigationButton().set(leftNavButton));
        model.getSelectedNavigationTarget().set(navigationTarget);
        Navigation.navigateTo(navigationTarget);
    }

    void onToggleExpandMenu() {
        model.getMenuExpanded().set(!model.getMenuExpanded().get());
    }

    void onNavigationButtonCreated(LeftNavButton leftNavButton) {
        model.getLeftNavButtons().add(leftNavButton);
        model.getNavigationTargets().add(leftNavButton.getNavigationTarget());
    }

    Optional<LeftNavButton> findTabButton(NavigationTarget navigationTarget) {
        return model.getLeftNavButtons().stream()
                .filter(leftNavButton -> navigationTarget == leftNavButton.getNavigationTarget())
                .findAny();
    }
}
