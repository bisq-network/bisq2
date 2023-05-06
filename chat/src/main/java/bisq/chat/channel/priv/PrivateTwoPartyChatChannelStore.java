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
public class PrivateTwoPartyChatChannelStore implements PersistableStore<PrivateTwoPartyChatChannelStore> {
    private final ObservableArray<PrivateTwoPartyChatChannel> channels = new ObservableArray<>();

    public PrivateTwoPartyChatChannelStore() {
    }

    private PrivateTwoPartyChatChannelStore(List<PrivateTwoPartyChatChannel> privateTwoPartyChatChannels) {
        setAll(privateTwoPartyChatChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateTwoPartyChatChannelStore toProto() {
        bisq.chat.protobuf.PrivateTwoPartyChatChannelStore.Builder builder = bisq.chat.protobuf.PrivateTwoPartyChatChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateTwoPartyChatChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PrivateTwoPartyChatChannelStore fromProto(bisq.chat.protobuf.PrivateTwoPartyChatChannelStore proto) {
        List<PrivateTwoPartyChatChannel> privateTwoPartyChatChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateTwoPartyChatChannel) PrivateTwoPartyChatChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PrivateTwoPartyChatChannelStore(privateTwoPartyChatChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PrivateTwoPartyChatChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateTwoPartyChatChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateTwoPartyChatChannelStore getClone() {
        return new PrivateTwoPartyChatChannelStore(channels);
    }

    public void setAll(List<PrivateTwoPartyChatChannel> privateTwoPartyChatChannels) {
        this.channels.clear();
        this.channels.addAll(privateTwoPartyChatChannels);
    }
}