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

package bisq.social.user.profile;

import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import bisq.social.user.proof.Proof;
import bisq.social.user.proof.ProofOfBurnProof;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persists my user profiles and the selected user profile.
 */
@Slf4j
public class UserProfileStore implements PersistableStore<UserProfileStore> {
    private final Observable<ChatUserIdentity> selectedUserProfile = new Observable<>();
    private final ObservableSet<ChatUserIdentity> chatUserIdentities;
    private final Map<String, ProofOfBurnProof> verifiedProofOfBurnProofs = new HashMap<>();

    public UserProfileStore() {
        chatUserIdentities = new ObservableSet<>();
    }

    private UserProfileStore(ChatUserIdentity selectedChatUserIdentity,
                             Set<ChatUserIdentity> chatUserIdentities,
                             Map<String, ProofOfBurnProof> verifiedProofOfBurnProofs) {
        this.selectedUserProfile.set(selectedChatUserIdentity);
        this.chatUserIdentities = new ObservableSet<>(chatUserIdentities);
        this.verifiedProofOfBurnProofs.putAll(verifiedProofOfBurnProofs);
    }

    @Override
    public bisq.social.protobuf.UserProfileStore toProto() {
        return bisq.social.protobuf.UserProfileStore.newBuilder()
                .setSelectedUserProfile(selectedUserProfile.get().toProto())
                .addAllUserProfiles(chatUserIdentities.stream().map(ChatUserIdentity::toProto).collect(Collectors.toSet()))
                .putAllVerifiedProofOfBurnProofs(verifiedProofOfBurnProofs.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toProto())))
                .build();
    }

    public static UserProfileStore fromProto(bisq.social.protobuf.UserProfileStore proto) {
        return new UserProfileStore(ChatUserIdentity.fromProto(proto.getSelectedUserProfile()),
                proto.getUserProfilesList().stream()
                        .map(ChatUserIdentity::fromProto)
                        .collect(Collectors.toSet()),
                proto.getVerifiedProofOfBurnProofsMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> (ProofOfBurnProof) Proof.fromProto(e.getValue()))));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.social.protobuf.UserProfileStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public UserProfileStore getClone() {
        return new UserProfileStore(selectedUserProfile.get(), chatUserIdentities, verifiedProofOfBurnProofs);
    }

    @Override
    public void applyPersisted(UserProfileStore persisted) {
        selectedUserProfile.set(persisted.getSelectedUserProfile().get());
        chatUserIdentities.addAll(persisted.getUserProfiles());
        verifiedProofOfBurnProofs.putAll(persisted.getVerifiedProofOfBurnProofs());
    }

    Observable<ChatUserIdentity> getSelectedUserProfile() {
        return selectedUserProfile;
    }

    ObservableSet<ChatUserIdentity> getUserProfiles() {
        return chatUserIdentities;
    }

    Map<String, ProofOfBurnProof> getVerifiedProofOfBurnProofs() {
        return verifiedProofOfBurnProofs;
    }
}