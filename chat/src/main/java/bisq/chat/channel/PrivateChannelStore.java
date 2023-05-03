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
public class PrivateChannelStore implements PersistableStore<PrivateChannelStore> {
    private final ObservableArray<PrivateTwoPartyChannel> channels = new ObservableArray<>();

    public PrivateChannelStore() {
    }

    private PrivateChannelStore(List<PrivateTwoPartyChannel> privateTwoPartyChannels) {
        setAll(privateTwoPartyChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateChannelStore toProto() {
        bisq.chat.protobuf.PrivateChannelStore.Builder builder = bisq.chat.protobuf.PrivateChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateTwoPartyChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PrivateChannelStore fromProto(bisq.chat.protobuf.PrivateChannelStore proto) {
        List<PrivateTwoPartyChannel> privateTwoPartyChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateTwoPartyChannel) PrivateTwoPartyChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PrivateChannelStore(privateTwoPartyChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PrivateChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateChannelStore getClone() {
        return new PrivateChannelStore(channels);
    }

    public void setAll(List<PrivateTwoPartyChannel> privateTwoPartyChannels) {
        this.channels.clear();
        this.channels.addAll(privateTwoPartyChannels);
    }
}