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
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.contract.poc.MultiPartyContract;
import bisq.offer.Offer;
import lombok.Getter;

/**
 * Defines the terms of the financial interaction with the counterparty/parties.
 */
@Getter
public abstract class Contract<T extends Offer> implements Proto {
    protected final T offer;
    protected final TradeProtocolType protocolType;

    protected transient final Party maker;

    public Contract(T offer, TradeProtocolType protocolType) {
        this.offer = offer;
        this.protocolType = protocolType;
        this.maker = new Party(Role.MAKER, offer.getMakerNetworkId());
    }

    public bisq.contract.protobuf.Contract.Builder getContractBuilder() {
        return bisq.contract.protobuf.Contract.newBuilder()
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
}
