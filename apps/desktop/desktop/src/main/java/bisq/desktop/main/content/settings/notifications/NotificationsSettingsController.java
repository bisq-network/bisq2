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

import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.common.platform.OS;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.settings.ChatNotificationType;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.evolution.updater.UpdaterService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NotificationsSettingsController implements Controller {
    @Getter
    private final NotificationsSettingsView view;
    private final NotificationsSettingsModel model;
    private final SettingsService settingsService;
    private final ChatNotificationService chatNotificationService;
    private final UpdaterService updaterService;

    private Pin chatNotificationTypePin;
    private Subscription notifyForPreReleasePin, useTransientNotificationsPin;

    public NotificationsSettingsController(ServiceProvider serviceProvider) {
        settingsService = serviceProvider.getSettingsService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        updaterService = serviceProvider.getUpdaterService();
        model = new NotificationsSettingsModel();
        view = new NotificationsSettingsView(model, this);
    }

    @Override
    public void onActivate() {
        chatNotificationTypePin = FxBindings.bindBiDir(model.getChatNotificationType())
                .to(settingsService.getChatNotificationType());
        model.getNotifyForPreRelease().set(settingsService.getCookie().asBoolean(CookieKey.NOTIFY_FOR_PRE_RELEASE).orElse(false));
        notifyForPreReleasePin = EasyBind.subscribe(model.getNotifyForPreRelease(),
                value -> {
                    settingsService.setCookie(CookieKey.NOTIFY_FOR_PRE_RELEASE, value);
                    updaterService.reapplyAllReleaseNotifications();
                });

        // Currently we support transient notifications only for Linux
        if (OS.isLinux()) {
            model.setUseTransientNotificationsVisible(true);
            model.getUseTransientNotifications().set(settingsService.getCookie().asBoolean(CookieKey.USE_TRANSIENT_NOTIFICATIONS).orElse(true));
            useTransientNotificationsPin = EasyBind.subscribe(model.getUseTransientNotifications(),
                    value -> settingsService.setCookie(CookieKey.USE_TRANSIENT_NOTIFICATIONS, value));
        }
    }

    @Override
    public void onDeactivate() {
        chatNotificationTypePin.unbind();
        notifyForPreReleasePin.unsubscribe();

        if (useTransientNotificationsPin != null) {
            useTransientNotificationsPin.unsubscribe();
        }
    }

    void onClearNotifications() {
        chatNotificationService.consumeAllNotifications();
    }

    void onSetChatNotificationType(ChatNotificationType type) {
        model.getChatNotificationType().set(type);
    }
}
