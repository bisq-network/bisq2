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
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.pow.ProofOfWorkService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class UserProfileService implements PersistenceClient<UserProfileStore>, DataService.Listener, Service {
    private static final String SEPARATOR_START = " [";
    private static final String SEPARATOR_END = "]";

    @Getter
    private final UserProfileStore persistableStore = new UserProfileStore();
    @Getter
    private final Persistence<UserProfileStore> persistence;
    private final NetworkService networkService;
    private final ProofOfWorkService proofOfWorkService;
    @Getter
    private final Observable<Boolean> userProfilesUpdateFlag = new Observable<>(true);

    public UserProfileService(PersistenceService persistenceService,
                              NetworkService networkService,
                              ProofOfWorkService proofOfWorkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.networkService = networkService;
        this.proofOfWorkService = proofOfWorkService;
        UserNameLookup.setUserProfileService(this);
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(ds -> ds.getAuthenticatedData().forEach(this::onAuthenticatedDataAdded));
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof UserProfile) {
            UserProfile userProfile = (UserProfile) distributedData;
            Optional<UserProfile> optionalChatUser = findUserProfile(userProfile.getId());
            if (optionalChatUser.isEmpty() || !optionalChatUser.get().equals(userProfile)) {
                getUserProfileById().put(userProfile.getId(), userProfile);
                notifyObservers();
                persist();
            }
        }
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        DistributedData distributedData = authenticatedData.getDistributedData();
        if (distributedData instanceof UserProfile) {
            UserProfile userProfile = (UserProfile) distributedData;
            getUserProfileById().remove(userProfile.getId());
            notifyObservers();
            persist();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<UserProfile> findUserProfile(String id) {
        return Optional.ofNullable(getUserProfileById().get(id));
    }

    public List<UserProfile> getUserProfiles() {
        return new ArrayList<>(getUserProfileById().values());
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
        getIgnoredUserProfileIds().add(userProfile.getId());
        persist();
    }

    public void undoIgnoreUserProfile(UserProfile userProfile) {
        getIgnoredUserProfileIds().remove(userProfile.getId());
        persist();
    }

    public ObservableSet<String> getIgnoredUserProfileIds() {
        return persistableStore.getIgnoredUserProfileIds();
    }

    public String getUserName(String nym, String nickName) {
        Map<String, Set<String>> nymsByNickName = getNymsByNickName();
        if (!nymsByNickName.containsKey(nickName)) {
            nymsByNickName.put(nickName, new HashSet<>());
        }

        Set<String> nyms = nymsByNickName.get(nickName);
        nyms.add(nym);
        persist();
        if (nyms.size() == 1) {
            return nickName;
        } else {
            return nickName + SEPARATOR_START + nym + SEPARATOR_END;
        }
    }


    private Map<String, Set<String>> getNymsByNickName() {
        return persistableStore.getNymsByNickName();
    }

    public Map<String, UserProfile> getUserProfileById() {
        return persistableStore.getUserProfileById();
    }

    private void notifyObservers() {
        userProfilesUpdateFlag.set(!userProfilesUpdateFlag.get());
    }
}
