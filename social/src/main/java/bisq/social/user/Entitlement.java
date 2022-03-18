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

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * Entitlement of a user profile. Requires some proof for verifying that the associated Entitlement to a user profile 
 * is valid. 
 */
public record Entitlement(Type entitlementType, Proof proof) implements Serializable {
    public interface Proof extends Serializable {
    }

    public record ProofOfBurnProof(String txId) implements Proof {
    }

    public record BondedRoleProof(String txId, String signature) implements Proof {
    }

    public record ChannelAdminInvitationProof(String invitationCode) implements Proof {
    }

    public enum Type implements Serializable {
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

    public enum ProofType implements Serializable {
        PROOF_OF_BURN,
        BONDED_ROLE,
        CHANNEL_ADMIN_INVITATION
    }
}