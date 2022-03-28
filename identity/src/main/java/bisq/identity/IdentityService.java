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


import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * todo
 * add identity selection strategy. E.g. one identity per domain ID or one identity per context
 * type (e.g. fiat trades) or one global identity...
 * Add support for userName mapping with identity (not sure if should be done here or in social module)
 */
@Slf4j
public class IdentityService implements PersistenceClient<IdentityStore> {
    public final static String DEFAULT = "default";
    private final int minPoolSize;

    public static record Config(int minPoolSize) {
        public static Config from(com.typesafe.config.Config typeSafeConfig) {
            return new Config(typeSafeConfig.getInt("minPoolSize"));
        }
    }

    @Getter
    private final IdentityStore persistableStore = new IdentityStore();
    @Getter
    private final Persistence<IdentityStore> persistence;
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    private final Object lock = new Object();

    public IdentityService(PersistenceService persistenceService,
                           KeyPairService keyPairService,
                           NetworkService networkService,
                           Config config) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.keyPairService = keyPairService;
        this.networkService = networkService;
        minPoolSize = config.minPoolSize;
    }

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        initializeActiveIdentities();
        initializePooledIdentities();
        initializeMissingPooledIdentities();
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * We first look up if we find an identity in the active identities map, if not we take one from the pool and
     * clone it with the new domainId. If none present we create a fresh identity and initialize it.
     * The active and pooled identities get initialized as start-up, so it can be expected that they are already
     * initialized, but there is no guarantee for that.
     * Client code has to deal with the async nature of the node initialisation which takes a few seconds usually,
     * but user experience should in most cases not suffer from an additional delay.
     *
     * @param domainId The id used to map the identity to some domain aspect (e.g. offerId)
     * @return A future which completes when the node associated with that identity is initialized.
     */
    public CompletableFuture<Identity> getOrCreateIdentity(String domainId) {
        return findActiveIdentity(domainId).map(CompletableFuture::completedFuture)
                .orElseGet(() -> swapPooledIdentity(domainId).map(CompletableFuture::completedFuture)
                        .orElseGet(() -> createAndInitializeNewActiveIdentity(domainId)));
    }

    public Optional<Identity> findActiveIdentity(String domainId) {
        synchronized (lock) {
            return Optional.ofNullable(persistableStore.getActiveIdentityByDomainId().get(domainId));
        }
    }

    public void retireIdentity(String domainId) {
        boolean wasRemoved;
        synchronized (lock) {
            Identity identity = persistableStore.getActiveIdentityByDomainId().remove(domainId);
            wasRemoved = identity != null;
            if (wasRemoved) {
                persistableStore.getRetired().add(identity);
            }
        }
        if (wasRemoved) {
            persist();
        }
    }

    public Optional<Identity> findActiveIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return persistableStore.getActiveIdentityByDomainId().values().stream()
                    .filter(e -> e.networkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findPooledIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return persistableStore.getPool().stream()
                    .filter(e -> e.networkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findRetiredIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return persistableStore.getRetired().stream()
                    .filter(e -> e.networkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findAnyIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return Streams.concat(persistableStore.getActiveIdentityByDomainId().values().stream(),
                            Streams.concat(persistableStore.getRetired().stream(),
                                    persistableStore.getPool().stream()))
                    .filter(e -> e.networkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public CompletableFuture<Identity> createNewInitializedIdentity(String domainId, String keyId, KeyPair keyPair) {
        keyPairService.persistKeyPair(keyId, keyPair);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        String nodeId = StringUtils.createUid();
        return networkService.getInitializedNetworkId(nodeId, pubKey)
                .thenApply(networkId -> {
                    Identity identity = new Identity(domainId, networkId, keyPair);
                    synchronized (lock) {
                        persistableStore.getPool().add(identity);
                        persistableStore.getActiveIdentityByDomainId().put(domainId, identity);
                    }
                    persist();
                    return identity;
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // If the pool is not empty we take an identity from the pool and clone it with the new domainId.
    // We search first for identities with initialized nodes, otherwise we take any.
    private Optional<Identity> swapPooledIdentity(String domainId) {
        synchronized (lock) {
            return persistableStore.getPool().stream()
                    .filter(identity -> networkService.findNetworkId(identity.nodeId(), identity.pubKey()).isPresent())
                    .findAny()
                    .or(() -> persistableStore.getPool().stream().findAny()) // If none is initialized we take any
                    .map(pooledIdentity -> {
                        Identity clonedIdentity;
                        synchronized (lock) {
                            clonedIdentity = new Identity(domainId, pooledIdentity.networkId(), pooledIdentity.keyPair());
                            persistableStore.getPool().remove(pooledIdentity);
                            persistableStore.getActiveIdentityByDomainId().put(domainId, clonedIdentity);
                        }
                        persist();

                        // Refill pool if needed
                        if (numMissingPooledIdentities() > 0) {
                            createAndInitializeNewPooledIdentity();
                        }
                        return clonedIdentity;
                    });
        }
    }

    private void initializeMissingPooledIdentities() {
        for (int i = 0; i < numMissingPooledIdentities(); i++) {
            createAndInitializeNewPooledIdentity();
        }
    }

    private int numMissingPooledIdentities() {
        synchronized (lock) {
            return Math.max(0, minPoolSize - persistableStore.getPool().size());
        }
    }

    private void initializeActiveIdentities() {
        persistableStore.getActiveIdentityByDomainId().values().forEach(identity ->
                networkService.maybeInitializeServer(identity.nodeId(), identity.pubKey()).values()
                        .forEach(value -> value.whenComplete((result, throwable) -> {
                                    if (throwable == null && result) {
                                        log.info("Network node for active identity {} initialized. NetworkId={}",
                                                identity.domainId(), identity.networkId());
                                    } else {
                                        log.error("Initializing network node for active identity {} failed. NetworkId={}",
                                                identity.domainId(), identity.networkId());
                                    }
                                })
                        ));
    }

    private void initializePooledIdentities() {
        persistableStore.getPool().forEach(identity ->
                networkService.maybeInitializeServer(identity.nodeId(), identity.pubKey()).values()
                        .forEach(value -> value.whenComplete((result, throwable) -> {
                                    if (throwable == null && result) {
                                        log.info("Network node for pooled identity {} initialized. NetworkId={}",
                                                identity.domainId(), identity.networkId());
                                    } else {
                                        log.error("Initializing network node for pooled identity {} failed. NetworkId={}",
                                                identity.domainId(), identity.networkId());
                                    }
                                })
                        ));
    }

    private void createAndInitializeNewPooledIdentity() {
        createAndInitializeNewIdentity(Res.get("na"))
                .whenComplete((identity, throwable) -> {
                    if (throwable == null) {
                        log.info("Network node for pooled identity {} created and initialized. NetworkId={}",
                                identity.domainId(), identity.networkId());
                        synchronized (lock) {
                            persistableStore.getPool().add(identity);
                        }
                        persist();
                    } else {
                        log.error("Creation and initializing network node for pooled identity {} failed. NetworkId={}",
                                identity.domainId(), identity.networkId());
                    }
                });
    }

    private CompletableFuture<Identity> createAndInitializeNewActiveIdentity(String domainId) {
        return createAndInitializeNewIdentity(domainId)
                .thenApply(identity -> {
                    synchronized (lock) {
                        persistableStore.getActiveIdentityByDomainId().put(domainId, identity);
                    }
                    persist();
                    return identity;
                }).whenComplete((identity, throwable) -> {
                    if (throwable == null) {
                        log.info("Network node for active identity {} created and initialized. NetworkId={}",
                                identity.domainId(), identity.networkId());
                    } else {
                        log.error("Creation and initializing network node for active identity {} failed. NetworkId={}",
                                identity.domainId(), identity.networkId());
                    }
                });
    }

    private CompletableFuture<Identity> createAndInitializeNewIdentity(String domainId) {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        String nodeId = StringUtils.createUid();
        return networkService.getInitializedNetworkId(nodeId, pubKey)
                .thenApply(networkId -> new Identity(domainId, networkId, keyPair));
    }
}
