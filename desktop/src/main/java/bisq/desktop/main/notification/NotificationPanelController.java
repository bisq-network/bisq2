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

package bisq.desktop.main.notification;

import bisq.bisq_easy.BisqEasyNotificationsService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.i18n.Res;
import bisq.presentation.notifications.NotificationsService;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class NotificationPanelController implements Controller {
    @Getter
    private final NotificationPanelView view;
    private final NotificationPanelModel model;
    private final NotificationsService notificationsService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private Pin isNotificationVisiblePin;

    public NotificationPanelController(ServiceProvider serviceProvider) {
        notificationsService = serviceProvider.getNotificationsService();
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();

        model = new NotificationPanelModel();
        view = new NotificationPanelView(model, this);
    }

    @Override
    public void onActivate() {
        notificationsService.subscribe(this::updateNumNotifications);
        isNotificationVisiblePin = FxBindings.bind(model.getIsNotificationVisible())
                .to(bisqEasyNotificationsService.getIsNotificationPanelVisible());
    }

    @Override
    public void onDeactivate() {
        notificationsService.unsubscribe(this::updateNumNotifications);
        isNotificationVisiblePin.unbind();
    }

    void onClose() {
        notificationsService.getIsNotificationPanelDismissed().set(true);
    }

    void onNavigateToTarget() {
        Navigation.navigateTo(model.isMediationNotification() ?
                NavigationTarget.MEDIATOR :
                NavigationTarget.BISQ_EASY_OPEN_TRADES);
    }

    private void updateNumNotifications(String notificationId) {
        UIThread.run(() -> {
            boolean notificationForMediator = bisqEasyNotificationsService.isNotificationForMediator(notificationId);
            model.setMediationNotification(notificationForMediator);
            Set<String> tradeIdsOfNotifications = bisqEasyNotificationsService.getTradeIdsOfNotifications();
            if (tradeIdsOfNotifications.size() == 1) {
                String tradeId = tradeIdsOfNotifications.iterator().next();
                if (notificationForMediator) {
                    model.getHeadline().set(Res.get("notificationPanel.mediationCases.headline.single", tradeId));
                    model.getButtonText().set(Res.get("notificationPanel.mediationCases.button"));
                } else {
                    model.getHeadline().set(Res.get("notificationPanel.trades.headline.single", tradeId));
                    model.getButtonText().set(Res.get("notificationPanel.trades.button"));
                }
            } else if (tradeIdsOfNotifications.size() > 1) {
                String tradeIds = Joiner.on(", ").join(tradeIdsOfNotifications);
                if (notificationForMediator) {
                    model.getHeadline().set(Res.get("notificationPanel.mediationCases.headline.multiple", tradeIds));
                    model.getButtonText().set(Res.get("notificationPanel.mediationCases.button"));
                } else {
                    model.getHeadline().set(Res.get("notificationPanel.trades.headline.multiple", tradeIds));
                    model.getButtonText().set(Res.get("notificationPanel.trades.button"));
                }
            }
        });
    }
}
