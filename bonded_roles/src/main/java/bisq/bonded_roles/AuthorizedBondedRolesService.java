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

import bisq.bonded_roles.alert.AlertType;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import bisq.network.p2p.services.data.storage.auth.authorized.StaticallyAuthorizedPublicKeyValidation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class AuthorizedBondedRolesService implements Service, DataService.Listener {
    private final NetworkService networkService;
    @Getter
    private final ObservableSet<AuthorizedBondedRole> authorizedBondedRoleSet = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedData> authorizedDataSet = new ObservableSet<>();
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
                .ifPresent(dataService -> dataService.getAllAuthorizedData()
                        .forEach(this::processAddedAuthorizedData));
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
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        processAddedAuthorizedData(authorizedData);
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        processRemovedAuthorizedData(authorizedData);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isAuthorizedByBondedRole(AuthorizedData authorizedData, BondedRoleType bondedRoleType) {
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

    public Set<AuthorizedBondedRole> getAuthorizedBondedRoles(BondedRoleType bondedRoleType) {
        return getAuthorizedBondedRoleSet().stream()
                .filter(e -> e.getBondedRoleType() == bondedRoleType)
                .collect(Collectors.toSet());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAddedAuthorizedData(AuthorizedData authorizedData) {
        DistributedData distributedData = authorizedData.getDistributedData();
        if (distributedData instanceof AuthorizedOracleNode) {
            AuthorizedOracleNode authorizedOracleNode = (AuthorizedOracleNode) distributedData;
            authorizedOracleNodes.add(authorizedOracleNode);
            authorizedDataSet.add(authorizedData);
        } else if (distributedData instanceof AuthorizedBondedRole) {
            AuthorizedBondedRole authorizedBondedRole = (AuthorizedBondedRole) distributedData;
            authorizedBondedRoleSet.add(authorizedBondedRole);
            authorizedDataSet.add(authorizedData);
            if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                networkService.addSeedNodeAddressByTransport(authorizedBondedRole.getAddressByNetworkType());
            }
        } else if (distributedData instanceof AuthorizedAlertData) {
            AuthorizedAlertData authorizedAlertData = (AuthorizedAlertData) distributedData;
            if (authorizedAlertData.getAlertType() == AlertType.BAN &&
                    isAuthorizedByBondedRole(authorizedData, BondedRoleType.SECURITY_MANAGER) &&
                    authorizedAlertData.getBannedRoleProfileId().isPresent() &&
                    authorizedAlertData.getBannedBondedRoleType().isPresent()) {
                String bannedRoleProfileId = authorizedAlertData.getBannedRoleProfileId().get();
                BondedRoleType bannedBondedRoleType = authorizedAlertData.getBannedBondedRoleType().get();
                authorizedBondedRoleSet.stream()
                        .filter(authorizedBondedRole -> authorizedBondedRole.getBondedRoleType() == bannedBondedRoleType)
                        .filter(authorizedBondedRole -> authorizedBondedRole.getProfileId().equals(bannedRoleProfileId))
                        .forEach(bannedRole -> {
                            authorizedBondedRoleSet.remove(bannedRole);
                            authorizedDataSet.remove(authorizedData);
                        });
            }
        }
    }

    private void processRemovedAuthorizedData(AuthorizedData authorizedData) {
        DistributedData distributedData = authorizedData.getDistributedData();
        if (distributedData instanceof AuthorizedOracleNode) {
            AuthorizedOracleNode authorizedOracleNode = (AuthorizedOracleNode) distributedData;
            authorizedOracleNodes.remove(authorizedOracleNode);
            authorizedDataSet.remove(authorizedData);
        } else if (distributedData instanceof AuthorizedBondedRole) {
            AuthorizedBondedRole authorizedBondedRole = (AuthorizedBondedRole) distributedData;
            authorizedBondedRoleSet.remove(authorizedBondedRole);
            authorizedDataSet.remove(authorizedData);
            if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                networkService.removeSeedNodeAddressByTransport(authorizedBondedRole.getAddressByNetworkType());
            }
        }
    }
}