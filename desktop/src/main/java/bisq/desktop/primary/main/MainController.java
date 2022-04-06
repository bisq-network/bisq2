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
import bisq.desktop.common.view.CachingController;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.primary.main.content.ContentController;
import bisq.desktop.primary.main.nav.LeftNavController;
import bisq.desktop.primary.main.top.TopPanelController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MainController extends NavigationController implements CachingController, Navigation.Listener {
    @Getter
    private final MainModel model = new MainModel();
    @Getter
    private final MainView view;
    private final SettingsService settingsService;
    private final DefaultApplicationService applicationService;

    public MainController(DefaultApplicationService applicationService) {
        super(NavigationTarget.MAIN);

        settingsService = applicationService.getSettingsService();
        this.applicationService = applicationService;

        LeftNavController leftNavController = new LeftNavController(applicationService);
        TopPanelController topPanelController = new TopPanelController(applicationService);

        view = new MainView(model,
                this,
                leftNavController.getView(),
                topPanelController.getView());
    }

    @Override
    public void onActivate() {
        // By using ROOT we listen to all NavigationTargets
        Navigation.addListener(NavigationTarget.ROOT, this);
        String persisted = settingsService.getPersistableStore().getCookie().getValue(CookieKey.NAVIGATION_TARGET);
        if (persisted != null) {
            Navigation.navigateTo(NavigationTarget.valueOf(persisted));
        } else {
            Navigation.navigateTo(NavigationTarget.ONBOARDING);
        }
    }

    @Override
    public void onDeactivate() {
        Navigation.removeListener(NavigationTarget.ROOT, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CONTENT -> {
                return Optional.of(new ContentController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onNavigate(NavigationTarget navigationTarget, Optional<Object> data) {
        if (navigationTarget.isAllowPersistence()) {
            settingsService.getPersistableStore().getCookie().put(CookieKey.NAVIGATION_TARGET, navigationTarget.name());
            settingsService.persist();
        }
    }
}
