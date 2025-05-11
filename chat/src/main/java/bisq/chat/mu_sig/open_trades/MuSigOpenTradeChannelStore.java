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

package bisq.chat.mu_sig.open_trades;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class MuSigOpenTradeChannelStore implements PersistableStore<MuSigOpenTradeChannelStore> {
    private final ObservableSet<MuSigOpenTradeChannel> channels = new ObservableSet<>();

    private MuSigOpenTradeChannelStore(Set<MuSigOpenTradeChannel> privateTradeChannels) {
        setAll(privateTradeChannels);
    }

    @Override
    public bisq.chat.protobuf.MuSigOpenTradeChannelStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.MuSigOpenTradeChannelStore.newBuilder()
                .addAllChannels(channels.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.chat.protobuf.MuSigOpenTradeChannelStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MuSigOpenTradeChannelStore fromProto(bisq.chat.protobuf.MuSigOpenTradeChannelStore proto) {
        Set<MuSigOpenTradeChannel> privateTradeChannels = proto.getChannelsList().stream()
                .map(e -> (MuSigOpenTradeChannel) MuSigOpenTradeChannel.fromProto(e))
                .collect(Collectors.toSet());
        return new MuSigOpenTradeChannelStore(privateTradeChannels);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.chat.protobuf.MuSigOpenTradeChannelStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(MuSigOpenTradeChannelStore chatStore) {
        setAll(chatStore.getChannels());
    }

    @Override
    public MuSigOpenTradeChannelStore getClone() {
        return new MuSigOpenTradeChannelStore(new HashSet<>(channels));
    }

    void setAll(Set<MuSigOpenTradeChannel> privateTradeChannels) {
        this.channels.setAll(privateTradeChannels);
    }
}