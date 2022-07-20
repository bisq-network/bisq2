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

package bisq.chat.events.pub;

import bisq.chat.message.ChatMessage;
import bisq.chat.message.PublicChatMessage;
import bisq.chat.message.Quotation;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class PublicEventsChatMessage extends PublicChatMessage {
    public PublicEventsChatMessage(String channelId,
                                   String authorId,
                                   String text,
                                   Optional<Quotation> quotedMessage,
                                   long date,
                                   boolean wasEdited) {
        this(channelId,
                authorId,
                Optional.of(text),
                quotedMessage,
                date,
                wasEdited,
                new MetaData(ChatMessage.TTL, 100000, PublicEventsChatMessage.class.getSimpleName()));
    }

    private PublicEventsChatMessage(String channelId,
                                    String authorId,
                                    Optional<String> text,
                                    Optional<Quotation> quotedMessage,
                                    long date,
                                    boolean wasEdited,
                                    MetaData metaData) {
        super(channelId,
                authorId,
                text,
                quotedMessage,
                date,
                wasEdited,
                metaData);
    }

    public bisq.chat.protobuf.ChatMessage toProto() {
        return getChatMessageBuilder().setPublicEventsChatMessage(bisq.chat.protobuf.PublicEventsChatMessage.newBuilder()).build();
    }

    public static PublicEventsChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Quotation> quotedMessage = baseProto.hasQuotation() ?
                Optional.of(Quotation.fromProto(baseProto.getQuotation())) :
                Optional.empty();
        return new PublicEventsChatMessage(
                baseProto.getChannelId(),
                baseProto.getAuthorId(),
                Optional.of(baseProto.getText()),
                quotedMessage,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                MetaData.fromProto(baseProto.getMetaData()));
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}