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

package bisq.support;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public final class MediationResponse implements MailboxMessage {
    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(5),
            100000,
            MediationResponse.class.getSimpleName());

    private final BisqEasyOffer bisqEasyOffer;

    public MediationResponse(BisqEasyOffer bisqEasyOffer) {
        this.bisqEasyOffer = bisqEasyOffer;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toMediationResponseProto())))
                .build();
    }

    private bisq.support.protobuf.MediationResponse toMediationResponseProto() {
        return bisq.support.protobuf.MediationResponse.newBuilder()
                .setBisqEasyOffer(bisqEasyOffer.toProto())
                .build();
    }

    public static MediationResponse fromProto(bisq.support.protobuf.MediationResponse proto) {
        return new MediationResponse(BisqEasyOffer.fromProto(proto.getBisqEasyOffer()));
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MediationResponse proto = any.unpack(bisq.support.protobuf.MediationResponse.class);
                return MediationResponse.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}