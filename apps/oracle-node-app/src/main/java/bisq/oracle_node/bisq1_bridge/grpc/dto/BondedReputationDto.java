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
public class BondedReputationDto implements NetworkProto {
    private final long amount;
    private final byte[] bondedReputationHash;
    private final int lockTime;

    public BondedReputationDto(long amount, byte[] bondedReputationHash, int lockTime) {
        this.amount = amount;
        this.bondedReputationHash = bondedReputationHash;
        this.lockTime = lockTime;
    }@Override
    public void verify() {
        //   NetworkDataValidation.validateDate(timestamp);
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.BondedReputationDto.Builder getBuilder(boolean serializeForHash) {
        return bisq.oracle_node.bisq1_bridge.protobuf.BondedReputationDto.newBuilder()
                .setAmount(amount)
                .setBondedReputationHash(ByteString.copyFrom(bondedReputationHash))
                .setLockTime(lockTime);
    }

    @Override
    public bisq.oracle_node.bisq1_bridge.protobuf.BondedReputationDto toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BondedReputationDto fromProto(bisq.oracle_node.bisq1_bridge.protobuf.BondedReputationDto proto) {
        return new BondedReputationDto(proto.getAmount(),
                proto.getBondedReputationHash().toByteArray(),
                proto.getLockTime());
    }
}