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

package bisq.bonded_roles;

import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import bisq.network.p2p.services.data.storage.auth.authorized.StaticallyAuthorizedPublicKeyValidation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class AuthorizedBondedRolesService implements Service, DataService.Listener {
    private final NetworkService networkService;
    @Getter
    private final ObservableSet<AuthorizedBondedRole> authorizedBondedRoleSet = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedOracleNode> authorizedOracleNodes = new ObservableSet<>();

    public AuthorizedBondedRolesService(NetworkService networkService) {
        this.networkService = networkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAllAuthenticatedPayload()
                        .forEach(this::processAuthenticatedData));
        networkService.addDataServiceListener(this);
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
        processAuthenticatedData(authenticatedData);
    }

    @Override
    public void onAuthenticatedDataRemoved(AuthenticatedData authenticatedData) {
       /* if (authenticatedData.getDistributedData() instanceof AuthorizedRoleRegistrationData) {
            //todo
            // authorizedDataSetOfRoleRegistrationData.remove((AuthorizedData) authenticatedData);
        }*/
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isAuthorizedByBondedRole(AuthenticatedData authenticatedData, BondedRoleType bondedRoleType) {
        if (!(authenticatedData instanceof AuthorizedData)) {
            return false;
        }
        AuthorizedData authorizedData = (AuthorizedData) authenticatedData;

        DistributedData distributedData = authorizedData.getDistributedData();
        if (!(distributedData instanceof DeferredAuthorizedPublicKeyValidation) &&
                distributedData instanceof StaticallyAuthorizedPublicKeyValidation) {
            // In case the distributedData has not implemented DeferredAuthorizedPublicKeyValidation the hardcoded key-set is used for 
            // verification at the p2p network layer.
            return true;
        }

        String authorizedDataPubKey = Hex.encode(authorizedData.getAuthorizedPublicKeyBytes());
        boolean isStaticallyAuthorizedKey = false;
        if (distributedData instanceof StaticallyAuthorizedPublicKeyValidation) {
            StaticallyAuthorizedPublicKeyValidation data = (StaticallyAuthorizedPublicKeyValidation) distributedData;
            isStaticallyAuthorizedKey = data.getAuthorizedPublicKeys().contains(authorizedDataPubKey);
        }

        boolean isAuthorized = isStaticallyAuthorizedKey ||
                authorizedBondedRoleSet.stream()
                        .filter(bondedRole -> bondedRole.getBondedRoleType() == bondedRoleType)
                        .map(AuthorizedBondedRole::getAuthorizedPublicKey)
                        .anyMatch(pubKey -> pubKey.equals(authorizedDataPubKey));
        if (!isAuthorized) {
            log.warn("authorizedPublicKey is not matching any key from our authorizedBondedRolesPubKeys and does not provide a matching static key");
        }
        return isAuthorized;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAuthenticatedData(AuthenticatedData authenticatedData) {
        if (authenticatedData.getDistributedData() instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.add((AuthorizedOracleNode) authenticatedData.getDistributedData());
        } else if (authenticatedData.getDistributedData() instanceof AuthorizedBondedRole) {
            AuthorizedBondedRole authorizedBondedRole = (AuthorizedBondedRole) authenticatedData.getDistributedData();
            authorizedBondedRoleSet.add(authorizedBondedRole);
            if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                networkService.addSeedNodeAddressByTransport(authorizedBondedRole.getAddressByNetworkType());
            }
        }
    }
}