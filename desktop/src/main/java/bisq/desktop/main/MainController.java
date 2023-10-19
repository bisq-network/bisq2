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
import bisq.bonded_roles.alert.AlertService;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.ContentController;
import bisq.desktop.main.left.LeftNavController;
import bisq.desktop.main.notification.NotificationPanelController;
import bisq.desktop.main.top.TopPanelController;
import bisq.settings.SettingsService;
import bisq.updater.UpdaterService;
import bisq.updater.UpdaterUtils;
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
    private final AlertService alertService;
    private final SettingsService settingsService;
    private final UpdaterService updaterService;
    private final ApplicationService.Config config;
    private final NotificationPanelController notificationPanelController;

    public MainController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MAIN);

        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        updaterService = serviceProvider.getUpdaterService();
        config = serviceProvider.getConfig();

        leftNavController = new LeftNavController(serviceProvider);
        TopPanelController topPanelController = new TopPanelController(serviceProvider);
        notificationPanelController = new NotificationPanelController(serviceProvider);
        view = new MainView(model,
                this,
                leftNavController.getView().getRoot(),
                topPanelController.getView().getRoot(),
                notificationPanelController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        if (getClass().getClassLoader() instanceof URLClassLoader) {
            // We only verify version if we have been loaded as jar into the launcher. 
            // In that case our class loader is of typ URLClassLoader.
            String baseDirPath = config.getBaseDir().toAbsolutePath().toString();
            Optional<String> versionFromVersionFile = UpdaterUtils.readVersionFromVersionFile(baseDirPath);
            if (versionFromVersionFile.isPresent()) {
                if (!config.getVersion().toString().equals(versionFromVersionFile.get())) {
                    String errorMsg = "Version of application (v" + config.getVersion() +
                            ") does not match version from version file in data directory (v" + versionFromVersionFile.get() + ")";
                    new Popup().warning(errorMsg)
                            .useShutDownButton()
                            .hideCloseButton()
                            .show();
                    return;
                }
            }
        }

        alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData == null) {
                    return;
                }
                UIThread.run(() -> {
                    if (settingsService.getConsumedAlertIds().contains(authorizedAlertData.getId())) {
                        return;
                    }
                    settingsService.addConsumedAlertId(authorizedAlertData.getId());
                    Optional<String> optionalMessage = authorizedAlertData.getMessage();
                    switch (authorizedAlertData.getAlertType()) {
                        case INFO:
                            optionalMessage.ifPresentOrElse(message -> new Popup().attention(message).show(),
                                    () -> log.warn("optionalMessage not present"));
                            break;
                        case WARN:
                        case EMERGENCY:
                            optionalMessage.ifPresentOrElse(message -> new Popup().warning(message).show(),
                                    () -> log.warn("optionalMessage not present"));
                            break;
                        case BAN:
                            break;
                    }
                });
            }

            @Override
            public void remove(Object element) {
            }

            @Override
            public void clear() {
            }
        });

        updaterService.getReleaseNotification().addObserver(releaseNotification -> {
            if (releaseNotification == null) {
                return;
            }
            UIThread.run(() -> Navigation.navigateTo(NavigationTarget.UPDATER));
        });
    }

    @Override
    public void onDeactivate() {
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case CONTENT: {
                return Optional.of(new ContentController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onStartProcessNavigationTarget(NavigationTarget navigationTarget, Optional<Object> data) {
        leftNavController.setNavigationTarget(navigationTarget);
        notificationPanelController.setNavigationTarget(navigationTarget);
    }
}
