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

package bisq.chat.channel.priv;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class TwoPartyPrivateChatChannelStore implements PersistableStore<TwoPartyPrivateChatChannelStore> {
    private final ObservableArray<TwoPartyPrivateChatChannel> channels = new ObservableArray<>();

    public TwoPartyPrivateChatChannelStore() {
    }

    private TwoPartyPrivateChatChannelStore(List<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels) {
        setAll(twoPartyPrivateChatChannels);
    }

    @Override
    public bisq.chat.protobuf.TwoPartyPrivateChatChannelStore toProto() {
        bisq.chat.protobuf.TwoPartyPrivateChatChannelStore.Builder builder = bisq.chat.protobuf.TwoPartyPrivateChatChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(TwoPartyPrivateChatChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static TwoPartyPrivateChatChannelStore fromProto(bisq.chat.protobuf.TwoPartyPrivateChatChannelStore proto) {
        List<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels = proto.getChannelsList().stream()
                .map(e -> (TwoPartyPrivateChatChannel) TwoPartyPrivateChatChannel.fromProto(e))
                .collect(Collectors.toList());
        return new TwoPartyPrivateChatChannelStore(twoPartyPrivateChatChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.TwoPartyPrivateChatChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(TwoPartyPrivateChatChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public TwoPartyPrivateChatChannelStore getClone() {
        return new TwoPartyPrivateChatChannelStore(channels);
    }

    public void setAll(List<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels) {
        this.channels.clear();
        this.channels.addAll(twoPartyPrivateChatChannels);
    }
}