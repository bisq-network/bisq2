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
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.common.application.ApplicationVersion;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.platform.OS;
import bisq.common.platform.Version;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.alert.AlertBannerController;
import bisq.desktop.main.banner.BannerNotificationController;
import bisq.desktop.main.content.ContentController;
import bisq.desktop.main.left.LeftNavController;
import bisq.desktop.main.notification.NotificationPanelController;
import bisq.desktop.main.top.TopPanelController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.restore.RestoreFromBackupController;
import bisq.desktop.overlay.unreadable_store_files.UnreadableStoreFilesController;
import bisq.evolution.updater.UpdaterService;
import bisq.evolution.updater.UpdaterUtils;
import bisq.i18n.Res;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.transport.TorTransportService;
import bisq.network.p2p.node.transport.TransportService;
import bisq.persistence.UnreadableStoreArchive;
import bisq.persistence.backup.RestoreService;
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
    private final RestoreService restoreService;
    private final UnreadableStoreArchive unreadableStoreArchive;
    private final ApplicationService.Config config;
    private Pin torTransportStatePin;
    private boolean isTorSandboxNoticeHandled;

    public MainController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MAIN);

        this.serviceProvider = serviceProvider;
        updaterService = serviceProvider.getUpdaterService();
        restoreService = serviceProvider.getPersistenceService().getRestoreService();
        unreadableStoreArchive = serviceProvider.getPersistenceService().getUnreadableStoreArchive();
        config = serviceProvider.getConfig();

        leftNavController = new LeftNavController(serviceProvider);
        BannerNotificationController bannerNotificationController = new BannerNotificationController(serviceProvider);
        TopPanelController topPanelController = new TopPanelController(serviceProvider);
        NotificationPanelController notificationPanelController = new NotificationPanelController(serviceProvider);
        AlertBannerController alertBannerController = new AlertBannerController(serviceProvider);
        view = new MainView(model,
                this,
                leftNavController.getView().getRoot(),
                bannerNotificationController.getView().getRoot(),
                topPanelController.getView().getRoot(),
                notificationPanelController.getView().getRoot(),
                alertBannerController.getView().getRoot());
    }

    @Override
    public void onActivate() {
        if (getClass().getClassLoader() instanceof URLClassLoader) {
            // We only verify version if we have been loaded as jar into the launcher. 
            // In that case our class loader is of typ URLClassLoader.
            Optional<String> versionFromVersionFile = UpdaterUtils.readVersionFromVersionFile(config.getAppDataDirPath());
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

        UIThread.run(this::maybeShowRestorePopup);
    }

    @Override
    public void onDeactivate() {
        if (torTransportStatePin != null) {
            torTransportStatePin.unbind();
            torTransportStatePin = null;
        }
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

    private void maybeShowRestorePopup() {
        boolean showUnreadableStoreFiles = UnreadableStoreFilesController.isToBeShown(unreadableStoreArchive);
        if (!restoreService.getRestoredBackupFileInfos().isEmpty()) {
            // The next overlay and the updater wait for the restore dialog to close, so that they do not replace it
            Runnable closeHandler = showUnreadableStoreFiles
                    ? this::showUnreadableStoreFiles
                    : this::onStartupOverlaysClosed;
            RestoreFromBackupController.InitData initData = new RestoreFromBackupController.InitData(closeHandler);
            UIScheduler.run(() -> Navigation.navigateTo(NavigationTarget.RESTORE_FROM_BACKUP, initData)).after(500);
        } else if (showUnreadableStoreFiles) {
            UIScheduler.run(this::showUnreadableStoreFiles).after(500);
        } else {
            onStartupOverlaysClosed();
        }
    }

    private void showUnreadableStoreFiles() {
        // The updater observers start after this overlay closes, so that the updater does not replace it
        Navigation.navigateTo(NavigationTarget.UNREADABLE_STORE_FILES,
                new UnreadableStoreFilesController.InitData(this::onStartupOverlaysClosed));
    }

    private void onStartupOverlaysClosed() {
        addUpdaterServiceObservers();
        observeTorSandbox();
    }

    // The level of the sandbox is known once the Tor of the start answers, before the Tor transport has initialized.
    // The network service completes with its first initialized transport, so with several transports Tor can come later.
    private void observeTorSandbox() {
        serviceProvider.getNetworkService().findServiceNode(TransportType.TOR)
                .map(ServiceNode::getTransportService)
                .map(TorTransportService.class::cast)
                .ifPresent(torTransportService -> torTransportStatePin = torTransportService.getTransportState()
                        .addObserver(transportState -> {
                            if (transportState == TransportService.TransportState.INITIALIZED) {
                                UIThread.run(() -> maybeShowTorSandboxNotice(torTransportService));
                            }
                        }));
    }

    // Once per start: the user can turn the notice off with the check box of the popup
    private void maybeShowTorSandboxNotice(TorTransportService torTransportService) {
        if (isTorSandboxNoticeHandled) {
            return;
        }
        isTorSandboxNoticeHandled = true;
        TorSandboxNotice.find(torTransportService.getSandboxWeaknesses(), OS.getOS())
                .ifPresent(notice -> new Popup().headline(Res.get("network.torSandbox.notice.headline"))
                        .information(notice.message())
                        .dontShowAgainId(notice.dontShowAgainId())
                        .show());
    }

    private void addUpdaterServiceObservers() {
        updaterService.getIsNewReleaseAvailable().addObserver(isNewReleaseAvailable -> UIThread.run(this::maybeShowUpdatePopup));
        updaterService.getIgnoreNewRelease().addObserver(ignoreNewRelease -> UIThread.run(this::maybeShowUpdatePopup));
    }

    private void maybeShowUpdatePopup() {
        Boolean isNewReleaseAvailable = updaterService.getIsNewReleaseAvailable().get();
        Observable<Boolean> ignoreNewRelease = updaterService.getIgnoreNewRelease();
        ReleaseNotification releaseNotification = updaterService.getMostRelevantReleaseNotification().get();
        if (isNewReleaseAvailable == null ||
                !isNewReleaseAvailable ||
                releaseNotification == null ||
                ignoreNewRelease == null ||
                ignoreNewRelease.get()) {
            return;
        }

        // At frist start after user profile creation, when entering dashboard and an update is available
        // calling Navigation.navigateTo without delay lead to a darkened screen not displaying the popup.
        UIScheduler.run(() -> Navigation.navigateTo(NavigationTarget.UPDATER)).after(500);
    }
}
