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

package bisq.chat.support.priv;

import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class PrivateSupportChannelStore implements PersistableStore<PrivateSupportChannelStore> {
    private final ObservableSet<PrivateSupportChannel> channels = new ObservableSet<>();

    public PrivateSupportChannelStore() {
    }

    private PrivateSupportChannelStore(Set<PrivateSupportChannel> privateSupportChannels) {
        setAll(privateSupportChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateSupportChannelStore toProto() {
        bisq.chat.protobuf.PrivateSupportChannelStore.Builder builder = bisq.chat.protobuf.PrivateSupportChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateSupportChannel::toProto).collect(Collectors.toSet()));
        return builder.build();
    }

    public static PrivateSupportChannelStore fromProto(bisq.chat.protobuf.PrivateSupportChannelStore proto) {
        Set<PrivateSupportChannel> privateSupportChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateSupportChannel) PrivateSupportChannel.fromProto(e))
                .collect(Collectors.toSet());
        return new PrivateSupportChannelStore(privateSupportChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PrivateSupportChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateSupportChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateSupportChannelStore getClone() {
        return new PrivateSupportChannelStore(channels);
    }

    public void setAll(Set<PrivateSupportChannel> privateSupportChannels) {
        this.channels.clear();
        this.channels.addAll(privateSupportChannels);
    }
}