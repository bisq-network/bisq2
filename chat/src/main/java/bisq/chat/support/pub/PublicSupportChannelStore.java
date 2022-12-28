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

package bisq.chat.support.pub;

import bisq.common.observable.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PublicSupportChannelStore implements PersistableStore<PublicSupportChannelStore> {
    private final ObservableArray<PublicSupportChannel> channels = new ObservableArray<>();

    public PublicSupportChannelStore() {
    }

    private PublicSupportChannelStore(List<PublicSupportChannel> privateSupportChannels) {
        setAll(privateSupportChannels);
    }

    @Override
    public bisq.chat.protobuf.PublicSupportChannelStore toProto() {
        bisq.chat.protobuf.PublicSupportChannelStore.Builder builder = bisq.chat.protobuf.PublicSupportChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PublicSupportChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PublicSupportChannelStore fromProto(bisq.chat.protobuf.PublicSupportChannelStore proto) {
        List<PublicSupportChannel> privateSupportChannels = proto.getChannelsList().stream()
                .map(e -> (PublicSupportChannel) PublicSupportChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PublicSupportChannelStore(privateSupportChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(bisq.common.util.ProtobufUtils.unpack(any, bisq.chat.protobuf.PublicSupportChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PublicSupportChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PublicSupportChannelStore getClone() {
        return new PublicSupportChannelStore(channels);
    }

    public void setAll(List<PublicSupportChannel> privateSupportChannels) {
        this.channels.clear();
        this.channels.addAll(privateSupportChannels);
    }
}