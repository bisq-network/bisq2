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
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
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
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    private final UserIdentityService userIdentityService;
    private final boolean staticPublicKeysProvided;

    public SecurityManagerService(Config config,
                                  NetworkService networkService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService) {
        userIdentityService = userService.getUserIdentityService();
        this.networkService = networkService;
        staticPublicKeysProvided = config.isStaticPublicKeysProvided();
    }


    /* --------------------------------------------------------------------- */
    // Service

    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    /* --------------------------------------------------------------------- */
    // API

    /* --------------------------------------------------------------------- */

    public CompletableFuture<Boolean> publishAlert(AlertType alertType,
                                                   Optional<String> headline,
                                                   Optional<String> message,
                                                   boolean haltTrading,
                                                   boolean requireVersionForTrading,
                                                   Optional<String> minVersion,
                                                   Optional<AuthorizedBondedRole> bannedRole,
                                                   Optional<String> bannedAccountData
    ) {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        String securityManagerProfileId = userIdentity.getId();
        KeyPair keyPair = userIdentity.getIdentity().getKeyBundle().getKeyPair();
        AuthorizedAlertData authorizedAlertData = new AuthorizedAlertData(StringUtils.createUid(),
                new Date().getTime(),
                alertType,
                headline,
                message,
                haltTrading,
                requireVersionForTrading,
                minVersion,
                bannedRole,
                securityManagerProfileId,
                staticPublicKeysProvided,
                bannedAccountData);
        return networkService.publishAuthorizedData(authorizedAlertData, keyPair)
                .thenApply(broadCastDataResult -> true);
    }

    public CompletableFuture<Boolean> rePublishAlert(AuthorizedAlertData authorizedAlertData, KeyPair ownerKeyPair) {
        return networkService.publishAuthorizedData(authorizedAlertData, ownerKeyPair)
                .thenApply(broadCastDataResult -> true);
    }

    public CompletableFuture<Boolean> removeAlert(AuthorizedAlertData authorizedAlertData, KeyPair ownerKeyPair) {
        return networkService.removeAuthorizedData(authorizedAlertData, ownerKeyPair, ownerKeyPair.getPublic())
                .thenApply(broadCastDataResult -> true);
    }

    public CompletableFuture<Boolean> publishDifficultyAdjustment(double difficultyAdjustmentFactor) {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        String securityManagerProfileId = userIdentity.getId();
        KeyPair keyPair = userIdentity.getIdentity().getKeyBundle().getKeyPair();
        AuthorizedDifficultyAdjustmentData data = new AuthorizedDifficultyAdjustmentData(new Date().getTime(),
                difficultyAdjustmentFactor,
                securityManagerProfileId,
                staticPublicKeysProvided);
        return networkService.publishAuthorizedData(data, keyPair)
                .thenApply(broadCastDataResult -> true);
    }

    public CompletableFuture<Boolean> removeDifficultyAdjustment(AuthorizedDifficultyAdjustmentData data,
                                                                 KeyPair ownerKeyPair) {
        return networkService.removeAuthorizedData(data, ownerKeyPair, ownerKeyPair.getPublic())
                .thenApply(broadCastDataResult -> true);
    }
}