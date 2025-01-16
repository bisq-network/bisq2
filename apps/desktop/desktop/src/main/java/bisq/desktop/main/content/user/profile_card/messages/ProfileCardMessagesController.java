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

package bisq.desktop.main.content.user.profile_card.messages;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfileCardMessagesController implements Controller {
    @Getter
    private final ProfileCardMessagesView view;
    private final ProfileCardMessagesModel model;
    private final List<ChannelMessagesDisplayList<?>> channelMessagesDisplayList = new ArrayList<>();
    private final ServiceProvider serviceProvider;
    private final ChatService chatService;
    private final Set<Subscription> channelMessagesPins = new HashSet<>();

    public ProfileCardMessagesController(ServiceProvider serviceProvider) {
        model = new ProfileCardMessagesModel();
        view = new ProfileCardMessagesView(model, this);
        this.serviceProvider = serviceProvider;
        chatService = serviceProvider.getChatService();
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
        channelMessagesPins.forEach(Subscription::unsubscribe);
        channelMessagesPins.clear();
    }

    public void setUserProfile(UserProfile userProfile) {
        channelMessagesDisplayList.clear();

        chatService.getBisqEasyOfferbookChannelService().getChannels().forEach(channel ->
                channelMessagesDisplayList.add(new ChannelMessagesDisplayList<>(serviceProvider, channel, userProfile)));

        chatService.getCommonPublicChatChannelServices().get(ChatChannelDomain.DISCUSSION).getChannels().forEach(channel ->
                channelMessagesDisplayList.add(new ChannelMessagesDisplayList<>(serviceProvider, channel, userProfile)));

        chatService.getCommonPublicChatChannelServices().get(ChatChannelDomain.SUPPORT).getChannels().forEach(channel ->
                channelMessagesDisplayList.add(new ChannelMessagesDisplayList<>(serviceProvider, channel, userProfile)));

        channelMessagesDisplayList.forEach(messageDisplayList -> UIThread.runOnNextRenderFrame(() ->
                channelMessagesPins.add(EasyBind.subscribe(messageDisplayList.shouldShowMessageDisplayList(), shouldShow -> updateShouldShowMessages()))));

        view.updateProfileCardMessages(channelMessagesDisplayList.stream()
                .map(ChannelMessagesDisplayList::getRoot).toList());
    }

    private void updateShouldShowMessages() {
        boolean shouldShowMessages = channelMessagesDisplayList.stream()
                .anyMatch(displayList -> displayList.shouldShowMessageDisplayList().get());
        model.getShouldShowMessages().set(shouldShowMessages);
    }
}
