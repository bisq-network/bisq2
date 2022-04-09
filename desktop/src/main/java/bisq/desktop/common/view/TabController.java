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

package bisq.desktop.common.view;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class TabController<T extends TabModel> extends NavigationController {
    @Getter
    protected final T model;

    public TabController(T model, NavigationTarget host) {
        super(host);

        this.model = model;
    }

    void onTabSelected(NavigationTarget navigationTarget) {
        findTabButton(navigationTarget).ifPresent(tabButton -> model.getSelectedTabButton().set(tabButton));
        Navigation.navigateTo(navigationTarget);
    }

    void onTabButtonCreated(TabButton tabButton) {
        model.getTabButtons().add(tabButton);
    }

    void onTabButtonRemoved(TabButton tabButton) {
        model.getTabButtons().remove(tabButton);
        if (model.getSelectedTabButton().get().getNavigationTarget() == tabButton.getNavigationTarget()) {
            model.getSelectedTabButton().set(null);
        }
    }

    Optional<TabButton> findTabButton(NavigationTarget navigationTarget) {
        return model.getTabButtons().stream()
                .filter(tabButton -> navigationTarget == tabButton.getNavigationTarget())
                .findAny();
    }
}
