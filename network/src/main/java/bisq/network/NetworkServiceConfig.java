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
        Map<Transport.Type, Set<Address>> seedAddressesByTransport = supportedTransportTypes.stream()
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

        Map<Transport.Type, Integer> defaultNodePortByTransportType = new HashMap<>();
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
        }
        Transport.Config transportConfig = new Transport.Config(baseDir);
        return new NetworkServiceConfig(baseDir,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                defaultNodePortByTransportType,
                seedAddressesByTransport,
                Optional.empty());
    }

    private static Set<Address> getSeedAddresses(Transport.Type transportType, Config config) {
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
    private final Transport.Config transportConfig;
    private final Set<Transport.Type> supportedTransportTypes;
    private final ServiceNode.Config serviceNodeConfig;
    private final Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport;
    private final Map<Transport.Type, Integer> defaultNodePortByTransportType;
    private final Map<Transport.Type, Set<Address>> seedAddressesByTransport;
    private final Optional<String> socks5ProxyAddress;

    public NetworkServiceConfig(String baseDir,
                                Transport.Config transportConfig,
                                Set<Transport.Type> supportedTransportTypes,
                                ServiceNode.Config serviceNodeConfig,
                                Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport,
                                Map<Transport.Type, Integer> defaultNodePortByTransportType,
                                Map<Transport.Type, Set<Address>> seedAddressesByTransport,
                                Optional<String> socks5ProxyAddress) {
        this.baseDir = baseDir;
        this.transportConfig = transportConfig;
        this.supportedTransportTypes = supportedTransportTypes;
        this.serviceNodeConfig = serviceNodeConfig;
        this.peerGroupServiceConfigByTransport = peerGroupServiceConfigByTransport;
        this.defaultNodePortByTransportType = defaultNodePortByTransportType;
        this.seedAddressesByTransport = seedAddressesByTransport;
        this.socks5ProxyAddress = socks5ProxyAddress;
    }
}