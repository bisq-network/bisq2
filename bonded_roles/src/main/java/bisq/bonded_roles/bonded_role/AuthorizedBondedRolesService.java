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

package bisq.bonded_roles.bonded_role;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.alert.AlertType;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class AuthorizedBondedRolesService implements Service, DataService.Listener {
    private final NetworkService networkService;
    private final boolean ignoreSecurityManager;
    @Getter
    private final ObservableSet<BondedRole> bondedRoles = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedOracleNode> authorizedOracleNodes = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedAlertData> authorizedAlertDataSet = new ObservableSet<>();

    public AuthorizedBondedRolesService(NetworkService networkService, boolean ignoreSecurityManager) {
        this.networkService = networkService;
        this.ignoreSecurityManager = ignoreSecurityManager;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAuthorizedData()
                        .forEach(this::onAuthorizedDataAdded));
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
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        if (data instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.add((AuthorizedOracleNode) data);
        } else if (data instanceof AuthorizedBondedRole) {
            validateBondedRole(authorizedData, (AuthorizedBondedRole) data).ifPresent(authorizedBondedRole -> {
                bondedRoles.add(new BondedRole(authorizedBondedRole));
                if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                    networkService.addSeedNodeAddressByTransport(authorizedBondedRole.getAddressByNetworkType());
                }

                networkService.getDataService().orElseThrow().getAuthorizedData()
                        .filter(e -> e.getAuthorizedDistributedData() instanceof AuthorizedAlertData)
                        .forEach(e -> onAuthorizedAlertData(e, (AuthorizedAlertData) e.getAuthorizedDistributedData(), true));
            });
        } else if (data instanceof AuthorizedAlertData) {
            onAuthorizedAlertData(authorizedData, (AuthorizedAlertData) data, true);
        }
    }

    private void onAuthorizedAlertData(AuthorizedData authorizedData, AuthorizedAlertData data, boolean isAdded) {
        validateAlert(authorizedData, data).ifPresent(alertData -> {
            if (isAdded) {
                authorizedAlertDataSet.add(alertData);
            } else {
                authorizedAlertDataSet.remove(alertData);
            }
            maybeApplyBannedState(alertData, isAdded);
        });
    }

    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        if (data instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.remove((AuthorizedOracleNode) data);
        } else if (data instanceof AuthorizedBondedRole) {
            validateBondedRole(authorizedData, (AuthorizedBondedRole) data).ifPresent(authorizedBondedRole -> {
                Optional<BondedRole> toRemove = bondedRoles.stream().filter(bondedRole -> bondedRole.getAuthorizedBondedRole().equals(authorizedBondedRole)).findAny();
                toRemove.ifPresent(bondedRoles::remove);
                if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                    networkService.removeSeedNodeAddressByTransport(authorizedBondedRole.getAddressByNetworkType());
                }

                networkService.getDataService().orElseThrow().getAuthorizedData()
                        .filter(e -> e.getAuthorizedDistributedData() instanceof AuthorizedAlertData)
                        .forEach(e -> onAuthorizedAlertData(e, (AuthorizedAlertData) e.getAuthorizedDistributedData(), false));
            });
        } else if (data instanceof AuthorizedAlertData) {
            onAuthorizedAlertData(authorizedData, (AuthorizedAlertData) data, false);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<AuthorizedBondedRole> getAuthorizedBondedRoleStream() {
        return bondedRoles.stream()
                .filter(bondedRole -> ignoreSecurityManager || bondedRole.isNotBanned())
                .map(BondedRole::getAuthorizedBondedRole);
    }

    public boolean hasAuthorizedPubKey(AuthorizedData authorizedData, BondedRoleType bondedRoleType) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        if (data.staticPublicKeysProvided()) {
            log.debug("The verification was already done at the p2p network layer. data={}", data);
            return true;
        } else {
            String authorizedDataPubKey = Hex.encode(authorizedData.getAuthorizedPublicKeyBytes());
            boolean matchFound = getAuthorizedBondedRoleStream()
                    .filter(bondedRole -> bondedRole.getBondedRoleType() == bondedRoleType)
                    .anyMatch(bondedRole -> {
                        boolean match = bondedRole.getAuthorizedPublicKey().equals(authorizedDataPubKey);
                        if (match) {
                            log.debug("Found a matching authorizedPublicKey from bondedRole: {}. data={}", bondedRole, data);
                        }
                        return match;
                    });
            if (matchFound) {
                log.debug("authorizedPublicKey provided by a bonded role. data={}", data);
            } else {
                log.warn("authorizedPublicKey is not matching any key from our authorizedBondedRolesPubKeys and does not provide a matching static key. data={}", data);
            }
            return matchFound;
        }
    }

    private Optional<AuthorizedAlertData> validateAlert(AuthorizedData authorizedData, AuthorizedAlertData authorizedAlertData) {
        if (hasAuthorizedPubKey(authorizedData, BondedRoleType.SECURITY_MANAGER)) {
            return Optional.of(authorizedAlertData);
        }
        return Optional.empty();
    }

    private Optional<AuthorizedBondedRole> validateBondedRole(AuthorizedData authorizedData, AuthorizedBondedRole authorizedBondedRole) {
        // AuthorizedBondedRoles are published only by an oracle node. The oracle node use either a hard coded pubKey
        // or has been authorized by another already authorized oracle node. There need to be at least one root node 
        // with a hard coded pubKey.
        if (hasAuthorizedPubKey(authorizedData, BondedRoleType.ORACLE_NODE)) {
            return Optional.of(authorizedBondedRole);
        }
        return Optional.empty();
    }

    private void maybeApplyBannedState(AuthorizedAlertData alertData, boolean value) {
        if (alertData.getAlertType() == AlertType.BAN && alertData.getBannedRole().isPresent()) {
            bondedRoles.stream()
                    .filter(bondedRole -> bondedRole.getAuthorizedBondedRole().equals(alertData.getBannedRole().get()))
                    .findAny()
                    .ifPresent(bondedRole -> bondedRole.setIsBanned(value));
        }
    }

}