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

import java.util.HashSet;
import java.util.Set;

@Slf4j
final class ResendMessageStore implements PersistableStore<ResendMessageStore> {
    private final Set<ResendMessageData> resendMessageDataSet = new HashSet<>();

    ResendMessageStore() {
    }

    ResendMessageStore(Set<ResendMessageData> resendMessageDataSet) {
        this.resendMessageDataSet.addAll(resendMessageDataSet);
    }

    @Override
    public bisq.network.protobuf.MessageDeliveryStatusStore toProto() {
        return null; // TODO
    }

    public static PersistableStore<?> fromProto(bisq.network.protobuf.MessageDeliveryStatusStore proto) {
        return null;// TODO
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.network.protobuf.MessageDeliveryStatusStore.class)); // TODO
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void applyPersisted(ResendMessageStore persisted) {
        resendMessageDataSet.clear();
        resendMessageDataSet.addAll(persisted.getResendMessageDataSet());
    }

    @Override
    public ResendMessageStore getClone() {
        return new ResendMessageStore(resendMessageDataSet);
    }

    Set<ResendMessageData> getResendMessageDataSet() {
        return resendMessageDataSet;
    }
}