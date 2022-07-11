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

package bisq.chat.channels;

import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class PublicDiscussionChannelStore implements PersistableStore<PublicDiscussionChannelStore> {
    private final ObservableSet<PublicDiscussionChannel> channels = new ObservableSet<>();

    public PublicDiscussionChannelStore() {
    }

    private PublicDiscussionChannelStore(Set<PublicDiscussionChannel> privateDiscussionChannels) {
        setAll(privateDiscussionChannels);
    }

    @Override
    public bisq.chat.protobuf.PublicDiscussionChannelStore toProto() {
        bisq.chat.protobuf.PublicDiscussionChannelStore.Builder builder = bisq.chat.protobuf.PublicDiscussionChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PublicDiscussionChannel::toProto).collect(Collectors.toSet()));
        return builder.build();
    }

    public static PublicDiscussionChannelStore fromProto(bisq.chat.protobuf.PublicDiscussionChannelStore proto) {
        Set<PublicDiscussionChannel> privateDiscussionChannels = proto.getChannelsList().stream()
                .map(e -> (PublicDiscussionChannel) PublicDiscussionChannel.fromProto(e))
                .collect(Collectors.toSet());
        return new PublicDiscussionChannelStore(privateDiscussionChannels);
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
    public void applyPersisted(PublicDiscussionChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PublicDiscussionChannelStore getClone() {
        return new PublicDiscussionChannelStore(channels);
    }

    public void setAll(Set<PublicDiscussionChannel> privateDiscussionChannels) {
        this.channels.clear();
        this.channels.addAll(privateDiscussionChannels);
    }
}