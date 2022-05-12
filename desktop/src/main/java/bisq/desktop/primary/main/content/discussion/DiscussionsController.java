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
import bisq.common.data.ByteArray;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.ChatController;
import bisq.desktop.primary.main.content.components.PublicDiscussionChannelSelection;
import bisq.social.chat.channels.Channel;
import bisq.social.chat.channels.PrivateDiscussionChannel;
import bisq.social.chat.messages.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

@Slf4j
public class DiscussionsController extends ChatController<DiscussionsView, DiscussionsModel> implements Controller {
    private PublicDiscussionChannelSelection publicDiscussionChannelSelection;

    public DiscussionsController(DefaultApplicationService applicationService) {
        super(applicationService, true);
    }

    @Override
    public void onActivate() {
        super.onActivate();

        notificationSettingSubscription = EasyBind.subscribe(notificationsSettings.getNotificationSetting(),
                value -> {
                    Channel<? extends ChatMessage> channel = chatService.getSelectedDiscussionChannel().get();
                    if (channel != null) {
                        chatService.setNotificationSetting(channel, value);
                    }
                });

        selectedChannelPin = chatService.getSelectedDiscussionChannel().addObserver(this::handleChannelChange);
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
                userProfileSelection.getRoot(),
                publicDiscussionChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                notificationsSettings.getRoot(),
                channelInfo.getRoot(),
                filterBox);
    }

    @Override
    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        super.handleChannelChange(channel);

        if (channel instanceof PrivateDiscussionChannel privateDiscussionChannel) {
            model.getPeersRoboIconImage().set(RoboHash.getImage(new ByteArray(privateDiscussionChannel.getPeer().getPubKeyHash())));
            model.getPeersRoboIconVisible().set(true);
            publicDiscussionChannelSelection.deSelectChannel();
        } else {
            model.getPeersRoboIconVisible().set(false);
            privateChannelSelection.deSelectChannel();
        }
    }
}
