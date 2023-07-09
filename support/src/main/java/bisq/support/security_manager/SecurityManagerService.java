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
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.identity.IdentityService;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.security.KeyGeneration;
import bisq.support.alert.AuthorizedAlertData;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
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
    private final UserProfileService userProfileService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final UserIdentityService userIdentityService;

    public SecurityManagerService(NetworkService networkService,
                                  IdentityService identityService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService) {
        this.userProfileService = userService.getUserProfileService();
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
                                                   String privateKey,
                                                   String publicKey) throws GeneralSecurityException {
        //     checkArgument(notificationSenders.stream().anyMatch(data -> data.getUserProfile().getNetworkId().equals(networkIdWithKeyPair.getNetworkId())));
        PrivateKey authorizedPrivateKey = KeyGeneration.generatePrivate(Hex.decode(privateKey));
        PublicKey authorizedPublicKey = KeyGeneration.generatePublic(Hex.decode(publicKey));

        Optional<NetworkIdWithKeyPair> rr = findMyNodeIdAndKeyPair();

        return networkService.publishAuthorizedData(authorizedAlertData,
                        networkIdWithKeyPair,
                        authorizedPrivateKey,
                        authorizedPublicKey)
                .thenApply(broadCastDataResult -> true);
    }

    private Optional<NetworkIdWithKeyPair> findMyNodeIdAndKeyPair() {
        return authorizedBondedRolesService.getAuthorizedBondedRoleSet().stream()
                .filter(r -> r.getBondedRoleType() == BondedRoleType.SECURITY_MANAGER)
                .flatMap(r -> userIdentityService.findUserIdentity(r.getProfileId()).stream())
                .map(userIdentity -> userIdentity.getNodeIdAndKeyPair()).findAny();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    private void updateHasNotificationSenderIdentity() {
       /* hasNotificationSenderIdentity.set(notificationSenders.stream()
                .anyMatch(data -> userProfileService.findUserProfile(data.getUserProfile().getId()).isPresent()));*/
    }
}