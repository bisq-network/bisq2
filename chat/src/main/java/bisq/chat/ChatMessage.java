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

package bisq.chat;

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trade.BisqEasyOpenTradeMessage;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.proto.Proto;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Optional;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public abstract class ChatMessage implements Proto, Comparable<ChatMessage> {
    public static final int MAX_TEXT_LENGTH = 10_000;

    protected final String id;
    private final ChatChannelDomain chatChannelDomain;
    protected final String channelId;
    protected final Optional<String> optionalText;
    protected String authorUserProfileId;
    protected final Optional<Citation> citation;
    protected final long date;
    protected final boolean wasEdited;
    protected final ChatMessageType chatMessageType;

    protected ChatMessage(String id,
                          ChatChannelDomain chatChannelDomain,
                          String channelId,
                          String authorUserProfileId,
                          Optional<String> text,
                          Optional<Citation> citation,
                          long date,
                          boolean wasEdited,
                          ChatMessageType chatMessageType) {
        this.id = id;
        this.chatChannelDomain = chatChannelDomain;
        this.channelId = channelId;
        this.authorUserProfileId = authorUserProfileId;
        this.optionalText = text.map(e -> StringUtils.truncate(e, MAX_TEXT_LENGTH - 10));
        this.citation = citation;
        this.date = date;
        this.wasEdited = wasEdited;
        this.chatMessageType = chatMessageType;

        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateText(channelId, 200); // For private channels we combine user profile IDs for channelId
        NetworkDataValidation.validateProfileId(authorUserProfileId);
        NetworkDataValidation.validateText(text, MAX_TEXT_LENGTH);
        NetworkDataValidation.validateDate(date);
    }

    public bisq.chat.protobuf.ChatMessage.Builder getChatMessageBuilder() {
        bisq.chat.protobuf.ChatMessage.Builder builder = bisq.chat.protobuf.ChatMessage.newBuilder()
                .setId(id)
                .setChatChannelDomain(chatChannelDomain.toProto())
                .setChannelId(channelId)
                .setAuthorUserProfileId(authorUserProfileId)
                .setDate(date)
                .setWasEdited(wasEdited)
                .setChatMessageType(chatMessageType.toProto());
        citation.ifPresent(citation -> builder.setCitation(citation.toProto()));
        optionalText.ifPresent(builder::setText);
        return builder;
    }

    public static ChatMessage fromProto(bisq.chat.protobuf.ChatMessage proto) {
        switch (proto.getMessageCase()) {
            case TWOPARTYPRIVATECHATMESSAGE: {
                return TwoPartyPrivateChatMessage.fromProto(proto);
            }

            case BISQEASYOFFERBOOKMESSAGE: {
                return BisqEasyOfferbookMessage.fromProto(proto);
            }
            case BISQEASYOPENTRADEMESSAGE: {
                return BisqEasyOpenTradeMessage.fromProto(proto);
            }

            case COMMONPUBLICCHATMESSAGE: {
                return CommonPublicChatMessage.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static ProtoResolver<DistributedData> getDistributedDataResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessage proto = any.unpack(bisq.chat.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case BISQEASYOFFERBOOKMESSAGE: {
                        return BisqEasyOfferbookMessage.fromProto(proto);
                    }
                    case COMMONPUBLICCHATMESSAGE: {
                        return CommonPublicChatMessage.fromProto(proto);
                    }
                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public static ProtoResolver<NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.chat.protobuf.ChatMessage proto = any.unpack(bisq.chat.protobuf.ChatMessage.class);
                switch (proto.getMessageCase()) {
                    case TWOPARTYPRIVATECHATMESSAGE: {
                        return TwoPartyPrivateChatMessage.fromProto(proto);
                    }

                    case BISQEASYOPENTRADEMESSAGE: {
                        return BisqEasyOpenTradeMessage.fromProto(proto);
                    }

                    case MESSAGE_NOT_SET: {
                        throw new UnresolvableProtobufMessageException(proto);
                    }
                }
                throw new UnresolvableProtobufMessageException(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public String getText() {
        return optionalText.orElse(Res.get("data.na"));
    }

    public boolean wasMentioned(UserIdentity userIdentity) {
        return getText().contains("@" + userIdentity.getUserName());
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - getDate() > getMetaData().getTtl());
    }

    protected abstract MetaData getMetaData();

    public boolean isMyMessage(UserIdentityService userIdentityService) {
        return userIdentityService.isUserIdentityPresent(authorUserProfileId);
    }

    @Override
    public int compareTo(@Nonnull ChatMessage o) {
        return id.compareTo(o.getId());
    }
}