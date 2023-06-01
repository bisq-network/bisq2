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
import bisq.offer.bisq_easy.BisqEasyOffer;
import lombok.Getter;

@Getter
public class BisqEasyContract extends TwoPartyContract<BisqEasyOffer> {

    public BisqEasyContract(BisqEasyOffer bisqEasyOffer, Party taker) {
        super(bisqEasyOffer, SwapProtocolType.BISQ_EASY, taker);
    }

    @Override
    public bisq.contract.protobuf.SwapContract toProto() {
        return getSwapContractBuilder().setBisqEasyContract(
                        bisq.contract.protobuf.BisqEasyContract.newBuilder()
                                .setTaker(taker.toProto()))
                .build();
    }

    public static BisqEasyContract fromProto(bisq.contract.protobuf.SwapContract proto) {
        return new BisqEasyContract(BisqEasyOffer.fromProto(proto.getSwapOffer()),
                Party.fromProto(proto.getBisqEasyContract().getTaker()));
    }
}