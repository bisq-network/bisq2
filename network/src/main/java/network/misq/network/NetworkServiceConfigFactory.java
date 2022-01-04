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

package network.misq.network;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.ConfigUtil;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import network.misq.network.p2p.services.peergroup.keepalive.KeepAliveService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses the program arguments which are relevant for that domain and stores it in the options field.
 */
@Slf4j
public class NetworkServiceConfigFactory {

    public static NetworkService.Config getConfig(String baseDir, Config typesafeConfig) {
        //  Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR, Transport.Type.I2P);
        Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR);
        // Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR);

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

        Transport.Config transportConfig = new Transport.Config(baseDir);
        return new NetworkService.Config(baseDir,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
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
                        .map(Address::new)
                        .collect(Collectors.toList());
            }
            default -> {
                List<Address> seedAddresses = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    seedAddresses.add(Address.localHost(8000 + i));
                }
                return seedAddresses;
            }
        }
    }
}