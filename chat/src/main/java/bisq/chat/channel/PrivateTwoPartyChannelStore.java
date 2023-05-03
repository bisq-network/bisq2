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

package bisq.chat.channel;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PrivateTwoPartyChannelStore implements PersistableStore<PrivateTwoPartyChannelStore> {
    private final ObservableArray<PrivateTwoPartyChannel> channels = new ObservableArray<>();

    public PrivateTwoPartyChannelStore() {
    }

    private PrivateTwoPartyChannelStore(List<PrivateTwoPartyChannel> privateTwoPartyChannels) {
        setAll(privateTwoPartyChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateTwoPartyChannelStore toProto() {
        bisq.chat.protobuf.PrivateTwoPartyChannelStore.Builder builder = bisq.chat.protobuf.PrivateTwoPartyChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateTwoPartyChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PrivateTwoPartyChannelStore fromProto(bisq.chat.protobuf.PrivateTwoPartyChannelStore proto) {
        List<PrivateTwoPartyChannel> privateTwoPartyChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateTwoPartyChannel) PrivateTwoPartyChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PrivateTwoPartyChannelStore(privateTwoPartyChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PrivateTwoPartyChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateTwoPartyChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateTwoPartyChannelStore getClone() {
        return new PrivateTwoPartyChannelStore(channels);
    }

    public void setAll(List<PrivateTwoPartyChannel> privateTwoPartyChannels) {
        this.channels.clear();
        this.channels.addAll(privateTwoPartyChannels);
    }
}