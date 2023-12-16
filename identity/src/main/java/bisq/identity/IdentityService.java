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
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.Node;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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

    public IdentityService(PersistenceService persistenceService,
                           KeyBundleService keyBundleService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.keyBundleService = keyBundleService;
        this.networkService = networkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates and initialized the default identity. This includes initialisation of the associated network node
     * on at least one transport.
     */
    // TODO Would be good to decouple that dependency on network node initialisation
    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        Map<TransportType, CompletableFuture<Node>> map = networkService.getInitializedDefaultNodeByTransport();
        if (map.isEmpty()) {
            return CompletableFuture.failedFuture(new RuntimeException("networkService.getInitializedDefaultNodeByTransport returns an empty map"));
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        AtomicInteger failures = new AtomicInteger();
        map.forEach((transportType, future) -> {
            future.whenComplete((node, throwable) -> {
                if (throwable == null && node != null) {
                    // After each successful initialisation of the default node on a transport we start to
                    // initialize the active identities for that transport
                    initializeActiveIdentities(transportType);
                    if (!result.isDone()) {
                        result.complete(true);
                    }
                } else if (!result.isDone()) {
                    if (failures.incrementAndGet() == map.size()) {
                        // All failed
                        result.completeExceptionally(new RuntimeException("Default node initialization on all transports failed"));
                    }
                }
            });
        });
        return result;
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        return getPersistence().persistAsync(getPersistableStore().getClone())
                .handle((nil, throwable) -> throwable == null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Identity getOrCreateDefaultIdentity() {
        return persistableStore.getDefaultIdentity()
                .orElseGet(() -> {
                    Identity identity = createIdentity(DEFAULT_IDENTITY_TAG);
                    synchronized (lock) {
                        persistableStore.setDefaultIdentity(Optional.of(identity));
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
        NetworkId networkId = networkService.createAndGetNetworkId(keyBundle);
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
            return Streams.concat(persistableStore.getDefaultIdentity().stream(),
                            getActiveIdentityByTag().values().stream(),
                            getRetired().stream())
                    .filter(e -> e.getNetworkId().equals(networkId))
                    .findAny();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void initializeActiveIdentities(TransportType transportType) {
        getActiveIdentityByTag().values().stream()
                .filter(identity -> !identity.getTag().equals(IdentityService.DEFAULT_IDENTITY_TAG))
                .forEach(identity -> networkService.supplyInitializedNode(transportType, identity.getNetworkId()));
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
        NetworkId networkId = networkService.createAndGetNetworkId(keyBundle);
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
}