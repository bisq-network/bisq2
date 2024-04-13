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

package bisq.contract.multisig;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.TwoPartyContract;
import bisq.network.identity.NetworkId;
import bisq.offer.multisig.MultisigOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class MultisigContract extends TwoPartyContract<MultisigOffer> {

    public MultisigContract(long takeOfferDate,
                            MultisigOffer offer,
                            NetworkId takerNetworkId) {
        this(takeOfferDate,
                offer,
                TradeProtocolType.MULTISIG,
                new Party(Role.TAKER, takerNetworkId));
    }

    private MultisigContract(long takeOfferDate,
                             MultisigOffer offer,
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
    public bisq.contract.protobuf.Contract.Builder getBuilder(boolean ignoreAnnotation) {
        var bisqEasyContract = bisq.contract.protobuf.MultisigContract.newBuilder();
        var twoPartyContract = getTwoPartyContractBuilder(ignoreAnnotation).setMultisigContract(bisqEasyContract);
        return getContractBuilder(ignoreAnnotation).setTwoPartyContract(twoPartyContract);
    }

    @Override
    public bisq.contract.protobuf.Contract toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static MultisigContract fromProto(bisq.contract.protobuf.Contract proto) {
        bisq.contract.protobuf.TwoPartyContract twoPartyContractProto = proto.getTwoPartyContract();
        return new MultisigContract(proto.getTakeOfferDate(),
                MultisigOffer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                Party.fromProto(twoPartyContractProto.getTaker()));
    }

}