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
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import bisq.security.SecurityService;
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class IdentityService implements PersistenceClient<IdentityStore>, Service {
    public final static String POOL_PREFIX = "pool-";
    public final static String DEFAULT = "default";

    @Getter
    @ToString
    public static final class Config {
        private final int minPoolSize;

        public Config(int minPoolSize) {
            this.minPoolSize = minPoolSize;
        }

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
    private final int minPoolSize;

    public IdentityService(Config config,
                           PersistenceService persistenceService,
                           SecurityService securityService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        keyPairService = securityService.getKeyPairService();
        this.networkService = networkService;
        minPoolSize = config.minPoolSize;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        initializeActiveIdentities();
        initializePooledIdentities();
        maybeFillUpPool();
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Use the default KeyId, default nodeId and default identityTag.
     * This is used usually by network nodes like the seed node or oracle node, which do not have privacy concerns.
     *
     * @return A future with the identity
     */
    public CompletableFuture<Identity> createAndInitializeIdentity(String keyId, String nodeId, String identityTag) {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        return networkService.getInitializedNetworkId(nodeId, pubKey)
                .thenApply(networkId -> new Identity(identityTag, networkId, keyPair));
    }

    /**
     * We first look up if we find an identity in the active identities map, if not we take one from the pool and
     * clone it with the new tag. If none present we create a fresh identity and initialize it.
     * The active and pooled identities get initialized at start-up, so it can be expected that they are already
     * initialized, but there is no guarantee for that.
     * Client code has to deal with the async nature of the node initialisation which takes a few seconds usually,
     * but user experience should in most cases not suffer from an additional delay.
     *
     * @param tag The id used to map the identity to some domain aspect (e.g. offerId)
     * @return A future which completes when the node associated with that identity is initialized.
     */
    public CompletableFuture<Identity> getOrCreateIdentity(String tag) {
        return findActiveIdentity(tag).map(CompletableFuture::completedFuture)
                .orElseGet(() -> swapAnyInitializedPooledIdentity(tag).map(CompletableFuture::completedFuture)
                        .orElseGet(() -> createAndInitializeNewActiveIdentity(tag)));
    }

    /**
     * Takes a pooled identity and swaps it with the given tag
     *
     * @param tag            The new domain ID for the active identity
     * @param pooledIdentity The pooled identity we want to swap.
     * @return The new active identity which is a clone of the pooled identity with the new domain ID.
     */
    public Identity swapPooledIdentity(String tag, Identity pooledIdentity) {
        checkArgument(!getActiveIdentityByTag().containsKey(tag),
                "We got already an active identity with the newDomainId");
        checkArgument(!getActiveIdentityByTag().containsKey(pooledIdentity.getTag()),
                "We got already an active identity with the tag of the pooledIdentity");
        Identity newIdentity = Identity.from(tag, pooledIdentity);
        synchronized (lock) {
            boolean existed = persistableStore.getPool().remove(pooledIdentity);
            checkArgument(existed, "The pooledIdentity did not exist in our pool");
            getActiveIdentityByTag().put(tag, newIdentity);
        }
        persist();

        // Refill pool if needed
        if (numMissingPooledIdentities() > 0) {
            createAndInitializeNewPooledIdentity();
        }
        return checkNotNull(newIdentity);
    }

    /**
     * Creates new identity based on given parameters.
     */
    public CompletableFuture<Identity> createNewActiveIdentity(String tag,
                                                               String keyId,
                                                               KeyPair keyPair) {
        keyPairService.persistKeyPair(keyId, keyPair);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        String nodeId = StringUtils.createUid();
        return networkService.getInitializedNetworkId(nodeId, pubKey)
                .thenApply(networkId -> {
                    Identity identity = new Identity(tag, networkId, keyPair);
                    synchronized (lock) {
                        getActiveIdentityByTag().put(tag, identity);
                    }
                    persist();
                    return identity;
                });
    }

    public boolean retireActiveIdentity(String tag) {
        boolean wasRemoved;
        synchronized (lock) {
            Identity identity = getActiveIdentityByTag().remove(tag);
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

    public Optional<Identity> findActiveIdentity(String tag) {
        synchronized (lock) {
            return Optional.ofNullable(getActiveIdentityByTag().get(tag));
        }
    }

    public Optional<Identity> findActiveIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return getActiveIdentityByTag().values().stream()
                    .filter(e -> e.getNetworkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findPooledIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return getPool().stream()
                    .filter(e -> e.getNetworkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findRetiredIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return getRetired().stream()
                    .filter(e -> e.getNetworkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findAnyIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return Streams.concat(getActiveIdentityByTag().values().stream(),
                            Streams.concat(getRetired().stream(),
                                    getPool().stream()))
                    .filter(e -> e.getNetworkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Map<String, Identity> getActiveIdentityByTag() {
        return persistableStore.getActiveIdentityByTag();
    }

    public Set<Identity> getPool() {
        return persistableStore.getPool();
    }

    public Set<Identity> getRetired() {
        return persistableStore.getRetired();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    // If the pool is not empty we take an identity from the pool and clone it with the new tag.
    // We search first for identities with initialized nodes, otherwise we take any.
    private Optional<Identity> swapAnyInitializedPooledIdentity(String tag) {
        synchronized (lock) {
            return persistableStore.getPool().stream()
                    .filter(identity -> networkService.isInitialized(identity.getNodeId()))
                    .findAny()
                    .or(() -> persistableStore.getPool().stream().findAny())
                    .map(pooledIdentity -> swapPooledIdentity(tag, pooledIdentity));
        }
    }

    private void maybeFillUpPool() {
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
        getActiveIdentityByTag().values().forEach(identity ->
                networkService.initializeNode(identity.getNodeId(), identity.getPubKey()).values()
                        .forEach(future -> future.whenComplete((__, throwable) -> {
                                    if (throwable == null) {
                                        log.info("Network node for active identity {} initialized. NetworkId={}",
                                                identity.getTag(), identity.getNetworkId());
                                    } else {
                                        log.error("Initializing network node for active identity {} failed. NetworkId={}",
                                                identity.getTag(), identity.getNetworkId());
                                    }
                                })
                        ));
    }

    private void initializePooledIdentities() {
        persistableStore.getPool().forEach(identity ->
                networkService.initializeNode(identity.getNodeId(), identity.getPubKey()).values()
                        .forEach(future -> future.whenComplete((__, throwable) -> {
                                    if (throwable == null) {
                                        log.info("Network node for pooled identity {} initialized. NetworkId={}",
                                                identity.getTag(), identity.getNetworkId());
                                    } else {
                                        log.error("Initializing network node for pooled identity {} failed. NetworkId={}",
                                                identity.getTag(), identity.getNetworkId());
                                    }
                                })
                        ));
    }

    private CompletableFuture<Identity> createAndInitializeNewPooledIdentity() {
        String tag = POOL_PREFIX + StringUtils.createUid();
        return createAndInitializeNewIdentity(tag)
                .thenApply(identity -> {
                    synchronized (lock) {
                        persistableStore.getPool().add(identity);
                    }
                    persist();
                    return identity;
                }).whenComplete((identity, throwable) -> {
                    if (throwable == null) {
                        log.info("Network node for pooled identity {} created and initialized. NetworkId={}",
                                identity.getTag(), identity.getNetworkId());
                    } else {
                        log.error("Creation and initializing network node for pooled identity {} failed. NetworkId={}",
                                identity.getTag(), identity.getNetworkId());
                    }
                });
    }

    private CompletableFuture<Identity> createAndInitializeNewActiveIdentity(String tag) {
        return createAndInitializeNewIdentity(tag)
                .thenApply(identity -> {
                    synchronized (lock) {
                        getActiveIdentityByTag().put(tag, identity);
                    }
                    persist();
                    return identity;
                }).whenComplete((identity, throwable) -> {
                    if (throwable == null) {
                        log.info("Network node for active identity {} created and initialized. NetworkId={}",
                                identity.getTag(), identity.getNetworkId());
                    } else {
                        log.error("Creation and initializing network node for active identity {} failed. NetworkId={}",
                                identity.getTag(), identity.getNetworkId());
                    }
                });
    }

    private CompletableFuture<Identity> createAndInitializeNewIdentity(String tag) {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        String nodeId = StringUtils.createUid();
        return networkService.getInitializedNetworkId(nodeId, pubKey)
                .thenApply(networkId -> new Identity(tag, networkId, keyPair));
    }
}