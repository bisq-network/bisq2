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

package bisq.desktop.main;

import bisq.application.ApplicationService;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.common.application.ApplicationVersion;
import bisq.common.observable.Observable;
import bisq.common.platform.Version;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.alert.AlertBannerController;
import bisq.desktop.main.content.ContentController;
import bisq.desktop.main.left.LeftNavController;
import bisq.desktop.main.notification.NotificationPanelController;
import bisq.desktop.main.top.TopPanelController;
import bisq.evolution.updater.UpdaterService;
import bisq.evolution.updater.UpdaterUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URLClassLoader;
import java.util.Optional;

@Slf4j
public class MainController extends NavigationController {
    @Getter
    private final MainModel model = new MainModel();
    @Getter
    private final MainView view;
    private final ServiceProvider serviceProvider;
    private final LeftNavController leftNavController;
    private final UpdaterService updaterService;
    private final ApplicationService.Config config;

    public MainController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MAIN);

        this.serviceProvider = serviceProvider;
        updaterService = serviceProvider.getUpdaterService();
        config = serviceProvider.getConfig();

        leftNavController = new LeftNavController(serviceProvider);
        TopPanelController topPanelController = new TopPanelController(serviceProvider);
        NotificationPanelController notificationPanelController = new NotificationPanelController(serviceProvider);
        AlertBannerController alertBannerController = new AlertBannerController(serviceProvider);
        view = new MainView(model,
                this,
                leftNavController.getView().getRoot(),
                topPanelController.getView().getRoot(),
                notificationPanelController.getView().getRoot(),
                alertBannerController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        if (getClass().getClassLoader() instanceof URLClassLoader) {
            // We only verify version if we have been loaded as jar into the launcher. 
            // In that case our class loader is of typ URLClassLoader.
            String baseDirPath = config.getBaseDir().toAbsolutePath().toString();
            Optional<String> versionFromVersionFile = UpdaterUtils.readVersionFromVersionFile(baseDirPath);
            if (versionFromVersionFile.isPresent()) {
                Version version = ApplicationVersion.getVersion();
                if (!version.toString().equals(versionFromVersionFile.get())) {
                    String errorMsg = "Version of application (v" + version +
                            ") does not match version from version file in data directory (v" + versionFromVersionFile.get() + ")";
                    new Popup().warning(errorMsg)
                            .useShutDownButton()
                            .hideCloseButton()
                            .show();
                    return;
                }
            }
        }

        updaterService.getIsNewReleaseAvailable().addObserver(isNewReleaseAvailable -> UIThread.run(this::maybeShowUpdatePopup));
        updaterService.getIgnoreNewRelease().addObserver(isNewReleaseAvailable -> UIThread.run(this::maybeShowUpdatePopup));
    }

    @Override
    public void onDeactivate() {
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case CONTENT -> Optional.of(new ContentController(serviceProvider));
            default -> Optional.empty();
        };
    }

    @Override
    public void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        leftNavController.setNavigationTarget(navigationTarget);
    }

    private void maybeShowUpdatePopup() {
        Boolean isNewReleaseAvailable = updaterService.getIsNewReleaseAvailable().get();
        Observable<Boolean> ignoreNewRelease = updaterService.getIgnoreNewRelease();
        ReleaseNotification releaseNotification = updaterService.getReleaseNotification().get();
        if (isNewReleaseAvailable == null ||
                !isNewReleaseAvailable ||
                releaseNotification == null ||
                ignoreNewRelease == null ||
                ignoreNewRelease.get()) {
            return;
        }
        Navigation.navigateTo(NavigationTarget.UPDATER);
    }
}
