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
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.contract.submarine.SubmarineContract;
import bisq.offer.Offer;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
public abstract class TwoPartyContract<T extends Offer<?, ?>> extends Contract<T> {
    protected final Party taker;

    public TwoPartyContract(long takeOfferDate, T offer, TradeProtocolType protocolType, Party taker) {
        super(takeOfferDate, offer, protocolType);
        this.taker = taker;
    }

    protected bisq.contract.protobuf.TwoPartyContract.Builder getTwoPartyContractBuilder(boolean serializeForHash) {
        return bisq.contract.protobuf.TwoPartyContract.newBuilder().setTaker(taker.toProto(serializeForHash));
    }

    protected bisq.contract.protobuf.TwoPartyContract toTwoPartyContract(boolean serializeForHash) {
        return resolveBuilder(getTwoPartyContractBuilder(serializeForHash), serializeForHash).build();
    }

    public static TwoPartyContract<?> fromProto(bisq.contract.protobuf.Contract proto) {
        return switch (proto.getTwoPartyContract().getMessageCase()) {
            case BISQEASYCONTRACT -> BisqEasyContract.fromProto(proto);
            case BISQMUSIGCONTRACT -> BisqMuSigContract.fromProto(proto);
            case SUBMARINECONTRACT -> SubmarineContract.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}