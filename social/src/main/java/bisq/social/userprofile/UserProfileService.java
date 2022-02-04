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

package bisq.social.userprofile;


import bisq.common.util.StringUtils;
import bisq.identity.IdentityService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class UserProfileService implements PersistenceClient<UserProfileStore> {
    @Getter
    private final UserProfileStore persistableStore = new UserProfileStore();
    @Getter
    private final Persistence<UserProfileStore> persistence;
    private final KeyPairService keyPairService;
    private final IdentityService identityService;
    private final Object lock = new Object();

    public UserProfileService(PersistenceService persistenceService, KeyPairService keyPairService, IdentityService identityService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.keyPairService = keyPairService;
        this.identityService = identityService;
    }

    public CompletableFuture<Boolean> initialize() {
        if (persistableStore.getUserProfiles().isEmpty()) {
            String keyId = StringUtils.createUid();
            KeyPair keyPair = keyPairService.generateKeyPair();
            String useName = "Satoshi";
            return createNewInitializedUserProfile(useName, keyId, keyPair).thenApply(userProfile -> true);
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }
    

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<UserProfile> createNewInitializedUserProfile(String domainId, String keyId, KeyPair keyPair) {
        return identityService.createNewInitializedIdentity(domainId, keyId, keyPair)
                .thenApply(identity -> {
                    UserProfile userProfile = new UserProfile(identity);
                    synchronized (lock) {
                        persistableStore.getUserProfiles().add(userProfile);
                        persistableStore.getSelectedUserProfile().set(userProfile);
                    }
                    persist();
                    return userProfile;
                });
    }

    public void selectUserProfile(UserProfile value) {
        persistableStore.getSelectedUserProfile().set(value);
        persist();
    }
}
