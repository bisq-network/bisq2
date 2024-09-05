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
import bisq.bonded_roles.market_price.AuthorizedMarketPriceData;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

/**
 * AuthorizedData can have dependencies to other AuthorizedData. E.g. AuthorizedBondedRoles require an
 * AuthorizedBondedRole of type ORACLE_NODE for validating the pubKey.
 * When we request the Inventory from other nodes we received data sorted by priority so that AuthorizedBondedRole have
 * higher priority than other AuthorizedData. Though due size limitations and parallel requests we cannot guarantee
 * the expected order required for our dependency graph.
 * To reduce risks for out of order processing, we delay initial processing and process the data types in the
 * correct dependency order. If validation fails we add the AuthorizedData into a queue for later reprocessing
 * when we receive relevant AuthorizedData which might fulfill the missing dependency.
 * <p>
 * Other classes which process AuthorizedData should use the AuthorizedBondedRolesService.Listener to get notified
 * on new AuthorizedData so that they take benefit of the implemented handling for out or order data.
 */
@Slf4j
public class AuthorizedBondedRolesService implements Service, DataService.Listener {
    public interface Listener {
        void onAuthorizedDataAdded(AuthorizedData authorizedData);

        default void onAuthorizedDataRemoved(AuthorizedData authorizedData) {
        }
    }

    private final NetworkService networkService;
    private final boolean ignoreSecurityManager;
    @Getter
    private final ObservableSet<BondedRole> bondedRoles = new ObservableSet<>();
    @Getter
    private final ObservableSet<AuthorizedOracleNode> authorizedOracleNodes = new ObservableSet<>();
    @Getter
    private final DataService.Listener initialDataServiceListener;
    private final Set<AuthorizedData> failedAuthorizedData = new CopyOnWriteArraySet<>();
    private Scheduler initialDataScheduler, reprocessScheduler;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private boolean initializeCalled;

