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

package bisq.chat.common;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.*;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class CommonPublicChatMessage extends PublicChatMessage {
    public static final long COMMON_PUBLIC_CHAT_MESSAGE_TTL = TTL_10_DAYS;

    // MetaData needs to be symmetric with CommonPublicChatMessageReaction.
    public static final MetaData META_DATA = new MetaData(COMMON_PUBLIC_CHAT_MESSAGE_TTL, LOW_PRIORITY, CommonPublicChatMessage.class.getSimpleName(), MAX_MAP_SIZE_10_000);

    public CommonPublicChatMessage(ChatChannelDomain chatChannelDomain,
                                   String channelId,
                                   String authorUserProfileId,
                                   String text,
                                   Optional<Citation> citation,
                                   long date,
                                   boolean wasEdited) {
        this(StringUtils.createUid(),
                chatChannelDomain,
                channelId,
                authorUserProfileId,
                Optional.of(text),
                citation,
                date,
                wasEdited,
                ChatMessageType.TEXT);
    }

    public CommonPublicChatMessage(String messageId,
                                   ChatChannelDomain chatChannelDomain,
                                   String channelId,
                                   String authorUserProfileId,
                                   Optional<String> text,
                                   Optional<Citation> citation,
                                   long date,
                                   boolean wasEdited,
                                   ChatMessageType chatMessageType) {
        super(messageId,
                chatChannelDomain,
                channelId,
                authorUserProfileId,
                text,
                citation,
                date,
                wasEdited,
                chatMessageType);

        verify();
    }

    // Unmodified clients of all versions publish only in the channels of SubDomain. A node of version 2.1.13 or earlier
    // fails at its start when it stored a message whose channel ID it cannot resolve, so a node does not accept, store
    // or relay such a message (see docs/specifications/chat/message-types.md). The message names no value which it
    // rejects (rule 7 of docs/specifications/network/validation-at-parsing.md).
    @Override
    public void verify() {
        super.verify();
        checkArgument(SubDomain.find(channelId).isPresent(),
                "The channel ID does not name a channel of the discussion or support chat");
    }

    @Override
    public bisq.chat.protobuf.ChatMessage.Builder getBuilder(boolean serializeForHash) {
        return getChatMessageBuilder(serializeForHash)
                .setCommonPublicChatMessage(toCommonPublicChatMessageProto(serializeForHash));
    }

    private bisq.chat.protobuf.CommonPublicChatMessage toCommonPublicChatMessageProto(boolean serializeForHash) {
        return resolveBuilder(getCommonPublicChatMessageBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.CommonPublicChatMessage.Builder getCommonPublicChatMessageBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.CommonPublicChatMessage.newBuilder();
    }

    public static CommonPublicChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto)
            throws UnresolvableProtobufException {
        try {
            Optional<Citation> citation = baseProto.hasCitation()
                    ? Optional.of(Citation.fromProto(baseProto.getCitation()))
                    : Optional.empty();
            return new CommonPublicChatMessage(
                    baseProto.getId(),
                    ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                    baseProto.getChannelId(),
                    baseProto.getAuthorUserProfileId(),
                    Optional.of(baseProto.getText()),
                    citation,
                    baseProto.getDate(),
                    baseProto.getWasEdited(),
                    ChatMessageType.fromProto(baseProto.getChatMessageType()));
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    @Override
    public ChatChannelDomain getChatChannelDomain() {
        return chatChannelDomain.migrate();
    }

    // The sender chooses the channel ID, and isValid ignores a message whose ID cannot be migrated (hasValidChannelId).
    // The getter must still not throw for such an ID, because the generated equals, hashCode and toString call it
    // before any check, for example when a received message is put into a hash-based set.
    @Override
    public String getChannelId() {
        return SubDomain.find(channelId)
                .map(subDomain -> subDomain.migrate().getChannelId())
                .orElse(channelId);
    }

    @Override
    public MetaData getMetaData() {
        return META_DATA;
    }

    @Override
    public boolean hasValidChannelId() {
        return SubDomain.find(channelId).isPresent();
    }

    @Override
    public boolean canShowReactions() {
        return true;
    }
}
