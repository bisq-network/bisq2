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

package bisq.chat.trade.priv;

import bisq.common.observable.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PrivateTradeChannelStore implements PersistableStore<PrivateTradeChannelStore> {
    private final ObservableArray<PrivateTradeChannel> channels = new ObservableArray<>();

    public PrivateTradeChannelStore() {
    }

    private PrivateTradeChannelStore(List<PrivateTradeChannel> privateTradeChannels) {
        setAll(privateTradeChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateTradeChannelStore toProto() {
        bisq.chat.protobuf.PrivateTradeChannelStore.Builder builder = bisq.chat.protobuf.PrivateTradeChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateTradeChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PrivateTradeChannelStore fromProto(bisq.chat.protobuf.PrivateTradeChannelStore proto) {
        List<PrivateTradeChannel> privateTradeChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateTradeChannel) PrivateTradeChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PrivateTradeChannelStore(privateTradeChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(bisq.common.util.ProtobufUtils.unpack(any, bisq.chat.protobuf.PrivateTradeChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateTradeChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateTradeChannelStore getClone() {
        return new PrivateTradeChannelStore(channels);
    }

    public void setAll(List<PrivateTradeChannel> privateTradeChannels) {
        this.channels.clear();
        this.channels.addAll(privateTradeChannels);
    }
}