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

import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.observable.ObservableSet;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.oracle.ots.OpenTimestampService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UserIdentityService implements PersistenceClient<UserIdentityStore> {
    @Getter
    @ToString
    public static final class Config {
        private final long chatUserRepublishAge;

        public Config(long chatUserRepublishAge) {
            this.chatUserRepublishAge = TimeUnit.HOURS.toMillis(chatUserRepublishAge);
        }

        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            return new Config(typeSafeConfig.getLong("chatUserRepublishAge"));
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
    private final OpenTimestampService openTimestampService;
    private final Map<String, Long> publishTimeByChatUserId = new ConcurrentHashMap<>();

    public UserIdentityService(Config config,
                               PersistenceService persistenceService,
                               IdentityService identityService,
                               OpenTimestampService openTimestampService,
                               NetworkService networkService) {
        this.config = config;
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.openTimestampService = openTimestampService;
        this.identityService = identityService;
        this.networkService = networkService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        // todo delay
        getUserIdentities().forEach(userProfile ->
                maybePublicUserProfile(userProfile.getUserProfile(), userProfile.getNodeIdAndKeyPair()));
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public UserIdentity createAndPublishNewUserProfile(Identity pooledIdentity,
                                                       String nickName,
                                                       ProofOfWork proofOfWork,
                                                       String terms,
                                                       String bio) {
        String tag = getTag(nickName, proofOfWork);
        Identity identity = identityService.swapPooledIdentity(tag, pooledIdentity);
        UserIdentity userIdentity = createUserIdentity(nickName, proofOfWork, terms, bio, identity);
        maybeCreateOrUpgradeTimestampAsync(userIdentity);
        publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair());
        return userIdentity;
    }

    public CompletableFuture<UserIdentity> createAndPublishNewUserProfile(String nickName,
                                                                          String keyId,
                                                                          KeyPair keyPair,
                                                                          ProofOfWork proofOfWork,
                                                                          String terms,
                                                                          String bio) {
        String tag = getTag(nickName, proofOfWork);
        return identityService.createNewActiveIdentity(tag, keyId, keyPair)
                .thenApply(identity -> createUserIdentity(nickName, proofOfWork, terms, bio, identity))
                .thenApply(userIdentity -> {
                    maybeCreateOrUpgradeTimestampAsync(userIdentity);
                    publishPublicUserProfile(userIdentity.getUserProfile(), userIdentity.getIdentity().getNodeIdAndKeyPair());
                    return userIdentity;
                });
    }

    public void selectChatUserIdentity(UserIdentity userIdentity) {
        persistableStore.getSelectedUserProfile().set(userIdentity);
        persist();
    }

    public CompletableFuture<DataService.BroadCastDataResult> editUserProfile(UserIdentity userIdentity, String terms, String bio) {
        Identity identity = userIdentity.getIdentity();
        UserProfile oldUserProfile = userIdentity.getUserProfile();
        UserProfile newUserProfile = UserProfile.from(oldUserProfile, terms, bio);
        UserIdentity newUserIdentity = new UserIdentity(identity, newUserProfile);

        synchronized (lock) {
            persistableStore.getUserIdentities().remove(userIdentity);
            persistableStore.getUserIdentities().add(newUserIdentity);
            persistableStore.getSelectedUserProfile().set(newUserIdentity);
        }
        persist();

        return networkService.removeAuthenticatedData(oldUserProfile, identity.getNodeIdAndKeyPair())
                .thenCompose(result -> networkService.publishAuthenticatedData(newUserProfile, identity.getNodeIdAndKeyPair()));
    }

    public CompletableFuture<DataService.BroadCastDataResult> deleteUserProfile(UserIdentity userIdentity) {
        //todo add more checks if deleting profile is permitted (e.g. not used in trades, PM,...)
        if (persistableStore.getUserIdentities().size() <= 1) {
            return CompletableFuture.failedFuture(new RuntimeException("Deleting userProfile is not permitted if we only have one left."));
        }
        synchronized (lock) {
            persistableStore.getUserIdentities().remove(userIdentity);
            persistableStore.getUserIdentities().stream().findAny()
                    .ifPresentOrElse(e -> persistableStore.getSelectedUserProfile().set(e),
                            () -> persistableStore.getSelectedUserProfile().set(null));
        }
        persist();
        identityService.retireActiveIdentity(userIdentity.getIdentity().getTag());
        return networkService.removeAuthenticatedData(userIdentity.getUserProfile(),
                userIdentity.getIdentity().getNodeIdAndKeyPair());
    }

    public CompletableFuture<Boolean> maybePublicUserProfile(UserProfile userProfile,
                                                             NetworkIdWithKeyPair nodeIdAndKeyPair) {
        long lastPublished = Optional.ofNullable(publishTimeByChatUserId.get(userProfile.getId())).orElse(0L);
        long passed = System.currentTimeMillis() - lastPublished;
        if (passed > config.getChatUserRepublishAge()) {
            return publishPublicUserProfile(userProfile, nodeIdAndKeyPair).thenApply(result -> true);
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    public boolean hasUserProfiles() {
        return persistableStore.getUserIdentities().isEmpty();
    }

    public Observable<UserIdentity> getSelectedUserProfile() {
        return persistableStore.getSelectedUserProfile();
    }

    public ObservableSet<UserIdentity> getUserIdentities() {
        return persistableStore.getUserIdentities();
    }

    public boolean isUserIdentityPresent(String id) {
        return findUserIdentity(id).isPresent();
    }

    public Optional<UserIdentity> findUserIdentity(String id) {
        return getUserIdentities().stream().filter(userProfile -> userProfile.getId().equals(id)).findAny();
    }

    private UserIdentity createUserIdentity(String nickName,
                                            ProofOfWork proofOfWork,
                                            String terms,
                                            String bio,
                                            Identity identity) {
        UserProfile userProfile = new UserProfile(nickName, proofOfWork, identity.getNodeIdAndKeyPair().getNetworkId(), terms, bio);
        UserIdentity userIdentity = new UserIdentity(identity, userProfile);
        synchronized (lock) {
            persistableStore.getUserIdentities().add(userIdentity);
            persistableStore.getSelectedUserProfile().set(userIdentity);
        }
        persist();
        return userIdentity;
    }

    private CompletableFuture<DataService.BroadCastDataResult> publishPublicUserProfile(UserProfile userProfile,
                                                                                        NetworkIdWithKeyPair nodeIdAndKeyPair) {
        publishTimeByChatUserId.put(userProfile.getId(), System.currentTimeMillis());
        return networkService.publishAuthenticatedData(userProfile, nodeIdAndKeyPair);
    }

    private void maybeCreateOrUpgradeTimestampAsync(UserIdentity userIdentity) {
        // We don't wait for OTS is completed as it will become usable only after the tx is 
        // published and confirmed which can take half a day or more.
        openTimestampService.maybeCreateOrUpgradeTimestampAsync(new ByteArray(userIdentity.getPubKeyHash()));
    }

    private String getTag(String nickName, ProofOfWork proofOfWork) {
        return nickName + "-" + Hex.encode(proofOfWork.getPayload());
    }
}
