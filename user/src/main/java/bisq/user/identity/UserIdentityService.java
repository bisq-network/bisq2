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

package bisq.user.identity;

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.AesSecretKey;
import bisq.security.EncryptedData;
import bisq.security.SecurityService;
import bisq.security.pow.ProofOfWork;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class UserIdentityService implements PersistenceClient<UserIdentityStore>, Service, DataService.Listener {
    public final static int MINT_NYM_DIFFICULTY = 65536;  // Math.pow(2, 16) = 65536;

    @Getter
    private final UserIdentityStore persistableStore = new UserIdentityStore();
    @Getter
    private final Persistence<UserIdentityStore> persistence;
    private final HashCashProofOfWorkService hashCashProofOfWorkService;
    private final IdentityService identityService;
    private final NetworkService networkService;

    private final Object lock = new Object();
    @Getter
    private final Observable<UserIdentity> newlyCreatedUserIdentity = new Observable<>();

    public UserIdentityService(PersistenceService persistenceService,
                               SecurityService securityService,
                               IdentityService identityService,
                               NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        hashCashProofOfWorkService = securityService.getHashCashProofOfWorkService();
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
            processUserProfileAddedOrRefreshed(userProfile);
        }
    }

    @Override
    public void onAuthenticatedDataRefreshed(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
            processUserProfileAddedOrRefreshed(userProfile);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public ProofOfWork mintNymProofOfWork(byte[] pubKeyHash) {
        return hashCashProofOfWorkService.mint(pubKeyHash, null, MINT_NYM_DIFFICULTY);
    }

    public CompletableFuture<AesSecretKey> deriveKeyFromPassword(CharSequence password) {
        return persistableStore.deriveKeyFromPassword(password)
                .whenComplete((aesKey, throwable) -> {
                    if (throwable == null && aesKey != null) {
                        persist();
                    }
                });
    }

    public CompletableFuture<EncryptedData> encryptDataStore() {
        return persistableStore.encrypt()
                .whenComplete((encryptedData, throwable) -> {
                    if (throwable == null && encryptedData != null) {
                        persist();
                    }
                });
    }

    public CompletableFuture<Void> decryptDataStore(AesSecretKey aesSecretKey) {
        return persistableStore.decrypt(aesSecretKey)
                .whenComplete((nil, throwable) -> {
                    if (throwable == null) {
                        persist();
                    }
                });
    }

    public CompletableFuture<Void> removePassword(CharSequence password) {
        return decryptDataStore(getAESSecretKey().orElseThrow())
                .thenCompose(nil -> persistableStore.removeKey(password)
                        .whenComplete((nil2, throwable) -> {
                            if (throwable == null) {
                                persistableStore.clearEncryptedData();
                                persist();
                            }
                        }));
    }

    public boolean isDataStoreEncrypted() {
        return persistableStore.getEncryptedData().isPresent();
    }

    public Optional<AesSecretKey> getAESSecretKey() {
        return persistableStore.getAESSecretKey();
    }

    public CompletableFuture<UserIdentity> createAndPublishNewUserProfile(String nickName,
                                                                          KeyPair keyPair,
                                                                          byte[] pubKeyHash,
                                                                          ProofOfWork proofOfWork,
                                                                          int avatarVersion,
                                                                          String terms,
                                                                          String statement) {
        String identityTag = nickName + "-" + Hex.encode(pubKeyHash);
        return identityService.createNewActiveIdentity(identityTag, keyPair)
                .thenApply(identity -> createUserIdentity(nickName, proofOfWork, avatarVersion, terms, statement, identity))
                .thenApply(userIdentity -> {
                    publishUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNetworkIdWithKeyPair().getKeyPair());
                    return userIdentity;
                });
    }

    public void selectChatUserIdentity(UserIdentity userIdentity) {
        if (userIdentity == null) {
            log.warn("userIdentity is null at selectChatUserIdentity");
            return;
        }

        persistableStore.setSelectedUserIdentity(userIdentity);
        persist();
    }

    public CompletableFuture<BroadcastResult> editUserProfile(UserIdentity oldUserIdentity,
                                                              String terms,
                                                              String statement) {
        Identity oldIdentity = oldUserIdentity.getIdentity();
        UserProfile oldUserProfile = oldUserIdentity.getUserProfile();
        UserProfile newUserProfile = UserProfile.forEdit(oldUserProfile, terms, statement);
        UserIdentity newUserIdentity = new UserIdentity(oldIdentity, newUserProfile);

        synchronized (lock) {
            getUserIdentities().remove(oldUserIdentity);
            getUserIdentities().add(newUserIdentity);
            persistableStore.setSelectedUserIdentity(newUserIdentity);
        }
        persist();

        KeyPair keyPair = oldIdentity.getNetworkIdWithKeyPair().getKeyPair();
        return networkService.removeAuthenticatedData(oldUserProfile, keyPair)
                .thenCompose(result -> publishUserProfile(newUserProfile, keyPair));
    }

    // Unsafe to use if there are open private chats or messages from userIdentity
    public CompletableFuture<BroadcastResult> deleteUserIdentity(UserIdentity userIdentity) {
        if (getUserIdentities().size() <= 1) {
            return CompletableFuture.failedFuture(new RuntimeException("Deleting userProfile is not permitted if we only have one left."));
        }
        synchronized (lock) {
            getUserIdentities().remove(userIdentity);
            // We have at least 1 userIdentity left
            persistableStore.setSelectedUserIdentity(getUserIdentities().stream().findFirst().orElseThrow());
        }
        persist();
        identityService.retireActiveIdentity(userIdentity.getIdentity().getTag());
        return networkService.removeAuthenticatedData(userIdentity.getUserProfile(),
                userIdentity.getIdentity().getNetworkIdWithKeyPair().getKeyPair());
    }

    public boolean hasUserIdentities() {
        return !getUserIdentities().isEmpty();
    }

    public boolean hasMultipleUserIdentities() {
        return getUserIdentities().size() > 1;
    }

    public Observable<UserIdentity> getSelectedUserIdentityObservable() {
        return persistableStore.getSelectedUserIdentityObservable();
    }

    public UserIdentity getSelectedUserIdentity() {
        return persistableStore.getSelectedUserIdentity();
    }

    public ObservableSet<UserIdentity> getUserIdentities() {
        return persistableStore.getUserIdentities();
    }

    public boolean isUserIdentityPresent(String profileId) {
        return findUserIdentity(profileId).isPresent();
    }

    public Optional<UserIdentity> findUserIdentity(String id) {
        return getUserIdentities().stream().filter(userIdentity -> userIdentity.getId().equals(id)).findAny();
    }

    public Set<String> getMyUserProfileIds() {
        return getUserIdentities().stream()
                .map(userIdentity -> userIdentity.getUserProfile().getId())
                .collect(Collectors.toSet());
    }

    public CompletableFuture<BroadcastResult> publishUserProfile(UserProfile userProfile, KeyPair keyPair) {
        log.info("publishUserProfile {}", userProfile.getUserName());
        persist();

        // We publish both the old version and the new version to support old clients
        return networkService.publishAuthenticatedData(UserProfile.withVersion(userProfile, 0), keyPair)
                .thenCompose(e -> networkService.publishAuthenticatedData(userProfile, keyPair));
    }

    public CompletableFuture<BroadcastResult> refreshUserProfile(UserProfile userProfile, KeyPair keyPair) {
        log.info("refreshUserProfile {}", userProfile.getUserName());
        persist();

        return networkService.refreshAuthenticatedData(userProfile, keyPair);
    }

    private UserIdentity createUserIdentity(String nickName,
                                            ProofOfWork proofOfWork,
                                            int avatarVersion,
                                            String terms,
                                            String statement,
                                            Identity identity) {
        checkArgument(nickName.equals(nickName.trim()) && !nickName.isEmpty(),
                "Nickname must not have leading or trailing spaces and must not be empty.");
        UserProfile userProfile = UserProfile.createNew(nickName, proofOfWork, avatarVersion,
                identity.getNetworkIdWithKeyPair().getNetworkId(), terms, statement);
        UserIdentity userIdentity = new UserIdentity(identity, userProfile);

        synchronized (lock) {
            getUserIdentities().add(userIdentity);
            persistableStore.setSelectedUserIdentity(userIdentity);
        }
        newlyCreatedUserIdentity.set(userIdentity);
        persist();
        return userIdentity;
    }

    private void processUserProfileAddedOrRefreshed(UserProfile userProfile) {
        findUserIdentity(userProfile.getId())
                .map(UserIdentity::getUserProfile)
                .filter(myUserProfile -> userProfile.getPublishDate() > myUserProfile.getPublishDate())
                .ifPresent(myUserProfile -> myUserProfile.setPublishDate(userProfile.getPublishDate()));
    }
}
