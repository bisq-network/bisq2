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
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import bisq.network.p2p.services.data.storage.auth.authorized.DeferredAuthorizedPublicKeyValidation;
import bisq.network.p2p.services.data.storage.auth.authorized.StaticallyAuthorizedPublicKeyValidation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class AuthorizedBondedRolesService implements Service, DataService.Listener {
    private final NetworkService networkService;
    private final boolean ignoreSecurityManager;
    @Getter
    private final ObservableSet<AuthorizedBondedRole> authorizedBondedRoleSet = new ObservableSet<>();
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
            validate(authorizedData, (AuthorizedBondedRole) data).ifPresent(authorizedBondedRole -> {
                authorizedBondedRoleSet.add(authorizedBondedRole);
                if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                    networkService.addSeedNodeAddressByTransport(authorizedBondedRole.getAddressByNetworkType());
                }
            });
        } else if (data instanceof AuthorizedAlertData) {
            validate(authorizedData, (AuthorizedAlertData) data).ifPresent(authorizedAlertDataSet::add);
        }
    }


    @Override
    public void onAuthorizedDataRemoved(AuthorizedData authorizedData) {

        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        if (data instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.remove((AuthorizedOracleNode) data);
        } else if (data instanceof AuthorizedBondedRole) {
            validate(authorizedData, (AuthorizedBondedRole) data).ifPresent(authorizedBondedRole -> {
                authorizedBondedRoleSet.remove(authorizedBondedRole);
                if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                    networkService.removeSeedNodeAddressByTransport(authorizedBondedRole.getAddressByNetworkType());
                }
            });
        } else if (data instanceof AuthorizedAlertData) {
            validate(authorizedData, (AuthorizedAlertData) data).ifPresent(authorizedAlertDataSet::remove);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isAuthorizedByBondedRole(AuthorizedData authorizedData, BondedRoleType bondedRoleType) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        if (!(data instanceof DeferredAuthorizedPublicKeyValidation) &&
                data instanceof StaticallyAuthorizedPublicKeyValidation) {
            // In case the data has not implemented DeferredAuthorizedPublicKeyValidation the hardcoded key-set is used for 
            // verification at the p2p network layer.
            return true;
        }

        String authorizedDataPubKey = Hex.encode(authorizedData.getAuthorizedPublicKeyBytes());
        boolean isStaticallyAuthorizedKey = false;
        if (data instanceof StaticallyAuthorizedPublicKeyValidation) {
            isStaticallyAuthorizedKey = ((StaticallyAuthorizedPublicKeyValidation) data).getAuthorizedPublicKeys().contains(authorizedDataPubKey);
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

    private Optional<AuthorizedBondedRole> validate(AuthorizedData authorizedData, AuthorizedBondedRole authorizedBondedRole) {
        //todo

      /*  getBanAlerts().stream()
                .filter(alert->alert.getBannedBondedRoleType().orElseThrow()==authorizedBondedRole.getBondedRoleType())
                .*/

        if (!isBanned(authorizedData, authorizedBondedRole)) {
            return Optional.of(authorizedBondedRole);
        }
        return Optional.empty();
    }

    private Optional<AuthorizedAlertData> validate(AuthorizedData authorizedData, AuthorizedAlertData authorizedAlertData) {
        if (isAuthorizedByBondedRole(authorizedData, BondedRoleType.SECURITY_MANAGER)) {
            return Optional.of(authorizedAlertData);
        }
        return Optional.empty();
    }

    private boolean isBanned(AuthorizedData authorizedData, AuthorizedBondedRole authorizedBondedRole) {
        if (networkService.getDataService().isEmpty()) {
            return true;
        }

        Set<AuthorizedAlertData> banAlerts = getBanAlerts();

        Set<String> securityManagerProfileIds = banAlerts.stream()
                .map(AuthorizedAlertData::getSecurityManagerProfileId)
                .collect(Collectors.toSet());


        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAuthorizedData()
                        .filter(data -> data.getAuthorizedDistributedData() instanceof AuthorizedAlertData)
                        .map(data -> (AuthorizedAlertData) data.getAuthorizedDistributedData())
                        .filter(authorizedAlertData -> authorizedAlertData.getAlertType() == AlertType.BAN &&
                                /*isAuthorizedByBondedRole(authorizedData, BondedRoleType.SECURITY_MANAGER) &&*/
                                authorizedAlertData.getAuthorizedBondedRole().isPresent())
                        /* .filter(authorizedAlertData ->
                                 authorizedBondedRole.getBondedRoleType() == authorizedAlertData.getAuthorizedBondedRole().get() &&
                                         authorizedBondedRole.getProfileId().equals(authorizedAlertData.getBannedRoleProfileId().get()))*/
                        .forEach(authorizedAlertData -> {
                            if (ignoreSecurityManager) {
                                log.warn("We received an alert message from the security manager to ban a bonded role but " +
                                                "you have set ignoreSecurityManager to true, so this will have no effect.\n" +
                                                "bannedRole={}\nauthorizedData sent by security manager={}",
                                        authorizedBondedRole, authorizedData);
                            } else {
                                authorizedBondedRoleSet.remove(authorizedBondedRole);
                            }
                        }));

        return false;
    }

    private Set<AuthorizedAlertData> getBanAlerts() {
        return authorizedAlertDataSet.stream()
                .filter(authorizedAlertData -> authorizedAlertData.getAlertType() == AlertType.BAN &&
                        authorizedAlertData.getAuthorizedBondedRole().isPresent())
                .collect(Collectors.toSet());
    }

   /* private void applyAuthorizedAlertData(AuthorizedData authorizedData, AuthorizedAlertData authorizedAlertData) {
        if (authorizedAlertData.getAlertType() == AlertType.BAN &&
                isAuthorizedByBondedRole(authorizedData, BondedRoleType.SECURITY_MANAGER) &&
                authorizedAlertData.getBannedRoleProfileId().isPresent() &&
                authorizedAlertData.getAuthorizedBondedRole().isPresent()) {
            String bannedRoleProfileId = authorizedAlertData.getBannedRoleProfileId().get();
            BondedRoleType bannedBondedRoleType = authorizedAlertData.getAuthorizedBondedRole().get();
            authorizedBondedRoleSet.stream()
                    .filter(authorizedBondedRole -> authorizedBondedRole.getBondedRoleType() == bannedBondedRoleType)
                    .filter(authorizedBondedRole -> authorizedBondedRole.getProfileId().equals(bannedRoleProfileId))
                    .forEach(bannedRole -> {
                        if (ignoreSecurityManager) {
                            log.warn("We received an alert message from the security manager to ban a bonded role but " +
                                            "you have set ignoreSecurityManager to true, so this will have no effect.\n" +
                                            "bannedRole={}\nauthorizedData sent by security manager={}",
                                    bannedRole, authorizedData);
                        } else {
                            authorizedBondedRoleSet.remove(bannedRole);
                        }
                    });
        }
    }*/
}