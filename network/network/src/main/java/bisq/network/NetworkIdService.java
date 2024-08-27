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

package bisq.network;


import bisq.common.application.Service;
import bisq.common.file.FileUtils;
import bisq.common.util.NetworkUtils;
import bisq.network.common.Address;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.PubKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High level API for network access to p2p network as well to http services (over Tor). If user has only I2P selected
 * for p2p network a tor instance will be still bootstrapped for usage for the http requests. Only if user has
 * clearNet enabled clearNet is used for https.
 */
@Slf4j
public class NetworkIdService implements PersistenceClient<NetworkIdStore>, Service {

    @Getter
    private final NetworkIdStore persistableStore = new NetworkIdStore();
    @Getter
    private final Persistence<NetworkIdStore> persistence;

    private final Map<TransportType, Integer> defaultPortByTransportType;
    private final KeyBundleService keyBundleService;
    private final Set<TransportType> supportedTransportTypes;

    public NetworkIdService(PersistenceService persistenceService,
                            KeyBundleService keyBundleService,
                            Set<TransportType> supportedTransportTypes,
                            Map<TransportType, Integer> defaultPortByTransportType) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.keyBundleService = keyBundleService;
        this.supportedTransportTypes = supportedTransportTypes;
        this.defaultPortByTransportType = defaultPortByTransportType;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public NetworkId getOrCreateDefaultNetworkId() {
        // keyBundleService creates the defaultKeyBundle at initialize, and is called before we get initialized
        KeyBundle keyBundle = keyBundleService.findDefaultKeyBundle().orElseThrow();
        return getOrCreateNetworkId(keyBundle, "default");
    }

    public NetworkId getOrCreateNetworkId(KeyBundle keyBundle, String tag) {
        return findNetworkId(tag)
                .orElseGet(() -> createNetworkId(keyBundle, tag));
    }

    public Optional<NetworkId> findNetworkId(String tag) {
        return persistableStore.findNetworkId(tag);
    }

    public void migrateFromDeprecatedStore(Map<String, NetworkId> fromDeprecatedStore) {
        Map<String, NetworkId> persistedMap = persistableStore.getNetworkIdByTag();
        AtomicBoolean anyChange = new AtomicBoolean();
        fromDeprecatedStore.entrySet().stream()
                .filter(e -> !persistedMap.containsKey(e.getKey()))
                .forEach(e -> {
                    NetworkId previous = persistedMap.put(e.getKey(), e.getValue());
                    anyChange.set(anyChange.get() || previous == null);
                });
        if (anyChange.get()) {
            persist();
        }
    }

    public void recoverInvalidNetworkIds(NetworkId networkIdFromIdentity, String tag) {
        tryToBackupCorruptedStoreFile();
        Map<String, NetworkId> persistedMap = persistableStore.getNetworkIdByTag();
        persistedMap.put(tag, networkIdFromIdentity);
        persist();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private NetworkId createNetworkId(KeyBundle keyBundle, String tag) {
        AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();
        supportedTransportTypes.forEach(transportType -> {
            int port = getPortByTransport(tag, transportType);
            Address address = getAddressByTransport(keyBundle, port, transportType);
            addressByTransportTypeMap.put(transportType, address);
        });

        KeyPair keyPair = keyBundle.getKeyPair();
        PubKey pubKey = new PubKey(keyPair.getPublic(), keyBundle.getKeyId());
        NetworkId networkId = new NetworkId(addressByTransportTypeMap, pubKey);
        persistableStore.getNetworkIdByTag().put(tag, networkId);
        persist();
        return networkId;
    }

    private int getPortByTransport(String tag, TransportType transportType) {
        boolean isDefault = tag.equals("default");
        /*  return isDefault ?
                            defaultPorts.computeIfAbsent(TransportType.I2P, key-> NetworkUtils.selectRandomPort()) :
                            NetworkUtils.selectRandomPort();*/
        return switch (transportType) {
            case TOR -> isDefault ?
                    defaultPortByTransportType.computeIfAbsent(TransportType.TOR, key -> NetworkUtils.selectRandomPort()) :
                    NetworkUtils.selectRandomPort();
            case I2P -> throw new RuntimeException("I2P not unsupported yet");
            case CLEAR -> isDefault ?
                    defaultPortByTransportType.computeIfAbsent(TransportType.CLEAR, key -> NetworkUtils.findFreeSystemPort()) :
                    NetworkUtils.findFreeSystemPort();
        };
    }

    private Address getAddressByTransport(KeyBundle keyBundle, int port, TransportType transportType) {
        //return new Address(keyBundle.getI2pKeyPair().getDestination(), port);
        return switch (transportType) {
            case TOR -> new Address(keyBundle.getTorKeyPair().getOnionAddress(), port);
            case I2P -> throw new RuntimeException("I2P not unsupported yet");
            case CLEAR -> Address.localHost(port);
        };
    }

    private void tryToBackupCorruptedStoreFile() {
        Path storeFilePath = persistence.getStorePath();
        Path parentDirectoryPath = storeFilePath.getParent();
        try {
            FileUtils.backupCorruptedFile(
                    parentDirectoryPath.toAbsolutePath().toString(),
                    storeFilePath.toFile(),
                    storeFilePath.getFileName().toString(),
                    "corruptedNetworkId"
            );
        } catch (IOException e) {
            log.error("Error trying to backup corrupted file {}", storeFilePath, e);
        }
    }
}
