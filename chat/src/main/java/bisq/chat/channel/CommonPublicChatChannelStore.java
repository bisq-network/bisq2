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
public class CommonPublicChatChannelStore implements PersistableStore<CommonPublicChatChannelStore> {
    private final ObservableArray<CommonPublicChatChannel> channels = new ObservableArray<>();

    public CommonPublicChatChannelStore() {
    }

    private CommonPublicChatChannelStore(List<CommonPublicChatChannel> privateDiscussionChannels) {
        setAll(privateDiscussionChannels);
    }

    @Override
    public bisq.chat.protobuf.PublicChatChannelStore toProto() {
        bisq.chat.protobuf.PublicChatChannelStore.Builder builder = bisq.chat.protobuf.PublicChatChannelStore.newBuilder()
                .addAllChannels(channels.stream().map(CommonPublicChatChannel::toProto).collect(Collectors.toList()));
        return builder.build();
    }

    public static CommonPublicChatChannelStore fromProto(bisq.chat.protobuf.PublicChatChannelStore proto) {
        List<CommonPublicChatChannel> privateDiscussionChannels = proto.getChannelsList().stream()
                .map(e -> (CommonPublicChatChannel) CommonPublicChatChannel.fromProto(e))
                .collect(Collectors.toList());
        return new CommonPublicChatChannelStore(privateDiscussionChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.PublicChatChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(CommonPublicChatChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public CommonPublicChatChannelStore getClone() {
        return new CommonPublicChatChannelStore(channels);
    }

    public void setAll(List<CommonPublicChatChannel> privateDiscussionChannels) {
        this.channels.clear();
        this.channels.addAll(privateDiscussionChannels);
    }
}