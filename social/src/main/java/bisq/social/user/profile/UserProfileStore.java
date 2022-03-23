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

import bisq.common.data.ByteArray;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.persistence.PersistableStore;
import bisq.social.user.Entitlement;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Persists my user profiles and the selected user profile.
 */
public class UserProfileStore implements PersistableStore<UserProfileStore> {
    @Getter
    private final Observable<UserProfile> selectedUserProfile = new Observable<>();
    @Getter
    private final ObservableSet<UserProfile> userProfiles;
    @Getter
    private final Map<ByteArray, Entitlement.ProofOfBurnProof> verifiedProofOfBurnProofs = new HashMap<>();

    public UserProfileStore() {
        userProfiles = new ObservableSet<>();
    }

    private UserProfileStore(ObservableSet<UserProfile> userProfiles,
                             Observable<UserProfile> selectedUserProfile,
                             Map<ByteArray, Entitlement.ProofOfBurnProof> verifiedProofOfBurnProofs) {
        this.selectedUserProfile.set(selectedUserProfile.get());
        this.userProfiles = new ObservableSet<>(userProfiles);
        this.verifiedProofOfBurnProofs.putAll(verifiedProofOfBurnProofs);
    }

    @Override
    public UserProfileStore getClone() {
        return new UserProfileStore(userProfiles, selectedUserProfile, verifiedProofOfBurnProofs);
    }

    @Override
    public void applyPersisted(UserProfileStore persisted) {
        userProfiles.addAll(persisted.getUserProfiles());
        selectedUserProfile.set(persisted.getSelectedUserProfile().get());
        verifiedProofOfBurnProofs.putAll(persisted.getVerifiedProofOfBurnProofs());
    }
}