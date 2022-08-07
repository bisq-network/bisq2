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

package bisq.oracle.timestamp;

import bisq.common.data.StringLongPair;
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
public final class TimestampStore implements PersistableStore<TimestampStore> {
    @Getter
    private final Map<String, Long> timestampsByProfileId = new ConcurrentHashMap<>();

    public TimestampStore() {
    }

    private TimestampStore(Map<String, Long> timestampsByProfileId) {
        this.timestampsByProfileId.putAll(timestampsByProfileId);
    }

    @Override
    public bisq.oracle.protobuf.TimestampStore toProto() {
        return bisq.oracle.protobuf.TimestampStore.newBuilder()
                .addAllStringLongPairs(timestampsByProfileId.entrySet().stream()
                        .map(entry -> new StringLongPair(entry.getKey(), entry.getValue()))
                        .map(StringLongPair::toProto)
                        .collect(Collectors.toSet()))
                .build();
    }

    public static TimestampStore fromProto(bisq.oracle.protobuf.TimestampStore proto) {
        Map<String, Long> map = proto.getStringLongPairsList().stream()
                .map(StringLongPair::fromProto)
                .collect(Collectors.toMap(StringLongPair::getKey, StringLongPair::getValue));
        return new TimestampStore(map);
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.oracle.protobuf.TimestampStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public TimestampStore getClone() {
        return new TimestampStore(timestampsByProfileId);
    }

    @Override
    public void applyPersisted(TimestampStore persisted) {
        timestampsByProfileId.clear();
        timestampsByProfileId.putAll(persisted.getTimestampsByProfileId());
    }
}