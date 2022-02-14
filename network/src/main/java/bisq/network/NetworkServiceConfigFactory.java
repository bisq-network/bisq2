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
import bisq.i2p.I2pUtils;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.peergroup.PeerGroup;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import bisq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import bisq.network.p2p.services.peergroup.keepalive.KeepAliveService;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses the program arguments which are relevant for that domain and stores it in the options field.
 */
@Slf4j
public class NetworkServiceConfigFactory {
    public static NetworkService.Config getConfig(String baseDir, Config typesafeConfig) {
        Set<Transport.Type> supportedTransportTypes = new HashSet<>(typesafeConfig.getEnumList(Transport.Type.class,
                "supportedTransportTypes"));

     /*   supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR, Transport.Type.I2P);
        supportedTransportTypes = Set.of(Transport.Type.I2P);
        supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR);
        supportedTransportTypes = Set.of(Transport.Type.CLEAR);
        supportedTransportTypes = Set.of(Transport.Type.TOR);*/

        ServiceNode.Config serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.RELAY,
                ServiceNode.Service.MONITOR));

        Config seedConfig = typesafeConfig.getConfig("seedAddressByTransportType");
        Map<Transport.Type, List<Address>> seedAddressesByTransport = Map.of(
                Transport.Type.TOR, getSeedAddresses(Transport.Type.TOR, seedConfig),
                Transport.Type.I2P, getSeedAddresses(Transport.Type.I2P, seedConfig),
                Transport.Type.CLEAR, getSeedAddresses(Transport.Type.CLEAR, seedConfig)
        );

        PeerGroup.Config peerGroupConfig = PeerGroup.Config.from(typesafeConfig.getConfig("peerGroupConfig"));
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = PeerExchangeStrategy.Config.from(typesafeConfig.getConfig("peerExchangeStrategyConfig"));
        KeepAliveService.Config keepAliveServiceConfig = KeepAliveService.Config.from(typesafeConfig.getConfig("keepAliveServiceConfig"));

        PeerGroupService.Config defaultConf = PeerGroupService.Config.from(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                typesafeConfig.getConfig("defaultPeerGroupServiceConfig"));
        PeerGroupService.Config clearNetConf = PeerGroupService.Config.from(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                typesafeConfig.getConfig("clearNetPeerGroupServiceConfig"));

        Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR, clearNetConf
        );

        Map<Transport.Type, Integer> defaultNodePortByTransportType = new HashMap<>();
        if (typesafeConfig.hasPath("defaultNodePortByTransportType")) {
            Config portConfig = typesafeConfig.getConfig("defaultNodePortByTransportType");
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
        return new NetworkService.Config(baseDir,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                defaultNodePortByTransportType,
                seedAddressesByTransport,
                Optional.empty());
    }

    public static List<Address> getSeedAddresses(Transport.Type transportType, Config config) {
        switch (transportType) {
            case TOR -> {
                return ConfigUtil.getStringList(config, "tor").stream()
                        .map(Address::new)
                        .collect(Collectors.toList());
            }
            case I2P -> {
                return ConfigUtil.getStringList(config, "i2p").stream()
                        .map(fullAddress -> {
                            // Default constructor tokenizes host and port
                            Address i2pAddress = new Address(fullAddress);

                            // Clients can be initialized with b32.i2p address types
                            // To simplify connection mgmt and avoid future lookups, we immediately convert to base64
                            String destinationInUnknownFormat = i2pAddress.getHost();
                            String destinationBase64 = I2pUtils.maybeLookupAndConvertToBase64(destinationInUnknownFormat)
                                    .orElseThrow(RuntimeException::new);
                            // TODO When port is removed from I2P, remove this workaround
                            return new Address(destinationBase64, i2pAddress.getPort());
                        })
                        .collect(Collectors.toList());
            }
            default -> {
                List<Address> seedAddresses = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    seedAddresses.add(Address.localHost(8000 + i));
                }
                return seedAddresses;
            }
        }
    }
}