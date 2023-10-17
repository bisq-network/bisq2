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

import bisq.common.util.ConfigUtil;
import bisq.network.common.TransportConfig;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.ClearNetTransportService;
import bisq.network.p2p.node.transport.I2PTransportService;
import bisq.network.p2p.node.transport.TransportType;
import bisq.network.p2p.services.peergroup.PeerGroup;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import bisq.network.p2p.services.peergroup.keepalive.KeepAliveService;
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
        Set<TransportType> supportedTransportTypes = new HashSet<>(config.getEnumList(TransportType.class, "supportedTransportTypes"));

        ServiceNode.Config serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.MONITOR));

        Config seedConfig = config.getConfig("seedAddressByTransportType");
        // Only read seed addresses for explicitly supported address types
        Map<TransportType, Set<Address>> seedAddressesByTransport = supportedTransportTypes.stream()
                .collect(toMap(supportedTransportType -> supportedTransportType,
                        supportedTransportType -> getSeedAddresses(supportedTransportType, seedConfig)));

        PeerGroup.Config peerGroupConfig = PeerGroup.Config.from(config.getConfig("peerGroup"));
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = PeerExchangeStrategy.Config.from(config.getConfig("peerExchangeStrategy"));
        KeepAliveService.Config keepAliveServiceConfig = KeepAliveService.Config.from(config.getConfig("keepAlive"));

        PeerGroupService.Config defaultConf = PeerGroupService.Config.from(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                config.getConfig("defaultPeerGroup"));
        PeerGroupService.Config clearNetConf = PeerGroupService.Config.from(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                config.getConfig("clearNetPeerGroup"));

        Map<TransportType, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                TransportType.TOR, defaultConf,
                TransportType.I2P, defaultConf,
                TransportType.CLEAR, clearNetConf
        );

        Map<TransportType, Integer> defaultNodePortByTransportType = createDefaultNodePortByTransportType(config);
       /* Map<Transport.Type, Integer> defaultNodePortByTransportType = new HashMap<>();
        if (config.hasPath("defaultNodePortByTransportType")) {
            Config portConfig = config.getConfig("defaultNodePortByTransportType");
            if (portConfig.hasPath("tor")) {
                defaultNodePortByTransportType.put(Transport.Type.TOR, portConfig.getInt("tor"));
            }
            if (portConfig.hasPath("i2p")) {
                defaultNodePortByTransportType.put(Transport.Type.I2P, portConfig.getInt("i2p"));
            }
            if (portConfig.hasPath("clear")) {
                defaultNodePortByTransportType.put(Transport.Type.CLEAR, portConfig.getInt("clear"));
            }
        }*/


        Map<TransportType, TransportConfig> configByTransportType = createConfigByTransportType(config, baseDir);


        return new NetworkServiceConfig(baseDir.toAbsolutePath().toString(),
                supportedTransportTypes,
                configByTransportType,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                defaultNodePortByTransportType,
                seedAddressesByTransport,
                Optional.empty());
    }

    private static Map<TransportType, Integer> createDefaultNodePortByTransportType(Config config) {
        Map<TransportType, Integer> map = new HashMap<>();
        if (config.hasPath("defaultNodePortByTransportType")) {
            Config portConfig = config.getConfig("defaultNodePortByTransportType");
            if (portConfig.hasPath("tor")) {
                map.put(TransportType.TOR, portConfig.getInt("tor"));
            }
            if (portConfig.hasPath("i2p")) {
                map.put(TransportType.I2P, portConfig.getInt("i2p"));
            }
            if (portConfig.hasPath("clear")) {
                map.put(TransportType.CLEAR, portConfig.getInt("clear"));
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
        switch (transportType) {
            case TOR:
                dataDir = baseDir.resolve("tor");
                return TorTransportConfig.from(dataDir, transportConfig);
            case I2P:
                dataDir = baseDir.resolve("i2p");
                return I2PTransportService.Config.from(dataDir, transportConfig);
            case CLEAR:
                dataDir = baseDir;
                return ClearNetTransportService.Config.from(dataDir, transportConfig);
            default:
                throw new RuntimeException("Unhandled case. type=" + transportType);
        }
    }

    private static Set<Address> getSeedAddresses(TransportType transportType, Config config) {
        switch (transportType) {
            case TOR: {
                return ConfigUtil.getStringList(config, "tor").stream()
                        .map(Address::new).
                        collect(Collectors.toSet());
            }
            case I2P: {
                return ConfigUtil.getStringList(config, "i2p").stream()
                        .map(Address::new)
                        .collect(Collectors.toSet());
            }
            case CLEAR: {
                return ConfigUtil.getStringList(config, "clear").stream()
                        .map(Address::new)
                        .collect(Collectors.toSet());
            }
            default: {
                throw new RuntimeException("Unhandled case. transportType=" + transportType);
            }
        }
    }

    private final String baseDir;
    private final Set<TransportType> supportedTransportTypes;
    private final Map<TransportType, TransportConfig> configByTransportType;
    private final ServiceNode.Config serviceNodeConfig;
    private final Map<TransportType, PeerGroupService.Config> peerGroupServiceConfigByTransport;
    private final Map<TransportType, Integer> defaultNodePortByTransportType;
    private final Map<TransportType, Set<Address>> seedAddressesByTransport;
    private final Optional<String> socks5ProxyAddress;

    public NetworkServiceConfig(String baseDir,
                                Set<TransportType> supportedTransportTypes,
                                Map<TransportType, TransportConfig> configByTransportType,
                                ServiceNode.Config serviceNodeConfig,
                                Map<TransportType, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                Map<TransportType, Integer> defaultNodePortByTransportType,
                                Map<TransportType, Set<Address>> seedAddressesByTransport,
                                Optional<String> socks5ProxyAddress) {
        this.baseDir = baseDir;
        this.supportedTransportTypes = supportedTransportTypes;
        this.configByTransportType = filterMap(supportedTransportTypes, configByTransportType);
        this.serviceNodeConfig = serviceNodeConfig;
        this.peerGroupServiceConfigByTransport = filterMap(supportedTransportTypes, peerGroupServiceConfigByTransport);
        this.defaultNodePortByTransportType = filterMap(supportedTransportTypes, defaultNodePortByTransportType);
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