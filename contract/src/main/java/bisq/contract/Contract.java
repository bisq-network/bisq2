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

package bisq.contract;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.offer.Offer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Defines the terms of the financial interaction with the counterparty/parties.
 */
@ToString
@Getter
@EqualsAndHashCode
public abstract class Contract<T extends Offer<?, ?>> implements NetworkProto {
    protected final long takeOfferDate;
    protected final T offer;
    protected final TradeProtocolType protocolType;

    protected transient final Party maker;

    public Contract(long takeOfferDate, T offer, TradeProtocolType protocolType) {
        this.takeOfferDate = takeOfferDate;
        this.offer = offer;
        this.protocolType = protocolType;
        this.maker = new Party(Role.MAKER, offer.getMakerNetworkId());
    }

    @Override
    public abstract bisq.contract.protobuf.Contract toProto();

    protected bisq.contract.protobuf.Contract.Builder getContractBuilder() {
        return bisq.contract.protobuf.Contract.newBuilder()
                .setTakeOfferDate(takeOfferDate)
                .setOffer(offer.toProto())
                .setTradeProtocolType(protocolType.toProto());
    }

    public static Contract<?> fromProto(bisq.contract.protobuf.Contract proto) {
        switch (proto.getMessageCase()) {
            case TWOPARTYCONTRACT: {
                return TwoPartyContract.fromProto(proto);
            }
            case MULTIPARTYCONTRACT: {
                return MultiPartyContract.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public abstract Party getTaker();

    /**
     * We need to provide a deterministic serialisation of our data (including all child objects).
     * Any collection must be deterministically sorted.
     * To use protobuf serialisation comes with some risks, but it can be assumed that version updates will
     * not break byte representation even protobuf does not provide such guarantees. Worst case, we need to stick with
     * the version before a breaking change happens or implement our own serialisation format (which comes with
     * considerable effort as it need to cover all the object path downwards).
     */
    public byte[] getHashForSignature() {
        return toProto().toByteArray();
    }
}
