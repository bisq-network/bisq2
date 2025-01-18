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

package bisq.desktop.main.content.support;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.common.CommonChannelSelectionService;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.SubDomain;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.chat.common.pub.CommonPublicChatController;
import bisq.desktop.main.content.support.resources.ResourcesController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SupportController extends ContentTabController<SupportModel> {
    @Getter
    private final SupportView view;

    private final ChatNotificationService chatNotificationService;
    private final CommonPublicChatChannelService commonPublicChatChannelService;
    private final CommonChannelSelectionService chatChannelSelectionService;
    private Pin changedChatNotificationPin;

    public SupportController(ServiceProvider serviceProvider) {
        super(new SupportModel(), NavigationTarget.SUPPORT, serviceProvider);

        ChatService chatService = serviceProvider.getChatService();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(ChatChannelDomain.SUPPORT);
        chatChannelSelectionService = (CommonChannelSelectionService) chatService.getChatChannelSelectionServices().get(ChatChannelDomain.SUPPORT);

        view = new SupportView(model, this);
    }

    @Override
    public void onActivate() {
        super.onActivate();

        model.setAssistanceChannel(commonPublicChatChannelService.getChannels().stream()
                .filter(c -> c.getSubDomain() == SubDomain.SUPPORT_SUPPORT).findFirst()
                .orElseThrow());
        chatChannelSelectionService.selectChannel(model.getAssistanceChannel());

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        changedChatNotificationPin.unbind();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case SUPPORT_ASSISTANCE ->
                    Optional.of(new CommonPublicChatController(serviceProvider, ChatChannelDomain.SUPPORT, navigationTarget));
            case SUPPORT_RESOURCES -> Optional.of(new ResourcesController(serviceProvider));
            default -> Optional.empty();
        };
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null || notification.getChatChannelDomain() != ChatChannelDomain.SUPPORT) {
            return;
        }

        updateTabButtonNotifications();
    }

    private void updateTabButtonNotifications() {
        UIThread.run(() -> model.getTabButtons().stream()
                .filter(tabButton -> tabButton.getNavigationTarget() == NavigationTarget.SUPPORT_ASSISTANCE)
                .findAny()
                .ifPresent(tabButton -> tabButton.setNumNotifications(chatNotificationService.getNumNotifications(ChatChannelDomain.SUPPORT,
                        model.getAssistanceChannel().getId()))));
    }
}
