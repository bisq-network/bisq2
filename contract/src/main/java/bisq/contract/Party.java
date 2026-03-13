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
import bisq.common.util.OptionalUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
@ToString
public final class Party implements NetworkProto {
    private final Role role;
    private final NetworkId networkId;
    private final Optional<byte[]> saltedAccountPayloadHash;

    public Party(Role role, NetworkId networkId) {
        this(role, networkId, Optional.empty());
    }

    public Party(Role role, NetworkId networkId, Optional<byte[]> saltedAccountPayloadHash) {
        this.role = role;
        this.networkId = networkId;
        this.saltedAccountPayloadHash = saltedAccountPayloadHash.map(byte[]::clone);

        verify();
    }

    @Override
    public void verify() {
        saltedAccountPayloadHash.ifPresent(NetworkDataValidation::validateHash);
    }

    @Override
    public bisq.contract.protobuf.Party.Builder getBuilder(boolean serializeForHash) {
        bisq.contract.protobuf.Party.Builder builder = bisq.contract.protobuf.Party.newBuilder()
                .setRole(role.toProtoEnum())
                .setNetworkId(networkId.toProto(serializeForHash));
        saltedAccountPayloadHash.ifPresent(hash -> builder.setSaltedAccountPayloadHash(ByteString.copyFrom(hash)));
        return builder;
    }

    @Override
    public bisq.contract.protobuf.Party toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static Party fromProto(bisq.contract.protobuf.Party proto) {
        return new Party(Role.fromProto(proto.getRole()),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.hasSaltedAccountPayloadHash()
                        ? Optional.of(proto.getSaltedAccountPayloadHash().toByteArray())
                        : Optional.empty());
    }

    public Optional<byte[]> getSaltedAccountPayloadHash() {
        return saltedAccountPayloadHash.map(byte[]::clone);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Party party)) {
            return false;
        }
        return role == party.role &&
                Objects.equals(networkId, party.networkId) &&
                OptionalUtils.optionalByteArrayEquals(saltedAccountPayloadHash, party.saltedAccountPayloadHash);
    }

    @Override
    public int hashCode() {
        int result = role.hashCode();
        result = 31 * result + networkId.hashCode();
        result = 31 * result + saltedAccountPayloadHash.map(Arrays::hashCode).orElse(0);
        return result;
    }
}
