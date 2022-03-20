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

package bisq.desktop.primary.main;

import bisq.application.DefaultApplicationService;
import bisq.desktop.Navigation;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.nav.LeftNavController;
import bisq.desktop.primary.main.top.TopPanelController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MainController implements Controller, Navigation.Listener {
    private final MainModel model = new MainModel();
    @Getter
    private final MainView view;
    private final SettingsService settingsService;

    public MainController(DefaultApplicationService applicationService) {
        settingsService = applicationService.getSettingsService();
        ContentController contentController = new ContentController(applicationService);
        LeftNavController leftNavController = new LeftNavController(applicationService);
        TopPanelController topPanelController = new TopPanelController(applicationService);

        view = new MainView(model,
                this,
                contentController.getView(),
                leftNavController.getView(),
                topPanelController.getView());

        // By using ROOT we listen to all NavigationTargets
        Navigation.addListener(NavigationTarget.ROOT, this);
    }

    public void onViewAttached() {
        String persisted = settingsService.getPersistableStore().getCookie().get(CookieKey.NAVIGATION_TARGET);
        if (persisted != null) {
            Navigation.navigateTo(NavigationTarget.valueOf(persisted));
        } else {
            Navigation.navigateTo(NavigationTarget.CHAT);
        }
    }

    public void onViewDetached() {
    }

    public void onDomainInitialized() {
    }

    @Override
    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        if (navigationTarget.isAllowPersistence()) {
            settingsService.getPersistableStore().getCookie().put(CookieKey.NAVIGATION_TARGET, navigationTarget.name());
            settingsService.persist();
        }
    }
}
