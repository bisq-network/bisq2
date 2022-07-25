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
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.ClearNetTransport;
import bisq.network.p2p.node.transport.I2PTransport;
import bisq.network.p2p.node.transport.TorTransport;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.peergroup.PeerGroup;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import bisq.network.p2p.services.peergroup.keepalive.KeepAliveService;
import com.typesafe.config.Config;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Getter
public final class NetworkServiceConfig {
    public static NetworkServiceConfig from(String baseDir, Config config) {
        Set<Transport.Type> supportedTransportTypes = new HashSet<>(config.getEnumList(Transport.Type.class, "supportedTransportTypes"));

        ServiceNode.Config serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.MONITOR));

        Config seedConfig = config.getConfig("seedAddressByTransportType");
        // Only read seed addresses for explicitly supported address types
        Map<Transport.Type, List<Address>> seedAddressesByTransport = supportedTransportTypes.stream()
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

        Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR, clearNetConf
        );

        Map<Transport.Type, Integer> defaultNodePortByTransportType = createDefaultNodePortByTransportType(config);
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


        Map<Transport.Type, Transport.Config> configByTransportType = createConfigByTransportType(config, baseDir);


        return new NetworkServiceConfig(baseDir,
                supportedTransportTypes,
                configByTransportType,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                defaultNodePortByTransportType,
                seedAddressesByTransport,
                Optional.empty());
    }

    private static Map<Transport.Type, Integer> createDefaultNodePortByTransportType(Config config) {
        Map<Transport.Type, Integer> map = new HashMap<>();
        if (config.hasPath("defaultNodePortByTransportType")) {
            Config portConfig = config.getConfig("defaultNodePortByTransportType");
            if (portConfig.hasPath("tor")) {
                map.put(Transport.Type.TOR, portConfig.getInt("tor"));
            }
            if (portConfig.hasPath("i2p")) {
                map.put(Transport.Type.I2P, portConfig.getInt("i2p"));
            }
            if (portConfig.hasPath("clear")) {
                map.put(Transport.Type.CLEAR, portConfig.getInt("clear"));
            }
        }
        return map;
    }

    private static Map<Transport.Type, Transport.Config> createConfigByTransportType(Config config, String baseDir) {
        Map<Transport.Type, Transport.Config> map = new HashMap<>();
        map.put(Transport.Type.CLEAR, createTransportConfig(Transport.Type.CLEAR, config, baseDir));
        map.put(Transport.Type.TOR, createTransportConfig(Transport.Type.TOR, config, baseDir));
        map.put(Transport.Type.I2P, createTransportConfig(Transport.Type.I2P, config, baseDir));
        return map;
    }

    private static Transport.Config createTransportConfig(Transport.Type type, Config config, String baseDir) {
        Config transportConfig = config.getConfig("configByTransportType." + type.name().toLowerCase());
        switch (type) {
            case TOR:
                return TorTransport.Config.from(baseDir, transportConfig);
            case I2P:
                return I2PTransport.Config.from(baseDir, transportConfig);
            case CLEAR:
                return ClearNetTransport.Config.from(baseDir, transportConfig);
            default:
                throw new RuntimeException("Unhandled case. type=" + type);
        }
    }

    private static List<Address> getSeedAddresses(Transport.Type transportType, Config config) {
        switch (transportType) {
            case TOR: {
                return ConfigUtil.getStringList(config, "tor").stream()
                        .map(Address::new).
                        collect(Collectors.toList());
            }
            case I2P: {
                return ConfigUtil.getStringList(config, "i2p").stream()
                        .map(Address::new)
                        .collect(Collectors.toList());
            }
            case CLEAR: {
                return ConfigUtil.getStringList(config, "clear").stream()
                        .map(Address::new)
                        .collect(Collectors.toList());
            }
            default: {
                throw new RuntimeException("Unhandled case. transportType=" + transportType);
            }
        }
    }

    private final String baseDir;
    private final Set<Transport.Type> supportedTransportTypes;
    private final Map<Transport.Type, Transport.Config> configByTransportType;
    private final ServiceNode.Config serviceNodeConfig;
    private final Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport;
    private final Map<Transport.Type, Integer> defaultNodePortByTransportType;
    private final Map<Transport.Type, List<Address>> seedAddressesByTransport;
    private final Optional<String> socks5ProxyAddress;

    public NetworkServiceConfig(String baseDir,
                                Set<Transport.Type> supportedTransportTypes,
                                Map<Transport.Type, Transport.Config> configByTransportType,
                                ServiceNode.Config serviceNodeConfig,
                                Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                Map<Transport.Type, Integer> defaultNodePortByTransportType,
                                Map<Transport.Type, List<Address>> seedAddressesByTransport,
                                Optional<String> socks5ProxyAddress) {
        this.baseDir = baseDir;
        this.supportedTransportTypes = supportedTransportTypes;
        this.configByTransportType = configByTransportType;
        this.serviceNodeConfig = serviceNodeConfig;
        this.peerGroupServiceConfigByTransport = peerGroupServiceConfigByTransport;
        this.defaultNodePortByTransportType = defaultNodePortByTransportType;
        this.seedAddressesByTransport = seedAddressesByTransport;
        this.socks5ProxyAddress = socks5ProxyAddress;
    }
}