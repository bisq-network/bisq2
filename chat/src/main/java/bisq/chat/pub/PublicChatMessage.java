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

package bisq.chat.pub;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.p2p.services.data.storage.DistributedData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * PublicChatMessage is added as public data to the distributed network storage.
 */
@Getter
@Slf4j
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class PublicChatMessage extends ChatMessage implements DistributedData {
    protected transient final ObservableSet<ChatMessageReaction> chatMessageReactions = new ObservableSet<>();

    protected PublicChatMessage(String messageId,
                                ChatChannelDomain chatChannelDomain,
                                String channelId,
                                String authorUserProfileId,
                                Optional<String> text,
                                Optional<Citation> citation,
                                long date,
                                boolean wasEdited,
                                ChatMessageType chatMessageType) {
        super(messageId, chatChannelDomain, channelId, authorUserProfileId, text, citation, date, wasEdited, chatMessageType);
    }

    // We are part of other proto messages via DistributedData thus, toProto and getBuilder are our entry points
    @Override
    public bisq.chat.protobuf.ChatMessage toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    abstract public bisq.chat.protobuf.ChatMessage.Builder getBuilder(boolean serializeForHash);

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        // AuthorId must be pubKeyHash. We get pubKeyHash passed from the data storage layer where the signature is 
        // verified as well, so we can be sure it's the sender of the message. This check prevents against 
        // impersonation attack.
        return !authorUserProfileId.equals(Hex.encode(pubKeyHash));
    }

    @Override
    public void addChatMessageReaction(ChatMessageReaction reaction) {
        getChatMessageReactions().add(reaction);
    }
}
