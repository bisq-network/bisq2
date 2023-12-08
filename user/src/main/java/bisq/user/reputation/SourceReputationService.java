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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.data.ByteArray;
import bisq.common.observable.Observable;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;


@Slf4j
public abstract class SourceReputationService<T extends AuthorizedDistributedData> implements DataService.Listener, Service {
    protected static final long DAY_AS_MS = TimeUnit.DAYS.toMillis(1);

    public static long getAgeInDays(long date) {
        return (System.currentTimeMillis() - date) / DAY_AS_MS;
    }

    protected final NetworkService networkService;
    protected final UserIdentityService userIdentityService;
    protected final UserProfileService userProfileService;
    private final BannedUserService bannedUserService;
    protected final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    protected final Map<ByteArray, Set<T>> dataSetByHash = new ConcurrentHashMap<>();
    @Getter
    protected final Map<String, Long> scoreByUserProfileId = new ConcurrentHashMap<>();
    @Getter
    protected final Observable<String> userProfileIdOfUpdatedScore = new Observable<>();

    public SourceReputationService(NetworkService networkService,
                                   UserIdentityService userIdentityService,
                                   UserProfileService userProfileService,
                                   BannedUserService bannedUserService,
                                   AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.networkService = networkService;
        this.userIdentityService = userIdentityService;
        this.userProfileService = userProfileService;
        this.bannedUserService = bannedUserService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        networkService.getDataService().ifPresent(dataService -> dataService.getAuthorizedData().forEach(this::onAuthorizedDataAdded));
        networkService.addDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        findRelevantData(authorizedData.getAuthorizedDistributedData())
                .ifPresent(data -> {
                    ByteArray providedHash = getDataKey(data);
                    userProfileService.getUserProfileById().values().stream()
                            .filter(userProfile -> getUserProfileKey(userProfile).equals(providedHash))
                            .forEach(userProfile -> {
                                ByteArray hash = getUserProfileKey(userProfile);
                                if (!dataSetByHash.containsKey(hash)) {
                                    dataSetByHash.put(hash, new HashSet<>());
                                }
                                Set<T> dataSet = dataSetByHash.get(hash);
                                addToDataSet(dataSet, data);
                                putScore(userProfile.getId(), dataSet);
                            });
                });
    }

    protected abstract Optional<T> findRelevantData(AuthorizedDistributedData authorizedDistributedData);

    // Some services don't support multiple entries and will override that method
    protected void addToDataSet(Set<T> dataSet, T data) {
        dataSet.add(data);
    }

    protected abstract ByteArray getDataKey(T data);

    protected abstract ByteArray getUserProfileKey(UserProfile userProfile);

    protected void putScore(String userProfileId, Set<T> dataSet) {
        long score = dataSet.stream().mapToLong(this::calculateScore).sum();
        scoreByUserProfileId.put(userProfileId, score);
        userProfileIdOfUpdatedScore.set(userProfileId);
    }

    protected boolean send(UserIdentity userIdentity, EnvelopePayloadMessage request) {
        checkArgument(!bannedUserService.isUserProfileBanned(userIdentity.getUserProfile()));
        if (authorizedBondedRolesService.getAuthorizedOracleNodes().isEmpty()) {
            return false;
        }
        authorizedBondedRolesService.getAuthorizedOracleNodes().forEach(oracleNode ->
                networkService.confidentialSend(request, oracleNode.getNetworkId(), userIdentity.getNetworkIdWithKeyPair()));
        return true;
    }

    public long getScore(String userProfileId) {
        return scoreByUserProfileId.containsKey(userProfileId) ? scoreByUserProfileId.get(userProfileId) : 0;
    }

    public abstract long calculateScore(T data);
}