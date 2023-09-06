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

package bisq.chat.bisqeasy.channel.open_trades;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class BisqEasyOpenTradeChatChannelStore implements PersistableStore<BisqEasyOpenTradeChatChannelStore> {
    private final ObservableArray<BisqEasyOpenTradeChatChannel> channels = new ObservableArray<>();

    public BisqEasyOpenTradeChatChannelStore() {
    }

    private BisqEasyOpenTradeChatChannelStore(List<BisqEasyOpenTradeChatChannel> privateTradeChannels) {
        setAll(privateTradeChannels);
    }

    @Override
    public bisq.chat.protobuf.BisqEasyPrivateTradeChatChannelStore toProto() {
        bisq.chat.protobuf.BisqEasyPrivateTradeChatChannelStore.Builder builder = bisq.chat.protobuf.BisqEasyPrivateTradeChatChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(BisqEasyOpenTradeChatChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static BisqEasyOpenTradeChatChannelStore fromProto(bisq.chat.protobuf.BisqEasyPrivateTradeChatChannelStore proto) {
        List<BisqEasyOpenTradeChatChannel> privateTradeChannels = proto.getChannelsList().stream()
                .map(e -> (BisqEasyOpenTradeChatChannel) BisqEasyOpenTradeChatChannel.fromProto(e))
                .collect(Collectors.toList());
        return new BisqEasyOpenTradeChatChannelStore(privateTradeChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.BisqEasyPrivateTradeChatChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(BisqEasyOpenTradeChatChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public BisqEasyOpenTradeChatChannelStore getClone() {
        return new BisqEasyOpenTradeChatChannelStore(channels);
    }

    public void setAll(List<BisqEasyOpenTradeChatChannel> privateTradeChannels) {
        this.channels.setAll(privateTradeChannels);
    }
}