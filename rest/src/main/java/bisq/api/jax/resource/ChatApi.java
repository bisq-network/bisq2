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

package bisq.api.jax.resource;

import bisq.api.jax.RestApplication;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.social.chat.ChatService;
import bisq.social.protobuf.Channel;
import bisq.social.user.ChatUserService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Path("/api/v1/chat")
@Produces(MediaType.APPLICATION_JSON)
public class ChatApi {
    private final ChatService chatService;
    private final ChatUserService chatUserService;
    private final IdentityService identityService;

    public ChatApi(@Context RestApplication app) {
        chatService = app.getApplicationService().getChatService();
        chatUserService = app.getApplicationService().getChatUserService();
        identityService = app.getApplicationService().getIdentityService();
    }

    @GET
    @Path("/get-public-trade-channels")
    public List<Channel> getPublicTradeChannels() {
        return chatService.getPublicTradeChannels().stream().map(tc -> tc.toProto()).collect(Collectors.toList());
    }

//
//    @GET
//    @Path("/get-public-discussion-channels")
//    public ObservableSet<PublicDiscussionChannel> getPublicDiscussionChannels() {
//        return chatService.getPublicDiscussionChannels();
//    }
//
//
////
////    @GET
////    @Path("/get-selected-trade-channel")
////    public Channel<? extends ChatMessage> getSelectedTradeChannel() {
////        return chatService.getSelectedTradeChannel().get();
////    }
//
//
//    @GET
//    @Path("select-public-discussion-channel/{channelId}")
//    public boolean selectedPublicDiscussionChannel(@PathParam("channelId") String channelId) {
//        Optional<PublicDiscussionChannel> optionalPublicChannel = chatService.findPublicDiscussionChannel(channelId);
//        optionalPublicChannel.ifPresent(chatService::selectTradeChannel);
//        return optionalPublicChannel.isPresent();
//    }
//
//    @GET
//    @Path("publish-discussion-chat-message/{text}")
//    public boolean publishDiscussionChatMessage(@PathParam("text") String text) {
//        if (chatService.getSelectedDiscussionChannel().get() instanceof PublicDiscussionChannel publicDiscussionChannel) {
//            chatService.publishDiscussionChatMessage(text,
//                    Optional.empty(),
//                    publicDiscussionChannel,
//                    chatUserService.getSelectedUserProfile().get());
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    @GET
//    @Path("get-selected-user-profile")
//    public ChatUserIdentity getSelectedUserProfile() {
//        return chatUserService.getSelectedUserProfile().get();
//    }

    @GET
    @Path("get-or-create-identity/{domainId}")
    public Identity getOrCreateIdentity(@PathParam("domainId") String domainId) {
        return identityService.getOrCreateIdentity(domainId).join();
    }
//  bisq.identity.protobuf.Identity
    
//    @GET
//    @Path("create-user-profile/{domainId}/{nickName}")
//    public ChatUserIdentity createUserProfile(@PathParam("domainId") String domainId,
//                                              @PathParam("nickName") String nickName) {
//        Identity identity = identityService.getOrCreateIdentity(domainId).join();
//        String profileId = NymGenerator.fromHash(identity.getNodeIdAndKeyPair().pubKey().publicKey().getEncoded());
//        return chatUserService.createNewInitializedUserProfile(profileId,
//                nickName,
//                "default",
//                identity.keyPair()
//        ).join();
//    }
}
