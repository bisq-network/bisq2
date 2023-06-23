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

package bisq.desktop.primary.overlay.tac;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.settings.SettingsService;
import javafx.application.Platform;
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
    private final DefaultApplicationService applicationService;
    private final SettingsService settingsService;
    private Runnable completeHandler;

    public TacController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        settingsService = applicationService.getSettingsService();
        model = new TacModel();
        view = new TacView(model, this);
    }

    @Override
    public void initWithData(TacController.InitData data) {
        completeHandler = data.getCompleteHandler();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    public void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    public void onConfirm(boolean selected) {
        model.getTacConfirmed().set(selected);
        settingsService.setTacAccepted(selected);
    }

    void onAccept() {
        OverlayController.hide(() -> {
            if (completeHandler != null) {
                completeHandler.run();
            }
        });
    }

    void onReject() {
        settingsService.setTacAccepted(false);
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }
}
