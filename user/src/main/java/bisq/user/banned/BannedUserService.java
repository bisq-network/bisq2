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

package bisq.user.banned;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.RateLimiter;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class BannedUserService extends RateLimitedPersistenceClient<BannedUserStore> implements Service, AuthorizedBondedRolesService.Listener {
    // Limits of public chat messages and reactions per author (user profile ID)
    static final int MAX_MESSAGES_PER_SECOND = 3;
    static final int MAX_MESSAGES_PER_MINUTE = 30;
    static final int MAX_MESSAGES_PER_HOUR = 120;
    static final int MAX_MESSAGES_PER_DAY = 300;
    // Far more than the authors of honest chat traffic of one day. At the limit, an author with the newest events takes
    // at most about 7 KB, so the rate limiter takes at most about 70 MB.
    static final int MAX_RATE_LIMITED_AUTHORS = 10_000;

    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    private final BannedUserStore persistableStore = new BannedUserStore();
    @Getter
    private final Persistence<BannedUserStore> persistence;
    @Getter
    private final ObservableSet<String> rateLimitExceedingUserProfiles = new ObservableSet<>();
    private final RateLimiter rateLimiter;
    private final boolean rateLimitEnabled;

    public BannedUserService(PersistenceService persistenceService,
                             AuthorizedBondedRolesService authorizedBondedRolesService,
                             boolean rateLimitEnabled) {
        this(persistenceService,
                authorizedBondedRolesService,
                new RateLimiter(MAX_MESSAGES_PER_SECOND,
                        MAX_MESSAGES_PER_MINUTE,
                        MAX_MESSAGES_PER_HOUR,
                        MAX_MESSAGES_PER_DAY,
                        MAX_RATE_LIMITED_AUTHORS),
                rateLimitEnabled);
    }

    BannedUserService(PersistenceService persistenceService,
                      AuthorizedBondedRolesService authorizedBondedRolesService,
                      RateLimiter rateLimiter,
                      boolean rateLimitEnabled) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.CACHE, persistableStore);
        this.authorizedBondedRolesService = authorizedBondedRolesService;
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.rateLimitEnabled = rateLimitEnabled;
        if (rateLimitEnabled) {
            // A profile stays flagged at most while the rate limiter keeps its events. Otherwise the flag of a profile
            // which sends nothing more would stay until shutdown.
            rateLimiter.addRemovedKeyListener(rateLimitExceedingUserProfiles::remove);
        } else {
            log.warn("Chat profile rate limiting disabled. Use only for local/dev testing.");
        }
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        authorizedBondedRolesService.addListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        authorizedBondedRolesService.removeListener(this);
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // AuthorizedBondedRolesService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof BannedUserProfileData) {
            if (isAuthorized(authorizedData)) {
                BannedUserProfileData bannedUserProfileData = (BannedUserProfileData) authorizedData.getAuthorizedDistributedData();
                getBannedUserProfileDataSet().add(bannedUserProfileData);
                persist();
            }
        }
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof BannedUserProfileData) {
            if (isAuthorized(authorizedData)) {
                BannedUserProfileData bannedUserProfileData = (BannedUserProfileData) authorizedData.getAuthorizedDistributedData();
                getBannedUserProfileDataSet().remove(bannedUserProfileData);
                persist();
            }
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public ObservableSet<BannedUserProfileData> getBannedUserProfileDataSet() {
        return persistableStore.getBannedUserProfileDataSet();
    }

    public boolean isUserProfileBanned(UserProfile userProfile) {
        return isUserProfileBanned(userProfile.getId());
    }

    public boolean isUserProfileBanned(NetworkId networkId) {
        return isUserProfileBanned(networkId.getId());
    }

    public boolean isUserProfileBanned(String userProfileId) {
        return getBannedUserProfileDataSet().stream().anyMatch(e -> e.getUserProfile().getId().equals(userProfileId));
    }

    public void checkRateLimit(String userProfileId, long timeStamp) {
        if (!rateLimitEnabled) {
            return;
        }
        boolean exceedsLimit = rateLimiter.exceedsLimit(userProfileId, timeStamp);
        if (exceedsLimit) {
            log.warn("User with profile ID {} exceeded rate limit.", userProfileId);
            rateLimitExceedingUserProfiles.remove(userProfileId); // For triggering observable update
            rateLimitExceedingUserProfiles.add(userProfileId);
            persist();
        }
    }

    public boolean isRateLimitExceeding(UserProfile userProfile) {
        return isRateLimitExceeding(userProfile.getId());
    }

    public boolean isRateLimitExceeding(String userProfileId) {
        if (!rateLimitEnabled) {
            return false;
        }
        refresh(userProfileId);
        return rateLimitExceedingUserProfiles.contains(userProfileId);
    }

    public Optional<String> getExceedsLimitInfo(String userProfileId) {
        if (!rateLimitEnabled) {
            return Optional.empty();
        }
        return rateLimiter.getExceedsLimitInfo(userProfileId);
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void refresh(String userProfileId) {
        // A flagged profile loses its flag when the time windows have moved and it no longer exceeds the limit
        if (rateLimitExceedingUserProfiles.contains(userProfileId) && !rateLimiter.exceedsLimit(userProfileId)) {
            rateLimitExceedingUserProfiles.remove(userProfileId);
        }
    }


    private boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.MODERATOR);
    }
}
