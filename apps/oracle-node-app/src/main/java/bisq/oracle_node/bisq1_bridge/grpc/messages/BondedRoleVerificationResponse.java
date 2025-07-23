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
import bisq.common.util.OptionalUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

@Getter
@EqualsAndHashCode
public final class BondedRoleVerificationResponse implements NetworkProto {
    private final Optional<String> errorMessage;

    public BondedRoleVerificationResponse(Optional<String> errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public void verify() {

    }

    @Override
    public bisq.bridge.protobuf.BondedRoleVerificationResponse.Builder getBuilder(boolean serializeForHash) {
        bisq.bridge.protobuf.BondedRoleVerificationResponse.Builder builder = bisq.bridge.protobuf.BondedRoleVerificationResponse.newBuilder();
        errorMessage.ifPresent(builder::setErrorMessage);
        return builder;
    }

    @Override
    public bisq.bridge.protobuf.BondedRoleVerificationResponse toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static BondedRoleVerificationResponse fromProto(bisq.bridge.protobuf.BondedRoleVerificationResponse proto) {
        return new BondedRoleVerificationResponse(OptionalUtils.optionalIf(proto.hasErrorMessage(), proto::getErrorMessage));
    }
}
