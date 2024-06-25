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
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CommonPublicChatMessageReaction extends ChatMessageReaction {
    @EqualsAndHashCode.Exclude
    private final MetaData metaData = new MetaData(MetaData.TTL_10_DAYS, MetaData.LOW_PRIORITY, getClass().getSimpleName(), MetaData.MAX_MAP_SIZE_10_000);

    public CommonPublicChatMessageReaction(String id,
                                           String userProfileId,
                                           String chatChannelId,
                                           ChatChannelDomain chatChannelDomain,
                                           String chatMessageId,
                                           int reactionId,
                                           boolean isRemoved,
                                           long date) {
        super(id, userProfileId, chatChannelId, chatChannelDomain, chatMessageId, reactionId, isRemoved, date);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.chat.protobuf.ChatMessageReaction.Builder getBuilder(boolean serializeForHash) {
        return getChatMessageReactionBuilder(serializeForHash)
                .setCommonPublicChatMessageReaction(toCommonPublicChatMessageReactionProto(serializeForHash));
    }

    public static CommonPublicChatMessageReaction fromProto(bisq.chat.protobuf.ChatMessageReaction baseProto) {
//        System.out.println("From proto");
//        System.out.println(baseProto.toString());

        return new CommonPublicChatMessageReaction(
                baseProto.getId(),
                baseProto.getUserProfileId(),
                baseProto.getChatChannelId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChatMessageId(),
                baseProto.getReactionId(),
                baseProto.getIsRemoved(),
                baseProto.getDate());
    }

    private bisq.chat.protobuf.CommonPublicChatMessageReaction toCommonPublicChatMessageReactionProto(boolean serializeForHash) {
        return resolveBuilder(getCommonPublicChatMessageReactionBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.CommonPublicChatMessageReaction.Builder getCommonPublicChatMessageReactionBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.CommonPublicChatMessageReaction.newBuilder();
    }
}
