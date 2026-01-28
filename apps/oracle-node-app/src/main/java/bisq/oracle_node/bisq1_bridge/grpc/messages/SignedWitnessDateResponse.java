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

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.common.proto.NetworkProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class SignedWitnessDateResponse implements NetworkProto {
    private final long date;

    public SignedWitnessDateResponse(long date) {
        this.date = date;
    }

    @Override
    public void verify() {

    }

    @Override
    public bisq.bridge.protobuf.SignedWitnessDateResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.SignedWitnessDateResponse.newBuilder()
                .setDate(date);
    }

    @Override
    public bisq.bridge.protobuf.SignedWitnessDateResponse toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static SignedWitnessDateResponse fromProto(bisq.bridge.protobuf.SignedWitnessDateResponse proto) {
        return new SignedWitnessDateResponse(proto.getDate());
    }
}
