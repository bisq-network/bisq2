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

package bisq.chat.bisq_musig.offerbook;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.bisq_musig.BisqMusigOfferMessage;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.musig.MuSigOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.*;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BisqMusigOfferbookMessage extends PublicChatMessage implements BisqMusigOfferMessage {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, LOW_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_10_000);
    private final MuSigOffer musigOffer;

    public BisqMusigOfferbookMessage(String channelId,
                                     String authorUserProfileId,
                                     MuSigOffer musigOffer,
                                     long date) {
        this(StringUtils.createUid(),
                ChatChannelDomain.BISQ_MUSIG_OFFERBOOK,
                channelId,
                authorUserProfileId,
                musigOffer,
                date,
                ChatMessageType.TEXT);
    }

    public BisqMusigOfferbookMessage(String messageId,
                                     ChatChannelDomain chatChannelDomain,
                                     String channelId,
                                     String authorUserProfileId,
                                     MuSigOffer musigOffer,
                                     long date,
                                     ChatMessageType chatMessageType) {
        super(messageId,
                chatChannelDomain,
                channelId,
                authorUserProfileId,
                Optional.empty(),
                Optional.empty(),
                date,
                false,
                chatMessageType);
        this.musigOffer = musigOffer;
    }

    @Override
    public bisq.chat.protobuf.ChatMessage.Builder getBuilder(boolean serializeForHash) {
        return getChatMessageBuilder(serializeForHash)
                .setBisqMusigOfferbookMessage(toBisqMusigOfferbookMessageProto(serializeForHash));
    }

    private bisq.chat.protobuf.BisqMusigOfferbookMessage toBisqMusigOfferbookMessageProto(boolean serializeForHash) {
        return resolveBuilder(getBisqMusigOfferbookMessageBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.BisqMusigOfferbookMessage.Builder getBisqMusigOfferbookMessageBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.BisqMusigOfferbookMessage.newBuilder()
                .setBisqMusigOffer(musigOffer.toProto(serializeForHash));
    }

    public static BisqMusigOfferbookMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        return new BisqMusigOfferbookMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                baseProto.getAuthorUserProfileId(),
                MuSigOffer.fromProto(baseProto.getBisqMusigOfferbookMessage().getBisqMusigOffer()),
                baseProto.getDate(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()));
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }
}
