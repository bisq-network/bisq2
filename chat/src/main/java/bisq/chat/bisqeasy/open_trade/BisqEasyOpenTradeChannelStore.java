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

package bisq.chat.bisqeasy.open_trade;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class BisqEasyOpenTradeChannelStore implements PersistableStore<BisqEasyOpenTradeChannelStore> {
    private final ObservableArray<BisqEasyOpenTradeChannel> channels = new ObservableArray<>();

    public BisqEasyOpenTradeChannelStore() {
    }

    private BisqEasyOpenTradeChannelStore(List<BisqEasyOpenTradeChannel> privateTradeChannels) {
        setAll(privateTradeChannels);
    }

    @Override
    public bisq.chat.protobuf.BisqEasyOpenTradeChannelStore toProto() {
        bisq.chat.protobuf.BisqEasyOpenTradeChannelStore.Builder builder = bisq.chat.protobuf.BisqEasyOpenTradeChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(BisqEasyOpenTradeChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static BisqEasyOpenTradeChannelStore fromProto(bisq.chat.protobuf.BisqEasyOpenTradeChannelStore proto) {
        List<BisqEasyOpenTradeChannel> privateTradeChannels = proto.getChannelsList().stream()
                .map(e -> (BisqEasyOpenTradeChannel) BisqEasyOpenTradeChannel.fromProto(e))
                .collect(Collectors.toList());
        return new BisqEasyOpenTradeChannelStore(privateTradeChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.BisqEasyOpenTradeChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(BisqEasyOpenTradeChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public BisqEasyOpenTradeChannelStore getClone() {
        return new BisqEasyOpenTradeChannelStore(channels);
    }

    public void setAll(List<BisqEasyOpenTradeChannel> privateTradeChannels) {
        this.channels.setAll(privateTradeChannels);
    }
}