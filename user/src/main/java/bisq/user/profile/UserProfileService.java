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

package bisq.user.profile;

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.SecurityService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import bisq.user.contact_list.ContactListService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UserProfileService implements PersistenceClient<UserProfileStore>, DataService.Listener, Service {
    private static final String SEPARATOR_START = " [";
    private static final String SEPARATOR_END = "]";
    @Getter
    private static UserProfileService instance;

    @Getter
    private final UserProfileStore persistableStore = new UserProfileStore();
    @Getter
    private final Persistence<UserProfileStore> persistence;
    private final NetworkService networkService;
    private final ContactListService contactListService;
    @Getter
    private final Observable<Integer> numUserProfiles = new Observable<>(0);
    private final HashCashProofOfWorkService hashCashProofOfWorkService;
    private final ObservableHashMap<String, UserProfile> userProfileById = new ObservableHashMap<>();
    @Getter
    private final Map<String, Set<String>> nymsByNickName = new ConcurrentHashMap<>();

    public UserProfileService(PersistenceService persistenceService,
                              SecurityService securityService,
                              NetworkService networkService,
                              ContactListService contactListService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
        hashCashProofOfWorkService = securityService.getHashCashProofOfWorkService();
        this.networkService = networkService;
        this.contactListService = contactListService;

        instance = this;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(ds -> ds.getAuthenticatedData().forEach(authenticatedData -> {
            if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
                processUserProfileAddedOrRefreshed(userProfile, true);
                persist();
            }
        }));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // DataService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
            processUserProfileAddedOrRefreshed(userProfile);
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
            processUserProfileRemoved(userProfile);
        }
    }

    @Override
    public void onAuthenticatedDataRefreshed(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof UserProfile userProfile) {
            processUserProfileAddedOrRefreshed(userProfile);
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Optional<UserProfile> findUserProfile(String id) {
        return Optional.ofNullable(userProfileById.get(id));
    }

    // We update the publishDate in our managed userProfiles. Only if the userProfile is not found we return
    // the one used for requesting
    public UserProfile getManagedUserProfile(UserProfile userProfile) {
        return findUserProfile(userProfile.getId()).orElse(userProfile);
    }

    public List<UserProfile> getUserProfiles() {
        return new ArrayList<>(userProfileById.values());
    }

    public boolean isChatUserIgnored(String profileId) {
        return findUserProfile(profileId)
                .map(this::isChatUserIgnored)
                .orElse(false);
    }

    public boolean isChatUserIgnored(UserProfile userProfile) {
        return getIgnoredUserProfileIds().contains(userProfile.getId());
    }

    public void ignoreUserProfile(UserProfile userProfile) {
        persistableStore.addIgnoredUserProfileIds(userProfile.getId());
        persist();
    }

    public void undoIgnoreUserProfile(UserProfile userProfile) {
        persistableStore.removeIgnoredUserProfileIds(userProfile.getId());
        persist();
    }

    public ReadOnlyObservableSet<String> getIgnoredUserProfileIds() {
        return persistableStore.getIgnoredUserProfileIds();
    }

    public String evaluateUserName(String nickName, String nym) {
        if (shouldAddNymToNickName(nickName, nym, getNymsByNickName(), contactListService.getNymsByNickName())) {
            return nickName + SEPARATOR_START + nym + SEPARATOR_END;
        } else {
            return nickName;
        }
    }

    @VisibleForTesting
    static boolean shouldAddNymToNickName(String nickName,
                                          String nym,
                                          Map<String, Set<String>> nymsByNickNameFromNetwork,
                                          Map<String, Set<String>> nymsByNickNameFromContactList) {
        Optional<Set<String>> nymsForNickNameFromNetwork = Optional.ofNullable(nymsByNickNameFromNetwork.get(nickName));
        long numNymsWithSameNicknameInNetwork = nymsForNickNameFromNetwork.map(Set::size).orElse(0);
        int numNickNamesWithDifferentNymInContactList = Optional.ofNullable(nymsByNickNameFromContactList.get(nickName))
                .filter(set -> !set.contains(nym))
                .map(Set::size)
                .orElse(0);
        Optional<String> singleDifferentNymFromNetwork = nymsForNickNameFromNetwork
                .filter(set -> set.size() == 1)
                .map(set -> set.iterator().next())
                .filter(e -> !e.equals(nym));
        return numNymsWithSameNicknameInNetwork > 1 ||
                numNickNamesWithDifferentNymInContactList > 0 ||
                singleDifferentNymFromNetwork.isPresent();
    }

    private void processUserProfileAddedOrRefreshed(UserProfile userProfile) {
        processUserProfileAddedOrRefreshed(userProfile, false);
    }

    private void processUserProfileAddedOrRefreshed(UserProfile userProfile, boolean fromBatchProcessing) {
        String userProfileId = userProfile.getId();
        Optional<UserProfile> existingUserProfile = findUserProfile(userProfileId);
        // ApplicationVersion is excluded in equals check, so we check manually for it.
        if (existingUserProfile.isEmpty() ||
                !existingUserProfile.get().equals(userProfile) ||
                !existingUserProfile.get().getApplicationVersion().equals(userProfile.getApplicationVersion())) {
            if (verifyUserProfile(userProfile)) {
                synchronized (persistableStore) {
                    addNymToNickNameHashMap(userProfile.getNym(), userProfile.getNickName());
                    userProfileById.put(userProfileId, userProfile);
                }
                numUserProfiles.set(userProfileById.size());
                if (!fromBatchProcessing) {
                    // At initial batch processing we call persist at the end, to avoid many multiple persist calls
                    persist();
                }
            } else {
                log.warn("Invalid user profile {}", userProfile);
            }
        } else {
            if (userProfile.getPublishDate() > existingUserProfile.get().getPublishDate()) {
                existingUserProfile.get().setPublishDate(userProfile.getPublishDate());
                if (!fromBatchProcessing) {
                    // At initial batch processing we call persist at the end, to avoid many multiple persist calls
                    persist();
                }
            } else {
                log.debug("Ignore added userProfile as we have it already and nothing has changed");
            }
        }
    }

    private void processUserProfileRemoved(UserProfile userProfile) {
        synchronized (persistableStore) {
            removeNymFromNickNameHashMap(userProfile.getNym(), userProfile.getNickName());
            userProfileById.remove(userProfile.getId());
        }
        numUserProfiles.set(userProfileById.size());
        persist();
    }

    private boolean verifyUserProfile(UserProfile userProfile) {
        if (!Arrays.equals(userProfile.getProofOfWork().getPayload(), userProfile.getPubKeyHash())) {
            log.warn("Payload of proof of work not matching pubKeyHash of user profile {}", userProfile);
            return false;
        }

        if (!hashCashProofOfWorkService.verify(userProfile.getProofOfWork())) {
            log.warn("Proof of work verification of user profile {} failed", userProfile);
            return false;
        }

        return true;
    }

    public ReadOnlyObservableMap<String, UserProfile> getUserProfileById() {
        return userProfileById;
    }

    private void addNymToNickNameHashMap(String nym, String nickName) {
        Map<String, Set<String>> nymsByNickName = getNymsByNickName();
        if (!nymsByNickName.containsKey(nickName)) {
            nymsByNickName.put(nickName, new HashSet<>());
        }
        Set<String> nyms = nymsByNickName.get(nickName);
        nyms.add(nym);
        persist();
    }

    private void removeNymFromNickNameHashMap(String nym, String nickName) {
        Map<String, Set<String>> nymsByNickName = getNymsByNickName();
        if (!nymsByNickName.containsKey(nickName)) {
            return;
        }
        Set<String> nyms = nymsByNickName.get(nickName);
        nyms.remove(nym);
        persist();
    }
}
