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

package bisq.desktop.primary.main.content.discussion;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.Channel;
import bisq.chat.channel.discuss.DiscussionChannelSelectionService;
import bisq.chat.channel.discuss.priv.PrivateDiscussionChannel;
import bisq.chat.channel.discuss.pub.PublicDiscussionChannelService;
import bisq.chat.message.ChatMessage;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.ChatController;
import bisq.desktop.primary.main.content.components.PublicDiscussionChannelSelection;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

@Slf4j
public class DiscussionsController extends ChatController<DiscussionsView, DiscussionsModel> implements Controller {
    private final PublicDiscussionChannelService publicDiscussionChannelService;
    private final DiscussionChannelSelectionService discussionChannelSelectionService;
    private PublicDiscussionChannelSelection publicDiscussionChannelSelection;

    public DiscussionsController(DefaultApplicationService applicationService) {
        super(applicationService, true);

        publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
        discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        notificationSettingSubscription = EasyBind.subscribe(notificationsSettings.getNotificationSetting(),
                value -> {
                    Channel<? extends ChatMessage> channel = discussionChannelSelectionService.getSelectedChannel().get();
                    if (channel != null) {
                        publicDiscussionChannelService.setNotificationSetting(channel, value);
                    }
                });

        selectedChannelPin = discussionChannelSelectionService.getSelectedChannel().addObserver(this::handleChannelChange);
    }

    @Override
    public void createComponents() {
        publicDiscussionChannelSelection = new PublicDiscussionChannelSelection(applicationService);
    }

    @Override
    public DiscussionsModel getChatModel(boolean isDiscussionsChat) {
        return new DiscussionsModel(isDiscussionsChat);
    }

    @Override
    public DiscussionsView getChatView() {
        return new DiscussionsView(model,
                this,
                publicDiscussionChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                notificationsSettings.getRoot(),
                channelInfo.getRoot(),
                helpPane.getRoot(),
                filterBox);
    }

    @Override
    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        super.handleChannelChange(channel);

        if (channel instanceof PrivateDiscussionChannel) {
            model.getPeersRoboIconImage().set(RoboHash.getImage(((PrivateDiscussionChannel) channel).getPeer().getPubKeyHash()));
            model.getPeersRoboIconVisible().set(true);
            publicDiscussionChannelSelection.deSelectChannel();
        } else {
            model.getPeersRoboIconVisible().set(false);
            privateChannelSelection.deSelectChannel();
        }
    }
}
