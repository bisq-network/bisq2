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

package bisq.chat.two_party;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class TwoPartyPrivateChatChannelStore implements PersistableStore<TwoPartyPrivateChatChannelStore> {
    private final ObservableSet<TwoPartyPrivateChatChannel> channels = new ObservableSet<>();

    TwoPartyPrivateChatChannelStore() {
    }

    TwoPartyPrivateChatChannelStore(Set<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels) {
        setAll(twoPartyPrivateChatChannels);
    }

    @Override
    public bisq.chat.protobuf.TwoPartyPrivateChatChannelStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.TwoPartyPrivateChatChannelStore.newBuilder()
                .addAllChannels(channels.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.chat.protobuf.TwoPartyPrivateChatChannelStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static TwoPartyPrivateChatChannelStore fromProto(bisq.chat.protobuf.TwoPartyPrivateChatChannelStore proto) {
        Set<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels = proto.getChannelsList().stream()
                .map(e -> (TwoPartyPrivateChatChannel) TwoPartyPrivateChatChannel.fromProto(e))
                .collect(Collectors.toSet());
        return new TwoPartyPrivateChatChannelStore(twoPartyPrivateChatChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.TwoPartyPrivateChatChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(TwoPartyPrivateChatChannelStore persisted) {
        setAll(persisted.getChannels());
    }

    @Override
    public TwoPartyPrivateChatChannelStore getClone() {
        return new TwoPartyPrivateChatChannelStore(new HashSet<>(channels));
    }

    public void setAll(Set<TwoPartyPrivateChatChannel> twoPartyPrivateChatChannels) {
        this.channels.setAll(twoPartyPrivateChatChannels);
    }
}