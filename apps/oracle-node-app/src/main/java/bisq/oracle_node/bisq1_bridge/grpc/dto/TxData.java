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

package bisq.oracle_node.bisq1_bridge.grpc.dto;

import bisq.common.proto.NetworkProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class TxData implements NetworkProto {
    private final String txId;
    private final ProofOfBurnData proofOfBurnData;
    private final BondedReputationData bondedReputationData;

    public TxData(String txId, ProofOfBurnData proofOfBurnData, BondedReputationData bondedReputationData) {
        this.txId = txId;
        this.proofOfBurnData = proofOfBurnData;
        this.bondedReputationData = bondedReputationData;
    }

    @Override
    public void verify() {
        //   NetworkDataValidation.validateDate(timestamp);
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.TxData.Builder getBuilder(boolean serializeForHash) {
        return bisq.oracle_node.bisq1_bridge.protobuf.TxData.newBuilder()
                .setTxId(txId)
                .setProofOfBurnData(proofOfBurnData.toProto(serializeForHash))
                .setBondedReputationData(bondedReputationData.toProto(serializeForHash));
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.TxData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static TxData fromProto(bisq.oracle_node.bisq1_bridge.protobuf.TxData proto) {
        return new TxData(proto.getTxId(),
                ProofOfBurnData.fromProto(proto.getProofOfBurnData()),
                BondedReputationData.fromProto(proto.getBondedReputationData()));
    }
}
