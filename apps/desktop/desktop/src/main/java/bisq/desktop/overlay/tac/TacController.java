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

package bisq.desktop.overlay.tac;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.overlay.OverlayController;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TacController implements InitWithDataController<TacController.InitData> {
    @Getter
    public static final class InitData {
        private final Runnable completeHandler;

        public InitData(Runnable completeHandler) {
            this.completeHandler = completeHandler;
        }
    }

    private final TacModel model;
    @Getter
    private final TacView view;
    private final ServiceProvider serviceProvider;
    private final SettingsService settingsService;
    private final OverlayController overlayController;
    private Runnable completeHandler;

    public TacController(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();
        overlayController = OverlayController.getInstance();
        model = new TacModel();
        view = new TacView(model, this);
    }

    @Override
    public void initWithData(TacController.InitData data) {
        completeHandler = data.getCompleteHandler();
    }

    @Override
    public void onActivate() {
        model.getTacConfirmed().set(settingsService.getIsTacAccepted().get());

        overlayController.setEnterKeyHandler(null);
        overlayController.setUseEscapeKeyHandler(false);
    }

    @Override
    public void onDeactivate() {
    }

    void onQuit() {
        serviceProvider.getShutDownHandler().shutdown();
    }

    void onConfirm(boolean selected) {
        model.getTacConfirmed().set(selected);
        settingsService.setIsTacAccepted(selected);
        if (selected) {
            overlayController.setEnterKeyHandler(this::onAccept);
        } else {
            overlayController.setEnterKeyHandler(null);
        }
    }

    void onAccept() {
        OverlayController.hide(() -> {
            if (completeHandler != null) {
                completeHandler.run();
            }
        });
    }

    void onReject() {
        settingsService.setIsTacAccepted(false);
        onQuit();
    }
}
