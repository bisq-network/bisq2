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
public final class ChannelSelectionStore implements PersistableStore<ChannelSelectionStore> {
    @Nullable
    @Setter
    private String selectedChannelId;

    public ChannelSelectionStore() {
    }

    private ChannelSelectionStore(String selectedChannelId) {
        this.selectedChannelId = selectedChannelId;
    }

    @Override
    public bisq.chat.protobuf.ChannelSelectionStore toProto() {
        bisq.chat.protobuf.ChannelSelectionStore.Builder builder = bisq.chat.protobuf.ChannelSelectionStore.newBuilder();
        Optional.ofNullable(selectedChannelId).ifPresent(builder::setSelectedChannelId);
        return builder.build();
    }

    public static ChannelSelectionStore fromProto(bisq.chat.protobuf.ChannelSelectionStore proto) {
        return new ChannelSelectionStore(proto.hasSelectedChannelId() ? proto.getSelectedChannelId() : null);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.ChannelSelectionStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(ChannelSelectionStore persisted) {
        this.selectedChannelId = persisted.selectedChannelId;
    }

    @Override
    public ChannelSelectionStore getClone() {
        return new ChannelSelectionStore(selectedChannelId);
    }
}