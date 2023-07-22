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
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.update.service.DownloadInfo;
import bisq.desktop.overlay.update.service.UpdateService;
import bisq.settings.SettingsService;
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
    private Pin listItemPin;

    public UpdaterController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();
        updateService = new UpdateService();
        model = new UpdaterModel();
        view = new UpdaterView(model, this);
    }

    @Override
    public void onActivate() {
        model.setVersion("2.0.1");
        model.setDownloadUrl("https://github.com/bisq-network/bisq/releases/2.0.1");
        model.setReleaseNodes("Release nodes:\n\ntest line1 \ntest line2 \ntest line3 \ntest line4 \n");

        listItemPin = FxBindings.<DownloadInfo, UpdaterView.ListItem>bind(model.getListItems())
                .map(UpdaterView.ListItem::new)
                .to(updateService.getDownloadInfoList());
    }

    @Override
    public void onDeactivate() {
        listItemPin.unbind();
    }

    void onDownload() {
        OverlayController.hide(() -> {
        });
    }

    void onDownloadLater() {
        OverlayController.hide(() -> {
        });
    }

    void onIgnore() {
        /// settingsService.setTacAccepted(false);
    }
}
