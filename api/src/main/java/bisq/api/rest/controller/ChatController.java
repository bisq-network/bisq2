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
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.social.chat.ChatService;
import bisq.social.chat.PublicChannel;
import bisq.social.user.UserNameGenerator;
import bisq.social.user.profile.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
class ChatController extends ApiController {
    private final ChatService chatService;
    private final UserProfileService userProfileService;
    private final IdentityService identityService;

    public ChatController(ApiApplicationService apiApplicationService) {
        chatService = apiApplicationService.getChatService();
        userProfileService = apiApplicationService.getUserProfileService();
        identityService = apiApplicationService.getIdentityService();
    }

    @GetMapping(path = "/api/chat/get-market-channels")
    public List<String> getMarketChannels() {
        return chatService.getMarketChannels().stream().map(this::asJson).collect(Collectors.toList());
    }

    @GetMapping(path = "/api/chat/get-public-channels")
    public List<String> getPublicChannels() {
        return chatService.getPublicChannels().stream().map(this::asJson).collect(Collectors.toList());
    }

    @GetMapping(path = "/api/chat/get-selected-channel")
    public String getSelectedChannel() {
        return asJson(chatService.getSelectedChannel().get());
    }

    @GetMapping(path = "/api/chat/select-channel/{channelId}")
    public boolean selectedChannel(@PathVariable("channelId") String channelId) {
        Optional<PublicChannel> optionalPublicChannel = chatService.getPersistableStore().findPublicChannel(channelId);
        optionalPublicChannel.ifPresent(chatService::setSelectedChannel);
        return optionalPublicChannel.isPresent();
    }

    @GetMapping(path = "/api/chat/publish-public-msg/{text}")
    public boolean publishPublicChatMessage(@PathVariable("text") String text) {
        if (chatService.getSelectedChannel().get() instanceof PublicChannel publicChannel) {
            chatService.publishPublicChatMessage(text,
                    Optional.empty(),
                    publicChannel,
                    userProfileService.getSelectedUserProfile().get());
            return true;
        } else {
            return false;
        }
    }

    @GetMapping(path = "/api/chat/get-selected-user-profile")
    public String getSelectedUserProfile() {
        return asJson(userProfileService.getSelectedUserProfile().get());
    }

    @GetMapping(path = "/api/chat/get-or-create-identity/{domainId}")
    public String getOrCreateIdentity(@PathVariable("domainId") String domainId) {
        return asJson(identityService.getOrCreateIdentity(domainId).join());
    }

    @GetMapping(path = "/api/chat/create-user-profile/{domainId}/{nickName}")
    public String createUserProfile(@PathVariable("domainId") String domainId,
                                    @PathVariable("nickName") String nickName) {
        Identity identity = identityService.getOrCreateIdentity(domainId).join();
        String profileId = UserNameGenerator.fromHash(identity.getNodeIdAndKeyPair().pubKey().publicKey().getEncoded());
        return asJson(userProfileService.createNewInitializedUserProfile(profileId,
                nickName,
                "default",
                identity.keyPair(),
                new HashSet<>()).join());
    }
}
