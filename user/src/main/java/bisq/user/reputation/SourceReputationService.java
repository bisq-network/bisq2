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

package bisq.user.reputation;

import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.observable.Observable;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public abstract class SourceReputationService<T extends AuthorizedDistributedData> implements DataService.Listener, Service {
    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    protected final Map<ByteArray, Set<T>> dataSetByHash = new ConcurrentHashMap<>();
    @Getter
    protected final Map<String, Long> scoreByUserProfileId = new ConcurrentHashMap<>();
    @Getter
    protected final Observable<String> changedUserProfileScore = new Observable<>();

    public SourceReputationService(NetworkService networkService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService) {
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
        this.userProfileService = userProfileService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAllAuthenticatedPayload()
                        .forEach(this::processAuthenticatedData));
        networkService.addDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        processAuthenticatedData(authenticatedData);
    }

    protected abstract void processAuthenticatedData(AuthenticatedData authenticatedData);

    protected void processData(T data) {
        ByteArray opReturnHash = getOpReturnHash(data);
        userProfileService.getUserProfileById().values().stream()
                .filter(userProfile -> getHash(userProfile).equals(opReturnHash))
                .forEach(userProfile -> {
                    ByteArray hash = getHash(userProfile);
                    if (!dataSetByHash.containsKey(hash)) {
                        dataSetByHash.put(hash, new HashSet<>());
                    }
                    dataSetByHash.get(hash).add(data);
                    Set<T> dataSet = dataSetByHash.get(hash);
                    putScore(userProfile.getId(), dataSet);
                });
    }

    protected abstract ByteArray getOpReturnHash(T data);

    protected abstract ByteArray getHash(UserProfile userProfile);

    protected void putScore(String userProfileId, Set<T> dataSet) {
        long score = dataSet.stream().mapToLong(this::calculateScore).sum();
        scoreByUserProfileId.put(userProfileId, score);
        changedUserProfileScore.set(userProfileId);
    }

    public long getScore(String userProfileId) {
        return scoreByUserProfileId.containsKey(userProfileId) ? scoreByUserProfileId.get(userProfileId) : 0;
    }

    protected abstract long calculateScore(T data);
}