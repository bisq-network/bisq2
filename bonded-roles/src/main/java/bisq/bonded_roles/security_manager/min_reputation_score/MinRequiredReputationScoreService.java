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

package bisq.bonded_roles.security_manager.min_reputation_score;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MinRequiredReputationScoreService implements Service, AuthorizedBondedRolesService.Listener {
    public final static long DEFAULT_MIN_REPUTATION_SCORE = 10_000;
    @Getter
    private final Observable<Boolean> hasNotificationSenderIdentity = new Observable<>();
    @Getter
    private final Observable<Long> mostRecentValueOrDefault = new Observable<>();
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    @Getter
    private final ObservableSet<AuthorizedMinRequiredReputationScoreData> authorizedMinRequiredReputationScoreDataSet = new ObservableSet<>();

    public MinRequiredReputationScoreService(AuthorizedBondedRolesService authorizedBondedRolesService) {
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
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedMinRequiredReputationScoreData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedMinRequiredReputationScoreData authorizedMinRequiredReputationScoreData = (AuthorizedMinRequiredReputationScoreData) authorizedData.getAuthorizedDistributedData();
                authorizedMinRequiredReputationScoreDataSet.add(authorizedMinRequiredReputationScoreData);
                updateMostRecentValueOrDefault();
            }
        }
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData() instanceof AuthorizedMinRequiredReputationScoreData) {
            if (isAuthorized(authorizedData)) {
                AuthorizedMinRequiredReputationScoreData authorizedMinRequiredReputationScoreData = (AuthorizedMinRequiredReputationScoreData) authorizedData.getAuthorizedDistributedData();
                authorizedMinRequiredReputationScoreDataSet.remove(authorizedMinRequiredReputationScoreData);
                updateMostRecentValueOrDefault();
            }
        }
    }

    private boolean isAuthorized(AuthorizedData authorizedData) {
        return authorizedBondedRolesService.hasAuthorizedPubKey(authorizedData, BondedRoleType.SECURITY_MANAGER);
    }

    private void updateMostRecentValueOrDefault() {
        long value = authorizedMinRequiredReputationScoreDataSet.stream()
                .sorted(Comparator.comparingLong(AuthorizedMinRequiredReputationScoreData::getDate).reversed())
                .map(AuthorizedMinRequiredReputationScoreData::getMinRequiredReputationScore)
                .findFirst()
                .orElse(DEFAULT_MIN_REPUTATION_SCORE);
        mostRecentValueOrDefault.set(value);
    }
}