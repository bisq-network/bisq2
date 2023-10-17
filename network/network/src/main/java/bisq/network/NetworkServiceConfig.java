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
import bisq.network.p2p.node.transport.ClearNetTransport;
import bisq.network.p2p.node.transport.I2PTransport;
import bisq.network.p2p.node.transport.Type;
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
        Set<Type> supportedTransportTypes = new HashSet<>(config.getEnumList(Type.class, "supportedTransportTypes"));

        ServiceNode.Config serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.MONITOR));

        Config seedConfig = config.getConfig("seedAddressByTransportType");
        // Only read seed addresses for explicitly supported address types
        Map<Type, Set<Address>> seedAddressesByTransport = supportedTransportTypes.stream()
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

        Map<Type, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                Type.TOR, defaultConf,
                Type.I2P, defaultConf,
                Type.CLEAR, clearNetConf
        );

        Map<Type, Integer> defaultNodePortByTransportType = createDefaultNodePortByTransportType(config);
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


        Map<Type, TransportConfig> configByTransportType = createConfigByTransportType(config, baseDir);


        return new NetworkServiceConfig(baseDir.toAbsolutePath().toString(),
                supportedTransportTypes,
                configByTransportType,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                defaultNodePortByTransportType,
                seedAddressesByTransport,
                Optional.empty());
    }

    private static Map<Type, Integer> createDefaultNodePortByTransportType(Config config) {
        Map<Type, Integer> map = new HashMap<>();
        if (config.hasPath("defaultNodePortByTransportType")) {
            Config portConfig = config.getConfig("defaultNodePortByTransportType");
            if (portConfig.hasPath("tor")) {
                map.put(Type.TOR, portConfig.getInt("tor"));
            }
            if (portConfig.hasPath("i2p")) {
                map.put(Type.I2P, portConfig.getInt("i2p"));
            }
            if (portConfig.hasPath("clear")) {
                map.put(Type.CLEAR, portConfig.getInt("clear"));
            }
        }
        return map;
    }

    private static Map<Type, TransportConfig> createConfigByTransportType(Config config, Path baseDir) {
        Map<Type, TransportConfig> map = new HashMap<>();
        map.put(Type.CLEAR, createTransportConfig(Type.CLEAR, config, baseDir));
        map.put(Type.TOR, createTransportConfig(Type.TOR, config, baseDir));
        map.put(Type.I2P, createTransportConfig(Type.I2P, config, baseDir));
        return map;
    }

    private static TransportConfig createTransportConfig(Type type, Config config, Path baseDir) {
        Config transportConfig = config.getConfig("configByTransportType." + type.name().toLowerCase());
        Path dataDir;
        switch (type) {
            case TOR:
                dataDir = baseDir.resolve("tor");
                return TorTransportConfig.from(dataDir, transportConfig);
            case I2P:
                dataDir = baseDir.resolve("i2p");
                return I2PTransport.Config.from(dataDir, transportConfig);
            case CLEAR:
                dataDir = baseDir;
                return ClearNetTransport.Config.from(dataDir, transportConfig);
            default:
                throw new RuntimeException("Unhandled case. type=" + type);
        }
    }

    private static Set<Address> getSeedAddresses(Type transportType, Config config) {
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
    private final Set<Type> supportedTransportTypes;
    private final Map<Type, TransportConfig> configByTransportType;
    private final ServiceNode.Config serviceNodeConfig;
    private final Map<Type, PeerGroupService.Config> peerGroupServiceConfigByTransport;
    private final Map<Type, Integer> defaultNodePortByTransportType;
    private final Map<Type, Set<Address>> seedAddressesByTransport;
    private final Optional<String> socks5ProxyAddress;

    public NetworkServiceConfig(String baseDir,
                                Set<Type> supportedTransportTypes,
                                Map<Type, TransportConfig> configByTransportType,
                                ServiceNode.Config serviceNodeConfig,
                                Map<Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                Map<Type, Integer> defaultNodePortByTransportType,
                                Map<Type, Set<Address>> seedAddressesByTransport,
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
    private <V> Map<Type, V> filterMap(Set<Type> supportedTransportTypes,
                                       Map<Type, V> map) {
        return map.entrySet().stream()
                .filter(e -> supportedTransportTypes.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}