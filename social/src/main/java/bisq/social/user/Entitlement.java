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

package bisq.social.user;

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ProtobufUtils;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

/**
 * Entitlement of a user profile. Requires some proof for verifying that the associated Entitlement to a user profile
 * is valid.
 */
public record Entitlement(Type entitlementType, Proof proof) implements Proto, Comparable<Entitlement> {
    public bisq.social.protobuf.Entitlement toProto() {
        return bisq.social.protobuf.Entitlement.newBuilder()
                .setEntitlementType(entitlementType.name())
                .setProof(proof.toProto())
                .build();
    }

    public static Entitlement fromProto(bisq.social.protobuf.Entitlement proto) {
        return new Entitlement(ProtobufUtils.enumFromProto(Type.class, proto.getEntitlementType()),
                Proof.fromProto(proto.getProof()));
    }

    @Override
    public int compareTo(@NonNull Entitlement o) {
        return entitlementType.compareTo(o.entitlementType);
    }

    public interface Proof extends Proto {
        default bisq.social.protobuf.Proof.Builder getProofBuilder() {
            return bisq.social.protobuf.Proof.newBuilder();
        }

        bisq.social.protobuf.Proof toProto();

        static Proof fromProto(bisq.social.protobuf.Proof proto) {
            switch (proto.getMessageCase()) {
                case PROOFOFBURNPROOF -> {
                    return ProofOfBurnProof.fromProto(proto.getProofOfBurnProof());
                }
                case BONDEDROLEPROOF -> {
                    return BondedRoleProof.fromProto(proto.getBondedRoleProof());
                }
                case INVITATIONPROOF -> {
                    return InvitationProof.fromProto(proto.getInvitationProof());
                }
                case MESSAGE_NOT_SET -> {
                    throw new UnresolvableProtobufMessageException(proto);
                }
            }
            throw new UnresolvableProtobufMessageException(proto);
        }
    }

    public record ProofOfBurnProof(String txId, long burntAmount, long date) implements Proof {
        @Override
        public bisq.social.protobuf.Proof toProto() {
            return getProofBuilder().setProofOfBurnProof(
                            bisq.social.protobuf.ProofOfBurnProof.newBuilder()
                                    .setTxId(txId)
                                    .setBurntAmount(burntAmount)
                                    .setDate(date))
                    .build();
        }

        public static ProofOfBurnProof fromProto(bisq.social.protobuf.ProofOfBurnProof proto) {
            return new ProofOfBurnProof(proto.getTxId(), proto.getBurntAmount(), proto.getDate());
        }
    }

    public record BondedRoleProof(String txId, String signature) implements Proof {
        @Override
        public bisq.social.protobuf.Proof toProto() {
            return getProofBuilder().setBondedRoleProof(
                            bisq.social.protobuf.BondedRoleProof.newBuilder()
                                    .setTxId(txId)
                                    .setSignature(signature))
                    .build();
        }

        public static BondedRoleProof fromProto(bisq.social.protobuf.BondedRoleProof proto) {
            return new BondedRoleProof(proto.getTxId(), proto.getSignature());
        }
    }

    public record InvitationProof(String invitationCode) implements Proof {
        @Override
        public bisq.social.protobuf.Proof toProto() {
            return getProofBuilder().setInvitationProof(
                            bisq.social.protobuf.InvitationProof.newBuilder()
                                    .setInvitationCode(invitationCode))
                    .build();
        }

        public static InvitationProof fromProto(bisq.social.protobuf.InvitationProof proto) {
            return new InvitationProof(proto.getInvitationCode());
        }
    }

    public enum Type implements Proto {
        LIQUIDITY_PROVIDER(ProofType.PROOF_OF_BURN),
        CHANNEL_ADMIN(ProofType.BONDED_ROLE),
        CHANNEL_MODERATOR(ProofType.CHANNEL_ADMIN_INVITATION, ProofType.PROOF_OF_BURN),
        MEDIATOR(ProofType.BONDED_ROLE);

        @Getter
        private final List<ProofType> proofTypes;

        Type(ProofType... proofTypes) {
            this.proofTypes = List.of(proofTypes);
        }
    }

    public enum ProofType implements Proto {
        PROOF_OF_BURN,
        BONDED_ROLE,
        CHANNEL_ADMIN_INVITATION
    }
}