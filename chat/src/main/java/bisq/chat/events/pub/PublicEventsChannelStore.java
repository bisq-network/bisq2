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

package bisq.chat.events.pub;

import bisq.common.observable.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PublicEventsChannelStore implements PersistableStore<PublicEventsChannelStore> {
    private final ObservableArray<PublicEventsChannel> channels = new ObservableArray<>();

    public PublicEventsChannelStore() {
    }

    private PublicEventsChannelStore(List<PublicEventsChannel> privateEventsChannels) {
        setAll(privateEventsChannels);
    }

    @Override
    public bisq.chat.protobuf.PublicEventsChannelStore toProto() {
        bisq.chat.protobuf.PublicEventsChannelStore.Builder builder = bisq.chat.protobuf.PublicEventsChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PublicEventsChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PublicEventsChannelStore fromProto(bisq.chat.protobuf.PublicEventsChannelStore proto) {
        List<PublicEventsChannel> privateEventsChannels = proto.getChannelsList().stream()
                .map(e -> (PublicEventsChannel) PublicEventsChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PublicEventsChannelStore(privateEventsChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PublicEventsChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PublicEventsChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PublicEventsChannelStore getClone() {
        return new PublicEventsChannelStore(channels);
    }

    public void setAll(List<PublicEventsChannel> privateEventsChannels) {
        this.channels.clear();
        this.channels.addAll(privateEventsChannels);
    }
}