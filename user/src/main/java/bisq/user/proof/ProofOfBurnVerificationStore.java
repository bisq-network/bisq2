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

package bisq.user.proof;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persists my user profiles and the selected user profile.
 */
@Slf4j
public final class ProofOfBurnVerificationStore implements PersistableStore<ProofOfBurnVerificationStore> {
    private final Map<String, ProofOfBurnProof> verifiedProofOfBurnProofs = new HashMap<>();

    public ProofOfBurnVerificationStore() {
    }

    private ProofOfBurnVerificationStore(Map<String, ProofOfBurnProof> verifiedProofOfBurnProofs) {
        this.verifiedProofOfBurnProofs.putAll(verifiedProofOfBurnProofs);
    }

    @Override
    public bisq.user.protobuf.ProofOfBurnVerificationStore toProto() {
        return bisq.user.protobuf.ProofOfBurnVerificationStore.newBuilder()
                .putAllVerifiedProofOfBurnProofs(verifiedProofOfBurnProofs.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toProto())))
                .build();
    }

    public static ProofOfBurnVerificationStore fromProto(bisq.user.protobuf.ProofOfBurnVerificationStore proto) {
        return new ProofOfBurnVerificationStore(proto.getVerifiedProofOfBurnProofsMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> (ProofOfBurnProof) Proof.fromProto(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.ProofOfBurnVerificationStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public ProofOfBurnVerificationStore getClone() {
        return new ProofOfBurnVerificationStore(verifiedProofOfBurnProofs);
    }

    @Override
    public void applyPersisted(ProofOfBurnVerificationStore persisted) {
        verifiedProofOfBurnProofs.putAll(persisted.getVerifiedProofOfBurnProofs());
    }

    Map<String, ProofOfBurnProof> getVerifiedProofOfBurnProofs() {
        return verifiedProofOfBurnProofs;
    }
}