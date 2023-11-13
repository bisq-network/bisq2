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
import bisq.common.util.NetworkUtils;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.identity.TorIdentity;
import bisq.network.p2p.node.Node;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import bisq.security.SecurityService;
import com.google.common.base.Supplier;
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class IdentityService implements PersistenceClient<IdentityStore>, Service {
    public final static String DEFAULT = "default";

    @Getter
    private final IdentityStore persistableStore = new IdentityStore();
    @Getter
    private final Persistence<IdentityStore> persistence;
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    private final Object lock = new Object();

    public IdentityService(PersistenceService persistenceService,
                           SecurityService securityService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        keyPairService = securityService.getKeyPairService();
        this.networkService = networkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        NetworkId defaultNetworkIdentity = getOrCreateDefaultIdentity().getNetworkId();
        networkService.createDefaultServiceNodes(defaultNetworkIdentity);

        initializeActiveIdentities();
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Identity> createAndInitializeIdentity(String keyId, String nodeId, String identityTag) {
        Identity identity = createIdentity(keyId, nodeId, identityTag);
        return networkService.getNetworkIdOfInitializedNode(identity.getNetworkId())
                .thenApply(nodes -> identity);
    }

    private Identity createIdentity(String keyId, String nodeId, String identityTag) {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        NetworkId networkId = createNetworkId(nodeId, pubKey);
        return new Identity(identityTag, networkId, keyPair);
    }

    private NetworkId createNetworkId(String nodeId, PubKey pubKey) {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();
        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        Map<TransportType, Integer> defaultPorts = networkService.getDefaultNodePortByTransportType();

        boolean isTorSupported = supportedTransportTypes.contains(TransportType.TOR);
        int torPort = isTorSupported && nodeId.equals(Node.DEFAULT) ?
                defaultPorts.getOrDefault(TransportType.TOR, NetworkUtils.selectRandomPort()) : NetworkUtils.selectRandomPort();
        TorIdentity torIdentity = TorIdentity.generate(torPort);

        if (nodeId.equals(Node.DEFAULT)) {
            if (supportedTransportTypes.contains(TransportType.CLEAR)) {
                int port = defaultPorts.getOrDefault(TransportType.CLEAR, NetworkUtils.findFreeSystemPort());
                Address address = Address.localHost(port);
                addressByTransportTypeMap.put(TransportType.CLEAR, address);
            }

            if (isTorSupported) {
                Address address = new Address(torIdentity.getOnionAddress(), torPort);
                addressByTransportTypeMap.put(TransportType.TOR, address);
            }
        } else {
            if (supportedTransportTypes.contains(TransportType.CLEAR)) {
                int port = NetworkUtils.findFreeSystemPort();
                Address address = Address.localHost(port);
                addressByTransportTypeMap.put(TransportType.CLEAR, address);
            }

            if (isTorSupported) {
                Address address = new Address(torIdentity.getOnionAddress(), torPort);
                addressByTransportTypeMap.put(TransportType.TOR, address);
            }
        }

        return new NetworkId(addressByTransportTypeMap, pubKey, nodeId, torIdentity);
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
                .orElseGet(() -> CompletableFuture.completedFuture(createAndInitializeNewActiveIdentity(tag)));
    }

    public Identity getOrCreateDefaultIdentity() {
        return findActiveIdentity(Node.DEFAULT)
                .orElseGet((Supplier<Identity>) () -> {
                    Identity identity = createIdentity(Node.DEFAULT, Node.DEFAULT, Node.DEFAULT);
                    synchronized (lock) {
                        getActiveIdentityByTag().put(Node.DEFAULT, identity);
                    }
                    persist();
                    return identity;
                });

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

        NetworkId networkId = createNetworkId(nodeId, pubKey);
        Identity identity = new Identity(tag, networkId, keyPair);

        synchronized (lock) {
            getActiveIdentityByTag().put(tag, identity);
        }
        persist();

        return networkService.getNetworkIdOfInitializedNode(networkId)
                .thenApply(nodes -> identity);
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

    public Optional<Identity> findRetiredIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return getRetired().stream()
                    .filter(e -> e.getNetworkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Optional<Identity> findAnyIdentityByNodeId(String nodeId) {
        synchronized (lock) {
            return Streams.concat(getActiveIdentityByTag().values().stream(), getRetired().stream())
                    .filter(e -> e.getNetworkId().getNodeId().equals(nodeId))
                    .findAny();
        }
    }

    public Map<String, Identity> getActiveIdentityByTag() {
        return persistableStore.getActiveIdentityByTag();
    }

    public Set<Identity> getRetired() {
        return persistableStore.getRetired();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void initializeActiveIdentities() {
        getActiveIdentityByTag().values().stream()
                .filter(identity -> !identity.getTag().equals(Node.DEFAULT))
                .forEach(identity ->
                        networkService.getInitializedNodeByTransport(identity.getNetworkId(), identity.getPubKey()).values()
                                .forEach(future -> future.whenComplete((node, throwable) -> {
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

    public Identity createAndInitializeNewActiveIdentity(String tag) {
        Identity identity = createAndInitializeNewIdentity(tag);

        synchronized (lock) {
            getActiveIdentityByTag().put(tag, identity);
        }
        persist();

        return identity;
    }

    private Identity createAndInitializeNewIdentity(String tag) {
        Identity identity = createTemporaryIdentity(tag);
        networkService.getNetworkIdOfInitializedNode(identity.getNetworkId());
        return identity;
    }

    private Identity createTemporaryIdentity(String tag) {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        String nodeId = StringUtils.createUid();

        NetworkId networkId = createNetworkId(nodeId, pubKey);
        return new Identity(tag, networkId, keyPair);
    }

    public Identity createTemporaryIdentity() {
        return createTemporaryIdentity(StringUtils.createUid());
    }
}