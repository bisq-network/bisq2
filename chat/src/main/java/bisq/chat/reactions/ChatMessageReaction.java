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
import bisq.common.encoding.Hex;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public abstract class ChatMessageReaction implements DistributedData {
    public static String createId(String channelId, String messageId, String reactionId) {
        return channelId + "." + messageId + "." + reactionId;
    }

    private final String id;
    private final String userProfileId;
    private final String chatChannelId;
    private final ChatChannelDomain chatChannelDomain;
    private final String chatMessageId;
    private final long reactionId;
    private final boolean isRemoved;
    private final long date;

    public ChatMessageReaction(String id,
                               String userProfileId,
                               String chatChannelId,
                               ChatChannelDomain chatChannelDomain,
                               String chatMessageId,
                               long reactionId,
                               boolean isRemoved,
                               long date) {
        this.id = id;
        this.userProfileId = userProfileId;
        this.chatChannelId = chatChannelId;
        this.chatChannelDomain = chatChannelDomain;
        this.chatMessageId = chatMessageId;
        this.reactionId = reactionId;
        this.isRemoved = isRemoved;
        this.date = date;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        // AuthorId must be pubKeyHash. We get pubKeyHash passed from the data storage layer where the signature is
        // verified as well, so we can be sure it's the sender of the message. This check prevents against
        // impersonation attack.
        return !userProfileId.equals(Hex.encode(pubKeyHash));
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }

    @Override
    public void verify() {
        // TODO: Add CheckArgument that the reactionId exists
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateProfileId(userProfileId);
        NetworkDataValidation.validateText(chatChannelId, 200); // For private channels we combine user profile IDs for channelId
        NetworkDataValidation.validateDate(date);
    }

    @Override
    public bisq.chat.protobuf.ChatMessageReaction toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    protected bisq.chat.protobuf.ChatMessageReaction.Builder getChatMessageReactionBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.ChatMessageReaction.newBuilder()
                .setId(id)
                .setUserProfileId(userProfileId)
                .setChatChannelId(chatChannelId)
                .setChatChannelDomain(chatChannelDomain.toProtoEnum())
                .setChatMessageId(chatMessageId)
                .setReactionId(reactionId)
                .setIsRemoved(isRemoved)
                .setDate(date);
    }

    // PUBLIC API
    //getReactionsForMessage(String messageId, String channelId)
}
