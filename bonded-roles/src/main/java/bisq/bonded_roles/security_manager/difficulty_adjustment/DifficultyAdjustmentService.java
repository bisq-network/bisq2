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

package bisq.bonded_roles.security_manager.difficulty_adjustment;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

/**
 * We do not apply the mostRecentValueOrDefault to NetworkLoadService.difficultyAdjustmentFactor here as different
 * application can use different strategies. E.g. Desktop allow the user to ignore the value from the security manager
 * and use a user defined value from settings.
 */
@Slf4j
public class DifficultyAdjustmentService implements Service, AuthorizedBondedRolesService.Listener {
    @Getter
    private final Observable<Double> mostRecentValueOrDefault = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    private final ObservableSet<AuthorizedDifficultyAdjustmentData> authorizedDifficultyAdjustmentDataSet = new ObservableSet<>();

    public DifficultyAdjustmentService(AuthorizedBondedRolesService authorizedBondedRolesService) {
        this.authorizedBondedRolesService = authorizedBondedRolesService;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        authorizedBondedRolesService.addListener(this);
        updateMostRecentValueOrDefault();
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
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedDifficultyAdjustmentData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedDifficultyAdjustmentData authorizedDifficultyAdjustmentData = (AuthorizedDifficultyAdjustmentData) authorizedData.getAuthorizedDistributedData();
                authorizedDifficultyAdjustmentDataSet.add(authorizedDifficultyAdjustmentData);
                updateMostRecentValueOrDefault();
            }
        }
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedDifficultyAdjustmentData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedDifficultyAdjustmentData authorizedDifficultyAdjustmentData = (AuthorizedDifficultyAdjustmentData) authorizedData.getAuthorizedDistributedData();
                authorizedDifficultyAdjustmentDataSet.remove(authorizedDifficultyAdjustmentData);
                updateMostRecentValueOrDefault();
            }
        }
    }

    private boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.SECURITY_MANAGER);
    }


    private void updateMostRecentValueOrDefault() {
        double value = authorizedDifficultyAdjustmentDataSet.stream()
                .sorted(Comparator.comparingLong(AuthorizedDifficultyAdjustmentData::getDate).reversed())
                .map(AuthorizedDifficultyAdjustmentData::getDifficultyAdjustmentFactor)
                .findFirst()
                .orElse(NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT);
        mostRecentValueOrDefault.set(value);
    }
}