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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.common.proto.NetworkProto;
import bisq.user.reputation.WitnessReputationProtocol;

import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class AccountAgeWitnessOwnershipResponse implements NetworkProto {
    private final long dateBucket;
    private final byte[] witnessNullifier;

    public AccountAgeWitnessOwnershipResponse(long dateBucket, byte[] witnessNullifier) {
        this.dateBucket = dateBucket;
        this.witnessNullifier = witnessNullifier.clone();
        verify();
    }

    @Override
    public void verify() {
        WitnessReputationProtocol.validateDateBucket(dateBucket);
        WitnessReputationProtocol.validateNullifier(witnessNullifier);
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessOwnershipResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.AccountAgeWitnessOwnershipResponse.newBuilder()
                .setDateBucket(dateBucket)
                .setWitnessNullifier(ByteString.copyFrom(witnessNullifier));
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessOwnershipResponse toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static AccountAgeWitnessOwnershipResponse fromProto(
            bisq.bridge.protobuf.AccountAgeWitnessOwnershipResponse proto) {
        return new AccountAgeWitnessOwnershipResponse(
                proto.getDateBucket(),
                proto.getWitnessNullifier().toByteArray());
    }

    public byte[] getWitnessNullifier() {
        return witnessNullifier.clone();
    }
}
