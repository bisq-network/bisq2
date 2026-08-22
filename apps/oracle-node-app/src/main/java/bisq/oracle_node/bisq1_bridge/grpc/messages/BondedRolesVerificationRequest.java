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
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@EqualsAndHashCode
public final class BondedRolesVerificationRequest implements NetworkProto {
    public static final int MAX_REGISTRATIONS = 1000;

    private final List<BondedRoleVerificationRequest> registrations;

    public BondedRolesVerificationRequest(List<BondedRoleVerificationRequest> registrations) {
        this.registrations = List.copyOf(registrations);
        verify();
    }

    @Override
    public void verify() {
        checkArgument(registrations.size() <= MAX_REGISTRATIONS,
                "A bonded-role verification batch must not exceed " + MAX_REGISTRATIONS + " registrations");
        registrations.forEach(BondedRoleVerificationRequest::verify);
    }

    @Override
    public bisq.bridge.protobuf.BondedRolesVerificationRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BondedRolesVerificationRequest.newBuilder()
                .addAllRegistrations(registrations.stream()
                        .map(registration -> registration.toProto(serializeForHash))
                        .toList());
    }

    @Override
    public bisq.bridge.protobuf.BondedRolesVerificationRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.bridge.protobuf.BondedRolesVerificationRequest completeProto() {
        return toProto(false);
    }

    public static BondedRolesVerificationRequest fromProto(bisq.bridge.protobuf.BondedRolesVerificationRequest proto) {
        return new BondedRolesVerificationRequest(proto.getRegistrationsList().stream()
                .map(BondedRoleVerificationRequest::fromProto)
                .toList());
    }
}
