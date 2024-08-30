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

package bisq.contract.submarine;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.TwoPartyContract;
import bisq.network.identity.NetworkId;
import bisq.offer.submarine.SubmarineOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class SubmarineContract extends TwoPartyContract<SubmarineOffer> {

    public SubmarineContract(long takeOfferDate,
                             SubmarineOffer offer,
                             NetworkId takerNetworkId) {
        this(takeOfferDate,
                offer,
                TradeProtocolType.SUBMARINE,
                new Party(Role.TAKER, takerNetworkId));
    }

    private SubmarineContract(long takeOfferDate,
                              SubmarineOffer offer,
                              TradeProtocolType protocolType,
                              Party taker) {
        super(takeOfferDate, offer, protocolType, taker);
        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.contract.protobuf.Contract.Builder getBuilder(boolean serializeForHash) {
        var bisqEasyContract = bisq.contract.protobuf.SubmarineContract.newBuilder();
        var twoPartyContract = getTwoPartyContractBuilder(serializeForHash).setSubmarineContract(bisqEasyContract);
        return getContractBuilder(serializeForHash).setTwoPartyContract(twoPartyContract);
    }

    @Override
    public bisq.contract.protobuf.Contract toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static SubmarineContract fromProto(bisq.contract.protobuf.Contract proto) {
        bisq.contract.protobuf.TwoPartyContract twoPartyContractProto = proto.getTwoPartyContract();
        return new SubmarineContract(proto.getTakeOfferDate(),
                SubmarineOffer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                Party.fromProto(twoPartyContractProto.getTaker()));
    }

}