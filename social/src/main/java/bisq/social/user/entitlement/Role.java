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

package bisq.social.user.entitlement;

import bisq.common.proto.Proto;
import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;
import bisq.social.user.proof.Proof;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

/**
 * Role of a user profile. Requires some proof for verifying that the associated Role to a user profile
 * is valid.
 */
public record Role(Type type, Proof proof) implements Proto, Comparable<Role> {
    public enum Type implements ProtoEnum {
        LIQUIDITY_PROVIDER(Proof.Type.PROOF_OF_BURN),
        CHANNEL_ADMIN(Proof.Type.BONDED_ROLE),
        CHANNEL_MODERATOR(Proof.Type.CHANNEL_ADMIN_INVITATION, Proof.Type.PROOF_OF_BURN),
        MEDIATOR(Proof.Type.BONDED_ROLE);

        @Getter
        private final List<Proof.Type> types;

        Type(Proof.Type... types) {
            this.types = List.of(types);
        }

        @Override
        public bisq.social.protobuf.Role.Type toProto() {
            return bisq.social.protobuf.Role.Type.valueOf(name());
        }

        public static Type fromProto(bisq.social.protobuf.Role.Type proto) {
            return ProtobufUtils.enumFromProto(Type.class, proto.name());
        }
    }

    @Override
    public bisq.social.protobuf.Role toProto() {
        return bisq.social.protobuf.Role.newBuilder()
                .setType(type.name())
                .setProof(proof.toProto())
                .build();
    }

    public static Role fromProto(bisq.social.protobuf.Role proto) {
        return new Role(ProtobufUtils.enumFromProto(Type.class, proto.getType()),
                Proof.fromProto(proto.getProof()));
    }

    @Override
    public int compareTo(@NonNull Role o) {
        return type.compareTo(o.type);
    }
}