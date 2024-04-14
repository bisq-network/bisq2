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
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.priv.PrivateChatMessage;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_30_DAYS;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class TwoPartyPrivateChatMessage extends PrivateChatMessage {
    private final MetaData metaData = new MetaData(TTL_30_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    public TwoPartyPrivateChatMessage(String messageId,
                                      ChatChannelDomain chatChannelDomain,
                                      String channelId,
                                      UserProfile senderUserProfile,
                                      String receiverUserProfileId,
                                      NetworkId receiverNetworkId,
                                      String text,
                                      Optional<Citation> citation,
                                      long date,
                                      boolean wasEdited,
                                      ChatMessageType chatMessageType) {
        super(messageId,
                chatChannelDomain,
                channelId,
                senderUserProfile,
                receiverUserProfileId,
                receiverNetworkId,
                text,
                citation,
                date,
                wasEdited,
                chatMessageType);
    }

    @Override
    public bisq.chat.protobuf.ChatMessage.Builder getValueBuilder(boolean serializeForHash) {
        return getChatMessageBuilder(serializeForHash)
                .setTwoPartyPrivateChatMessage(toTwoPartyPrivateChatMessageProto(serializeForHash));
    }

    private bisq.chat.protobuf.TwoPartyPrivateChatMessage toTwoPartyPrivateChatMessageProto(boolean serializeForHash) {
        return getTweakedBuilder(getTwoPartyPrivateChatMessageBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.TwoPartyPrivateChatMessage.Builder getTwoPartyPrivateChatMessageBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.TwoPartyPrivateChatMessage.newBuilder()
                .setReceiverUserProfileId(receiverUserProfileId)
                .setReceiverNetworkId(receiverNetworkId.toProto(serializeForHash))
                .setSender(senderUserProfile.toProto(serializeForHash));
    }

    public static TwoPartyPrivateChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        bisq.chat.protobuf.TwoPartyPrivateChatMessage privateChatMessage = baseProto.getTwoPartyPrivateChatMessage();
        return new TwoPartyPrivateChatMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                UserProfile.fromProto(privateChatMessage.getSender()),
                privateChatMessage.getReceiverUserProfileId(),
                NetworkId.fromProto(privateChatMessage.getReceiverNetworkId()),
                baseProto.getText(),
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()));
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.3);
    }
}