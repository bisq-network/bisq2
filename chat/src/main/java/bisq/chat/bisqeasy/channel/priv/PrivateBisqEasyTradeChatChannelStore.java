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

package bisq.chat.bisqeasy.channel.priv;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PrivateBisqEasyTradeChatChannelStore implements PersistableStore<PrivateBisqEasyTradeChatChannelStore> {
    private final ObservableArray<PrivateBisqEasyTradeChatChannel> channels = new ObservableArray<>();

    public PrivateBisqEasyTradeChatChannelStore() {
    }

    private PrivateBisqEasyTradeChatChannelStore(List<PrivateBisqEasyTradeChatChannel> privateTradeChannels) {
        setAll(privateTradeChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateBisqEasyTradeChatChannelStore toProto() {
        bisq.chat.protobuf.PrivateBisqEasyTradeChatChannelStore.Builder builder = bisq.chat.protobuf.PrivateBisqEasyTradeChatChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateBisqEasyTradeChatChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PrivateBisqEasyTradeChatChannelStore fromProto(bisq.chat.protobuf.PrivateBisqEasyTradeChatChannelStore proto) {
        List<PrivateBisqEasyTradeChatChannel> privateTradeChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateBisqEasyTradeChatChannel) PrivateBisqEasyTradeChatChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PrivateBisqEasyTradeChatChannelStore(privateTradeChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PrivateBisqEasyTradeChatChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateBisqEasyTradeChatChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateBisqEasyTradeChatChannelStore getClone() {
        return new PrivateBisqEasyTradeChatChannelStore(channels);
    }

    public void setAll(List<PrivateBisqEasyTradeChatChannel> privateTradeChannels) {
        this.channels.clear();
        this.channels.addAll(privateTradeChannels);
    }
}