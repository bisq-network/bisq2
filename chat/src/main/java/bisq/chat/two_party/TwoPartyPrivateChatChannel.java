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

package bisq.chat.two_party;

import bisq.chat.ChatChannelDomain;
import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.chat.priv.PrivateChatChannel;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public final class TwoPartyPrivateChatChannel extends PrivateChatChannel<TwoPartyPrivateChatMessage> {
    // Channel id must be deterministic, so we sort both userIds and use that order for the concatenated string.
    public static String createId(ChatChannelDomain chatChannelDomain, String userProfileId1, String userProfileId2) {
        List<String> userIds = Stream.of(userProfileId1, userProfileId2)
                .sorted()
                .toList();
        return chatChannelDomain.migrate().name().toLowerCase() + "." + userIds.get(0) + "-" + userIds.get(1);
    }

    @Getter
    private final UserProfile peer;

    public TwoPartyPrivateChatChannel(UserProfile peer,
                                      UserIdentity myUserIdentity,
                                      ChatChannelDomain chatChannelDomain) {
        this(createId(chatChannelDomain, peer.getId(), myUserIdentity.getId()),
                chatChannelDomain,
                peer,
                myUserIdentity,
                new HashSet<>(),
                ChatChannelNotificationType.ALL
        );
    }

    private TwoPartyPrivateChatChannel(String id,
                                       ChatChannelDomain chatChannelDomain,
                                       UserProfile peer,
                                       UserIdentity myUserIdentity,
                                       Set<TwoPartyPrivateChatMessage> chatMessages,
                                       ChatChannelNotificationType chatChannelNotificationType) {
        super(id, chatChannelDomain, myUserIdentity, chatMessages, chatChannelNotificationType);
        this.peer = peer;
    }

    @Override
    public bisq.chat.protobuf.ChatChannel.Builder getBuilder(boolean serializeForHash) {
        return getChatChannelBuilder().setTwoPartyPrivateChatChannel(bisq.chat.protobuf.TwoPartyPrivateChatChannel.newBuilder()
                .setPeer(peer.toProto(serializeForHash))
                .setMyUserIdentity(myUserIdentity.toProto(serializeForHash))
                .addAllChatMessages(chatMessages.stream()
                        .map(e -> e.toValueProto(serializeForHash))
                        .collect(Collectors.toList())));
    }

    public static TwoPartyPrivateChatChannel fromProto(bisq.chat.protobuf.ChatChannel baseProto,
                                                       bisq.chat.protobuf.TwoPartyPrivateChatChannel proto) {
        return new TwoPartyPrivateChatChannel(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                UserProfile.fromProto(proto.getPeer()),
                UserIdentity.fromProto(proto.getMyUserIdentity()),
                proto.getChatMessagesList().stream()
                        .map(TwoPartyPrivateChatMessage::fromProto)
                        .collect(Collectors.toSet()),
                ChatChannelNotificationType.fromProto(baseProto.getChatChannelNotificationType())
        );
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ChatChannelDomain getChatChannelDomain() {
        return chatChannelDomain.migrate();
    }

    @Override
    public String getId() {
        return Migration.migrateChannelId(id);
    }

    @Override
    public String getDisplayString() {
        return getPeer().getUserName();
    }

    @Slf4j
    public static class Migration {
        public static String migrateChannelId(String chatChannelId) {
            try {
                String[] tokens = chatChannelId.split("\\.");
                String chatChannelDomainName = tokens[0].toUpperCase();
                ChatChannelDomain chatChannelDomain = ChatChannelDomain.valueOf(chatChannelDomainName).migrate();
                String[] peers = tokens[1].split("-");
                String userProfileId1 = peers[0];
                String userProfileId2 = peers[1];
                return createId(chatChannelDomain, userProfileId1, userProfileId2);
            } catch (Exception e) {
                log.error("Cannot migrate chatChannelId from chatChannelId={}.", chatChannelId, e);
                throw e;
            }
        }
    }
}