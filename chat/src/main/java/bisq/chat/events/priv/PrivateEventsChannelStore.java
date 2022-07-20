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

package bisq.chat.events.priv;

import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class PrivateEventsChannelStore implements PersistableStore<PrivateEventsChannelStore> {
    private final ObservableSet<PrivateEventsChannel> channels = new ObservableSet<>();

    public PrivateEventsChannelStore() {
    }

    private PrivateEventsChannelStore(Set<PrivateEventsChannel> privateEventsChannels) {
        setAll(privateEventsChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateEventsChannelStore toProto() {
        bisq.chat.protobuf.PrivateEventsChannelStore.Builder builder = bisq.chat.protobuf.PrivateEventsChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateEventsChannel::toProto).collect(Collectors.toSet()));
        return builder.build();
    }

    public static PrivateEventsChannelStore fromProto(bisq.chat.protobuf.PrivateEventsChannelStore proto) {
        Set<PrivateEventsChannel> privateEventsChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateEventsChannel) PrivateEventsChannel.fromProto(e))
                .collect(Collectors.toSet());
        return new PrivateEventsChannelStore(privateEventsChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PrivateEventsChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateEventsChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateEventsChannelStore getClone() {
        return new PrivateEventsChannelStore(channels);
    }

    public void setAll(Set<PrivateEventsChannel> privateEventsChannels) {
        this.channels.clear();
        this.channels.addAll(privateEventsChannels);
    }
}