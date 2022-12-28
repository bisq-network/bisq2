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

package bisq.chat.support;

import bisq.chat.channel.Channel;
import bisq.chat.message.ChatMessage;
import bisq.common.observable.Observable;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public final class SupportChannelSelectionStore implements PersistableStore<SupportChannelSelectionStore> {
    private final Observable<Channel<? extends ChatMessage>> selectedChannel = new Observable<>();

    public SupportChannelSelectionStore() {
    }

    private SupportChannelSelectionStore(Channel<? extends ChatMessage> selectedChannel) {
        this.selectedChannel.set(selectedChannel);
    }

    @Override
    public bisq.chat.protobuf.SupportChannelSelectionStore toProto() {
        bisq.chat.protobuf.SupportChannelSelectionStore.Builder builder = bisq.chat.protobuf.SupportChannelSelectionStore.newBuilder();
        Optional.ofNullable(selectedChannel.get()).ifPresent(selectedChannel -> builder.setSelectedChannel(selectedChannel.toProto()));
        return builder.build();
    }

    public static SupportChannelSelectionStore fromProto(bisq.chat.protobuf.SupportChannelSelectionStore proto) {
        return new SupportChannelSelectionStore(proto.hasSelectedChannel() ? Channel.fromProto(proto.getSelectedChannel()) : null);
    }


    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(bisq.common.util.ProtobufUtils.unpack(any, bisq.chat.protobuf.SupportChannelSelectionStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(SupportChannelSelectionStore persisted) {
        this.selectedChannel.set(persisted.selectedChannel.get());
    }

    @Override
    public SupportChannelSelectionStore getClone() {
        return new SupportChannelSelectionStore(selectedChannel.get());
    }
}