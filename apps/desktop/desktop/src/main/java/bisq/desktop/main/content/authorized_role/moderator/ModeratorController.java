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

package bisq.desktop.main.content.authorized_role.moderator;

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.authorized_role.info.RoleInfo;
import bisq.support.SupportService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModeratorController implements Controller {
    private final SupportService supportService;
    @Getter
    private final ModeratorView view;
    private Pin changedModeratorNotificationPin;

    public ModeratorController(ServiceProvider serviceProvider) {
        supportService = serviceProvider.getSupportService();
        ReportToModeratorTable reportToModeratorTable = new ReportToModeratorTable(serviceProvider);
        BannedUserProfileTable bannedUserProfileTable = new BannedUserProfileTable(serviceProvider);
        RoleInfo roleInfo = new RoleInfo(serviceProvider);
        ModeratorModel model = new ModeratorModel();
        view = new ModeratorView(model, this, reportToModeratorTable.getRoot(), bannedUserProfileTable.getRoot(), roleInfo.getRoot());
    }

    @Override
    public void onActivate() {
        supportService.getSupportNotificationsService().consumeModeratorNotifications();

        changedModeratorNotificationPin = supportService.getSupportNotificationsService().getChangedNotification().addObserver(notification -> {
            if (notification != null && notification.isModeratorNotification() && notification.isNotConsumed()) {
                supportService.getSupportNotificationsService().consumeModeratorNotification(notification);
            }
        });
    }

    @Override
    public void onDeactivate() {
        changedModeratorNotificationPin.unbind();
    }
}
