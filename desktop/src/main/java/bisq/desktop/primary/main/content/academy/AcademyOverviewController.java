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

package bisq.desktop.primary.main.content.academy;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import lombok.Getter;

public class AcademyOverviewController implements Controller {
    @Getter
    private final AcademyOverviewView view;

    public AcademyOverviewController(DefaultApplicationService applicationService) {
        AcademyOverviewModel model = new AcademyOverviewModel();
        view = new AcademyOverviewView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(NavigationTarget navigationTarget) {
        Navigation.navigateTo(navigationTarget);
    }
}
