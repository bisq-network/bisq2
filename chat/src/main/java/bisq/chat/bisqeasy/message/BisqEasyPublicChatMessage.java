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

package bisq.chat.bisqeasy.message;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.Citation;
import bisq.chat.message.PublicChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_10_000;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyPublicChatMessage extends PublicChatMessage implements BisqEasyOfferMessage {
    private final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_10_000);
    private final Optional<BisqEasyOffer> bisqEasyOffer;

    public BisqEasyPublicChatMessage(String channelId,
                                     String authorUserProfileId,
                                     Optional<BisqEasyOffer> bisqEasyOffer,
                                     Optional<String> text,
                                     Optional<Citation> citation,
                                     long date,
                                     boolean wasEdited) {
        this(StringUtils.createShortUid(),
                ChatChannelDomain.BISQ_EASY,
                channelId,
                authorUserProfileId,
                bisqEasyOffer,
                text,
                citation,
                date,
                wasEdited,
                ChatMessageType.TEXT);
    }

    private BisqEasyPublicChatMessage(String messageId,
                                      ChatChannelDomain chatChannelDomain,
                                      String channelId,
                                      String authorUserProfileId,
                                      Optional<BisqEasyOffer> bisqEasyOffer,
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
        this.bisqEasyOffer = bisqEasyOffer;

        // log.error("{} {}", metaData.getClassName(), toProto().getSerializedSize()); //768
    }

    public bisq.chat.protobuf.ChatMessage toProto() {
        bisq.chat.protobuf.BisqEasyPublicChatMessage.Builder builder = bisq.chat.protobuf.BisqEasyPublicChatMessage.newBuilder();
        bisqEasyOffer.ifPresent(e -> builder.setBisqEasyOffer(e.toProto()));
        return getChatMessageBuilder().setPublicBisqEasyOfferChatMessage(builder).build();
    }

    public static BisqEasyPublicChatMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        Optional<String> text = baseProto.hasText() ?
                Optional.of(baseProto.getText()) :
                Optional.empty();
        Optional<BisqEasyOffer> bisqEasyOffer = baseProto.getPublicBisqEasyOfferChatMessage().hasBisqEasyOffer() ?
                Optional.of(BisqEasyOffer.fromProto(baseProto.getPublicBisqEasyOfferChatMessage().getBisqEasyOffer())) :
                Optional.empty();
        return new BisqEasyPublicChatMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                baseProto.getAuthorUserProfileId(),
                bisqEasyOffer,
                text,
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()));
    }

    @Override
    public boolean hasBisqEasyOffer() {
        return bisqEasyOffer.isPresent();
    }
}