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

package bisq.social.user.proof;

import bisq.common.proto.Proto;
import bisq.common.proto.ProtoEnum;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ProtobufUtils;

public interface Proof extends Proto {
    enum Type implements ProtoEnum {
        PROOF_OF_BURN,
        BONDED_ROLE,
        CHANNEL_ADMIN_INVITATION;

        @Override
        public bisq.user.protobuf.Proof.Type toProto() {
            return bisq.user.protobuf.Proof.Type.valueOf(name());
        }

        public static Type fromProto(bisq.user.protobuf.Proof.Type proto) {
            return ProtobufUtils.enumFromProto(Type.class, proto.name());
        }
    }

    default bisq.user.protobuf.Proof.Builder getProofBuilder() {
        return bisq.user.protobuf.Proof.newBuilder();
    }

    bisq.user.protobuf.Proof toProto();

    static Proof fromProto(bisq.user.protobuf.Proof proto) {
        switch (proto.getMessageCase()) {
            case PROOFOFBURNPROOF: {
                return ProofOfBurnProof.fromProto(proto.getProofOfBurnProof());
            }
            case BONDEDROLEPROOF: {
                return BondedRoleProof.fromProto(proto.getBondedRoleProof());
            }
            case INVITATIONPROOF: {
                return InvitationProof.fromProto(proto.getInvitationProof());
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}