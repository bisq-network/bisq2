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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.security.keys.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CommonPublicChatMessageReaction extends ChatMessageReaction implements OwnershipVerifiableData {
    // Metadata needs to be symmetric with CommonPublicChatMessage.
    public static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS, MetaData.LOW_PRIORITY, CommonPublicChatMessageReaction.class.getSimpleName(), MetaData.MAX_MAP_SIZE_10_000);

    public CommonPublicChatMessageReaction(String id,
                                           String userProfileId,
                                           String chatChannelId,
                                           ChatChannelDomain chatChannelDomain,
                                           String chatMessageId,
                                           int reactionId,
                                           long date) {
        super(id, userProfileId, chatChannelId, chatChannelDomain, chatMessageId, reactionId, date);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        // The profile ID must be the ID of a public key, so that it names the reacting user (see getOwnerPubKeyHash())
        checkArgument(PubKey.isId(userProfileId), "userProfileId is not the ID of a public key");
    }

    @Override
    public bisq.chat.protobuf.ChatMessageReaction toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    // The reacting user owns the reaction. The profile ID is the ID of the public key of the user (PubKey.getId()).
    @Override
    public byte[] getOwnerPubKeyHash() {
        return PubKey.getHashFromId(userProfileId);
    }

    @Override
    public MetaData getMetaData() {
        return META_DATA;
    }

    @Override
    public bisq.chat.protobuf.ChatMessageReaction.Builder getBuilder(boolean serializeForHash) {
        return getChatMessageReactionBuilder(serializeForHash)
                .setCommonPublicChatMessageReaction(toCommonPublicChatMessageReactionProto(serializeForHash));
    }

    public static CommonPublicChatMessageReaction fromProto(bisq.chat.protobuf.ChatMessageReaction baseProto)
            throws UnresolvableProtobufException {
        try {
            return new CommonPublicChatMessageReaction(
                    baseProto.getId(),
                    baseProto.getUserProfileId(),
                    baseProto.getChatChannelId(),
                    ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                    baseProto.getChatMessageId(),
                    baseProto.getReactionId(),
                    baseProto.getDate());
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    private bisq.chat.protobuf.CommonPublicChatMessageReaction toCommonPublicChatMessageReactionProto(boolean serializeForHash) {
        return resolveBuilder(getCommonPublicChatMessageReactionBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.CommonPublicChatMessageReaction.Builder getCommonPublicChatMessageReactionBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.CommonPublicChatMessageReaction.newBuilder();
    }
}
