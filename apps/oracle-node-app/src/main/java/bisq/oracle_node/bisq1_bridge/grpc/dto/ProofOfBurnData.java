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
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class ProofOfBurnData implements NetworkProto {
    private final long amount;
    private final byte[] proofOfBurnHash;

    public ProofOfBurnData(long amount, byte[] proofOfBurnHash) {
        this.amount = amount;
        this.proofOfBurnHash = proofOfBurnHash;
    }

    @Override
    public void verify() {
        //   NetworkDataValidation.validateDate(timestamp);
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.ProofOfBurnData.Builder getBuilder(boolean serializeForHash) {
        return bisq.oracle_node.bisq1_bridge.protobuf.ProofOfBurnData.newBuilder()
                .setAmount(amount)
                .setProofOfBurnHash(ByteString.copyFrom(proofOfBurnHash));
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.ProofOfBurnData toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ProofOfBurnData fromProto(bisq.oracle_node.bisq1_bridge.protobuf.ProofOfBurnData proto) {
        return new ProofOfBurnData(proto.getAmount(),
                proto.getProofOfBurnHash().toByteArray()
        );
    }
}