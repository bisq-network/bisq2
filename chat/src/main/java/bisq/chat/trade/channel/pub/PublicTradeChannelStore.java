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

package bisq.chat.trade.channel.pub;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class PublicTradeChannelStore implements PersistableStore<PublicTradeChannelStore> {
    private final ObservableArray<PublicTradeChannel> channels = new ObservableArray<>();
    private final ObservableSet<String> visibleChannelNames = new ObservableSet<>();

    public PublicTradeChannelStore() {
    }

    private PublicTradeChannelStore(List<PublicTradeChannel> privateTradeChannels,
                                    Set<String> visibleChannelNames) {
        setAll(privateTradeChannels, visibleChannelNames);
    }

    @Override
    public bisq.chat.protobuf.PublicTradeChannelStore toProto() {
        bisq.chat.protobuf.PublicTradeChannelStore.Builder builder = bisq.chat.protobuf.PublicTradeChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PublicTradeChannel::toProto).collect(Collectors.toList()))
                .addAllVisibleChannelNames(visibleChannelNames);
        return builder.build();
    }

    public static PublicTradeChannelStore fromProto(bisq.chat.protobuf.PublicTradeChannelStore proto) {
        List<PublicTradeChannel> privateTradeChannels = proto.getChannelsList().stream()
                .map(e -> (PublicTradeChannel) PublicTradeChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PublicTradeChannelStore(privateTradeChannels, new HashSet<>(proto.getVisibleChannelNamesList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PublicTradeChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PublicTradeChannelStore chatStore) {
        setAll(chatStore.getChannels(), chatStore.getVisibleChannelNames());
    }

    @Override
    public PublicTradeChannelStore getClone() {
        return new PublicTradeChannelStore(channels, visibleChannelNames);
    }

    public void setAll(List<PublicTradeChannel> privateTradeChannels, Set<String> visibleChannelIds) {
        this.channels.clear();
        this.channels.addAll(privateTradeChannels);
        this.visibleChannelNames.clear();
        this.visibleChannelNames.addAll(visibleChannelIds);
    }
}