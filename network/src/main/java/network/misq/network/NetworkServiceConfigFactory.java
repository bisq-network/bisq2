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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Parses the program arguments which are relevant for that domain and stores it in the options field.
 */
@Slf4j
public class NetworkServiceConfigFactory {

    // TODO Resolve ambiguity between 'Config' types and variable names:
    //      Config typesafeNetworkServiceConfig
    //                  vs
    //      NetworkService.Config networkServiceConfig
    private final Config typesafeNetworkServiceConfig;
    private final NetworkService.Config networkServiceConfig;

    public NetworkService.Config get() {
        return networkServiceConfig;
    }

    public NetworkServiceConfigFactory(String baseDir) {
        this(baseDir, Optional.empty(), Optional.empty());
    }

    public NetworkServiceConfigFactory(String baseDir, Config typesafeNetworkServiceConfig, String[] args) {
        this(baseDir, Optional.of(typesafeNetworkServiceConfig), Optional.of(args));
    }

    public NetworkServiceConfigFactory(String baseDir,
                                       @SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Config> typesafeNetworkServiceConfig,
                                       @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"}) Optional<String[]> args) {
        // TODO Is throwing an exception when typesafeNetworkServiceConfig option.isEmpty too severe?
        //  Could ALL network config values be passed as -DjvmOptions  (I assume this is not likely.)
        this.typesafeNetworkServiceConfig = typesafeNetworkServiceConfig.orElseThrow(() ->
                new IllegalStateException("typesafe 'misq.networkServiceConfig' not found"));

        //todo apply networkConfig
        //todo use option parser lib for args (define priority)

        // Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR, Transport.Type.I2P);
       // Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR, Transport.Type.TOR);
         Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR);

        ServiceNode.Config serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.RELAY,
                ServiceNode.Service.MONITOR));

        Map<Transport.Type, List<Address>> seedAddressesByTransport = Map.of(
                Transport.Type.TOR, getSeedAddresses(Transport.Type.TOR),
                Transport.Type.I2P, getSeedAddresses(Transport.Type.I2P),
                Transport.Type.CLEAR, getSeedAddresses(Transport.Type.CLEAR)
        );

        Transport.Config transportConfig = new Transport.Config(baseDir);

        Config typesafePeerGroupConfig = this.typesafeNetworkServiceConfig.getConfig("peerGroupConfig");
        PeerGroup.Config peerGroupConfig = new PeerGroup.Config(
                typesafePeerGroupConfig.getInt("minNumConnectedPeers"),
                typesafePeerGroupConfig.getInt("maxNumConnectedPeers"),
                typesafePeerGroupConfig.getInt("minNumReportedPeers"));

        Config typesafePeerExchangeStrategyConfig = this.typesafeNetworkServiceConfig.getConfig("peerExchangeStrategyConfig");
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = new PeerExchangeStrategy.Config(
                typesafePeerExchangeStrategyConfig.getInt("numSeedNodesAtBoostrap"),
                typesafePeerExchangeStrategyConfig.getInt("numPersistedPeersAtBoostrap"),
                typesafePeerExchangeStrategyConfig.getInt("numReportedPeersAtBoostrap"));

        Config typesafeKeepAliveServiceConfig = this.typesafeNetworkServiceConfig.getConfig("keepAliveServiceConfig");
        KeepAliveService.Config keepAliveServiceConfig = new KeepAliveService.Config(
                SECONDS.toMillis(typesafeKeepAliveServiceConfig.getLong("maxIdleTimeInSeconds")),
                SECONDS.toMillis(typesafeKeepAliveServiceConfig.getLong("intervalInSeconds")));

        Config typesafeDefaultPeerGroupServiceConfig = this.typesafeNetworkServiceConfig.getConfig("defaultPeerGroupServiceConfig");
        PeerGroupService.Config defaultConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                SECONDS.toMillis(typesafeDefaultPeerGroupServiceConfig.getLong("bootstrapTimeInSeconds")),
                SECONDS.toMillis(typesafeDefaultPeerGroupServiceConfig.getLong("intervalInSeconds")),
                SECONDS.toMillis(typesafeDefaultPeerGroupServiceConfig.getLong("timeoutInSeconds")),
                HOURS.toMillis(typesafeDefaultPeerGroupServiceConfig.getLong("maxAgeInHours")),
                typesafeDefaultPeerGroupServiceConfig.getInt("maxPersisted"),
                typesafeDefaultPeerGroupServiceConfig.getInt("maxReported"),
                typesafeDefaultPeerGroupServiceConfig.getInt("maxSeeds")
        );

        Config typesafeClearNetPeerGroupServiceConfig = this.typesafeNetworkServiceConfig.getConfig("clearNetPeerGroupServiceConfig");
        PeerGroupService.Config clearNetConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                SECONDS.toMillis(typesafeClearNetPeerGroupServiceConfig.getLong("bootstrapTimeInSeconds")),
                SECONDS.toMillis(typesafeClearNetPeerGroupServiceConfig.getLong("intervalInSeconds")),
                SECONDS.toMillis(typesafeClearNetPeerGroupServiceConfig.getLong("timeoutInSeconds")),
                HOURS.toMillis(typesafeClearNetPeerGroupServiceConfig.getLong("maxAgeInHours")),
                typesafeClearNetPeerGroupServiceConfig.getInt("maxPersisted"),
                typesafeClearNetPeerGroupServiceConfig.getInt("maxReported"),
                typesafeClearNetPeerGroupServiceConfig.getInt("maxSeeds")
        );

        Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR, clearNetConf
        );

        networkServiceConfig = new NetworkService.Config(baseDir,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                seedAddressesByTransport,
                Optional.empty());
    }

    public List<Address> getSeedAddresses(Transport.Type transportType) {
        Supplier<Config> seedAddressByTransportTypeConfig = () ->
                typesafeNetworkServiceConfig.getConfig("seedAddressByTransportType");
        switch (transportType) {
            case TOR -> {
                return ConfigUtil.getStringList(seedAddressByTransportTypeConfig.get(), "tor").stream()
                        .map(Address::new)
                        .collect(Collectors.toList());
            }
            case I2P -> {
                return ConfigUtil.getStringList(seedAddressByTransportTypeConfig.get(), "i2p").stream()
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