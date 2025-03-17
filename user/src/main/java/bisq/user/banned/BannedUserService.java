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
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class BannedUserService implements PersistenceClient<BannedUserStore>, Service, AuthorizedBondedRolesService.Listener {
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    private final BannedUserStore persistableStore = new BannedUserStore();
    @Getter
    private final Persistence<BannedUserStore> persistence;
    @Getter
    private final ObservableSet<String> rateLimitExceedingUserProfiles = new ObservableSet<>();
    private final RateLimiter rateLimiter = new RateLimiter();

    public BannedUserService(PersistenceService persistenceService,
                             AuthorizedBondedRolesService authorizedBondedRolesService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.CACHE, persistableStore);
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        authorizedBondedRolesService.addListener(this);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
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

    public boolean isUserProfileBanned(String userProfileId) {
        return getBannedUserProfileDataSet().stream().anyMatch(e -> e.getUserProfile().getId().equals(userProfileId));
    }

    public boolean isUserProfileBanned(UserProfile userProfile) {
        return getBannedUserProfileDataSet().stream().anyMatch(e -> e.getUserProfile().equals(userProfile));
    }

    public boolean isNetworkIdBanned(NetworkId networkId) {
        return getBannedUserProfileDataSet().stream()
                .anyMatch(e -> e.getUserProfile().getNetworkId().equals(networkId));
    }

    public void checkRateLimit(String userProfileId, long timeStamp) {
        boolean exceedsLimit = rateLimiter.exceedsLimit(userProfileId, timeStamp);
        if (exceedsLimit) {
            log.warn("User with profile ID {} exceeded rate limit.", userProfileId);
            rateLimitExceedingUserProfiles.remove(userProfileId); // For triggering observable update
            rateLimitExceedingUserProfiles.add(userProfileId);
            persist();
        }
    }

    public boolean isRateLimitExceeding(String userProfileId) {
        refresh(userProfileId);
        return rateLimitExceedingUserProfiles.contains(userProfileId);
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void refresh(String userProfileId) {
        Set<String> clone = new HashSet<>(rateLimitExceedingUserProfiles);
        clone.stream().filter(e -> e.equals(userProfileId))
                .filter(e -> !rateLimiter.exceedsLimit(e)) // Time window has moved so we are not exceeding anymore
                .findAny()
                .ifPresent(rateLimitExceedingUserProfiles::remove);
    }

    private boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.MODERATOR);
    }
}