    public AuthorizedBondedRolesService(NetworkService networkService,
                                        boolean ignoreSecurityManager) {
        this.networkService = networkService;
        this.ignoreSecurityManager = ignoreSecurityManager;

        initialDataServiceListener = new DataService.Listener() {
            @Override
            public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
                // We delay a bit to mitigate potential race conditions
                if (initialDataScheduler == null) {
                    initialDataScheduler = Scheduler.run(() -> delayedApplyInitialData())
                            .host(this)
                            .runnableName("delayedApplyInitialData")
                            .after(1000);
                }
            }
        };
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        initializeCalled = true;

        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedOracleNode", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedOracleNode
                                ? Optional.of((AuthorizedOracleNode) authorizedDistributedData)
                                : Optional.empty()));

        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedBondedRole", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedBondedRole
                                ? Optional.of((AuthorizedBondedRole) authorizedDistributedData)
                                : Optional.empty()));

        networkService.getDataService().ifPresent(dataService ->
                dataService.getStorageService().cleanupMap("AuthorizedMarketPriceData", authorizedDistributedData ->
                        authorizedDistributedData instanceof AuthorizedMarketPriceData
                                ? Optional.of((AuthorizedMarketPriceData) authorizedDistributedData)
                                : Optional.empty()));

        networkService.addDataServiceListener(initialDataServiceListener);
        // It can be that there are no new data received from the inventory request, so we apply the existing data
        applyInitialData();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        initializeCalled = false;
        networkService.removeDataServiceListener(initialDataServiceListener);
        networkService.removeDataServiceListener(this);
        if (initialDataScheduler != null) {
            initialDataScheduler.stop();
        }
        return CompletableFuture.completedFuture(true);
    }

    private void delayedApplyInitialData() {
        networkService.removeDataServiceListener(initialDataServiceListener);
        applyInitialData();
        initialDataScheduler.stop();
        initialDataScheduler = null;
    }

    private void applyInitialData() {
        // Start with the AuthorizedOracleNode
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAuthorizedData()
                        .filter(e -> e.getAuthorizedDistributedData() instanceof AuthorizedOracleNode)
                        .forEach(this::onAuthorizedDataAdded));

        // Then we process the AuthorizedBondedRole of type ORACLE_NODE
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAuthorizedData()
                        .filter(e -> e.getAuthorizedDistributedData() instanceof AuthorizedBondedRole)
                        .filter(e -> ((AuthorizedBondedRole) e.getAuthorizedDistributedData()).getBondedRoleType() == BondedRoleType.ORACLE_NODE)
                        .forEach(this::onAuthorizedDataAdded));

        // Then we process the other AuthorizedBondedRoles
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAuthorizedData()
                        .filter(e -> e.getAuthorizedDistributedData() instanceof AuthorizedBondedRole)
                        .filter(e -> ((AuthorizedBondedRole) e.getAuthorizedDistributedData()).getBondedRoleType() != BondedRoleType.ORACLE_NODE)
                        .forEach(this::onAuthorizedDataAdded));

        // Now we can apply other data
        networkService.getDataService()
                .ifPresent(dataService -> dataService.getAuthorizedData()
                        .filter(e -> !(e.getAuthorizedDistributedData() instanceof AuthorizedOracleNode))
                        .filter(e -> !(e.getAuthorizedDistributedData() instanceof AuthorizedBondedRole))
                        .forEach(this::onAuthorizedDataAdded));

        networkService.addDataServiceListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        log.debug("onAuthorizedDataAdded {}", data.getClass().getSimpleName());
        if (data instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.add((AuthorizedOracleNode) data);
            reProcessFailedAuthorizedData();
        } else if (data instanceof AuthorizedBondedRole) {
            log.debug("BondedRoleType {}", ((AuthorizedBondedRole) data).getBondedRoleType());
            validateBondedRole(authorizedData, (AuthorizedBondedRole) data).ifPresent(authorizedBondedRole -> {
                bondedRoles.add(new BondedRole(authorizedBondedRole));
                if (authorizedBondedRole.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                    networkService.addSeedNodeAddressByTransport(authorizedBondedRole.getAddressByTransportTypeMap().orElseThrow());
                }
            });
            reProcessFailedAuthorizedData();
        }
        listeners.forEach(listener -> {
            try {
                listener.onAuthorizedDataAdded(authorizedData);
            } catch (Exception e) {
                log.error("Error at listener.onAuthorizedDataAdded. listener={}", listener, e);
            }
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
                    networkService.removeSeedNodeAddressByTransport(authorizedBondedRole.getAddressByTransportTypeMap().orElseThrow());
                }
            });
        }
        listeners.forEach(listener -> {
            try {
                listener.onAuthorizedDataRemoved(authorizedData);
            } catch (Exception e) {
                log.error("Error at onAuthorizedDataAdded", e);
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Stream<AuthorizedBondedRole> getAuthorizedBondedRoleStream() {
        return bondedRoles.stream()
                .filter(bondedRole -> ignoreSecurityManager || bondedRole.isNotBanned())
                .map(BondedRole::getAuthorizedBondedRole);
    }

    public boolean hasAuthorizedPubKey(AuthorizedData authorizedData, BondedRoleType authorizingBondedRoleType) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        if (data.staticPublicKeysProvided()) {
            log.debug("The verification was already done at the p2p network layer. data={};authorizingBondedRoleType={}",
                    data.getClass().getSimpleName(), authorizingBondedRoleType);
            return true;
        } else {
            // Signature check is done in AuthorizedData
            String authorizedDataPubKey = Hex.encode(authorizedData.getAuthorizedPublicKeyBytes());
            boolean matchFound = getAuthorizedBondedRoleStream()
                    .filter(bondedRole -> bondedRole.getBondedRoleType() == authorizingBondedRoleType)
                    .anyMatch(bondedRole -> {
                        boolean match = bondedRole.getAuthorizedPublicKey().equals(authorizedDataPubKey);
                        if (match) {
                            log.debug("Found a matching authorizedPublicKey from bondedRole: {}. data={}",
                                    bondedRole.getBondedRoleType(),
                                    data.getClass().getSimpleName());
                        } else {
                            log.debug("No authorizedPublicKey found in our list of bonded roles.\n" +
                                            "bondedRole.getAuthorizedPublicKey()={}\n" +
                                            "authorizedDataPubKey={}\n" +
                                            "data={}",
                                    bondedRole.getAuthorizedPublicKey(),
                                    authorizedDataPubKey,
                                    data.getClass().getSimpleName());

                        }
                        return match;
                    });
            if (matchFound) {
                log.debug("authorizedPublicKey provided by a bonded role. data={}", data.getClass().getSimpleName());
                // In case we are reprocessing previous failed authorizedData we clear it from the queue.
                if (failedAuthorizedData.remove(authorizedData)) {
                    log.debug("We successfully reprocessed authorizedData.\n" +
                                    "AuthorizedDistributedData={}, {}",
                            data.getClass().getSimpleName(), data.toString().substring(0, 100));
                }
            } else {
                failedAuthorizedData.add(authorizedData);
                // TODO Set log level for to debug for now, as too many logs are printed.
                //  Once the TTL has cleared the old data we can change back to warn level.
                log.debug("hasAuthorizedPubKey failed for AuthorizedDistributedData={}, {}",
                        data.getClass().getSimpleName(), StringUtils.truncate(data.toString(), 200));
                log.debug("AuthorizedPublicKey is not matching any key from our authorizedBondedRolesPubKeys and does " +
                                "not provide a matching static key.\n" +
                                "We add the authorizedData to a retry queue for later reprocessing.\n" +
                                "AuthorizedDistributedData={}, {}",
                        data.getClass().getSimpleName(), StringUtils.truncate(data.toString(), 200));
            }
            return matchFound;
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);

        if (initializeCalled) {
            log.info("We get added a listener after we have been already initialized. This is expected for higher level domain listeners. " +
                    "We apply all data from the network store to the listener. " +
                    "listener={}", listener);
            networkService.getDataService()
                    .ifPresent(dataService -> dataService.getAuthorizedData().forEach(listener::onAuthorizedDataAdded));
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void reProcessFailedAuthorizedData() {
        // Reprocess AuthorizedData which previously failed due potential out-of-order issues
        // We delay to avoid getting too many data queued up
        if (reprocessScheduler == null) {
            reprocessScheduler = Scheduler.run(this::reprocess)
                    .host(this)
                    .runnableName("reprocess")
                    .after(1000);
        }
    }

    private void reprocess() {
        Set<AuthorizedData> clone = new HashSet<>(failedAuthorizedData);
        clone.forEach(this::onAuthorizedDataAdded);
        reprocessScheduler.stop();
        reprocessScheduler = null;
    }

    private Optional<AuthorizedBondedRole> validateBondedRole(AuthorizedData authorizedData,
                                                              AuthorizedBondedRole authorizedBondedRole) {
        // AuthorizedBondedRoles are published only by an oracle node. The oracle node use either a hard coded pubKey
        // or has been authorized by another already authorized oracle node. There need to be at least one root node 
        // with a hard coded pubKey.
        if (hasAuthorizedPubKey(authorizedData, BondedRoleType.ORACLE_NODE)) {
            return Optional.of(authorizedBondedRole);
        } else {
            return Optional.empty();
        }
    }
}