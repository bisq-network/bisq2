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

import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SecurityManagerService implements Service {
    private final NetworkService networkService;
    @Getter
    private final ObservableSet<AuthorizedAlertData> authorizedAlertData = new ObservableSet<>();
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

    public CompletableFuture<Boolean> publishAlert(NetworkIdWithKeyPair networkIdWithKeyPair,
                                                   AuthorizedAlertData authorizedAlertData,
                                                   PrivateKey privateKey,
                                                   PublicKey publicKey) {
        return networkService.publishAuthorizedData(authorizedAlertData,
                        networkIdWithKeyPair,
                        privateKey,
                        publicKey)
                .thenApply(broadCastDataResult -> true);
    }

    public CompletableFuture<Boolean> removeAlert(AuthorizedData authorizedData, NetworkIdWithKeyPair ownerNetworkIdWithKeyPair) {
        return networkService.removeAuthorizedData(authorizedData, ownerNetworkIdWithKeyPair)
                .thenApply(broadCastDataResult -> true);
    }

    private Optional<NetworkIdWithKeyPair> findMyNodeIdAndKeyPair() {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        return authorizedBondedRolesService.getAuthorizedBondedRoleSet().stream()
                .filter(bondedRole -> bondedRole.getBondedRoleType() == BondedRoleType.SECURITY_MANAGER)
                .filter(bondedRole -> selectedUserIdentity != null)
                .filter(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId()))
                .map(bondedRole -> selectedUserIdentity.getNodeIdAndKeyPair())
                .findAny();
    }
}