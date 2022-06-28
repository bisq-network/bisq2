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

package bisq.oracle.ots;

import bisq.common.data.ByteArray;
import bisq.common.data.ByteArrayMapEntry;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class OpenTimestampStore implements PersistableStore<OpenTimestampStore> {
    @Getter
    private final Map<ByteArray, ByteArray> timestampByPubKeyHash = new ConcurrentHashMap<>();

    public OpenTimestampStore() {
    }

    private OpenTimestampStore(Map<ByteArray, ByteArray> timestampByPubKeyHash) {
        this.timestampByPubKeyHash.putAll(timestampByPubKeyHash);
    }

    @Override
    public bisq.oracle.protobuf.OpenTimestampStore toProto() {
        return bisq.oracle.protobuf.OpenTimestampStore.newBuilder()
                .addAllTimestampEntries(timestampByPubKeyHash.entrySet().stream()
                        .map(entry -> new ByteArrayMapEntry(entry.getKey(), entry.getValue()))
                        .map(ByteArrayMapEntry::toProto)
                        .collect(Collectors.toSet()))
                .build();
    }

    public static OpenTimestampStore fromProto(bisq.oracle.protobuf.OpenTimestampStore proto) {
        Map<ByteArray, ByteArray> timestampByPubKeyHash = proto.getTimestampEntriesList().stream()
                .map(ByteArrayMapEntry::fromProto)
                .collect(Collectors.toMap(ByteArrayMapEntry::getKey, ByteArrayMapEntry::getValue));
        return new OpenTimestampStore(timestampByPubKeyHash);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.oracle.protobuf.OpenTimestampStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public OpenTimestampStore getClone() {
        return new OpenTimestampStore(timestampByPubKeyHash);
    }

    @Override
    public void applyPersisted(OpenTimestampStore persisted) {
        timestampByPubKeyHash.clear();
        timestampByPubKeyHash.putAll(persisted.getTimestampByPubKeyHash());
    }
}