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

package bisq.offer;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_2_DAYS;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public final class OfferMessage implements DistributedData {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_2_DAYS, getClass().getSimpleName());
    private final Offer<?, ?> offer;

    public OfferMessage(Offer<?, ?> offer) {
        this.offer = offer;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.offer.protobuf.OfferMessage.Builder getBuilder(boolean serializeForHash) {
        return bisq.offer.protobuf.OfferMessage.newBuilder()
                .setOffer(offer.toProto(serializeForHash));
    }

    @Override
    public bisq.offer.protobuf.OfferMessage toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static OfferMessage fromProto(bisq.offer.protobuf.OfferMessage proto) {
        return new OfferMessage(Offer.fromProto(proto.getOffer()));
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return OfferMessage.fromProto(any.unpack(bisq.offer.protobuf.OfferMessage.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }
}