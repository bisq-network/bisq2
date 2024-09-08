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

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import bisq.settings.ChatNotificationType;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NotificationsSettingsView extends View<VBox, NotificationsSettingsModel, NotificationsSettingsController> {
    private final Button clearNotifications;
    private final Switch notifyForPreRelease, useTransientNotifications;
    private final ToggleGroup notificationsToggleGroup = new ToggleGroup();
    private final RadioButton all, mention, off;
    private final ChangeListener<Toggle> notificationsToggleListener;
    private Subscription selectedNotificationTypePin;

    public NotificationsSettingsView(NotificationsSettingsModel model, NotificationsSettingsController controller) {
        super(new VBox(50), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        // Notifications
        Label notificationsHeadline = SettingsViewUtils.getHeadline(Res.get("settings.notification.options"));

        all = new RadioButton(Res.get("settings.notification.option.all"));
        all.setToggleGroup(notificationsToggleGroup);
        all.setUserData(ChatNotificationType.ALL);
        mention = new RadioButton(Res.get("settings.notification.option.mention"));
        mention.setToggleGroup(notificationsToggleGroup);
        mention.setUserData(ChatNotificationType.MENTION);
        off = new RadioButton(Res.get("settings.notification.option.off"));
        off.setToggleGroup(notificationsToggleGroup);
        off.setUserData(ChatNotificationType.OFF);

        notifyForPreRelease = new Switch(Res.get("settings.notification.notifyForPreRelease"));

        useTransientNotifications = new Switch(Res.get("settings.notification.useTransientNotifications"));

        clearNotifications = new Button(Res.get("settings.notification.clearNotifications"));
        clearNotifications.getStyleClass().add("grey-transparent-outlined-button");

        VBox.setMargin(notifyForPreRelease, new Insets(10, 0, 0, 0));
        VBox.setMargin(clearNotifications, new Insets(10, 0, 0, 0));
        VBox notificationsVBox = new VBox(10, all, mention, off,
                notifyForPreRelease, useTransientNotifications, clearNotifications);

        Insets insets = new Insets(0, 5, 0, 5);
        VBox.setMargin(notificationsVBox, insets);
        root.getChildren().addAll(notificationsHeadline, SettingsViewUtils.getLineAfterHeadline(root.getSpacing()), notificationsVBox);

        notificationsToggleListener = (observable, oldValue, newValue) -> controller.onSetChatNotificationType((ChatNotificationType) newValue.getUserData());
    }

    @Override
    protected void onViewAttached() {
        useTransientNotifications.setVisible(model.isUseTransientNotificationsVisible());
        useTransientNotifications.setManaged(model.isUseTransientNotificationsVisible());

        notificationsToggleGroup.selectedToggleProperty().addListener(notificationsToggleListener);
        selectedNotificationTypePin = EasyBind.subscribe(model.getChatNotificationType(), selected -> applyChatNotificationType());
        notifyForPreRelease.selectedProperty().bindBidirectional(model.getNotifyForPreRelease());
        useTransientNotifications.selectedProperty().bindBidirectional(model.getUseTransientNotifications());
        clearNotifications.setOnAction(e -> controller.onClearNotifications());
    }

    @Override
    protected void onViewDetached() {
        notificationsToggleGroup.selectedToggleProperty().removeListener(notificationsToggleListener);
        selectedNotificationTypePin.unsubscribe();
        notifyForPreRelease.selectedProperty().unbindBidirectional(model.getNotifyForPreRelease());
        useTransientNotifications.selectedProperty().unbindBidirectional(model.getUseTransientNotifications());
        clearNotifications.setOnAction(null);
    }

    private void applyChatNotificationType() {
        switch (model.getChatNotificationType().get()) {
            case ALL: {
                notificationsToggleGroup.selectToggle(all);
                break;
            }
            case MENTION: {
                notificationsToggleGroup.selectToggle(mention);
                break;
            }
            case OFF: {
                notificationsToggleGroup.selectToggle(off);
                break;
            }
        }
    }
}
