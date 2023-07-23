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

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.update.DownloadInfo;
import bisq.update.UpdateService;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdaterController implements Controller {
    private final UpdaterModel model;
    @Getter
    private final UpdaterView view;
    private final ServiceProvider serviceProvider;
    private final SettingsService settingsService;
    private final UpdateService updateService;
    private Pin getDownloadInfoListPin, getDownloadCompletedPin, getVersionPin, getDownloadUrlPin, releaseNotificationPin;

    public UpdaterController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();
        updateService = serviceProvider.getUpdateService();
        model = new UpdaterModel();
        view = new UpdaterView(model, this);
    }

    @Override
    public void onActivate() {
        getDownloadInfoListPin = FxBindings.<DownloadInfo, UpdaterView.ListItem>bind(model.getListItems())
                .map(UpdaterView.ListItem::new)
                .to(updateService.getDownloadInfoList());

        getDownloadCompletedPin = FxBindings.bind(model.getRestartButtonVisible()).to(updateService.getDownloadCompleted());
        releaseNotificationPin = updateService.getReleaseNotification().addObserver(releaseNotification -> {
            if (releaseNotification == null) {
                return;
            }
            model.getVersion().set(releaseNotification.getVersionString());
            model.getReleaseNotes().set(releaseNotification.getReleaseNotes());
        });
        getDownloadUrlPin = FxBindings.bind(model.getDownloadUrl()).to(updateService.getDownloadUrl());
    }

    @Override
    public void onDeactivate() {
        getDownloadInfoListPin.unbind();
        getDownloadCompletedPin.unbind();
        releaseNotificationPin.unbind();
        getDownloadUrlPin.unbind();
    }

    void onDownload() {
        model.getTableVisible().set(true);
        updateService.download();
    }

    void onDownloadLater() {
        OverlayController.hide();
    }

    void onIgnore() {
        settingsService.setCookie(CookieKey.IGNORE_VERSION, model.getVersion().get(), true);

        OverlayController.hide();
    }

    void onCancel() {
        OverlayController.hide();
    }

    void onRestart() {
        serviceProvider.getShotDownHandler().shutdown().thenAccept(result -> Platform.exit());
    }

    void onOpenUrl() {
        Browser.open(model.getDownloadUrl().get());
    }
}
