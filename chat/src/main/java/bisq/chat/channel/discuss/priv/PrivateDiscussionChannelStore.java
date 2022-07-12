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

package bisq.chat.channel.discuss.priv;

import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class PrivateDiscussionChannelStore implements PersistableStore<PrivateDiscussionChannelStore> {
    private final ObservableSet<PrivateDiscussionChannel> channels = new ObservableSet<>();

    public PrivateDiscussionChannelStore() {
    }

    private PrivateDiscussionChannelStore(Set<PrivateDiscussionChannel> privateDiscussionChannels) {
        setAll(privateDiscussionChannels);
    }

    @Override
    public bisq.chat.protobuf.PrivateDiscussionChannelStore toProto() {
        bisq.chat.protobuf.PrivateDiscussionChannelStore.Builder builder = bisq.chat.protobuf.PrivateDiscussionChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(PrivateDiscussionChannel::toProto).collect(Collectors.toSet()));
        return builder.build();
    }

    public static PrivateDiscussionChannelStore fromProto(bisq.chat.protobuf.PrivateDiscussionChannelStore proto) {
        Set<PrivateDiscussionChannel> privateDiscussionChannels = proto.getChannelsList().stream()
                .map(e -> (PrivateDiscussionChannel) PrivateDiscussionChannel.fromProto(e))
                .collect(Collectors.toSet());
        return new PrivateDiscussionChannelStore(privateDiscussionChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PrivateDiscussionChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(PrivateDiscussionChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public PrivateDiscussionChannelStore getClone() {
        return new PrivateDiscussionChannelStore(channels);
    }

    public void setAll(Set<PrivateDiscussionChannel> privateDiscussionChannels) {
        this.channels.clear();
        this.channels.addAll(privateDiscussionChannels);
    }
}