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
public class PublishDepositTxRequest implements Proto {
    private final String tradeId;
    private final DepositPsbt depositPsbt;

    public PublishDepositTxRequest(String tradeId,
                                   DepositPsbt depositPsbt) {
        this.tradeId = tradeId;
        this.depositPsbt = depositPsbt;
    }

    @Override
    public bisq.trade.protobuf.PublishDepositTxRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.PublishDepositTxRequest.newBuilder()
                .setTradeId(tradeId)
                .setDepositPsbt(depositPsbt.toProto(serializeForHash));
    }

    @Override
    public bisq.trade.protobuf.PublishDepositTxRequest toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static PublishDepositTxRequest fromProto(bisq.trade.protobuf.PublishDepositTxRequest proto) {
        return new PublishDepositTxRequest(proto.getTradeId(),
                DepositPsbt.fromProto(proto.getDepositPsbt()));
    }
}