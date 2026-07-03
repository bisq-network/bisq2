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

package bisq.trade.mu_sig.messages.grpc;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class CustomPayoutPsbtRequest implements Proto {
    private final String tradeId;
    private final long sellersPayoutAmountExcludingFee;
    private final long feeRate;

    public CustomPayoutPsbtRequest(String tradeId, long sellersPayoutAmountExcludingFee, long feeRate) {
        this.tradeId = tradeId;
        this.sellersPayoutAmountExcludingFee = sellersPayoutAmountExcludingFee;
        this.feeRate = feeRate;
    }

    @Override
    public bisq.trade.protobuf.CustomPayoutPsbtRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.CustomPayoutPsbtRequest.newBuilder()
                .setTradeId(tradeId)
                .setSellersPayoutAmountExcludingFee(sellersPayoutAmountExcludingFee)
                .setFeeRate(feeRate);
    }

    @Override
    public bisq.trade.protobuf.CustomPayoutPsbtRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static CustomPayoutPsbtRequest fromProto(bisq.trade.protobuf.CustomPayoutPsbtRequest proto) {
        return new CustomPayoutPsbtRequest(proto.getTradeId(),
                proto.getSellersPayoutAmountExcludingFee(),
                proto.getFeeRate());
    }
}
