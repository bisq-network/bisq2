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

package network.misq.api.options;

import lombok.Getter;
import network.misq.application.options.ApplicationOptions;
import network.misq.network.NetworkService;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.network.p2p.services.peergroup.SeedNodeRepository;
import network.misq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import network.misq.network.p2p.services.peergroup.keepalive.KeepAliveService;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Parses the program arguments which are relevant for that domain and stores it in the options field.
 */
public class NetworkServiceOptionsParser {
    @Getter
    private final NetworkService.Config config;

    public NetworkServiceOptionsParser(ApplicationOptions applicationOptions, String[] args) {
        String baseDirPath = applicationOptions.appDir();

        Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR, Transport.Type.I2P);

        ServiceNode.Config serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.RELAY,
                ServiceNode.Service.MONITOR));

        PeerGroup.Config peerGroupConfig = new PeerGroup.Config();
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = new PeerExchangeStrategy.Config();

        Map<Transport.Type, List<Address>> seedsByTransportType = Map.of(Transport.Type.TOR, Arrays.asList(Address.localHost(1000), Address.localHost(1001)),
                Transport.Type.I2P, Arrays.asList(Address.localHost(1000), Address.localHost(1001)),
                Transport.Type.CLEAR, Arrays.asList(Address.localHost(1000), Address.localHost(1001)));

        SeedNodeRepository seedNodeRepository = new SeedNodeRepository(seedsByTransportType);
        Transport.Config transportConfig = new Transport.Config(baseDirPath);

        KeepAliveService.Config keepAliveServiceConfig = new KeepAliveService.Config(TimeUnit.SECONDS.toMillis(180), TimeUnit.SECONDS.toMillis(90));
        PeerGroupService.Config defaultConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                TimeUnit.SECONDS.toMillis(30),  // bootstrapTime
                TimeUnit.SECONDS.toMillis(30),  // interval
                TimeUnit.SECONDS.toMillis(60),  // timeout
                TimeUnit.HOURS.toMillis(2),     // maxAge
                100,                        // maxReported
                100,                        // maxPersisted
                4                             // maxSeeds
        );
        PeerGroupService.Config clearNetConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                TimeUnit.SECONDS.toMillis(1),  // bootstrapTime
                TimeUnit.SECONDS.toMillis(1),  // interval
                TimeUnit.SECONDS.toMillis(60),  // timeout
                TimeUnit.HOURS.toMillis(2),     // maxAge
                100,                        // maxReported
                100,                        // maxPersisted
                4                             // maxSeeds
        );
        Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR, clearNetConf
        );
        
        config = new NetworkService.Config(baseDirPath,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                seedNodeRepository,
                Optional.empty());
    }
}