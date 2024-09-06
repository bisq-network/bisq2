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

package bisq.user;

import bisq.common.application.Service;
import bisq.common.timer.Scheduler;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RepublishUserProfileService implements Service {
    public static final long MIN_PAUSE_TO_NEXT_REPUBLISH = TimeUnit.MINUTES.toMillis(5);

    private final UserIdentityService userIdentityService;
    private UserIdentity selectedUserIdentity;
    private long lastPublished;
    private long republishCounter;

    public RepublishUserProfileService(UserIdentityService userIdentityService) {
        this.userIdentityService = userIdentityService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> {
            if (userIdentity != null && !userIdentity.getIdentity().isDefaultTag()) {
                selectedUserIdentity = userIdentity;
                userActivityDetected();
            }
        });
        Scheduler.run(() -> {
                    KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();
                    UserProfile userProfile = selectedUserIdentity.getUserProfile();
                    userIdentityService.publishUserProfile(userProfile, keyPair);
                })
                .host(this)
                .runnableName("republish")
                .after(1, TimeUnit.MINUTES);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    public void userActivityDetected() {
        long now = System.currentTimeMillis();
        if (now - lastPublished < MIN_PAUSE_TO_NEXT_REPUBLISH || selectedUserIdentity == null) {
            return;
        }
        lastPublished = now;
        KeyPair keyPair = selectedUserIdentity.getNetworkIdWithKeyPair().getKeyPair();
        UserProfile userProfile = selectedUserIdentity.getUserProfile();

        // Every 10 times we publish instead of refresh for more resilience in case the data has not reached the whole network.
        republishCounter++;
        if (republishCounter % 10 == 0) {
            userIdentityService.publishUserProfile(userProfile, keyPair);
        } else {
            userIdentityService.refreshUserProfile(userProfile, keyPair);
        }
    }
}
