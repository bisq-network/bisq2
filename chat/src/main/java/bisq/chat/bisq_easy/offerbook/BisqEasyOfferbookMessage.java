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

package bisq.chat.bisq_easy.offerbook;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.bisq_easy.BisqEasyOfferMessage;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.bisq_easy.BisqEasyOffer;
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
public final class BisqEasyOfferbookMessage extends PublicChatMessage implements BisqEasyOfferMessage {
    public static final long BISQ_EASY_OFFERBOOK_MESSAGE_TTL = TTL_10_DAYS;

    /**
     * Bump this when adding new PaymentMethodSpec types or other breaking offer
     * features. Receivers that see minSupportedVersion > CURRENT_VERSION will
     * skip the offer gracefully instead of crashing.
     */
    public static final int CURRENT_VERSION = 1;

    // MetaData needs to be symmetric with BisqEasyOfferbookMessageReaction.
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(BISQ_EASY_OFFERBOOK_MESSAGE_TTL, LOW_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_10_000);
    private final Optional<BisqEasyOffer> bisqEasyOffer;
    private final int minSupportedVersion;

    /**
     * Set during deserialization when an offer was skipped because its
     * minSupportedVersion exceeds CURRENT_VERSION or because the offer
     * proto could not be parsed (try/catch safety net).
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private transient boolean unsupportedOffer;

    public BisqEasyOfferbookMessage(String channelId,
                                    String authorUserProfileId,
                                    Optional<BisqEasyOffer> bisqEasyOffer,
                                    Optional<String> text,
                                    Optional<Citation> citation,
                                    long date,
                                    boolean wasEdited) {
        this(channelId, authorUserProfileId, bisqEasyOffer, text, citation, date, wasEdited, 0);
    }

    public BisqEasyOfferbookMessage(String channelId,
                                    String authorUserProfileId,
                                    Optional<BisqEasyOffer> bisqEasyOffer,
                                    Optional<String> text,
                                    Optional<Citation> citation,
                                    long date,
                                    boolean wasEdited,
                                    int minSupportedVersion) {
        this(StringUtils.createUid(),
                ChatChannelDomain.BISQ_EASY_OFFERBOOK,
                channelId,
                authorUserProfileId,
                bisqEasyOffer,
                text,
                citation,
                date,
                wasEdited,
                ChatMessageType.TEXT,
                minSupportedVersion);
    }

    public BisqEasyOfferbookMessage(String messageId,
                                     ChatChannelDomain chatChannelDomain,
                                     String channelId,
                                     String authorUserProfileId,
                                     Optional<BisqEasyOffer> bisqEasyOffer,
                                     Optional<String> text,
                                     Optional<Citation> citation,
                                     long date,
                                     boolean wasEdited,
                                     ChatMessageType chatMessageType) {
        this(messageId, chatChannelDomain, channelId, authorUserProfileId,
                bisqEasyOffer, text, citation, date, wasEdited, chatMessageType, 0);
    }

    public BisqEasyOfferbookMessage(String messageId,
                                     ChatChannelDomain chatChannelDomain,
                                     String channelId,
                                     String authorUserProfileId,
                                     Optional<BisqEasyOffer> bisqEasyOffer,
                                     Optional<String> text,
                                     Optional<Citation> citation,
                                     long date,
                                     boolean wasEdited,
                                     ChatMessageType chatMessageType,
                                     int minSupportedVersion) {
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
        this.minSupportedVersion = minSupportedVersion;
    }

    @Override
    public bisq.chat.protobuf.ChatMessage.Builder getBuilder(boolean serializeForHash) {
        return getChatMessageBuilder(serializeForHash)
                .setBisqEasyOfferbookMessage(toBisqEasyOfferbookMessageProto(serializeForHash));
    }

    private bisq.chat.protobuf.BisqEasyOfferbookMessage toBisqEasyOfferbookMessageProto(boolean serializeForHash) {
        return resolveBuilder(getBisqEasyOfferbookMessageBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.chat.protobuf.BisqEasyOfferbookMessage.Builder getBisqEasyOfferbookMessageBuilder(boolean serializeForHash) {
        bisq.chat.protobuf.BisqEasyOfferbookMessage.Builder builder = bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder();
        bisqEasyOffer.ifPresent(e -> builder.setBisqEasyOffer(e.toProto(serializeForHash)));
        if (minSupportedVersion > 0) {
            builder.setMinSupportedVersion(minSupportedVersion);
        }
        return builder;
    }

    public static BisqEasyOfferbookMessage fromProto(bisq.chat.protobuf.ChatMessage baseProto) {
        Optional<Citation> citation = baseProto.hasCitation() ?
                Optional.of(Citation.fromProto(baseProto.getCitation())) :
                Optional.empty();
        Optional<String> text = baseProto.hasText() ?
                Optional.of(baseProto.getText()) :
                Optional.empty();

        bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg = baseProto.getBisqEasyOfferbookMessage();
        int minVersion = offerbookMsg.hasMinSupportedVersion()
                ? offerbookMsg.getMinSupportedVersion()
                : 0;

        Optional<BisqEasyOffer> bisqEasyOffer;
        boolean skippedOffer = false;

        // --- Explicit version gate: don't even try if the sender requires a newer version ---
        if (minVersion > CURRENT_VERSION) {
            log.warn("Offer requires version {} but we only support {}. Skipping offer from message {}.",
                    minVersion, CURRENT_VERSION, baseProto.getId());
            bisqEasyOffer = Optional.empty();
            skippedOffer = true;
        } else if (offerbookMsg.hasBisqEasyOffer()) {
            // --- Try/catch safety net: catches corrupted payloads or unknown spec types
            //     even when the version check passes ---
            try {
                bisqEasyOffer = Optional.of(BisqEasyOffer.fromProto(offerbookMsg.getBisqEasyOffer()));
            } catch (Exception e) {
                log.warn("Failed to deserialize BisqEasyOffer despite version {} <= {}. Skipping: {}",
                        minVersion, CURRENT_VERSION, e.getMessage());
                bisqEasyOffer = Optional.empty();
                skippedOffer = true;
            }
        } else {
            bisqEasyOffer = Optional.empty();
        }

        BisqEasyOfferbookMessage message = new BisqEasyOfferbookMessage(
                baseProto.getId(),
                ChatChannelDomain.fromProto(baseProto.getChatChannelDomain()),
                baseProto.getChannelId(),
                baseProto.getAuthorUserProfileId(),
                bisqEasyOffer,
                text,
                citation,
                baseProto.getDate(),
                baseProto.getWasEdited(),
                ChatMessageType.fromProto(baseProto.getChatMessageType()),
                minVersion);
        message.unsupportedOffer = skippedOffer;
        return message;
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }

    @Override
    public boolean hasBisqEasyOffer() {
        return bisqEasyOffer.isPresent();
    }

    public boolean isUnsupportedOffer() {
        return unsupportedOffer;
    }

    @Override
    public boolean canShowReactions() {
        return bisqEasyOffer.isEmpty();
    }
}
