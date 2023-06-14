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
import bisq.offer.Offer;
import lombok.Getter;

@Getter
public abstract class TwoPartyContract<T extends Offer<?, ?>> extends Contract<T> {
    protected final Party taker;

    public TwoPartyContract(T swapOffer, TradeProtocolType protocolType, Party taker) {
        super(swapOffer, protocolType);
        this.taker = taker;
    }

    @Override
    public bisq.contract.protobuf.Contract toProto() {
        return getContractBuilder().setTwoPartyContract(
                        bisq.contract.protobuf.TwoPartyContract.newBuilder()
                                .setTaker(taker.toProto()))
                .build();
    }

  /*  public static TwoPartyContract<? extends Offer<?, ?>> fromProto(bisq.contract.protobuf.Contract proto) {
        return new TwoPartyContract<>(Offer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                Party.fromProto(proto.getTwoPartyContract().getTaker()));
    }*/
}