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

package bisq.desktop.overlay.update;

import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.application.ApplicationVersion;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.platform.PlatformUtils;
import bisq.common.platform.Version;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.overlay.OverlayController;
import bisq.evolution.updater.DownloadItem;
import bisq.evolution.updater.UpdaterService;
import bisq.evolution.updater.UpdaterUtils;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import static bisq.evolution.updater.UpdaterUtils.RELEASES_URL;

@Slf4j
public class UpdaterController implements Controller {
    private final UpdaterModel model;
    @Getter
    private final UpdaterView view;
    private final ServiceProvider serviceProvider;
    private final SettingsService settingsService;
    private final UpdaterService updaterService;
    private final AlertService alertService;
    private Pin getDownloadInfoListPin, isNewReleaseAvailablePin, authorizedAlertDataSetPin;

    public UpdaterController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();
        updaterService = serviceProvider.getUpdaterService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        model = new UpdaterModel();
        view = new UpdaterView(model, this);
    }

    @Override
    public void onActivate() {
        model.setRequireVersionForTrading(false);
        model.setMinRequiredVersionForTrading(Optional.empty());

        getDownloadInfoListPin = FxBindings.<DownloadItem, UpdaterView.ListItem>bind(model.getListItems())
                .map(UpdaterView.ListItem::new)
                .to(updaterService.getDownloadItemList());

        isNewReleaseAvailablePin = updaterService.getIsNewReleaseAvailable().addObserver(isNewReleaseAvailable -> {
            UIThread.run(() -> {
                ReleaseNotification releaseNotification = updaterService.getReleaseNotification().get();
                if (isNewReleaseAvailable == null || !isNewReleaseAvailable || releaseNotification == null) {
                    return;
                }

                String version = releaseNotification.getVersionString();
                model.getVersion().set(version);
                model.getReleaseNotes().set(releaseNotification.getReleaseNotes());
                model.getDownloadUrl().set(RELEASES_URL + version);

                boolean isLauncherUpdate = releaseNotification.isLauncherUpdate();
                model.getIsLauncherUpdate().set(isLauncherUpdate);

                model.getHeadline().set(isLauncherUpdate ?
                        Res.get("updater.headline.isLauncherUpdate") :
                        Res.get("updater.headline"));
                model.getFurtherInfo().set(isLauncherUpdate ?
                        Res.get("updater.furtherInfo.isLauncherUpdate") :
                        Res.get("updater.furtherInfo"));
                model.getVerificationInfo().set(isLauncherUpdate ?
                        Res.get("updater.downloadAndVerify.info.isLauncherUpdate") :
                        Res.get("updater.downloadAndVerify.info"));
                model.getShutDownButtonText().set(isLauncherUpdate ?
                        Res.get("updater.shutDown.isLauncherUpdate") :
                        Res.get("updater.shutDown"));

                updateIgnoreVersionState();
            });
        });

        authorizedAlertDataSetPin = alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY && authorizedAlertData.isRequireVersionForTrading()) {
                    model.setRequireVersionForTrading(true);
                    model.setMinRequiredVersionForTrading(authorizedAlertData.getMinVersion());
                    updateIgnoreVersionState();
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof AuthorizedAlertData authorizedAlertData) {
                    if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY && authorizedAlertData.isRequireVersionForTrading()) {
                        model.setRequireVersionForTrading(false);
                        model.setMinRequiredVersionForTrading(Optional.empty());
                        updateIgnoreVersionState();
                    }
                }
            }

            @Override
            public void clear() {
                model.setRequireVersionForTrading(false);
                model.setMinRequiredVersionForTrading(Optional.empty());
                updateIgnoreVersionState();
            }
        });

        updateIgnoreVersionState();
        model.getFilteredList().setPredicate(e -> !e.getDownloadItem().getDestinationFile().getName().startsWith(UpdaterUtils.FROM_BISQ_WEBPAGE_PREFIX));
    }

    @Override
    public void onDeactivate() {
        getDownloadInfoListPin.unbind();
        isNewReleaseAvailablePin.unbind();
        authorizedAlertDataSetPin.unbind();
    }

    void onDownload() {
        model.getDownloadStarted().set(true);
        updateIgnoreVersionState();
        model.getHeadline().set(Res.get("updater.downloadAndVerify.headline"));
        try {
            updaterService.downloadAndVerify()
                    .whenComplete((nil, throwable) -> {
                        if (throwable == null) {
                            UIThread.run(() -> model.getDownloadAndVerifyCompleted().set(true));
                        } else if (!(throwable instanceof CancellationException)) {
                            UIThread.run(() -> new Popup().error(throwable).show());
                        }
                    });
        } catch (IOException e) {
            UIThread.run(() -> new Popup().error(e).show());
        }
    }

    void onDownloadLater() {
        OverlayController.hide();
    }

    void onIgnoreVersionSelected(boolean selected) {
        settingsService.setCookie(CookieKey.IGNORE_VERSION, model.getVersion().get(), selected);
        updateIgnoreVersionState();
        if (selected) {
            OverlayController.hide();
        }
    }

    void onShutdown() {
        ReleaseNotification releaseNotification = updaterService.getReleaseNotification().get();
        if (releaseNotification != null && releaseNotification.isLauncherUpdate()) {
            PlatformUtils.open(PlatformUtils.getDownloadOfHomeDir());
        }
        serviceProvider.getShutDownHandler().shutdown();
    }

    void onClose() {
        OverlayController.hide();
    }

    void onOpenUrl() {
        Browser.open(model.getDownloadUrl().get());
    }

    private boolean isRequireVersionForTradingAboveAppVersion() {
        Optional<String> minRequiredVersionForTrading = model.getMinRequiredVersionForTrading();
        return model.isRequireVersionForTrading() &&
                minRequiredVersionForTrading.isPresent() &&
                new Version(minRequiredVersionForTrading.get()).above(ApplicationVersion.getVersion());
    }

    private void updateIgnoreVersionState() {
        boolean requireVersionForTradingAboveAppVersion = isRequireVersionForTradingAboveAppVersion();
        model.getIgnoreVersion().set(getIgnoreVersionFromCookie() &&
                !model.getDownloadStarted().get() && !requireVersionForTradingAboveAppVersion);

        model.getIgnoreVersionSwitchVisible().set(!requireVersionForTradingAboveAppVersion);
    }

    private Boolean getIgnoreVersionFromCookie() {
        return settingsService.getCookie().asBoolean(CookieKey.IGNORE_VERSION, model.getVersion().get()).orElse(false);
    }
}
