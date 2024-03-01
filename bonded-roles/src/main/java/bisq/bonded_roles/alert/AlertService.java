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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class AlertService implements Service {

    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;

    public AlertService(AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.authorizedBondedRolesService = authorizedBondedRolesService;
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

    public ObservableSet<AuthorizedAlertData> getAuthorizedAlertDataSet() {
        return authorizedBondedRolesService.getAuthorizedAlertDataSet();
    }
}