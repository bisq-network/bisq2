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

import javax.annotation.Nullable;
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
    @Nullable
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


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        if (initializeCalled) {
            log.error("AuthorizedBondedRolesService already initialized, skipping initialization");
            return CompletableFuture.completedFuture(true);
        }
        initializeCalled = true;

        networkService.addDataServiceListener(initialDataServiceListener);
        // It can be that there are no new data received from the inventory request, so we apply the existing data
        applyInitialData();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        if (!initializeCalled) {
            return CompletableFuture.completedFuture(true);
        }
        initializeCalled = false;
        networkService.removeDataServiceListener(initialDataServiceListener);
        networkService.removeDataServiceListener(this);
        if (initialDataScheduler != null) {
            initialDataScheduler.stop();
            initialDataScheduler = null;
        }
        if (reprocessScheduler != null) {
            reprocessScheduler.stop();
            reprocessScheduler = null;
        }
        bondedRoles.clear();
        authorizedOracleNodes.clear();
        failedAuthorizedData.clear();
        listeners.clear();
        return CompletableFuture.completedFuture(true);
    }

    private void delayedApplyInitialData() {
        networkService.removeDataServiceListener(initialDataServiceListener);
        applyInitialData();
        if (initialDataScheduler != null) {
            initialDataScheduler.stop();
            initialDataScheduler = null;
        }
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


    /* --------------------------------------------------------------------- */
    // DataService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onAuthorizedDataAdded(AuthorizedData authorizedData) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        log.debug("onAuthorizedDataAdded {}", data.getClass().getSimpleName());
        if (data instanceof AuthorizedOracleNode) {
            authorizedOracleNodes.add((AuthorizedOracleNode) data);
            reProcessFailedAuthorizedData();
        } else if (data instanceof AuthorizedBondedRole authorizedBondedRole) {
            log.debug("BondedRoleType {}", authorizedBondedRole.getBondedRoleType());
            validateBondedRole(authorizedData, authorizedBondedRole).ifPresent(role -> {
                bondedRoles.add(new BondedRole(role));
                if (role.getBondedRoleType() == BondedRoleType.SEED_NODE) {
                    networkService.addSeedNodeAddressByTransport(role.getAddressByTransportTypeMap().orElseThrow());
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
                if (isPublishedByAnOracleNode(authorizedBondedRole)) {
                    log.info("A publication of the bonded role {} of {} was removed, but an oracle node still " +
                                    "publishes it. We keep the role.",
                            authorizedBondedRole.getBondedRoleType(), authorizedBondedRole.getProfileId());
                    return;
                }
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
                log.error("Error at onAuthorizedDataRemoved", e);
            }
        });
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Stream<AuthorizedBondedRole> getAuthorizedBondedRoleStream() {
        return getAuthorizedBondedRoleStream(false);
    }

    public Stream<AuthorizedBondedRole> getAuthorizedBondedRoleStream(boolean ignoreBannedState) {
        return bondedRoles.stream()
                .filter(bondedRole -> ignoreSecurityManager || ignoreBannedState || bondedRole.isNotBanned())
                .map(BondedRole::getAuthorizedBondedRole);
    }

    public Stream<AuthorizedBondedRole> getBannedAuthorizedBondedRoleStream() {
        return bondedRoles.stream()
                .filter(BondedRole::isBanned)
                .map(BondedRole::getAuthorizedBondedRole);
    }

    public boolean hasAuthorizedPubKey(AuthorizedData authorizedData, BondedRoleType authorizingBondedRoleType) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        boolean isAuthorized = isAuthorizedKey(authorizedData, authorizingBondedRoleType);
        if (data.staticPublicKeysProvided()) {
            if (!isAuthorized) {
                log.warn("The authorizedPublicKey is not one of the static keys of {}. authorizingBondedRoleType={}",
                        data.getClass().getSimpleName(), authorizingBondedRoleType);
            }
            return isAuthorized;
        }

        if (isAuthorized) {
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
        return isAuthorized;
    }

    // Rules 2 and 3 of docs/specifications/bonded-roles/authorized-data.md, without side effects. The signature check is
    // done in AuthorizedData.
    private boolean isAuthorizedKey(AuthorizedData authorizedData, BondedRoleType authorizingBondedRoleType) {
        AuthorizedDistributedData data = authorizedData.getAuthorizedDistributedData();
        String authorizedDataPubKey = Hex.encode(authorizedData.getAuthorizedPublicKeyBytes());
        if (data.staticPublicKeysProvided()) {
            // The network layer checks the static keys only when it adds data, not when it loads stored data. A later
            // version can remove a key from the static keys, and a node which updates still holds the data of that key.
            // So we check the key against the static keys of this version.
            return data.getAuthorizedPublicKeys().contains(authorizedDataPubKey);
        }
        return getAuthorizedBondedRoleStream()
                .filter(bondedRole -> bondedRole.getBondedRoleType() == authorizingBondedRoleType)
                .anyMatch(bondedRole -> bondedRole.getAuthorizedPublicKey().equals(authorizedDataPubKey));
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
        if (reprocessScheduler != null) {
            reprocessScheduler.stop();
            reprocessScheduler = null;
        }
    }

    // Several oracle nodes can publish the same role, each with its own authorized key. The store holds one entry per
    // publication, but bondedRoles holds one item per role: the equality of a role leaves out the oracle node which
    // authorized it. The removal or expiry of one publication, for example of a copy without signature V2 at the cutoff
    // date of signature V1, must not remove a role which another publication still authorizes. Otherwise
    // OracleNodeService would also remove its own publication, and the removal would spread to all oracle nodes (see
    // docs/specifications/bonded-roles/registration.md).
    private boolean isPublishedByAnOracleNode(AuthorizedBondedRole authorizedBondedRole) {
        return networkService.getDataService()
                .map(dataService -> dataService.getAuthenticatedPayloadStreamByStoreName(
                                AuthorizedBondedRole.class.getSimpleName())
                        .filter(AuthorizedData.class::isInstance)
                        .map(AuthorizedData.class::cast)
                        .filter(publication -> authorizedBondedRole.equals(publication.getAuthorizedDistributedData()))
                        .anyMatch(this::isAuthorizedByAnOracleNode))
                .orElse(false);
    }

    // As hasAuthorizedPubKey for BondedRoleType.ORACLE_NODE, but without its queue of data to reprocess
    private boolean isAuthorizedByAnOracleNode(AuthorizedData authorizedData) {
        if (authorizedData.getAuthorizedDistributedData().staticPublicKeysProvided()) {
            return true;
        }
        String authorizedPublicKey = Hex.encode(authorizedData.getAuthorizedPublicKeyBytes());
        return getAuthorizedBondedRoleStream()
                .filter(bondedRole -> bondedRole.getBondedRoleType() == BondedRoleType.ORACLE_NODE)
                .anyMatch(bondedRole -> bondedRole.getAuthorizedPublicKey().equals(authorizedPublicKey));
    }

    private Optional<AuthorizedBondedRole> validateBondedRole(AuthorizedData authorizedData,
                                                              AuthorizedBondedRole authorizedBondedRole) {
        try {
            authorizedBondedRole.verifyRegistration();
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring invalid AuthorizedBondedRole registration data", e);
            return Optional.empty();
        }

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
