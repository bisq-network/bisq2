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

import bisq.common.application.ConfigUtil;
import bisq.common.network.Address;
import bisq.common.network.TransportConfig;
import bisq.common.network.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.ClearNetTransportService;
import bisq.network.p2p.node.transport.I2PTransportService;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeStrategy;
import bisq.network.p2p.services.peer_group.keep_alive.KeepAliveService;
import bisq.tor.TorTransportConfig;
import com.typesafe.config.Config;
import lombok.Getter;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Getter
public final class NetworkServiceConfig {
    public static NetworkServiceConfig from(Path baseDir, Config config) {
        ServiceNode.Config serviceNodeConfig = ServiceNode.Config.from(config.getConfig("serviceNode"));
        InventoryService.Config inventoryServiceConfig = InventoryService.Config.from(config.getConfig("inventory"));
        AuthorizationService.Config authorizationServiceConfig = AuthorizationService.Config.from(config.getConfig("authorization"));
        Config seedConfig = config.getConfig("seedAddressByTransportType");
        // Only read seed addresses for explicitly supported address types
        Set<TransportType> supportedTransportTypes = new HashSet<>(config.getEnumList(TransportType.class, "supportedTransportTypes"));
        Map<TransportType, Set<Address>> seedAddressesByTransport = supportedTransportTypes.stream()
                .collect(toMap(supportedTransportType -> supportedTransportType,
                        supportedTransportType -> getSeedAddresses(supportedTransportType, seedConfig)));

        Set<Feature> features = new HashSet<>(config.getEnumList(Feature.class, "features"));

        PeerGroupService.Config peerGroupConfig = PeerGroupService.Config.from(config.getConfig("peerGroup"));
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = PeerExchangeStrategy.Config.from(config.getConfig("peerExchangeStrategy"));
        KeepAliveService.Config keepAliveServiceConfig = KeepAliveService.Config.from(config.getConfig("keepAlive"));

        PeerGroupManager.Config defaultConf = PeerGroupManager.Config.from(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                config.getConfig("defaultPeerGroup"));
        PeerGroupManager.Config clearNetConf = PeerGroupManager.Config.from(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                config.getConfig("clearNetPeerGroup"));

        Map<TransportType, PeerGroupManager.Config> peerGroupServiceConfigByTransport = Map.of(
                TransportType.TOR, defaultConf,
                TransportType.I2P, defaultConf,
                TransportType.CLEAR, clearNetConf
        );

        Map<TransportType, Integer> defaultPortByTransportType = createDefaultPortByTransportType(config);
        Map<TransportType, TransportConfig> configByTransportType = createConfigByTransportType(config, baseDir);

        return new NetworkServiceConfig(baseDir.toAbsolutePath().toString(),
                config.getInt("version"),
                supportedTransportTypes,
                features,
                configByTransportType,
                serviceNodeConfig,
                inventoryServiceConfig,
                authorizationServiceConfig,
                peerGroupServiceConfigByTransport,
                defaultPortByTransportType,
                seedAddressesByTransport,
                Optional.empty());
    }

    private static Map<TransportType, Integer> createDefaultPortByTransportType(Config config) {
        Map<TransportType, Integer> map = new HashMap<>();

        Config configByTransportType = config.getConfig("configByTransportType");
        if (configByTransportType.hasPath("tor")) {
            Config torConfig = configByTransportType.getConfig("tor");
            if (torConfig.hasPath("defaultNodePort")) {
                int port = configByTransportType.getConfig("tor")
                        .getInt("defaultNodePort");
                map.put(TransportType.TOR, port);
            }
        }
        if (configByTransportType.hasPath("i2p")) {
            Config i2pConfig = configByTransportType.getConfig("i2p");
            if (i2pConfig.hasPath("defaultNodePort")) {
                int port = configByTransportType.getConfig("i2p")
                        .getInt("defaultNodePort");
                map.put(TransportType.I2P, port);
            }
        }
        if (configByTransportType.hasPath("clear")) {
            Config clearConfig = configByTransportType.getConfig("clear");
            if (clearConfig.hasPath("defaultNodePort")) {
                int port = configByTransportType.getConfig("clear")
                        .getInt("defaultNodePort");
                map.put(TransportType.CLEAR, port);
            }
        }

        return map;
    }

