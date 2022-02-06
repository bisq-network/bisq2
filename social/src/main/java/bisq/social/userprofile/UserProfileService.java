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
import java.util.HashSet;
import java.util.Set;
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
            return createDefaultUserProfile();
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<UserProfile> createNewInitializedUserProfile(String domainId,
                                                                          String keyId, 
                                                                          KeyPair keyPair, 
                                                                          Set<Entitlement> entitlements) {
        return identityService.createNewInitializedIdentity(domainId, keyId, keyPair)
                .thenApply(identity -> {
                    UserProfile userProfile = new UserProfile(identity, entitlements);
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

    public CompletableFuture<Boolean> verifyEntitlement(Entitlement.Type entitlementType, String txId) {
        log.error("entitlementType {}, tx ID {}", entitlementType, txId);
        // todo 
        //  1. request from explorer nodes the tx data
        //  2. check if tx is valid bsq tx and match requirements (proof or burn or bonded role tx id, correct 
        //  amounts and op return data)
        //  3a. for proof of burn user need to use pubkey hash as pre image. Others can see pubkey and can verify if 
        //  its hash matches the attached proof of burn tx data.
        //  3b. For bonded roles the user need to provide a signature of the user profile pubkey hash signed with the 
        //  input EC key of the bonded role request tx. Others can verify if the signature matches the users pubkey hash
        //  by requesting the bonded role tx data.
        // 
        
        return CompletableFuture.completedFuture(txId.length() > 5); //todo
    }

    private CompletableFuture<Boolean> createDefaultUserProfile() {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.generateKeyPair();
        String useName = "Satoshi";
        return createNewInitializedUserProfile(useName, keyId, keyPair, new HashSet<>())
                .thenApply(userProfile -> true);
    }
}
