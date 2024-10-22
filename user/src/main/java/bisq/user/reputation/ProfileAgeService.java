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
import bisq.common.data.ByteArray;
import bisq.common.data.Pair;
import bisq.common.threading.ExecutorFactory;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedTimestampData;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * We do not apply a score for profile age as otherwise all users would have such a score after 1 day.
 * Consider to remove inheritance from the SourceReputationService and keep it outside the reputation framework.
 */
@Getter
@Slf4j
public class ProfileAgeService extends SourceReputationService<AuthorizedTimestampData> implements PersistenceClient<ProfileAgeStore> {

    @Getter
    private final ProfileAgeStore persistableStore = new ProfileAgeStore();
    @Getter
    private final Persistence<ProfileAgeStore> persistence;

    public ProfileAgeService(PersistenceService persistenceService,
                             NetworkService networkService,
                             UserIdentityService userIdentityService,
                             UserProfileService userProfileService,
                             BannedUserService bannedUserService,
                             AuthorizedBondedRolesService authorizedBondedRolesService) {
        super(networkService, userIdentityService, userProfileService, bannedUserService, authorizedBondedRolesService);
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        super.initialize();

        // We delay a bit to ensure the network is well established
        Scheduler.run(this::maybeRequestAgain)
                .host(this)
                .runnableName("maybeRequestAgain")
                .after(3, TimeUnit.SECONDS);

        userIdentityService.getNewlyCreatedUserIdentity().addObserver(userIdentity -> {
            if (userIdentity != null) {
                requestTimestamp(userIdentity.getUserProfile().getId());
            }
        });

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedTimestampData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedTimestampData timestampData = (AuthorizedTimestampData) authorizedData.getAuthorizedDistributedData();
                String userProfileId = timestampData.getProfileId();
                userProfileService.findUserProfile(userProfileId)
                        .map(this::getUserProfileKey)
                        .ifPresent(dataSetByHash::remove);
                if (scoreByUserProfileId.containsKey(userProfileId)) {
                    scoreByUserProfileId.remove(userProfileId);
                    userProfileIdScorePair.set(new Pair<>(userProfileId, 0L));
                }
            }
        }
    }

    @Override
    protected Optional<AuthorizedTimestampData> findRelevantData(AuthorizedDistributedData authorizedDistributedData) {
        return authorizedDistributedData instanceof AuthorizedTimestampData ?
                Optional.of((AuthorizedTimestampData) authorizedDistributedData) :
                Optional.empty();
    }

    @Override
    protected void addToDataSet(Set<AuthorizedTimestampData> dataSet, AuthorizedTimestampData data) {
        if (dataSet.isEmpty()) {
            dataSet.add(data);
            return;
        }

        // If new data is older than existing entry we clear set and add our new data, otherwise we ignore the new data.
        AuthorizedTimestampData existing = new ArrayList<>(dataSet).get(0);
        if (existing.getDate() > data.getDate()) {
            dataSet.clear();
            dataSet.add(data);
        }
    }

    @Override
    protected ByteArray getDataKey(AuthorizedTimestampData data) {
        return new ByteArray(data.getProfileId().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected ByteArray getUserProfileKey(UserProfile userProfile) {
        return userProfile.getProfileAgeKey();
    }

    @Override
    public long calculateScore(AuthorizedTimestampData data) {
        // We do not apply any reputation score to the profile age
        return 0;
        //Math.min(365, getAgeInDays(data.getDate())) * WEIGHT;
    }

    public Optional<Long> getProfileAge(UserProfile userProfile) {
        return Optional.ofNullable(dataSetByHash.get(userProfile.getProfileAgeKey()))
                .flatMap(e -> e.stream().findFirst())
                .map(AuthorizedTimestampData::getDate);
    }

    private boolean requestTimestamp(String profileId) {
        return userIdentityService.findUserIdentity(profileId).map(userIdentity -> {
                    AuthorizeTimestampRequest request = new AuthorizeTimestampRequest(profileId);
                    return send(userIdentity, request);
                })
                .orElse(false);
    }

    private void maybeRequestAgain() {
        boolean didRequestForAllProfileIds = requestForAllProfileIdsBeforeExpired();
        if (!didRequestForAllProfileIds) {
            // We check if we have some userProfiles which have not been timestamped yet.
            // If so, we request timestamping of the missing one.
            var timeStamped = networkService.getDataService()
                    .map(service -> service.getAuthorizedData()
                            .filter(authorizedData -> authorizedData.getAuthorizedDistributedData() instanceof AuthorizedTimestampData)
                            .map(authorizedData -> (AuthorizedTimestampData) authorizedData.getAuthorizedDistributedData())
                            .map(AuthorizedTimestampData::getProfileId)
                            .collect(Collectors.toSet()));
            userIdentityService.getUserIdentities().stream()
                    .map(userIdentity -> userIdentity.getUserProfile().getId())
                    .filter(profileId -> timeStamped.isEmpty() || !timeStamped.get().contains(profileId))
                    .forEach(this::requestTimestamp);
        }
    }

    private boolean requestForAllProfileIdsBeforeExpired() {
        // Before timeout gets triggered we request 
        long now = System.currentTimeMillis();
        if (now - persistableStore.getLastRequested() > AuthorizedTimestampData.TTL / 2) {
            persistableStore.setLastRequested(now);
            persist();

            Set<String> profileIds = new HashSet<>(persistableStore.getProfileIds());
            CompletableFuture.runAsync(() -> profileIds.forEach(userProfileId -> {
                        requestTimestamp(userProfileId);
                        long delay = 30_000 + new Random().nextInt(90_000);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ignore) {
                        }
                    }),
                    ExecutorFactory.newSingleThreadScheduledExecutor("requestForAllProfileIdsBeforeExpired"));
            return true;
        }
        return false;
    }
}