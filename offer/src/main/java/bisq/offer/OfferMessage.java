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

import java.util.concurrent.TimeUnit;

@ToString
@Getter
@EqualsAndHashCode
public class OfferMessage implements DistributedData {
    public final static long TTL = TimeUnit.DAYS.toMillis(2);

    private final Offer<?, ?> offer;
    protected final MetaData metaData;

    public OfferMessage(Offer<?, ?> offer) {
        this(offer, new MetaData(OfferMessage.TTL, 100000, OfferMessage.class.getSimpleName()));
    }

    private OfferMessage(Offer<?, ?> offer, MetaData metaData) {
        this.offer = offer;
        this.metaData = metaData;
    }

    @Override
    public bisq.offer.protobuf.OfferMessage toProto() {
        return bisq.offer.protobuf.OfferMessage.newBuilder()
                .setOffer(offer.toProto())
                .setMetaData(metaData.toProto())
                .build();
    }

    public static OfferMessage fromProto(bisq.offer.protobuf.OfferMessage proto) {
        return new OfferMessage(Offer.fromProto(proto.getOffer()), MetaData.fromProto(proto.getMetaData()));
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
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return false;
    }
}