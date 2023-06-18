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
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.contract.multisig.MultisigContract;
import bisq.contract.submarine.SubmarineContract;
import bisq.offer.Offer;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
public abstract class TwoPartyContract<T extends Offer<?, ?>> extends Contract<T> {
    protected final Party taker;

    public TwoPartyContract(T swapOffer, TradeProtocolType protocolType, Party taker) {
        super(swapOffer, protocolType);
        this.taker = taker;
    }

    protected bisq.contract.protobuf.TwoPartyContract.Builder getTwoPartyContractBuilder() {
        return bisq.contract.protobuf.TwoPartyContract.newBuilder().setTaker(taker.toProto());
    }

    public static TwoPartyContract<?> fromProto(bisq.contract.protobuf.Contract proto) {
        switch (proto.getTwoPartyContract().getMessageCase()) {
            case BISQEASYCONTRACT: {
                return BisqEasyContract.fromProto(proto);
            }
            case MULTISIGCONTRACT: {
                return MultisigContract.fromProto(proto);
            }
            case SUBMARINECONTRACT: {
                return SubmarineContract.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}