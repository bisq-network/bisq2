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
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class PrivateChatMessageReaction extends ChatMessageReaction implements MailboxMessage, ExternalNetworkMessage, AckRequestingMessage {
    @EqualsAndHashCode.Exclude
    protected final String receiverUserProfileId;
    protected final UserProfile senderUserProfile;
    @EqualsAndHashCode.Exclude
    protected final NetworkId receiverNetworkId;
    protected final boolean isRemoved;

    protected PrivateChatMessageReaction(String id,
                                         UserProfile senderUserProfile,
                                         String receiverUserProfileId,
                                         NetworkId receiverNetworkId,
                                         String chatChannelId,
                                         ChatChannelDomain chatChannelDomain,
                                         String chatMessageId,
                                         int reactionId,
                                         long date,
                                         boolean isRemoved) {
        super(id, senderUserProfile.getId(), chatChannelId, chatChannelDomain, chatMessageId, reactionId, date);

        this.receiverUserProfileId = receiverUserProfileId;
        this.senderUserProfile = senderUserProfile;
        this.receiverNetworkId = receiverNetworkId;
        this.isRemoved = isRemoved;

        NetworkDataValidation.validateProfileId(receiverUserProfileId);
    }

    @Override
    abstract public bisq.chat.protobuf.ChatMessageReaction.Builder getValueBuilder(boolean serializeForHash);

    @Override
    public bisq.chat.protobuf.ChatMessageReaction toValueProto(boolean serializeForHash) {
        return resolveBuilder(this.getValueBuilder(serializeForHash), serializeForHash).build();
    }

    @Override
    public NetworkId getSender() {
        return senderUserProfile.getNetworkId();
    }

    @Override
    public NetworkId getReceiver() {
        return receiverNetworkId;
    }
}
