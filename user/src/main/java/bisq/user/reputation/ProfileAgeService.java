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
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.data.AuthorizedTimestampData;
import bisq.user.reputation.requests.AuthorizeTimestampRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Nullable
    private CompletableFuture<Void> requestTimestampFuture;
    @Nullable
    private Scheduler requestTimestampScheduler;

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

        requestTimestampScheduler = Scheduler.run(this::maybeRequestTimestampForAllUserProfiles)
                .host(this)
                .runnableName("requestTimestamp")
                .repeated(5, 90, TimeUnit.SECONDS, 2);

        userIdentityService.getNewlyCreatedUserIdentity().addObserver(userIdentity -> {
            if (userIdentity != null) {
                requestTimestamp(userIdentity);
                // lastRequested is set only when we check for all user profiles. When we create a new user profile
                // we do not update it.
            }
        });

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (requestTimestampScheduler != null) {
            requestTimestampScheduler.stop();
            requestTimestampScheduler = null;
        }
        if (requestTimestampFuture != null) {
            requestTimestampFuture.cancel(true);
            requestTimestampFuture = null;
        }
        return super.shutdown();
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
        // We only have one item added as we add only if empty.
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
    }

    public Optional<Long> getProfileAge(UserProfile userProfile) {
        return Optional.ofNullable(dataSetByHash.get(userProfile.getProfileAgeKey()))
                .flatMap(e -> e.stream().findFirst()) // We have only 1 item in the dataSet
                .map(AuthorizedTimestampData::getDate);
    }

    private boolean requestTimestamp(UserIdentity userIdentity) {
        log.info("Request timestamp for {}", userIdentity.getUserProfile().getUserName());
        return send(userIdentity, new AuthorizeTimestampRequest(userIdentity.getId()));
    }

    private void maybeRequestTimestampForAllUserProfiles() {
        if (requestTimestampFuture != null) {
            log.warn("requestTimestampFuture is still not completed");
            return;
        }
        // We check if we have some userProfiles which have not been timestamped yet.
        // If so, we request timestamping of the missing one.
        Set<String> timeStampedProfileIds = getTimeStampedProfileIds();
        List<UserIdentity> candidates = userIdentityService.getUserIdentities().stream()
                .filter(userIdentity -> requireTimestamp(userIdentity, timeStampedProfileIds))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return;
        }

        Collections.shuffle(candidates);

        requestTimestampFuture = CompletableFuture.runAsync(() -> {
            try {
                AtomicBoolean hasRequested = new AtomicBoolean();
                for (int i = 0; i < candidates.size(); i++) {
                    long delay = i * (10_000 + new Random().nextLong(110_000));
                    UserIdentity userIdentity = candidates.get(i);
                    // We do not pass an Executor thus we the default ForkJoinPool.commonPool() is used.
                    // The requestTimestamp() is using the NetworkService.NETWORK_IO_POOL at network call level.
                    // At first iteration the delay is 0, thus no delay is used.
                    CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(() -> {
                        boolean requestSuccess = requestTimestamp(userIdentity);
                        if (!requestSuccess) {
                            log.warn("Requesting timestamp for {} failed", userIdentity.getUserProfile().getUserName());
                        }
                        hasRequested.compareAndSet(false, requestSuccess);
                    });
                }

                if (hasRequested.get()) {
                    // lastRequested is set only when we check for all user profiles. When we create a new user profile
                    // we do not update it.
                    persistableStore.setLastRequested(System.currentTimeMillis());
                    persist();
                }
            } finally {
                requestTimestampFuture = null;
            }
        }, ExecutorFactory.newSingleThreadScheduledExecutor("requestTimestamp"));
    }

    // If not already time stamped, or if time since last timestamp is > as half of TTL
    private boolean requireTimestamp(UserIdentity userIdentity, Set<String> timeStampedProfileIds) {
        boolean notAlreadyTimestamped = !timeStampedProfileIds.contains(userIdentity.getUserProfile().getId());
        return notAlreadyTimestamped || isExpiringSoon();
    }

    private boolean isExpiringSoon() {
        long lastRequested = persistableStore.getLastRequested();
        if (lastRequested == 0) {
            return false;
        }
        return System.currentTimeMillis() - lastRequested > AuthorizedTimestampData.TTL / 2;
    }

    private Set<String> getTimeStampedProfileIds() {
        return networkService.getDataService()
                .map(service -> service.getAuthorizedData()
                        .filter(authorizedData -> authorizedData.getAuthorizedDistributedData() instanceof AuthorizedTimestampData)
                        .map(authorizedData -> (AuthorizedTimestampData) authorizedData.getAuthorizedDistributedData())
                        .map(AuthorizedTimestampData::getProfileId)
                        .collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
    }
}