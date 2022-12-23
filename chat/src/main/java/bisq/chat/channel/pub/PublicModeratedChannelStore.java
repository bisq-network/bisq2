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

package bisq.chat.channel.pub;

import bisq.common.observable.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PublicModeratedChannelStore implements PersistableStore<PublicModeratedChannelStore> {
    private final ObservableArray<PublicModeratedChannel> channels = new ObservableArray<>();

    public PublicModeratedChannelStore() {
    }

    private PublicModeratedChannelStore(List<PublicModeratedChannel> privateDiscussionChannels) {
        setAll(privateDiscussionChannels);
    }

    @Override
    public bisq.chat.protobuf.PublicDiscussionChannelStore toProto() {
        bisq.chat.protobuf.PublicDiscussionChannelStore.Builder builder = bisq.chat.protobuf.PublicDiscussionChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PublicModeratedChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static PublicModeratedChannelStore fromProto(bisq.chat.protobuf.PublicDiscussionChannelStore proto) {
        List<PublicModeratedChannel> privateDiscussionChannels = proto.getChannelsList().stream()
                .map(e -> (PublicModeratedChannel) PublicModeratedChannel.fromProto(e))
                .collect(Collectors.toList());
        return new PublicModeratedChannelStore(privateDiscussionChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PublicDiscussionChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PublicModeratedChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PublicModeratedChannelStore getClone() {
        return new PublicModeratedChannelStore(channels);
    }

    public void setAll(List<PublicModeratedChannel> privateDiscussionChannels) {
        this.channels.clear();
        this.channels.addAll(privateDiscussionChannels);
    }
}