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
import bisq.common.timer.Scheduler;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.AesSecretKey;
import bisq.security.DigestUtil;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class UserIdentityService implements PersistenceClient<UserIdentityStore>, Service {
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
    private final Observable<UserIdentity> newlyCreatedUserIdentity = new Observable<>();

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

        // We delay publishing to be better bootstrapped 
        Scheduler.run(() -> getUserIdentities().forEach(userProfile ->
                        maybePublicUserProfile(userProfile.getUserProfile(), userProfile.getNodeIdAndKeyPair().getKeyPair())))
                .after(5, TimeUnit.SECONDS);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
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

    public UserIdentity createAndPublishNewUserProfile(String nickName,
                                                       ProofOfWork proofOfWork) {
        String tag = getIdentityTag(nickName, proofOfWork);
        Identity identity = identityService.createAndInitializeNewActiveIdentity(tag);
        UserIdentity userIdentity = createUserIdentity(nickName, proofOfWork, "", "", identity);
        publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair().getKeyPair());
        return userIdentity;
    }

    public UserIdentity createAndPublishNewUserProfile(String nickName,
                                                       ProofOfWork proofOfWork,
                                                       String terms,
                                                       String statement) {
        String tag = getIdentityTag(nickName, proofOfWork);
        Identity identity = identityService.createAndInitializeNewActiveIdentity(tag);
        UserIdentity userIdentity = createUserIdentity(nickName, proofOfWork, terms, statement, identity);
        publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair().getKeyPair());
        return userIdentity;
    }

    public CompletableFuture<UserIdentity> createAndPublishNewUserProfile(String nickName,
                                                                          KeyPair keyPair,
                                                                          ProofOfWork proofOfWork,
                                                                          String terms,
                                                                          String statement) {
        String identityTag = getIdentityTag(nickName, proofOfWork);
        String keyId = Hex.encode(DigestUtil.hash(keyPair.getPublic().getEncoded()));
        return identityService.createNewActiveIdentity(identityTag, keyId, keyPair)
                .thenApply(identity -> createUserIdentity(nickName, proofOfWork, terms, statement, identity))
                .thenApply(userIdentity -> {
                    publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair().getKeyPair());
                    return userIdentity;
                });
    }

    public void selectChatUserIdentity(UserIdentity userIdentity) {
        persistableStore.setSelectedUserIdentity(userIdentity);
        persist();
    }

    public CompletableFuture<BroadcastResult> editUserProfile(UserIdentity oldUserIdentity, String terms, String statement) {
        Identity oldIdentity = oldUserIdentity.getIdentity();
        UserProfile oldUserProfile = oldUserIdentity.getUserProfile();
        UserProfile newUserProfile = UserProfile.from(oldUserProfile, terms, statement);
        UserIdentity newUserIdentity = new UserIdentity(oldIdentity, newUserProfile);

        synchronized (lock) {
            getUserIdentities().remove(oldUserIdentity);
            getUserIdentities().add(newUserIdentity);
            persistableStore.setSelectedUserIdentity(newUserIdentity);
        }
        persist();

        return networkService.removeAuthenticatedData(oldUserProfile, oldIdentity.getNodeIdAndKeyPair().getKeyPair())
                .thenCompose(result -> networkService.publishAuthenticatedData(newUserProfile, oldIdentity.getNodeIdAndKeyPair().getKeyPair()));
    }

    // Unsafe to use if there are open private chats or messages from userIdentity
    public CompletableFuture<BroadcastResult> deleteUserIdentity(UserIdentity userIdentity) {
        if (getUserIdentities().size() <= 1) {
            return CompletableFuture.failedFuture(new RuntimeException("Deleting userProfile is not permitted if we only have one left."));
        }
        synchronized (lock) {
            getUserIdentities().remove(userIdentity);

            getUserIdentities().stream().findAny()
                    .ifPresentOrElse(persistableStore::setSelectedUserIdentity,
                            () -> persistableStore.setSelectedUserIdentity(null));
        }
        persist();
        identityService.retireActiveIdentity(userIdentity.getIdentity().getTag());
        return networkService.removeAuthenticatedData(userIdentity.getUserProfile(),
                userIdentity.getIdentity().getNodeIdAndKeyPair().getKeyPair());
    }

    public CompletableFuture<Boolean> maybePublicUserProfile(UserProfile userProfile, KeyPair keyPair) {
        long lastPublished = Optional.ofNullable(publishTimeByChatUserId.get(userProfile.getId())).orElse(0L);
        long passed = System.currentTimeMillis() - lastPublished;
        if (passed > config.getRepublishUserProfileDelay()) {
            return publishPublicUserProfile(userProfile, keyPair).thenApply(result -> true);
        } else {
            return CompletableFuture.completedFuture(false);
        }
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

    public Optional<UserIdentity> findUserIdentity(String id) {
        return getUserIdentities().stream().filter(userIdentity -> userIdentity.getId().equals(id)).findAny();
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

        synchronized (lock) {
            getUserIdentities().add(userIdentity);
            persistableStore.setSelectedUserIdentity(userIdentity);
        }
        newlyCreatedUserIdentity.set(userIdentity);
        persist();
        return userIdentity;
    }

    private CompletableFuture<BroadcastResult> publishPublicUserProfile(UserProfile userProfile, KeyPair keyPair) {
        publishTimeByChatUserId.put(userProfile.getId(), System.currentTimeMillis());
        return networkService.publishAuthenticatedData(userProfile, keyPair);
    }

    private String getIdentityTag(String nickName, ProofOfWork proofOfWork) {
        return nickName + "-" + Hex.encode(proofOfWork.getPayload());
    }
}
