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

import bisq.account.protocol_type.SwapProtocolType;
import bisq.offer.SwapOffer;
import lombok.Getter;

@Getter
public class TwoPartyContract<T extends SwapOffer> extends SwapContract<T> {
    private final Party taker;

    public TwoPartyContract(T swapOffer, SwapProtocolType protocolType, Party taker) {
        super(swapOffer, protocolType);
        this.taker = taker;
    }

    @Override
    public bisq.contract.protobuf.SwapContract toProto() {
        return getBuilder().setTwoPartyContract(
                        bisq.contract.protobuf.TwoPartyContract.newBuilder()
                                .setTaker(taker.toProto()))
                .build();
    }

    public static TwoPartyContract<? extends SwapOffer> fromProto(bisq.contract.protobuf.SwapContract proto) {
        return new TwoPartyContract<>(SwapOffer.fromProto(proto.getSwapOffer()),
                SwapProtocolType.fromProto(proto.getProtocolType()),
                Party.fromProto(proto.getTwoPartyContract().getTaker()));
    }
}