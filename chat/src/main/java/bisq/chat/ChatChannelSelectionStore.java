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

package bisq.chat;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;

@Slf4j
@Getter
public final class ChatChannelSelectionStore implements PersistableStore<ChatChannelSelectionStore> {
    @Nullable
    @Setter
    private String selectedChannelId;

    public ChatChannelSelectionStore() {
    }

    private ChatChannelSelectionStore(@Nullable String selectedChannelId) {
        this.selectedChannelId = selectedChannelId;
    }

    @Override
    public bisq.chat.protobuf.ChatChannelSelectionStore.Builder getBuilder(boolean serializeForHash) {
        bisq.chat.protobuf.ChatChannelSelectionStore.Builder builder = bisq.chat.protobuf.ChatChannelSelectionStore.newBuilder();
        Optional.ofNullable(selectedChannelId).ifPresent(builder::setSelectedChannelId);
        return builder;
    }

    @Override
    public bisq.chat.protobuf.ChatChannelSelectionStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ChatChannelSelectionStore fromProto(bisq.chat.protobuf.ChatChannelSelectionStore proto) {
        return new ChatChannelSelectionStore(proto.hasSelectedChannelId() ? proto.getSelectedChannelId() : null);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.ChatChannelSelectionStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(ChatChannelSelectionStore persisted) {
        this.selectedChannelId = persisted.selectedChannelId;
    }

    @Override
    public ChatChannelSelectionStore getClone() {
        return new ChatChannelSelectionStore(selectedChannelId);
    }
}