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

package bisq.desktop.overlay.restore;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.overlay.OverlayController;
import bisq.persistence.backup.RestoreService;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.time.ZoneId;

@Slf4j
public class RestoreFromBackupController implements InitWithDataController<RestoreFromBackupController.InitData> {
    @Getter
    public static final class InitData {
        private final Runnable closeHandler;

        public InitData(Runnable closeHandler) {
            this.closeHandler = closeHandler;
        }
    }

    private final RestoreFromBackupModel model;
    @Getter
    private final RestoreFromBackupView view;
    private final OverlayController overlayController;
    @Nullable
    private Runnable closeHandler;
    private boolean useEscapeKeyHandler;

    public RestoreFromBackupController(ServiceProvider serviceProvider) {
        overlayController = OverlayController.getInstance();
        RestoreService restoreService = serviceProvider.getPersistenceService().getRestoreService();
        model = new RestoreFromBackupModel();
        view = new RestoreFromBackupView(model, this);

        model.getListItems().addAll(restoreService.getRestoredBackupFileInfos()
                .stream()
                .map(bfi -> {
                    long timestamp = bfi.getLocalDateTime()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                            return new RestoreFromBackupView.ListItem(
                                    bfi.getPath().getParent().getFileName().toString(),
                                    DateFormatter.formatDateTime(timestamp),
                                    TimeFormatter.formatAge(Math.max(0, System.currentTimeMillis() - timestamp)),
                                    bfi.getPath().toString());
                        }
                ).toList());
    }

    @Override
    public void initWithData(InitData data) {
        closeHandler = data.getCloseHandler();
    }

    @Override
    public void onActivate() {
        // The escape key hides the overlay without the close handler
        useEscapeKeyHandler = overlayController.isUseEscapeKeyHandler();
        overlayController.setUseEscapeKeyHandler(false);
    }

    @Override
    public void onDeactivate() {
        overlayController.setUseEscapeKeyHandler(useEscapeKeyHandler);
    }

    void onClose() {
        OverlayController.hide(closeHandler);
    }
}
