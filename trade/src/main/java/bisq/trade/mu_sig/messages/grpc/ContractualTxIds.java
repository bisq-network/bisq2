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
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class ContractualTxIds implements Proto {
    private final String depositTxId;
    private final String buyersWarningTxId;
    private final String sellersWarningTxId;
    private final String buyersRedirectTxId;
    private final String sellersRedirectTxId;

    public ContractualTxIds(String depositTxId,
                            String buyersWarningTxId,
                            String sellersWarningTxId,
                            String buyersRedirectTxId,
                            String sellersRedirectTxId) {
        this.depositTxId = depositTxId;
        this.buyersWarningTxId = buyersWarningTxId;
        this.sellersWarningTxId = sellersWarningTxId;
        this.buyersRedirectTxId = buyersRedirectTxId;
        this.sellersRedirectTxId = sellersRedirectTxId;
    }

    @Override
    public bisq.trade.protobuf.ContractualTxIds.Builder getBuilder(boolean serializeForHash) {
        return bisq.trade.protobuf.ContractualTxIds.newBuilder()
                .setDepositTxId(depositTxId)
                .setBuyersWarningTxId(buyersWarningTxId)
                .setSellersWarningTxId(sellersWarningTxId)
                .setBuyersRedirectTxId(buyersRedirectTxId)
                .setSellersRedirectTxId(sellersRedirectTxId);
    }

    public static ContractualTxIds fromProto(bisq.trade.protobuf.ContractualTxIds proto) {
        return new ContractualTxIds(proto.getDepositTxId(),
                proto.getBuyersWarningTxId(),
                proto.getSellersWarningTxId(),
                proto.getBuyersRedirectTxId(),
                proto.getSellersRedirectTxId());
    }

    @Override
    public bisq.trade.protobuf.ContractualTxIds toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }
}
