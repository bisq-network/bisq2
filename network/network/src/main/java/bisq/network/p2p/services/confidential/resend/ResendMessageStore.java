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

package bisq.network.p2p.services.confidential.resend;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public final class ResendMessageStore implements PersistableStore<ResendMessageStore> {
    private final Map<String, ResendMessageData> resendMessageDataByMessageId = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> numResendsByMessageId = new ConcurrentHashMap<>();

    ResendMessageStore() {
    }

    ResendMessageStore(Map<String, ResendMessageData> resendMessageDataByMessageId, Map<String, AtomicInteger> numResendsByMessageId) {
        this.resendMessageDataByMessageId.putAll(resendMessageDataByMessageId);
        this.numResendsByMessageId.putAll(numResendsByMessageId);
    }

    @Override
    public bisq.network.protobuf.ResendMessageStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.ResendMessageStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.ResendMessageStore.newBuilder()
                .putAllResendMessageDataByMessageId(resendMessageDataByMessageId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().toProto(serializeForHash))))
                .putAllNumResendsByMessageId(numResendsByMessageId.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> e.getValue().get())));
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.ResendMessageStore proto) {
        return new ResendMessageStore(proto.getResendMessageDataByMessageIdMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> ResendMessageData.fromProto(e.getValue()))),
                proto.getNumResendsByMessageIdMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> new AtomicInteger(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.network.protobuf.ResendMessageStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(ResendMessageStore persisted) {
        resendMessageDataByMessageId.clear();
        resendMessageDataByMessageId.putAll(persisted.getResendMessageDataByMessageId());
        numResendsByMessageId.clear();
        numResendsByMessageId.putAll(persisted.getNumResendsByMessageId());
    }

    @Override
    public ResendMessageStore getClone() {
        return new ResendMessageStore(new HashMap<>(resendMessageDataByMessageId), new HashMap<>(numResendsByMessageId));
    }

    Map<String, ResendMessageData> getResendMessageDataByMessageId() {
        return resendMessageDataByMessageId;
    }

    Map<String, AtomicInteger> getNumResendsByMessageId() {
        return numResendsByMessageId;
    }
}