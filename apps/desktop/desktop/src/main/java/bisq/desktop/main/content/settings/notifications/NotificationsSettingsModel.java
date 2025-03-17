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

package bisq.desktop.main.content.settings.notifications;

import bisq.desktop.common.view.Model;
import bisq.settings.ChatNotificationType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class NotificationsSettingsModel implements Model {
    private final ObjectProperty<ChatNotificationType> chatNotificationType = new SimpleObjectProperty<>(ChatNotificationType.ALL);
    private final BooleanProperty notifyForPreRelease = new SimpleBooleanProperty();
    private final BooleanProperty useTransientNotifications = new SimpleBooleanProperty();
    @Setter
    private boolean isUseTransientNotificationsVisible;

    public NotificationsSettingsModel() {
    }
}
