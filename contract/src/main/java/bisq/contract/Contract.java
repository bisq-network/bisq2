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
import bisq.common.validation.NetworkDataValidation;
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
    public void verify() {
        NetworkDataValidation.validateDate(takeOfferDate);
    }

    @Override
    public abstract bisq.contract.protobuf.Contract toProto(boolean serializeForHash);

    protected bisq.contract.protobuf.Contract.Builder getContractBuilder(boolean serializeForHash) {
        return bisq.contract.protobuf.Contract.newBuilder()
                .setTakeOfferDate(takeOfferDate)
                .setOffer(offer.toProto(serializeForHash))
                .setTradeProtocolType(protocolType.toProtoEnum());
    }

    public static Contract<?> fromProto(bisq.contract.protobuf.Contract proto) {
        return switch (proto.getMessageCase()) {
            case TWOPARTYCONTRACT -> TwoPartyContract.fromProto(proto);
            case MULTIPARTYCONTRACT -> MultiPartyContract.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    public abstract Party getTaker();
}
