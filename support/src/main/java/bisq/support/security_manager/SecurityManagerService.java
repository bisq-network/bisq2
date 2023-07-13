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
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.network.NetworkService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SecurityManagerService implements Service {
    private final NetworkService networkService;
    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;

    public SecurityManagerService(NetworkService networkService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService) {
        userIdentityService = userService.getUserIdentityService();
        this.networkService = networkService;
        authorizedBondedRolesService = bondedRolesService.getAuthorizedBondedRolesService();
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

    public CompletableFuture<Boolean> publishAlert(KeyPair ownerKeyPair,
                                                   AuthorizedAlertData authorizedAlertData,
                                                   PrivateKey authorizedPrivateKey,
                                                   PublicKey authorizedPublicKey) {
        return networkService.publishAuthorizedData(authorizedAlertData,
                        ownerKeyPair,
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