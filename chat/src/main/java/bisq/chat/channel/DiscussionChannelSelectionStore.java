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

import bisq.chat.message.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public final class DiscussionChannelSelectionStore implements PersistableStore<DiscussionChannelSelectionStore> {
    private final Observable<Channel<? extends ChatMessage>> selectedChannel = new Observable<>();

    public DiscussionChannelSelectionStore() {
    }

    private DiscussionChannelSelectionStore(Channel<? extends ChatMessage> selectedChannel) {
        setAll(selectedChannel);
    }

    @Override
    public bisq.chat.protobuf.DiscussionChannelSelectionStore toProto() {
        return bisq.chat.protobuf.DiscussionChannelSelectionStore.newBuilder()
                .setSelectedChannel(selectedChannel.get().toProto())
                .build();
    }

    public static DiscussionChannelSelectionStore fromProto(bisq.chat.protobuf.DiscussionChannelSelectionStore proto) {
        return new DiscussionChannelSelectionStore(Channel.fromProto(proto.getSelectedChannel()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.DiscussionChannelSelectionStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(DiscussionChannelSelectionStore persisted) {
        setAll(persisted.selectedChannel.get());
    }

    @Override
    public DiscussionChannelSelectionStore getClone() {
        return new DiscussionChannelSelectionStore(selectedChannel.get());
    }

    public void setAll(Channel<? extends ChatMessage> selectedChannel) {
        this.selectedChannel.set(selectedChannel);
    }
}