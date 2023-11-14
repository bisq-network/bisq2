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
import com.google.common.collect.Streams;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
public class IdentityService implements PersistenceClient<IdentityStore>, Service {
    public static final String DEFAULT_IDENTITY_TAG = "default";

    @Getter
    private final IdentityStore persistableStore = new IdentityStore();
    @Getter
    private final Persistence<IdentityStore> persistence;
    private final KeyPairService keyPairService;
    private final NetworkService networkService;
    private final Object lock = new Object();

    public IdentityService(PersistenceService persistenceService,
                           KeyPairService keyPairService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.keyPairService = keyPairService;
        this.networkService = networkService;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        Identity defaultIdentity = getOrCreateDefaultIdentity();
        networkService.createDefaultServiceNodes(defaultIdentity.getNetworkId(), defaultIdentity.getTorIdentity());

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
    public Identity getOrCreateIdentity(String tag) {
        return findActiveIdentity(tag)
                .orElseGet(() -> createAndInitializeNewActiveIdentity(tag));
    }

    public Identity getOrCreateIdentity(String tag, String keyId, KeyPair keyPair) {
        return findActiveIdentity(tag)
                .orElseGet(() -> createAndInitializeNewActiveIdentity(tag, keyId, keyPair));
    }

    public Identity getOrCreateDefaultIdentity() {
        return persistableStore.getDefaultIdentity()
                .orElseGet(() -> {
                    Identity identity = createIdentity(DEFAULT_IDENTITY_TAG, DEFAULT_IDENTITY_TAG);
                    synchronized (lock) {
                        persistableStore.setDefaultIdentity(Optional.of(identity));
                    }
                    persist();
                    return identity;
                });
    }

    public Identity createAndInitializeNewActiveIdentity(String tag) {
        Identity identity = createAndInitializeNewIdentity(tag);

        synchronized (lock) {
            getActiveIdentityByTag().put(tag, identity);
        }
        persist();

        return identity;
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

    public Optional<Identity> findActiveIdentityByNetworkId(NetworkId networkId) {
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
            return Streams.concat(Stream.of(getOrCreateDefaultIdentity()), getActiveIdentityByTag().values().stream(), getRetired().stream())
                    .filter(e -> e.getNetworkId().equals(networkId))
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
                        networkService.getInitializedNodeByTransport(identity.getNetworkId(), identity.getPubKey(), identity.getTorIdentity()).values()
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

    private Identity createAndInitializeNewActiveIdentity(String tag, String keyId, KeyPair keyPair) {
        Identity identity = createAndInitializeNewIdentity(tag, keyId, keyPair);

        synchronized (lock) {
            getActiveIdentityByTag().put(tag, identity);
        }
        persist();

        return identity;
    }

    private Identity createAndInitializeNewIdentity(String tag) {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        return createAndInitializeNewIdentity(tag, keyId, keyPair);
    }

    private Identity createAndInitializeNewIdentity(String tag, String keyId, KeyPair keyPair) {
        Identity identity = createIdentity(keyId, tag, keyPair);
        networkService.getNetworkIdOfInitializedNode(identity.getNetworkId(), identity.getTorIdentity());
        return identity;
    }

    private Identity createIdentity(String keyId, String identityTag) {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        return createIdentity(keyId, identityTag, keyPair);
    }


    private Identity createIdentity(String keyId, String identityTag, KeyPair keyPair) {
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);

        boolean isDefaultIdentity = identityTag.equals(DEFAULT_IDENTITY_TAG);
        TorIdentity torIdentity = createTorIdentity(isDefaultIdentity);
        NetworkId networkId = createNetworkId(isDefaultIdentity, pubKey, torIdentity);

        return new Identity(identityTag, networkId, torIdentity, keyPair);
    }

    private TorIdentity createTorIdentity(boolean isForDefaultId) {
        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        Map<TransportType, Integer> defaultPorts = networkService.getDefaultNodePortByTransportType();

        boolean isTorSupported = supportedTransportTypes.contains(TransportType.TOR);
        int torPort = isTorSupported && isForDefaultId ?
                defaultPorts.getOrDefault(TransportType.TOR, NetworkUtils.selectRandomPort()) : NetworkUtils.selectRandomPort();
        return TorIdentity.generate(torPort);
    }

    private NetworkId createNetworkId(boolean isForDefaultId, PubKey pubKey, TorIdentity torIdentity) {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();
        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        Map<TransportType, Integer> defaultPorts = networkService.getDefaultNodePortByTransportType();

        boolean isTorSupported = supportedTransportTypes.contains(TransportType.TOR);
        int torPort = isTorSupported && isForDefaultId ?
                defaultPorts.getOrDefault(TransportType.TOR, NetworkUtils.selectRandomPort()) : NetworkUtils.selectRandomPort();

        if (isForDefaultId) {
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

        return new NetworkId(addressByTransportTypeMap, pubKey);
    }
}