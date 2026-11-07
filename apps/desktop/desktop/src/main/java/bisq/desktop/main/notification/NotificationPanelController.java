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
import bisq.desktop.navigation.NavigationTarget;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.i18n.Res;
import bisq.mu_sig.MusigNotificationService;
import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class NotificationPanelController implements Controller {
    @Getter
    private final NotificationPanelView view;
    private final NotificationPanelModel model;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private final MusigNotificationService musigNotificationService;
    private final ChatNotificationService chatNotificationService;
    private Pin changedChatNotificationPin, bisqEasyNotificationVisiblePin, musigNotificationVisiblePin,
            bisqEasyTradeNotificationsPin, musigTradeNotificationsPin;

    public NotificationPanelController(ServiceProvider serviceProvider) {
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();
        musigNotificationService = serviceProvider.getMuSigService().getMusigNotificationService();

        model = new NotificationPanelModel();
        view = new NotificationPanelView(model, this);
    }

    @Override
    public void onActivate() {
        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);

        bisqEasyNotificationVisiblePin = bisqEasyNotificationsService.getIsNotificationPanelVisible()
                .addObserver(isVisible -> UIThread.run(this::updateNotificationPanel));
        musigNotificationVisiblePin = musigNotificationService.getIsNotificationPanelVisible()
                .addObserver(isVisible -> UIThread.run(this::updateNotificationPanel));
        bisqEasyTradeNotificationsPin = bisqEasyNotificationsService.getTradeNotifications()
                .addObserver(() -> UIThread.run(this::updateNotificationPanel));
        musigTradeNotificationsPin = musigNotificationService.getTradeNotifications()
                .addObserver(() -> UIThread.run(this::updateNotificationPanel));
        updateNotificationPanel();
    }

    @Override
    public void onDeactivate() {
        if (changedChatNotificationPin != null) {
            changedChatNotificationPin.unbind();
            changedChatNotificationPin = null;
        }
        if (bisqEasyNotificationVisiblePin != null) {
            bisqEasyNotificationVisiblePin.unbind();
            bisqEasyNotificationVisiblePin = null;
        }
        if (musigNotificationVisiblePin != null) {
            musigNotificationVisiblePin.unbind();
            musigNotificationVisiblePin = null;
        }
        if (bisqEasyTradeNotificationsPin != null) {
            bisqEasyTradeNotificationsPin.unbind();
            bisqEasyTradeNotificationsPin = null;
        }
        if (musigTradeNotificationsPin != null) {
            musigTradeNotificationsPin.unbind();
            musigTradeNotificationsPin = null;
        }
    }

    void onClose() {
        UIThread.run(() -> {
            if (model.isMusigNotification()) {
                musigNotificationService.dismissNotification();
            } else {
                bisqEasyNotificationsService.dismissNotification();
            }
        });
    }

    void onNavigateToTarget() {
        Navigation.navigateTo(model.isMusigNotification()
                ? NavigationTarget.MU_SIG_OPEN_TRADES
                : model.isMediationNotification()
                ? NavigationTarget.BISQ_EASY_MEDIATOR
                : NavigationTarget.BISQ_EASY_OPEN_TRADES);
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null) {
            return;
        }

        UIThread.run(this::updateNotificationPanel);
    }

    private void updateNotificationPanel() {
        boolean isBisqEasyNotification = bisqEasyNotificationsService.getIsNotificationPanelVisible().get();
        boolean isMusigNotification = musigNotificationService.getIsNotificationPanelVisible().get();
        model.getIsNotificationVisible().set(isMusigNotification || isBisqEasyNotification);

        if (isMusigNotification) {
            model.setMusigNotification(true);
            model.setMediationNotification(false);
            updateTradeNotificationText(musigNotificationService.getTradeNotifications(), "notificationPanel.musigTrades");
        } else if (isBisqEasyNotification) {
            model.setMusigNotification(false);
            boolean hasMediatorNotConsumedNotifications = bisqEasyNotificationsService.hasMediatorNotConsumedNotifications();
            model.setMediationNotification(hasMediatorNotConsumedNotifications);
            updateTradeNotificationText(bisqEasyNotificationsService.getTradeNotifications(),
                    hasMediatorNotConsumedNotifications
                            ? "notificationPanel.bisqEasyMediationCases"
                            : "notificationPanel.trades");
        } else {
            model.setMusigNotification(false);
            model.setMediationNotification(false);
        }
    }

    private void updateTradeNotificationText(Set<ChatNotification> tradeNotifications, String textKeyPrefix) {
        Set<String> tradeIdsOfNotifications = tradeNotifications.stream()
                .flatMap(tradeNotification -> tradeNotification.getTradeId().stream())
                .map(e -> e.substring(0, 8)).collect(Collectors.toSet());
        if (tradeIdsOfNotifications.size() == 1) {
            String tradeId = tradeIdsOfNotifications.iterator().next();
            model.getHeadline().set(Res.get(textKeyPrefix + ".headline.single", tradeId));
            model.getButtonText().set(Res.get(textKeyPrefix + ".button"));
        } else if (tradeIdsOfNotifications.size() > 1) {
            String tradeIds = StringUtils.truncate(Joiner.on(", ").join(tradeIdsOfNotifications));
            model.getHeadline().set(Res.get(textKeyPrefix + ".headline.multiple", tradeIds));
            model.getButtonText().set(Res.get(textKeyPrefix + ".button"));
        }
    }
}
