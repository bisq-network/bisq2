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

import bisq.bonded_roles.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.AesSecretKey;
import bisq.security.EncryptedData;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class UserIdentityService implements PersistenceClient<UserIdentityStore>, Service, DataService.Listener {
    @Getter
    @ToString
    public static final class Config {
        private final long republishUserProfileDelay;

        public Config(long republishUserProfileDelay) {
            this.republishUserProfileDelay = TimeUnit.HOURS.toMillis(republishUserProfileDelay);
        }

        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            return new Config(typeSafeConfig.getLong("republishUserProfileDelay"));
        }
    }


    @Getter
    private final UserIdentityStore persistableStore = new UserIdentityStore();
    @Getter
    private final Persistence<UserIdentityStore> persistence;
    private final IdentityService identityService;
    private final NetworkService networkService;
    private final Object lock = new Object();
    private final Config config;
    private final Map<String, Long> publishTimeByChatUserId = new ConcurrentHashMap<>();
    @Getter
    private final Observable<Integer> userIdentityChangedFlag = new Observable<>(0);
    @Getter
    private final Observable<UserIdentity> newlyCreatedUserIdentity = new Observable<>();
    protected Set<NetworkId> daoBridgeServiceProviders = new CopyOnWriteArraySet<>();

    public UserIdentityService(Config config,
                               PersistenceService persistenceService,
                               IdentityService identityService,
                               NetworkService networkService) {
        this.config = config;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAllAuthenticatedData()
                        .forEach(this::processAuthenticatedData));
        networkService.addDataServiceListener(this);

        // We delay publishing to be better bootstrapped 
        Scheduler.run(() -> getUserIdentities().forEach(userProfile ->
                        maybePublicUserProfile(userProfile.getUserProfile(), userProfile.getNodeIdAndKeyPair())))
                .after(5, TimeUnit.SECONDS);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        processAuthenticatedData(authenticatedData);
    }

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedOracleNode) {
            AuthorizedOracleNode data = (AuthorizedOracleNode) authenticatedData.getDistributedData();
            daoBridgeServiceProviders.add(data.getNetworkId());
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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
                .thenCompose(nil -> {
                    persistableStore.clearEncryptedData();
                    return persistableStore.removeKey(password)
                            .whenComplete((nil2, throwable) -> {
                                if (throwable == null) {
                                    persist();
                                }
                            });
                });
    }

    public boolean isDataStoreEncrypted() {
        return persistableStore.getEncryptedData().isPresent();
    }

    public Optional<AesSecretKey> getAESSecretKey() {
        return persistableStore.getAESSecretKey();
    }

    public UserIdentity createAndPublishNewUserProfile(Identity pooledIdentity,
                                                       String nickName,
                                                       ProofOfWork proofOfWork) {
        String tag = getTag(nickName, proofOfWork);
        Identity identity = identityService.swapPooledIdentity(tag, pooledIdentity);
        UserIdentity userIdentity = createUserIdentity(nickName, proofOfWork, "", "", identity);
        publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair());
        return userIdentity;
    }

    public UserIdentity createAndPublishNewUserProfile(Identity pooledIdentity,
                                                       String nickName,
                                                       ProofOfWork proofOfWork,
                                                       String terms,
                                                       String statement) {
        String tag = getTag(nickName, proofOfWork);
        Identity identity = identityService.swapPooledIdentity(tag, pooledIdentity);
        UserIdentity userIdentity = createUserIdentity(nickName, proofOfWork, terms, statement, identity);
        publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair());
        return userIdentity;
    }

    public CompletableFuture<UserIdentity> createAndPublishNewUserProfile(String nickName,
                                                                          String keyId,
                                                                          KeyPair keyPair,
                                                                          ProofOfWork proofOfWork,
                                                                          String terms,
                                                                          String statement) {
        String tag = getTag(nickName, proofOfWork);
        return identityService.createNewActiveIdentity(tag, keyId, keyPair)
                .thenApply(identity -> createUserIdentity(nickName, proofOfWork, terms, statement, identity))
                .thenApply(userIdentity -> {
                    publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair());
                    return userIdentity;
                });
    }

    public void selectChatUserIdentity(UserIdentity userIdentity) {
        persistableStore.setSelectedUserIdentity(userIdentity);
        persist();
    }

    public CompletableFuture<DataService.BroadCastDataResult> editUserProfile(UserIdentity oldUserIdentity, String terms, String statement) {
        Identity oldIdentity = oldUserIdentity.getIdentity();
        UserProfile oldUserProfile = oldUserIdentity.getUserProfile();
        UserProfile newUserProfile = UserProfile.from(oldUserProfile, terms, statement);
        UserIdentity newUserIdentity = new UserIdentity(oldIdentity, newUserProfile);

        synchronized (lock) {
            persistableStore.getUserIdentities().remove(oldUserIdentity);
            persistableStore.getUserIdentities().add(newUserIdentity);
            persistableStore.setSelectedUserIdentity(newUserIdentity);
        }
        persist();

        return networkService.removeAuthenticatedData(oldUserProfile, oldIdentity.getNodeIdAndKeyPair())
                .thenCompose(result -> networkService.publishAuthenticatedData(newUserProfile, oldIdentity.getNodeIdAndKeyPair()));
    }

    public CompletableFuture<DataService.BroadCastDataResult> deleteUserProfile(UserIdentity userIdentity) {
        //todo add more checks if deleting profile is permitted (e.g. not used in trades, PM,...)
        if (persistableStore.getUserIdentities().size() <= 1) {
            return CompletableFuture.failedFuture(new RuntimeException("Deleting userProfile is not permitted if we only have one left."));
        }

        getUserIdentities().remove(userIdentity);
        getUserIdentities().stream().findAny()
                .ifPresentOrElse(persistableStore::setSelectedUserIdentity,
                        () -> persistableStore.setSelectedUserIdentity(null));
        synchronized (lock) {
            //todo
            persistableStore.getUserIdentities().remove(userIdentity);
            persistableStore.getUserIdentities().stream().findAny()
                    .ifPresentOrElse(persistableStore::setSelectedUserIdentity,
                            () -> persistableStore.setSelectedUserIdentity(null));
        }
        userIdentityChangedFlag.set(userIdentityChangedFlag.get() + 1);
        persist();
        identityService.retireActiveIdentity(userIdentity.getIdentity().getTag());
        return networkService.removeAuthenticatedData(userIdentity.getUserProfile(),
                userIdentity.getIdentity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<Boolean> maybePublicUserProfile(UserProfile userProfile,
                                                             NetworkIdWithKeyPair nodeIdAndKeyPair) {
        long lastPublished = Optional.ofNullable(publishTimeByChatUserId.get(userProfile.getId())).orElse(0L);
        long passed = System.currentTimeMillis() - lastPublished;
        if (passed > config.getRepublishUserProfileDelay()) {
            return publishPublicUserProfile(userProfile, nodeIdAndKeyPair).thenApply(result -> true);
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    public boolean hasUserIdentities() {
        return !getUserIdentities().isEmpty();
    }

    public boolean hasMultipleUserIdentities() {
        return getUserIdentities().size() == 1;
    }

    public Observable<UserIdentity> getSelectedUserIdentityObservable() {
        return persistableStore.getSelectedUserIdentityObservable();
    }

    @Nullable
    public UserIdentity getSelectedUserIdentity() {
        return persistableStore.getSelectedUserIdentity();
    }

    public ObservableSet<UserIdentity> getUserIdentities() {
        return persistableStore.getUserIdentities();
    }

    public boolean isUserIdentityPresent(String profileId) {
        return findUserIdentity(profileId).isPresent();
    }

    public Optional<UserIdentity> findUserIdentity(String profileId) {
        return getUserIdentities().stream().filter(userIdentity -> userIdentity.getId().equals(profileId)).findAny();
    }

    public Set<String> getMyUserProfileIds() {
        return getUserIdentities().stream()
                .map(userIdentity -> userIdentity.getUserProfile().getId())
                .collect(Collectors.toSet());
    }

    private UserIdentity createUserIdentity(String nickName,
                                            ProofOfWork proofOfWork,
                                            String terms,
                                            String statement,
                                            Identity identity) {
        checkArgument(nickName.equals(nickName.trim()) && !nickName.isEmpty(),
                "Nickname must not have leading or trailing spaces and must not be empty.");
        UserProfile userProfile = new UserProfile(nickName, proofOfWork, identity.getNodeIdAndKeyPair().getNetworkId(), terms, statement);
        UserIdentity userIdentity = new UserIdentity(identity, userProfile);

        getUserIdentities().add(userIdentity);
        persistableStore.setSelectedUserIdentity(userIdentity);

        synchronized (lock) {
            //todo
            persistableStore.getUserIdentities().add(userIdentity);
            persistableStore.setSelectedUserIdentity(userIdentity);
        }
        newlyCreatedUserIdentity.set(userIdentity);
        userIdentityChangedFlag.set(userIdentityChangedFlag.get() + 1);
        persist();
        return userIdentity;
    }

    private CompletableFuture<DataService.BroadCastDataResult> publishPublicUserProfile(UserProfile userProfile,
                                                                                        NetworkIdWithKeyPair nodeIdAndKeyPair) {
        publishTimeByChatUserId.put(userProfile.getId(), System.currentTimeMillis());
        return networkService.publishAuthenticatedData(userProfile, nodeIdAndKeyPair);
    }

    private String getTag(String nickName, ProofOfWork proofOfWork) {
        return nickName + "-" + Hex.encode(proofOfWork.getPayload());
    }
}
