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
import bisq.common.encoding.Hex;
import bisq.common.util.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    private final String baseDir;

    public IdentityService(PersistenceService persistenceService,
                           KeyPairService keyPairService,
                           NetworkService networkService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
        this.keyPairService = keyPairService;
        this.networkService = networkService;

        baseDir = persistenceService.getBaseDir();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.supplyAsync(() -> {
            Identity defaultIdentity = getOrCreateDefaultIdentity();
            networkService.createDefaultServiceNodes(defaultIdentity.getNetworkId(), defaultIdentity.getTorIdentity());

            initializeActiveIdentities();
            return true;
        });
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> persist() {
        maybePersistTorIdentitiesToTorDir();
        return getPersistence().persistAsync(getPersistableStore().getClone())
                .handle((nil, throwable) -> throwable == null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * We first look up if we find an identity in the active identities map, if not we take one from the pool and
     * clone it with the new identityTag. If none present we create a fresh identity and initialize it.
     * The active and pooled identities get initialized at start-up, so it can be expected that they are already
     * initialized, but there is no guarantee for that.
     * Client code has to deal with the async nature of the node initialisation which takes a few seconds usually,
     * but user experience should in most cases not suffer from an additional delay.
     *
     * @param identityTag The id used to map the identity to some domain aspect (e.g. offerId)
     * @return A future which completes when the node associated with that identity is initialized.
     */
    public Identity getOrCreateIdentity(String identityTag) {
        return findActiveIdentity(identityTag)
                .orElseGet(() -> createAndInitializeNewActiveIdentity(identityTag));
    }

    public Identity getOrCreateIdentity(String identityTag, String keyId, KeyPair keyPair) {
        return findActiveIdentity(identityTag)
                .orElseGet(() -> createAndInitializeNewActiveIdentity(identityTag, keyId, keyPair));
    }

    public Identity getOrCreateDefaultIdentity() {
        return persistableStore.getDefaultIdentity()
                .orElseGet(() -> {
                    Identity identity = createIdentity(keyPairService.getDefaultKeyId(), DEFAULT_IDENTITY_TAG);
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
    public CompletableFuture<Identity> createNewActiveIdentity(String identityTag,
                                                               String keyId,
                                                               KeyPair keyPair) {
        keyPairService.persistKeyPair(keyId, keyPair);
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);

        TorIdentity torIdentity = findOrCreateTorIdentity(identityTag);
        NetworkId networkId = createNetworkId(false, pubKey, torIdentity);
        Identity identity = new Identity(identityTag, networkId, torIdentity, keyPair);

        synchronized (lock) {
            getActiveIdentityByTag().put(identityTag, identity);
        }
        persist();

        return networkService.getNetworkIdOfInitializedNode(networkId, torIdentity)
                .thenApply(nodes -> identity);
    }

    public Identity createAndInitializeNewActiveIdentity(String identityTag) {
        Identity identity = createAndInitializeNewIdentity(identityTag);

        synchronized (lock) {
            getActiveIdentityByTag().put(identityTag, identity);
        }
        persist();

        return identity;
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

    private Identity createAndInitializeNewActiveIdentity(String identityTag, String keyId, KeyPair keyPair) {
        Identity identity = createAndInitializeNewIdentity(identityTag, keyId, keyPair);

        synchronized (lock) {
            getActiveIdentityByTag().put(identityTag, identity);
        }
        persist();

        return identity;
    }

    private Identity createAndInitializeNewIdentity(String identityTag) {
        String keyId = StringUtils.createUid();
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        return createAndInitializeNewIdentity(identityTag, keyId, keyPair);
    }

    private Identity createAndInitializeNewIdentity(String identityTag, String keyId, KeyPair keyPair) {
        Identity identity = createIdentity(keyId, identityTag, keyPair);
        networkService.getNetworkIdOfInitializedNode(identity.getNetworkId(), identity.getTorIdentity()).join();
        return identity;
    }

    private Identity createIdentity(String keyId, String identityTag) {
        KeyPair keyPair = keyPairService.getOrCreateKeyPair(keyId);
        return createIdentity(keyId, identityTag, keyPair);
    }

    private Identity createIdentity(String keyId, String identityTag, KeyPair keyPair) {
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyId);
        boolean isDefaultIdentity = identityTag.equals(DEFAULT_IDENTITY_TAG);
        TorIdentity torIdentity = findOrCreateTorIdentity(identityTag);
        NetworkId networkId = createNetworkId(isDefaultIdentity, pubKey, torIdentity);
        return new Identity(identityTag, networkId, torIdentity, keyPair);
    }

    private TorIdentity findOrCreateTorIdentity(String identityTag) {
        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        boolean isTorSupported = supportedTransportTypes.contains(TransportType.TOR);
        if (isTorSupported) {
            // If we find a persisted tor private_key in the tor hiddenservice directory for the given identityTag
            // we use that, otherwise we create a new one.
            Optional<TorIdentity> persistedTorIdentity = findPersistedTorIdentityFromTorDir(identityTag);
            if (persistedTorIdentity.isPresent()) {
                return persistedTorIdentity.get();
            }
        }

        Map<TransportType, Integer> defaultPorts = networkService.getDefaultNodePortByTransportType();
        int torPort = isTorSupported && identityTag.equals(DEFAULT_IDENTITY_TAG) ?
                defaultPorts.getOrDefault(TransportType.TOR, NetworkUtils.selectRandomPort()) :
                NetworkUtils.selectRandomPort();
        return TorIdentity.generate(torPort);
    }

    private NetworkId createNetworkId(boolean isForDefaultId, PubKey pubKey, TorIdentity torIdentity) {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();
        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        Map<TransportType, Integer> defaultPorts = networkService.getDefaultNodePortByTransportType();

        boolean isTorSupported = supportedTransportTypes.contains(TransportType.TOR);
        int torPort = isTorSupported && isForDefaultId ?
                defaultPorts.getOrDefault(TransportType.TOR, NetworkUtils.selectRandomPort()) :
                NetworkUtils.selectRandomPort();

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

    private Map<String, Identity> getActiveIdentityByTag() {
        return persistableStore.getActiveIdentityByTag();
    }

    private Set<Identity> getRetired() {
        return persistableStore.getRetired();
    }

    private void maybePersistTorIdentitiesToTorDir() {
        Set<TransportType> supportedTransportTypes = networkService.getSupportedTransportTypes();
        if (!supportedTransportTypes.contains(TransportType.TOR)) {
            return;
        }
        persistableStore.getDefaultIdentity().ifPresent(defaultIdentity ->
                maybePersistTorIdentityToTorDir(defaultIdentity, "default"));
        persistableStore.getActiveIdentityByTag().forEach((key, value) -> maybePersistTorIdentityToTorDir(value, key));
    }

    private void maybePersistTorIdentityToTorDir(Identity identity, String identityTag) {
        if (identity.getNetworkId().getAddressByTransportTypeMap().containsKey(TransportType.TOR)) {
            TorIdentity torIdentity = identity.getTorIdentity();
            String directory = getTorHiddenServiceDirectory(identityTag);
            try {
                FileUtils.makeDirs(directory);
                File privateKeyFile = Path.of(directory, "private_key").toFile();
                if (!privateKeyFile.exists()) {
                    byte[] privateKey = torIdentity.getPrivateKey();
                    String privateKeyAsHex = Hex.encode(privateKey);
                    FileUtils.writeToFile(privateKeyAsHex, Path.of(directory, "private_key_hex").toFile());
                    FileUtils.writeToFile(torIdentity.getTorOnionKey(), privateKeyFile);
                    FileUtils.writeToFile(torIdentity.getOnionAddress(), Path.of(directory, "hostname").toFile());
                    FileUtils.writeToFile(String.valueOf(torIdentity.getPort()), Path.of(directory, "port").toFile());
                    log.info("We persisted the default tor identity {} to {}.", torIdentity, directory);
                }
            } catch (IOException e) {
                log.error("Could not persist torIdentity", e);
            }
        }
    }

    private Optional<TorIdentity> findPersistedTorIdentityFromTorDir(String identityTag) {
        String directory = getTorHiddenServiceDirectory(identityTag);
        if (!new File(directory).exists()) {
            return Optional.empty();
        }

        try {
            String privateKeyAsHex = FileUtils.readStringFromFile(Path.of(directory, "private_key_hex").toFile());
            byte[] privateKey = Hex.decode(privateKeyAsHex);
            int port = Integer.parseInt(FileUtils.readStringFromFile(Path.of(directory, "port").toFile()));
            TorIdentity torIdentity = TorIdentity.from(privateKey, port);
            log.info("We found an existing tor identity {} at {} and use that for identityTag {}.", torIdentity, directory, identityTag);
            return Optional.of(torIdentity);
        } catch (IOException e) {
            log.warn("Could not read private_key_hex or port", e);
            return Optional.empty();
        }
    }

    private String getTorHiddenServiceDirectory(String nodeId) {
        return Path.of(baseDir, "tor", "hiddenservice", nodeId).toString();
    }
}