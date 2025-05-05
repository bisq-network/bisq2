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

package bisq.chat.mu_sig.offerbook;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.mu_sig.MuSigOfferMessage;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.mu_sig.MuSigOffer;
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
public class MuSigOfferbookMessage extends PublicChatMessage implements MuSigOfferMessage {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, LOW_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_10_000);
    private final MuSigOffer muSigOffer;

    public MuSigOfferbookMessage(String channelId,
                                 String authorUserProfileId,
                                 MuSigOffer muSigOffer,
                                 long date) {
        this(StringUtils.createUid(),
                ChatChannelDomain.MU_SIG_OFFERBOOK,
                channelId,
                authorUserProfileId,
                muSigOffer,
                date,
                ChatMessageType.TEXT);
    }

    public MuSigOfferbookMessage(String messageId,
                                 ChatChannelDomain chatChannelDomain,
                                 String channelId,
                                 String authorUserProfileId,
                                 MuSigOffer muSigOffer,
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
        this.muSigOffer = muSigOffer;
    }

    @Override
    public bisq.chat.protobuf.ChatMessage.Builder getBuilder(boolean serializeForHash) {
        return getChatMessageBuilder(serializeForHash)
                .setMuSigOfferbookMessage(toMuSigOfferbookMessageProto(serializeForHash));
    }

    private bisq.chat.protobuf.MuSigOfferbookMessage toMuSigOfferbookMessageProto(boolean serializeForHash) {
        return resolveBuilder(getMuSigOfferbookMessageBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.MuSigOfferbookMessage.Builder getMuSigOfferbookMessageBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.MuSigOfferbookMessage.newBuilder()
                .setMuSigOffer(muSigOffer.toProto(serializeForHash));
    }

    public static MuSigOfferbookMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        return new MuSigOfferbookMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                baseProto.getAuthorUserProfileId(),
                MuSigOffer.fromProto(baseProto.getMuSigOfferbookMessage().getMuSigOffer()),
                baseProto.getDate(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()));
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }
}
