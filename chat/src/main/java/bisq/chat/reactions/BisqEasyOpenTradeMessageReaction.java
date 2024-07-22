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

package bisq.chat.reactions;

import bisq.chat.ChatChannelDomain;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.services.data.storage.MetaData.*;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BisqEasyOpenTradeMessageReaction extends PrivateChatMessageReaction {
    // Metadata needs to be symmetric with BisqEasyOpenTradeMessage.
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_30_DAYS, HIGH_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    public BisqEasyOpenTradeMessageReaction(String id,
                                            UserProfile senderUserProfile,
                                            String receiverUserProfileId,
                                            NetworkId receiverNetworkId,
                                            String chatChannelId,
                                            ChatChannelDomain chatChannelDomain,
                                            String chatMessageId,
                                            int reactionId,
                                            long date,
                                            boolean isRemoved) {
        super(id, senderUserProfile, receiverUserProfileId, receiverNetworkId, chatChannelId, chatChannelDomain,
                chatMessageId, reactionId, date, isRemoved);
    }

    @Override
    public bisq.chat.protobuf.ChatMessageReaction.Builder getValueBuilder(boolean serializeForHash) {
        return getChatMessageReactionBuilder(serializeForHash)
                .setBisqEasyOpenTradeMessageReaction(toBisqEasyOpenTradeMessageReactionProto(serializeForHash));
    }

    private bisq.chat.protobuf.BisqEasyOpenTradeMessageReaction toBisqEasyOpenTradeMessageReactionProto(boolean serializeForHash) {
        return resolveBuilder(getBisqEasyOpenTradeMessageReactionBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.BisqEasyOpenTradeMessageReaction.Builder getBisqEasyOpenTradeMessageReactionBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.BisqEasyOpenTradeMessageReaction.newBuilder()
                .setReceiverUserProfileId(receiverUserProfileId)
                .setReceiverNetworkId(receiverNetworkId.toProto(serializeForHash))
                .setSender(senderUserProfile.toProto(serializeForHash))
                .setIsRemoved(isRemoved);
    }

    public static BisqEasyOpenTradeMessageReaction fromProto(bisq.chat.protobuf.ChatMessageReaction baseProto) {
        bisq.chat.protobuf.BisqEasyOpenTradeMessageReaction privateChatMessageReaction = baseProto.getBisqEasyOpenTradeMessageReaction();
        return new BisqEasyOpenTradeMessageReaction(
                baseProto.getId(),
                UserProfile.fromProto(privateChatMessageReaction.getSender()),
                privateChatMessageReaction.getReceiverUserProfileId(),
                NetworkId.fromProto(privateChatMessageReaction.getReceiverNetworkId()),
                baseProto.getChatChannelId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChatMessageId(),
                baseProto.getReactionId(),
                baseProto.getDate(),
                privateChatMessageReaction.getIsRemoved());
    }

    @Override
    public double getCostFactor() {
        return 0.5;
    }
}
