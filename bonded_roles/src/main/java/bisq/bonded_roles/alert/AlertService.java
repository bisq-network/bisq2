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

package bisq.bonded_roles.alert;

import bisq.bonded_roles.AuthorizedBondedRolesService;
import bisq.bonded_roles.BondedRoleType;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class AlertService implements Service, DataService.Listener {
    private final NetworkService networkService;
    @Getter
    private final ObservableSet<AuthorizedData> authorizedDataSet = new ObservableSet<>();
    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    public AlertService(NetworkService networkService, AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.networkService = networkService;
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.addDataServiceListener(this);
        networkService.getDataService().ifPresent(service -> service.getAuthenticatedData().forEach(this::processAddedAuthenticatedData));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        networkService.removeDataServiceListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
        processAddedAuthenticatedData(authenticatedData);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
        processRemovedAuthenticatedData(authenticatedData);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAddedAuthenticatedData(AuthenticatedData authenticatedData) {
        findAuthorizedDataOfAlertData(authenticatedData).ifPresent(authorizedDataSet::add);
    }

    private void processRemovedAuthenticatedData(AuthenticatedData authenticatedData) {
        findAuthorizedDataOfAlertData(authenticatedData).ifPresent(authorizedDataSet::remove);
    }

    private Optional<AuthorizedData> findAuthorizedDataOfAlertData(AuthenticatedData authenticatedData) {
        if (authenticatedData instanceof AuthorizedData) {
            AuthorizedData authorizedData = (AuthorizedData) authenticatedData;
            DistributedData distributedData = authorizedData.getDistributedData();
            if (authorizedBondedRolesService.isAuthorizedByBondedRole(authorizedData, BondedRoleType.SECURITY_MANAGER) &&
                    distributedData instanceof AuthorizedAlertData) {
                return Optional.of(authorizedData);
            }
        }
        return Optional.empty();
    }
}