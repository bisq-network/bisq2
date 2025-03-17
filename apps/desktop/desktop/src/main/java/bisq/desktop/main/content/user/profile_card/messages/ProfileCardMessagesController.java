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
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ProfileCardMessagesController implements Controller {
    @Getter
    private final ProfileCardMessagesView view;
    private final ProfileCardMessagesModel model;
    private final List<ChannelMessagesDisplayList<?>> channelMessagesDisplayLists = new ArrayList<>();
    private final ServiceProvider serviceProvider;
    private final Set<Subscription> channelMessagesPins = new HashSet<>();
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final Map<ChatChannelDomain, CommonPublicChatChannelService> commonPublicChatChannelServices;

    public ProfileCardMessagesController(ServiceProvider serviceProvider) {
        model = new ProfileCardMessagesModel();
        view = new ProfileCardMessagesView(model, this);
        this.serviceProvider = serviceProvider;
        ChatService chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        commonPublicChatChannelServices = chatService.getCommonPublicChatChannelServices();
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
        channelMessagesDisplayLists.clear();

        bisqEasyOfferbookChannelService.getChannels().forEach(channel ->
                channelMessagesDisplayLists.add(new ChannelMessagesDisplayList<>(serviceProvider, channel, userProfile)));

        commonPublicChatChannelServices.values().forEach(channelService ->
                    channelService.getChannels().forEach(channel ->
                        channelMessagesDisplayLists.add(new ChannelMessagesDisplayList<>(serviceProvider, channel, userProfile))));

        channelMessagesDisplayLists.forEach(messageDisplayList -> UIThread.runOnNextRenderFrame(() ->
                channelMessagesPins.add(EasyBind.subscribe(messageDisplayList.shouldShowMessageDisplayList(), shouldShow -> updateShouldShowMessages()))));

        view.updateProfileCardMessages(channelMessagesDisplayLists.stream()
                .map(ChannelMessagesDisplayList::getRoot).toList());
    }

    private void updateShouldShowMessages() {
        boolean shouldShowMessages = channelMessagesDisplayLists.stream()
                .anyMatch(displayList -> displayList.shouldShowMessageDisplayList().get());
        model.getShouldShowMessages().set(shouldShowMessages);
    }

    public String getNumberMessages(String userProfileId) {
        return String.valueOf(getNumPublicTextMessages(userProfileId).count());
    }

    private Stream<ChatMessage> getNumPublicTextMessages(String userProfileId) {
        Stream<CommonPublicChatMessage> commonPublicChatMessagesStream = commonPublicChatChannelServices.values().stream()
                .flatMap(service -> service.getChannels().stream())
                .flatMap(channel -> channel.getChatMessages().stream())
                .filter(message -> message.getChatMessageType() == ChatMessageType.TEXT)
                .filter(message -> message.getAuthorUserProfileId().equals(userProfileId));
        Stream<BisqEasyOfferbookMessage> bisqEasyOfferbookMessageStream = bisqEasyOfferbookChannelService.getChannels().stream()
                .flatMap(channel -> channel.getChatMessages().stream())
                .filter(message -> !message.hasBisqEasyOffer())
                .filter(message -> message.getChatMessageType() == ChatMessageType.TEXT) // Is redundant but let's keep it for covering potential future changes
                .filter(message -> message.getAuthorUserProfileId().equals(userProfileId));
        return Stream.concat(commonPublicChatMessagesStream, bisqEasyOfferbookMessageStream);
    }
}
