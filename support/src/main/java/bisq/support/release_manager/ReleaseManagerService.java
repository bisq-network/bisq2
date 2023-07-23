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

package bisq.support.release_manager;

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ReleaseManagerService implements Service {
    @Getter
    public static class Config {
        private final boolean staticPublicKeysProvided;

        public Config(boolean staticPublicKeysProvided) {
            this.staticPublicKeysProvided = staticPublicKeysProvided;
        }

        public static ReleaseManagerService.Config from(com.typesafe.config.Config config) {
            return new ReleaseManagerService.Config(config.getBoolean("staticPublicKeysProvided"));
        }
    }

    private final NetworkService networkService;
    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;
    private final boolean staticPublicKeysProvided;

    public ReleaseManagerService(Config config,
                                 NetworkService networkService,
                                 UserService userService,
                                 BondedRolesService bondedRolesService) {
        userIdentityService = userService.getUserIdentityService();
        this.networkService = networkService;
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
        staticPublicKeysProvided = config.isStaticPublicKeysProvided();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> publishReleaseNotification(boolean isPreRelease,
                                                                 String releaseNotes,
                                                                 String version) {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        String profileId = userIdentity.getId();
        KeyPair keyPair = userIdentity.getIdentity().getKeyPair();
        PublicKey authorizedPublicKey = keyPair.getPublic();
        PrivateKey authorizedPrivateKey = keyPair.getPrivate();
        ReleaseNotification releaseNotification = new ReleaseNotification(StringUtils.createUid(),
                new Date().getTime(),
                isPreRelease,
                releaseNotes,
                version,
                profileId,
                staticPublicKeysProvided);
        return networkService.publishAuthorizedData(releaseNotification,
                        keyPair,
                        authorizedPrivateKey,
                        authorizedPublicKey)
                .thenApply(broadCastDataResult -> true);
    }

    public CompletableFuture<Boolean> removeReleaseNotification(ReleaseNotification releaseNotification, KeyPair ownerKeyPair) {
        return networkService.removeAuthorizedData(releaseNotification,
                        ownerKeyPair,
                        ownerKeyPair.getPublic())
                .thenApply(broadCastDataResult -> true);
    }
}