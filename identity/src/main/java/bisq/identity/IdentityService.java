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

package bisq.identity;


import bisq.common.application.Service;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkIdService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.Node;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
public class IdentityService implements PersistenceClient<IdentityStore>, Service {
    public static final String DEFAULT_IDENTITY_TAG = "default";

    @Getter
    private final IdentityStore persistableStore = new IdentityStore();
    @Getter
    private final Persistence<IdentityStore> persistence;
    private final KeyBundleService keyBundleService;
    private final NetworkService networkService;
    private final Object lock = new Object();
    private final NetworkIdService networkIdService;
    @Getter
    private final Observable<RuntimeException> fatalException = new Observable<>();

    public IdentityService(PersistenceService persistenceService,
                           KeyBundleService keyBundleService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.keyBundleService = keyBundleService;
        this.networkService = networkService;
        networkIdService = networkService.getNetworkIdService();
    }


    /* --------------------------------------------------------------------- */
    // Service
    /* --------------------------------------------------------------------- */

    /**
     * Creates and initialized the default identity. This includes initialisation of the associated network node
     * on at least one transport.
     */
    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        // Create default identity
        getOrCreateDefaultIdentity();

        Map<TransportType, CompletableFuture<Node>> defaultNodeResultByTransport = networkService.getInitializedDefaultNodeByTransport();
        if (defaultNodeResultByTransport.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("networkService.getInitializedDefaultNodeByTransport returns an empty map"));
        }

        CompletableFuture<Boolean> resultFuture = new CompletableFuture<>();
        AtomicInteger failures = new AtomicInteger();
        defaultNodeResultByTransport.forEach((transportType, future) -> future
                .whenComplete((defaultNode, throwable) -> {
                    if (throwable == null && defaultNode != null) {
                        // After each successful initialisation of the default node on a transport we start to
                        // initialize the active identities for that transport using a blocking call.
                        // This will delay app startup until all identities are ready on the network side.
                        try {
                            initializeAllActiveIdentities(transportType).get();
                            if (!resultFuture.isDone()) {
                                resultFuture.complete(true);
                            }
                        } catch (Exception e) {
                            if (!resultFuture.isDone()) {
                                resultFuture.completeExceptionally(e);
                            }
                        }
                    } else if (!resultFuture.isDone()) {
                        if (failures.incrementAndGet() == defaultNodeResultByTransport.size()) {
                            // All failed
                            resultFuture.completeExceptionally(new RuntimeException("Default node initialization on all transports failed"));
                        }
                    }
                }));
        return resultFuture;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        return PersistenceClient.super.persist();
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public Identity getOrCreateDefaultIdentity() {
        Optional<Identity> defaultIdentity = persistableStore.getDefaultIdentity();
        defaultIdentity.ifPresent(this::maybeRecoverNetworkId);
        return defaultIdentity
                .orElseGet(() -> {
                    Identity identity = createIdentity(DEFAULT_IDENTITY_TAG);
                    synchronized (lock) {
                        persistableStore.setDefaultIdentity(identity);
                    }
                    persist();
                    return identity;
                });
    }

    /**
     * Creates new identity based on given parameters.
     */
    public CompletableFuture<Identity> createNewActiveIdentity(String identityTag, KeyPair keyPair) {
        KeyBundle keyBundle = keyBundleService.createAndPersistKeyBundle(identityTag, keyPair);
        NetworkId networkId = networkIdService.getOrCreateNetworkId(keyBundle, identityTag);
        Identity identity = new Identity(identityTag, networkId, keyBundle);

        synchronized (lock) {
            getActiveIdentityByTag().put(identityTag, identity);
        }
        persist();
        // We return the identity if at least one transport node got initialized
        return networkService.anySuppliedInitializedNode(networkId)
                .thenApply(nodes -> identity);
    }

    public boolean retireActiveIdentity(String identityTag) {
        boolean wasRemoved;
        synchronized (lock) {
            Identity identity = getActiveIdentityByTag().remove(identityTag);
            wasRemoved = identity != null;
            if (wasRemoved) {
                persistableStore.getRetired().add(identity);
            }
        }
        if (wasRemoved) {
            persist();
        }
        return wasRemoved;
    }

    public Optional<Identity> findActiveIdentity(String identityTag) {
        synchronized (lock) {
            return Optional.ofNullable(getActiveIdentityByTag().get(identityTag));
        }
    }

    public Optional<Identity> findActiveIdentity(NetworkId networkId) {
        synchronized (lock) {
            return getActiveIdentityByTag().values().stream()
                    .filter(e -> e.getNetworkId().equals(networkId))
                    .findAny();
        }
    }

    public Optional<Identity> findRetiredIdentityByNetworkId(NetworkId networkId) {
        synchronized (lock) {
            return getRetired().stream()
                    .filter(e -> e.getNetworkId().equals(networkId))
                    .findAny();
        }
    }

    public Optional<Identity> findAnyIdentityByNetworkId(NetworkId networkId) {
        synchronized (lock) {
            return Stream.concat(Stream.concat(persistableStore.getDefaultIdentity().stream(),
                                    getActiveIdentityByTag().values().stream()),
                            getRetired().stream())
                    .filter(e -> e.getNetworkId().equals(networkId))
                    .findAny();
        }
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private CompletableFuture<List<Node>> initializeAllActiveIdentities(TransportType transportType) {
        Stream<CompletableFuture<Node>> futures = getActiveIdentityByTag().values().stream()
                .filter(identity -> !identity.getTag().equals(IdentityService.DEFAULT_IDENTITY_TAG))
                .map(identity -> {
                    maybeRecoverNetworkId(identity);
                    return networkService.supplyInitializedNode(transportType, identity.getNetworkId());
                });
        return CompletableFutureUtils.allOf(futures);
    }

    private CompletableFuture<Identity> createAndInitializeNewActiveIdentity(String identityTag, Identity identity) {
        synchronized (lock) {
            getActiveIdentityByTag().put(identityTag, identity);
        }
        persist();

        return networkService.anySuppliedInitializedNode(identity.getNetworkId())
                .thenApply(node -> identity);
    }

    private Identity createIdentity(String identityTag) {
        String keyId = keyBundleService.getKeyIdFromTag(identityTag);
        KeyBundle keyBundle = keyBundleService.getOrCreateKeyBundle(keyId);
        NetworkId networkId = networkIdService.getOrCreateNetworkId(keyBundle, identityTag);
        return new Identity(identityTag, networkId, keyBundle);
    }

    private Map<String, Identity> getActiveIdentityByTag() {
        return persistableStore.getActiveIdentityByTag();
    }

    private Set<Identity> getRetired() {
        return persistableStore.getRetired();
    }


    @VisibleForTesting
    CompletableFuture<Identity> createAndInitializeNewActiveIdentity(String identityTag) {
        return createAndInitializeNewActiveIdentity(identityTag, createIdentity(identityTag));
    }


    @VisibleForTesting
    Identity getOrCreateIdentity(String identityTag) {
        return findActiveIdentity(identityTag)
                .orElseGet(() -> createAndInitializeNewActiveIdentity(identityTag).join());
    }

    private void maybeRecoverNetworkId(Identity identity) {
        networkIdService.findNetworkId(identity.getTag())
                .ifPresent(fromNetworkIdStore -> {
                    NetworkId fromIdentityStore = identity.getNetworkId();
                    if (!fromNetworkIdStore.equals(fromIdentityStore)) {
                        String errorMessage = "Data inconsistency detected.\n" +
                                "The inconsistent data got restored and requires a restart of the application.\n" +
                                "Details:\n" +
                                "The persisted networkId from our identity store does not match the persisted networkId from the networkIdStore.\n" +
                                "The issue could have happened if the cache directory was deleted in versions before 2.1.0. " +
                                "In that case the networkId from networkIdService got assigned a new random port.\n" +
                                "Data:\n" +
                                "FromIdentityStore=" + fromIdentityStore + "\nFromNetworkIdStore=" + fromNetworkIdStore;
                        log.error(errorMessage);
                        networkIdService.recoverInvalidNetworkIds(fromIdentityStore, identity.getTag());
                        fatalException.set(new RuntimeException(errorMessage));
                    }
                });
    }
}