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

package bisq.desktop.main.content.chat.priv;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.chat.priv.PrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.chat.ChatController;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class PrivateChatsController extends ChatController<PrivateChatsView, PrivateChatsModel> {
    private final TwoPartyPrivateChatChannelService channelService;
    private final ChatNotificationService chatNotificationService;
    private final ReputationService reputationService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private Pin channelItemPin, channelsPin, changedChatNotificationPin;
    private Subscription openPrivateChatsPin;

    public PrivateChatsController(ServiceProvider serviceProvider,
                                  ChatChannelDomain chatChannelDomain,
                                  NavigationTarget navigationTarget) {
        super(serviceProvider, chatChannelDomain, navigationTarget);

        channelService = chatService.getTwoPartyPrivateChatChannelService();
        leavePrivateChatManager = chatService.getLeavePrivateChatManager();
        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        reputationService = serviceProvider.getUserService().getReputationService();
    }

    @Override
    public void onActivate() {
        // We access the (model.getListItems() in selectedChannelChanged triggered by the super call,
        // thus we set up the binding before the super call
        channelItemPin = FxBindings.<TwoPartyPrivateChatChannel, PrivateChatsView.ListItem>bind(model.getListItems())
                .map(channel -> new PrivateChatsView.ListItem(channel, reputationService, userProfileService))
                .to(channelService.getChannels());

        super.onActivate();

        channelsPin = channelService.getChannels().addObserver(this::channelsChanged);

        openPrivateChatsPin = EasyBind.subscribe(model.getNoOpenChats(),
                noOpenChats -> chatMessageContainerController.enableChatDialog(!noOpenChats));

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedChatNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);

        maybeSelectFirst();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        channelItemPin.unbind();
        channelsPin.unbind();
        model.getListItems().clear();
        resetSelectedChildTarget();
        openPrivateChatsPin.unsubscribe();
        changedChatNotificationPin.unbind();
    }

    @Override
    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectedChannelChanged(chatChannel);

        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getSelectedItem().set(null);
                model.setMyUserReputationScore(null);
                model.getMyUserProfile().set(null);
                model.setPeersReputationScore(null);
                model.getPeersUserProfile().set(null);
                maybeSelectFirst();
            }

            if (chatChannel instanceof TwoPartyPrivateChatChannel channel) {
                // Set reputation score first since observable is userProfile, which updates both user and reputation
                UserProfile peer = userProfileService.getManagedUserProfile(channel.getPeer());
                model.setPeersReputationScore(reputationService.getReputationScore(peer));
                model.getPeersUserProfile().set(peer);
                UserProfile myProfile = userProfileService.getManagedUserProfile(channel.getMyUserIdentity().getUserProfile());
                model.setMyUserReputationScore(reputationService.getReputationScore(myProfile));
                model.getMyUserProfile().set(myProfile);

                model.getListItems().stream()
                        .filter(item -> item.getChannel().equals(channel))
                        .findAny()
                        .ifPresent(item -> model.getSelectedItem().set(item));
            }
        });
    }

    void onSelectItem(PrivateChatsView.ListItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
            selectionService.selectChannel(item.getChannel());
        }
    }

    void onLeaveChat() {
        new Popup().warning(Res.get("chat.private.leaveChat.confirmation"))
                .actionButtonText(Res.get("confirmation.yes"))
                .onAction(this::doLeaveChat)
                .closeButtonText(Res.get("confirmation.no"))
                .show();
    }

    void doLeaveChat() {
        ChatChannel<? extends ChatMessage> selectedChannel = model.getSelectedChannel();
        if (selectedChannel != null) {
            checkArgument(selectedChannel instanceof PrivateChatChannel,
                    "Not possible to leave a channel which is not a private chat.");
            leavePrivateChatManager.leaveChannel((PrivateChatChannel<?>) selectedChannel);
        }
    }

    private void channelsChanged() {
        UIThread.run(() -> {
            model.getNoOpenChats().set(model.getFilteredList().isEmpty());
            maybeSelectFirst();
        });
    }

    private void maybeSelectFirst() {
        if (selectionService.getSelectedChannel().get() == null &&
                !channelService.getChannels().isEmpty() &&
                !model.getSortedList().isEmpty()) {
            selectionService.selectChannel(model.getSortedList().getFirst().getChannel());
        }
    }

    private void handleNotification(ChatNotification notification) {
        if (notification == null || notification.getChatChannelDomain() != model.getChatChannelDomain()) {
            return;
        }

        handlePrivateNotification(notification.getChatChannelId());
    }

    private void handlePrivateNotification(String channelId) {
        UIThread.run(() -> channelService.findChannel(channelId).ifPresent(channel -> model.getFilteredList().stream()
                .filter(listItem -> listItem.getChannel() == channel)
                .findAny()
                .ifPresent(listItem -> listItem.setNumNotifications(chatNotificationService.getNumNotifications(channel)))));
    }
}
