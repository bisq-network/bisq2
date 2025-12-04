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
import bisq.desktop.common.view.Controller;
import bisq.desktop.overlay.OverlayController;
import bisq.persistence.backup.RestoreService;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;

@Slf4j
public class RestoreFromBackupController implements Controller {
    private final RestoreFromBackupModel model;
    @Getter
    private final RestoreFromBackupView view;

    public RestoreFromBackupController(ServiceProvider serviceProvider) {
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
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onClose() {
        OverlayController.hide();
    }
}
