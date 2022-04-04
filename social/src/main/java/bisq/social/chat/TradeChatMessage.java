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

package bisq.social.chat;

import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.social.intent.TradeIntent;
import bisq.social.user.ChatUser;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TradeChatMessage extends PublicChatMessage implements DistributedData {
    private final TradeIntent tradeIntent;

    public TradeChatMessage(String channelId,
                            ChatUser sender,
                            TradeIntent tradeIntent,
                            long date,
                            boolean wasEdited) {
        this(channelId,
                sender,
                tradeIntent,
                date,
                ChannelType.PUBLIC,
                wasEdited,
                new MetaData(TimeUnit.DAYS.toMillis(10), 100000, TradeChatMessage.class.getSimpleName()));
    }

    private TradeChatMessage(String channelId,
                             ChatUser sender,
                             TradeIntent tradeIntent,
                             long date,
                             ChannelType channelType,
                             boolean wasEdited,
                             MetaData metaData) {
        super(channelId,
                sender,
                Optional.empty(),
                Optional.empty(),
                date,
                channelType,
                wasEdited,
                metaData);
        this.tradeIntent = tradeIntent;
    }

    public bisq.social.protobuf.ChatMessage toProto() {
        return getChatMessageBuilder().setTradeChatMessage(
                        bisq.social.protobuf.TradeChatMessage.newBuilder()
                                .setTradeIntent(tradeIntent.toProto()))
                .build();
    }

    public static TradeChatMessage fromProto(bisq.social.protobuf.ChatMessage baseProto) {
        return new TradeChatMessage(
                baseProto.getChannelId(),
                ChatUser.fromProto(baseProto.getAuthor()),
                TradeIntent.fromProto(baseProto.getTradeChatMessage().getTradeIntent()),
                baseProto.getDate(),
                ChannelType.fromProto(baseProto.getChannelType()),
                baseProto.getWasEdited(),
                MetaData.fromProto(baseProto.getMetaData()));
    }

    @Override
    public String getText() {
        return tradeIntent.getChatMessageText();
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}