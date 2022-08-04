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

import bisq.common.observable.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PrivateSupportChannelStore implements PersistableStore<PrivateSupportChannelStore> {
    private final ObservableArray<PrivateSupportChannel> channels = new ObservableArray<>();

    public PrivateSupportChannelStore() {
    }

    private PrivateSupportChannelStore(List<PrivateSupportChannel> privateSupportChannels) {
        setAll(privateSupportChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateSupportChannelStore toProto() {
        bisq.chat.protobuf.PrivateSupportChannelStore.Builder builder = bisq.chat.protobuf.PrivateSupportChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateSupportChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PrivateSupportChannelStore fromProto(bisq.chat.protobuf.PrivateSupportChannelStore proto) {
        List<PrivateSupportChannel> privateSupportChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateSupportChannel) PrivateSupportChannel.fromProto(e))
                .collect(Collectors.toList());
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

    public void setAll(List<PrivateSupportChannel> privateSupportChannels) {
        this.channels.clear();
        this.channels.addAll(privateSupportChannels);
    }
}