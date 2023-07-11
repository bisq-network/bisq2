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

import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.common.data.ByteArray;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedTimestampData;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class ProfileAgeService extends SourceReputationService<AuthorizedTimestampData> implements PersistenceClient<ProfileAgeStore> {
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    public static final long WEIGHT = 1;

    @Getter
    private final ProfileAgeStore persistableStore = new ProfileAgeStore();
    @Getter
    private final Persistence<ProfileAgeStore> persistence;

    public ProfileAgeService(PersistenceService persistenceService,
                             NetworkService networkService,
                             UserIdentityService userIdentityService,
                             UserProfileService userProfileService,
                             AuthorizedBondedRolesService authorizedBondedRolesService) {
        super(networkService, userIdentityService, userProfileService, authorizedBondedRolesService);
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        super.initialize();

        // We delay a bit to ensure the network is well established
        Scheduler.run(this::maybeRequestAgain).after(3, TimeUnit.SECONDS);

        userIdentityService.getNewlyCreatedUserIdentity().addObserver(userIdentity -> {
            if (userIdentity != null) {
                requestTimestamp(userIdentity.getUserProfile().getId());
            }
        });

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedTimestampData) {
            AuthorizedTimestampData data = (AuthorizedTimestampData) authenticatedData.getDistributedData();
            String userProfileId = data.getProfileId();
            userProfileService.findUserProfile(userProfileId)
                    .map(this::getUserProfileKey)
                    .ifPresent(dataSetByHash::remove);
            if (scoreByUserProfileId.containsKey(userProfileId)) {
                scoreByUserProfileId.remove(userProfileId);
                userProfileIdOfUpdatedScore.set(userProfileId);
            }
        }
    }

    @Override
    protected void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedTimestampData) {
            processData((AuthorizedTimestampData) authenticatedData.getDistributedData());
        }
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
        return Math.min(365, getAgeInDays(data.getDate())) * WEIGHT;
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
        // We check if we have some userProfiles which have not been timestamped yet (using the p2p network data).
        // If so, we request timestamping of the missing one.
        Set<String> myProfileIds = userIdentityService.getUserIdentities().stream()
                .map(userIdentity -> userIdentity.getUserProfile().getId())
                .collect(Collectors.toSet());
        networkService.getDataService().ifPresent(service -> service.getAuthenticatedData().forEach(authenticatedData -> {
            if (authenticatedData.getDistributedData() instanceof AuthorizedTimestampData) {
                AuthorizedTimestampData data = (AuthorizedTimestampData) authenticatedData.getDistributedData();
                myProfileIds.remove(data.getProfileId());
            }
        }));
        myProfileIds.forEach(this::requestTimestamp);

        // Before timeout gets triggered we request 
        long now = System.currentTimeMillis();
        if (now - persistableStore.getLastRequested() > AuthorizedTimestampData.TTL / 2) {
            persistableStore.getProfileIds().forEach(this::requestTimestamp);
            persistableStore.setLastRequested(now);
        }
    }
}