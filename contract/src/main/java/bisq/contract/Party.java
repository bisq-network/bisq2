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

package bisq.contract;

import bisq.common.proto.NetworkProto;
import bisq.network.identity.NetworkId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class Party implements NetworkProto {
    private final Role role;
    private final NetworkId networkId;

    public Party(Role role, NetworkId networkId) {
        this.role = role;
        this.networkId = networkId;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.contract.protobuf.Party.Builder getBuilder(boolean serializeForHash) {
        return bisq.contract.protobuf.Party.newBuilder()
                .setRole(role.toProtoEnum())
                .setNetworkId(networkId.toProto(serializeForHash));
    }

    @Override
    public bisq.contract.protobuf.Party toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static Party fromProto(bisq.contract.protobuf.Party proto) {
        return new Party(Role.fromProto(proto.getRole()), NetworkId.fromProto(proto.getNetworkId()));
    }
}
