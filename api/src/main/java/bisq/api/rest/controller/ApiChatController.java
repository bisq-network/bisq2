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

package bisq.api.rest.controller;

import bisq.api.rest.ApiApplicationService;
import bisq.common.observable.ObservableSet;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.Channel;
import bisq.social.chat.channels.PublicDiscussionChannel;
import bisq.social.chat.channels.PublicTradeChannel;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.user.ChatUserIdentity;
import bisq.social.user.ChatUserService;
import bisq.social.user.NymGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
class ApiChatController extends ApiController {
    private final ChatService chatService;
    private final ChatUserService chatUserService;
    private final IdentityService identityService;

    public ApiChatController(ApiApplicationService apiApplicationService) {
        chatService = apiApplicationService.getChatService();
        chatUserService = apiApplicationService.getChatUserService();
        identityService = apiApplicationService.getIdentityService();
    }

    @GetMapping(path = "/api/chat/get-public-trade-channels")
    public ObservableSet<PublicTradeChannel> getPublicTradeChannels() {
        return chatService.getPublicTradeChannels();
    }

    @GetMapping(path = "/api/chat/get-public-discussion-channels")
    public ObservableSet<PublicDiscussionChannel> getPublicDiscussionChannels() {
        return chatService.getPublicDiscussionChannels();
    }

    @GetMapping(path = "/api/chat/get-selected-trade-channel")
    public Channel<? extends ChatMessage> getSelectedTradeChannel() {
        return chatService.getSelectedTradeChannel().get();
    }

    @PostMapping(path = "/api/chat/select-public-discussion-channel/{channelId}")
    public boolean selectedPublicDiscussionChannel(@PathVariable("channelId") String channelId) {
        Optional<PublicDiscussionChannel> optionalPublicChannel = chatService.findPublicDiscussionChannel(channelId);
        optionalPublicChannel.ifPresent(chatService::selectTradeChannel);
        return optionalPublicChannel.isPresent();
    }

    @PostMapping(path = "/api/chat/publish-discussion-chat-message/{text}")
    public boolean publishDiscussionChatMessage(@PathVariable("text") String text) {
        if (chatService.getSelectedDiscussionChannel().get() instanceof PublicDiscussionChannel publicDiscussionChannel) {
            chatService.publishDiscussionChatMessage(text,
                    Optional.empty(),
                    publicDiscussionChannel,
                    chatUserService.getSelectedUserProfile().get());
            return true;
        } else {
            return false;
        }
    }

    @GetMapping(path = "/api/chat/get-selected-user-profile")
    public ChatUserIdentity getSelectedUserProfile() {
        return chatUserService.getSelectedUserProfile().get();
    }

    @GetMapping(path = "/api/chat/get-or-create-identity/{domainId}")
    public Identity getOrCreateIdentity(@PathVariable("domainId") String domainId) {
        return identityService.getOrCreateIdentity(domainId).join();
    }

    @PostMapping(path = "/api/chat/create-user-profile/{domainId}/{nickName}")
    public ChatUserIdentity createUserProfile(@PathVariable("domainId") String domainId,
                                              @PathVariable("nickName") String nickName) {
        Identity identity = identityService.getOrCreateIdentity(domainId).join();
        String profileId = NymGenerator.fromHash(identity.getNodeIdAndKeyPair().pubKey().publicKey().getEncoded());
        return chatUserService.createNewInitializedUserProfile(profileId,
                nickName,
                "default",
                identity.keyPair()
        ).join();
    }
}