    private static Map<TransportType, TransportConfig> createConfigByTransportType(Config config, Path baseDir) {
        Map<TransportType, TransportConfig> map = new HashMap<>();
        map.put(TransportType.CLEAR, createTransportConfig(TransportType.CLEAR, config, baseDir));
        map.put(TransportType.TOR, createTransportConfig(TransportType.TOR, config, baseDir));
        map.put(TransportType.I2P, createTransportConfig(TransportType.I2P, config, baseDir));
        return map;
    }

    private static TransportConfig createTransportConfig(TransportType transportType, Config config, Path baseDir) {
        Config transportConfig = config.getConfig("configByTransportType." + transportType.name().toLowerCase());
        Path dataDir;
        return switch (transportType) {
            case TOR -> {
                dataDir = baseDir.resolve("tor");
                yield TorTransportConfig.from(dataDir, transportConfig);
            }
            case I2P -> {
                dataDir = baseDir.resolve("i2p");
                yield I2PTransportService.Config.from(dataDir, transportConfig);
            }
            case CLEAR -> {
                dataDir = baseDir;
                yield ClearNetTransportService.Config.from(dataDir, transportConfig);
            }
        };
    }

    private static Set<Address> getSeedAddresses(TransportType transportType, Config config) {
        return switch (transportType) {
            case TOR -> ConfigUtil.getStringList(config, "tor").stream()
                    .map(Address::fromFullAddress).
                    collect(Collectors.toSet());
            case I2P -> ConfigUtil.getStringList(config, "i2p").stream()
                    .map(Address::fromFullAddress)
                    .collect(Collectors.toSet());
            case CLEAR -> ConfigUtil.getStringList(config, "clear").stream()
                    .map(Address::fromFullAddress)
                    .collect(Collectors.toSet());
        };
    }

    private final String baseDir;
    private final int version;
    private final Set<TransportType> supportedTransportTypes;
    private final Set<Feature> features;
    private final InventoryService.Config inventoryServiceConfig;
    private final AuthorizationService.Config authorizationServiceConfig;
    private final Map<TransportType, TransportConfig> configByTransportType;
    private final ServiceNode.Config serviceNodeConfig;
    private final Map<TransportType, PeerGroupManager.Config> peerGroupServiceConfigByTransport;
    private final Map<TransportType, Integer> defaultPortByTransportType;
    private final Map<TransportType, Set<Address>> seedAddressesByTransport;
    private final Optional<String> socks5ProxyAddress;

    public NetworkServiceConfig(String baseDir,
                                int version,
                                Set<TransportType> supportedTransportTypes,
                                Set<Feature> features,
                                Map<TransportType, TransportConfig> configByTransportType,
                                ServiceNode.Config serviceNodeConfig,
                                InventoryService.Config inventoryServiceConfig,
                                AuthorizationService.Config authorizationServiceConfig,
                                Map<TransportType, PeerGroupManager.Config> peerGroupServiceConfigByTransport,
                                Map<TransportType, Integer> defaultPortByTransportType,
                                Map<TransportType, Set<Address>> seedAddressesByTransport,
                                Optional<String> socks5ProxyAddress) {
        this.baseDir = baseDir;
        this.version = version;
        this.supportedTransportTypes = supportedTransportTypes;
        this.features = features;
        this.inventoryServiceConfig = inventoryServiceConfig;
        this.authorizationServiceConfig = authorizationServiceConfig;
        this.configByTransportType = filterMap(supportedTransportTypes, configByTransportType);
        this.serviceNodeConfig = serviceNodeConfig;
        this.peerGroupServiceConfigByTransport = filterMap(supportedTransportTypes, peerGroupServiceConfigByTransport);
        this.defaultPortByTransportType = filterMap(supportedTransportTypes, defaultPortByTransportType);
        this.seedAddressesByTransport = filterMap(supportedTransportTypes, seedAddressesByTransport);
        this.socks5ProxyAddress = socks5ProxyAddress;
    }

    // In case our config contains not supported transport types we remove them
    private <V> Map<TransportType, V> filterMap(Set<TransportType> supportedTransportTypes,
                                                Map<TransportType, V> map) {
        return map.entrySet().stream()
                .filter(e -> supportedTransportTypes.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}