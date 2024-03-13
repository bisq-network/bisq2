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

package bisq.bonded_roles.security_manager.alert;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class AlertService implements Service, AuthorizedBondedRolesService.Listener {

    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    private final ObservableSet<AuthorizedAlertData> authorizedAlertDataSet = new ObservableSet<>();

    public AlertService(AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // AuthorizedBondedRolesService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedAlertData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedAlertData authorizedAlertData = (AuthorizedAlertData) authorizedData.getAuthorizedDistributedData();
                authorizedAlertDataSet.add(authorizedAlertData);
                maybeApplyBannedState(authorizedAlertData, true);
            }
        }
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedAlertData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedAlertData authorizedAlertData = (AuthorizedAlertData) authorizedData.getAuthorizedDistributedData();
                authorizedAlertDataSet.remove(authorizedAlertData);
                maybeApplyBannedState(authorizedAlertData, false);
            }
        }
    }

    private boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.SECURITY_MANAGER);
    }

    private void maybeApplyBannedState(AuthorizedAlertData authorizedAlertData, boolean wasAdded) {
        if (authorizedAlertData.getAlertType() == AlertType.BAN && authorizedAlertData.getBannedRole().isPresent()) {
            authorizedBondedRolesService.getBondedRoles().stream()
                    .filter(bondedRole -> bondedRole.getAuthorizedBondedRole().equals(authorizedAlertData.getBannedRole().get()))
                    .findAny()
                    .ifPresent(bondedRole -> bondedRole.setIsBanned(wasAdded));
        }
    }

}