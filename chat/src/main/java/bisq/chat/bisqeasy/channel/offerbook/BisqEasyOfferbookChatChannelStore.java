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

package bisq.chat.bisqeasy.channel.offerbook;

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
public class BisqEasyOfferbookChatChannelStore implements PersistableStore<BisqEasyOfferbookChatChannelStore> {
    private final ObservableArray<BisqEasyOfferbookChatChannel> channels = new ObservableArray<>();
    //todo not useful anymore. consider to remove it
    private final ObservableSet<String> visibleChannelIds = new ObservableSet<>();

    public BisqEasyOfferbookChatChannelStore() {
    }

    private BisqEasyOfferbookChatChannelStore(List<BisqEasyOfferbookChatChannel> privateTradeChannels,
                                              Set<String> visibleChannelIds) {
        setAll(privateTradeChannels, visibleChannelIds);
    }

    @Override
    public bisq.chat.protobuf.BisqEasyPublicChatChannelStore toProto() {
        bisq.chat.protobuf.BisqEasyPublicChatChannelStore.Builder builder = bisq.chat.protobuf.BisqEasyPublicChatChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(BisqEasyOfferbookChatChannel::toProto).collect(Collectors.toList()))
                .addAllVisibleChannelIds(visibleChannelIds);
        return builder.build();
    }

    public static BisqEasyOfferbookChatChannelStore fromProto(bisq.chat.protobuf.BisqEasyPublicChatChannelStore proto) {
        List<BisqEasyOfferbookChatChannel> privateTradeChannels = proto.getChannelsList().stream()
                .map(e -> (BisqEasyOfferbookChatChannel) BisqEasyOfferbookChatChannel.fromProto(e))
                .collect(Collectors.toList());
        return new BisqEasyOfferbookChatChannelStore(privateTradeChannels, new HashSet<>(proto.getVisibleChannelIdsList()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.BisqEasyPublicChatChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(BisqEasyOfferbookChatChannelStore chatStore) {
        setAll(chatStore.getChannels(), chatStore.getVisibleChannelIds());
    }

    @Override
    public BisqEasyOfferbookChatChannelStore getClone() {
        return new BisqEasyOfferbookChatChannelStore(channels, visibleChannelIds);
    }

    public void setAll(List<BisqEasyOfferbookChatChannel> privateTradeChannels, Set<String> visibleChannelIds) {
        this.channels.setAll(privateTradeChannels);
        this.visibleChannelIds.clear();
        this.visibleChannelIds.addAll(visibleChannelIds);
    }
}