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
import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileUtils;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.I2PAddress;
import bisq.common.network.TorAddress;
import bisq.common.network.TransportType;
import bisq.common.util.NetworkUtils;
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


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public NetworkId getOrCreateDefaultNetworkId() {
        // keyBundleService creates the defaultKeyBundle at initialize, and is called before we get initialized
        KeyBundle keyBundle = keyBundleService.findDefaultKeyBundle().orElseThrow();
        String tag = "default";
        maybeUpdateNetworkId(keyBundle, tag);
        return getOrCreateNetworkId(keyBundle, tag);
    }

    public NetworkId getOrCreateNetworkId(KeyBundle keyBundle, String tag) {
        return findNetworkId(tag)
                .orElseGet(() -> createNetworkId(keyBundle, tag));
    }

    public void maybeUpdateNetworkId(KeyBundle keyBundle, String tag) {
        findNetworkId(tag).ifPresent(networkId -> {
            // In case we had already a networkId persisted, but we get a new transportType
            // added we update and persist the networkId.
            AddressByTransportTypeMap addressByTransportTypeMap = networkId.getAddressByTransportTypeMap();
            int previousSize = addressByTransportTypeMap.size();
            supportedTransportTypes.stream()
                    .filter(transportType -> !addressByTransportTypeMap.containsKey(transportType))
                    .forEach(transportType -> {
                        int port = getPortByTransport(tag, transportType);
                        Address address = getAddressByTransport(keyBundle, port, transportType);
                        log.warn("We add a new address to the addressByTransportTypeMap for {}: {}", transportType, address);
                        addressByTransportTypeMap.put(transportType, address);
                    });
            if (addressByTransportTypeMap.size() > previousSize) {
                KeyPair keyPair = keyBundle.getKeyPair();
                PubKey pubKey = new PubKey(keyPair.getPublic(), keyBundle.getKeyId());
                NetworkId updatedNetworkId = new NetworkId(addressByTransportTypeMap, pubKey);
                log.warn("We updated the networkId for {}: {}", tag, updatedNetworkId);
                persistableStore.getNetworkIdByTag().put(tag, updatedNetworkId);
                persist();
            }
        });
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


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

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
        return switch (transportType) {
            case TOR -> isDefault ?
                    defaultPortByTransportType.computeIfAbsent(TransportType.TOR, key -> NetworkUtils.selectRandomPort()) :
                    NetworkUtils.selectRandomPort();
            case I2P -> isDefault ?
                    defaultPortByTransportType.computeIfAbsent(TransportType.I2P, key -> NetworkUtils.selectRandomPort()) :
                    NetworkUtils.selectRandomPort();
            case CLEAR -> isDefault ?
                    defaultPortByTransportType.computeIfAbsent(TransportType.CLEAR, key -> NetworkUtils.findFreeSystemPort()) :
                    NetworkUtils.findFreeSystemPort();
        };
    }

    private Address getAddressByTransport(KeyBundle keyBundle, int port, TransportType transportType) {
        return switch (transportType) {
            case TOR -> new TorAddress(keyBundle.getTorKeyPair().getOnionAddress(), port);
            case I2P -> new I2PAddress(keyBundle.getI2PKeyPair().getDestinationBase64(),
                    keyBundle.getI2PKeyPair().getDestinationBase32(),
                    port);
            case CLEAR -> FacadeProvider.getClearNetAddressTypeFacade().toMyLocalAddress(port);
        };
    }

    private void tryToBackupCorruptedStoreFile() {
        Path storeFilePath = persistence.getStorePath();
        Path parentDirectoryPath = storeFilePath.getParent();
        try {
            FileUtils.backupCorruptedFile(
                    parentDirectoryPath.toAbsolutePath().toString(),
                    storeFilePath,
                    storeFilePath.getFileName().toString(),
                    "corruptedNetworkId"
            );
        } catch (IOException e) {
            log.error("Error trying to backup corrupted file {}", storeFilePath, e);
        }
    }
}
