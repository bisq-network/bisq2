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

package bisq.support.security_manager;

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.alert.AlertType;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class SecurityManagerService implements Service {
    @Getter
    public static class Config {
        private final boolean staticPublicKeysProvided;

        public Config(boolean staticPublicKeysProvided) {
            this.staticPublicKeysProvided = staticPublicKeysProvided;
        }

        public static SecurityManagerService.Config from(com.typesafe.config.Config config) {
            return new SecurityManagerService.Config(config.getBoolean("staticPublicKeysProvided"));
        }
    }

    private final NetworkService networkService;
    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final Config config;
    private final UserIdentityService userIdentityService;
    private final boolean staticPublicKeysProvided;

    public SecurityManagerService(Config config,
                                  NetworkService networkService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService) {
        this.config = config;
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

    public CompletableFuture<Boolean> publishAlert(AlertType alertType,
                                                   Optional<String> message,
                                                   boolean haltTrading,
                                                   boolean requireVersionForTrading,
                                                   Optional<String> minVersion,
                                                   Optional<AuthorizedBondedRole> bannedRole) {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        String profileId = userIdentity.getId();
        KeyPair keyPair = userIdentity.getIdentity().getKeyPair();
        PublicKey authorizedPublicKey = keyPair.getPublic();
        PrivateKey authorizedPrivateKey = keyPair.getPrivate();
        AuthorizedAlertData authorizedAlertData = new AuthorizedAlertData(StringUtils.createUid(),
                new Date().getTime(),
                alertType,
                message,
                haltTrading,
                requireVersionForTrading,
                minVersion,
                bannedRole,
                profileId,
                staticPublicKeysProvided);
        return networkService.publishAuthorizedData(authorizedAlertData,
                        keyPair,
                        authorizedPrivateKey,
                        authorizedPublicKey)
                .thenApply(broadCastDataResult -> true);
    }

    public CompletableFuture<Boolean> removeAlert(AuthorizedAlertData authorizedAlertData, KeyPair ownerKeyPair) {
        return networkService.removeAuthorizedData(authorizedAlertData,
                        ownerKeyPair,
                        ownerKeyPair.getPublic())
                .thenApply(broadCastDataResult -> true);
    }
}