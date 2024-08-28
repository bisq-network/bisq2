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

package bisq.chat.common;

import bisq.common.observable.collection.ObservableArray;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.ArrayList;
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
    public bisq.chat.protobuf.CommonPublicChatChannelStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.CommonPublicChatChannelStore.newBuilder()
                .addAllChannels(channels.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.chat.protobuf.CommonPublicChatChannelStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static CommonPublicChatChannelStore fromProto(bisq.chat.protobuf.CommonPublicChatChannelStore proto) {
        List<CommonPublicChatChannel> privateDiscussionChannels = proto.getChannelsList().stream()
                .map(e -> (CommonPublicChatChannel) CommonPublicChatChannel.fromProto(e))
                .collect(Collectors.toList());
        return new CommonPublicChatChannelStore(privateDiscussionChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.CommonPublicChatChannelStore.class));
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
        return new CommonPublicChatChannelStore(new ArrayList<>(channels));
    }

    public void setAll(List<CommonPublicChatChannel> privateDiscussionChannels) {
        this.channels.setAll(privateDiscussionChannels);
    }
}