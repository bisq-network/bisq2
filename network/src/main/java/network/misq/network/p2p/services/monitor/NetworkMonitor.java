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

package network.misq.network.p2p.services.monitor;

import lombok.Getter;
import lombok.Setter;
import network.misq.common.util.OsUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.network.p2p.services.peergroup.SeedNodeRepository;
import network.misq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import network.misq.network.p2p.services.peergroup.keepalive.KeepAliveService;
import network.misq.security.KeyPairRepository;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkMonitor {
    private final Set<Transport.Type> supportedTransportTypes;
    private final ServiceNode.Config serviceNodeConfig;
    private final String baseDirPath;
    private final KeyPairRepository keyPairRepository;

    @Getter
    private final Transport.Type transportType = Transport.Type.CLEAR_NET;
    @Setter
    @Getter
    private int numSeeds = 8;
    @Setter
    @Getter
    private int numNodes = 20;
    private final PeerGroup.Config peerGroupConfig = new PeerGroup.Config(8, 16, 1);
    private final PeerExchangeStrategy.Config peerExchangeStrategyConfig = new PeerExchangeStrategy.Config(2, 10, 10);

    public NetworkMonitor() {
        baseDirPath = OsUtils.getUserDataDir() + File.separator + "NetworkMonitor";

        //Set<Transport.Type> supportedTransportTypes = Set.of(Transport.Type.CLEAR_NET, Transport.Type.TOR, Transport.Type.I2P);
        supportedTransportTypes = Set.of(transportType);

        serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.RELAY,
                ServiceNode.Service.MONITOR));

        KeyPairRepository.Conf keyPairRepositoryConf = new KeyPairRepository.Conf(baseDirPath);
        keyPairRepository = new KeyPairRepository(keyPairRepositoryConf);
    }

    public List<Address> getSeedAddresses(Transport.Type type) {
        switch (type) {
            case TOR -> {
                return Stream.of(
                                new Address("76ewqvsvh5nnuqnlro65nrxu3d4377aw5kv25p2uq7cpvoi4xslq7vyd.onion", 1000),
                                new Address("ucq3qw4qlzstpwtqig6lxll64tarmqi77u6t5iquvi52j66pqrsqcpad.onion", 1001),
                                new Address("fanvmqmbxyklaro3uanyotybrfcflc2ywlr5qd3ttvf2cva2huxrwuyd.onion", 1002),
                                new Address("4i45sndzsxnw24idfr57lzzbcxhfdjp3yikvxwb54p4bjay5j4lqjuqd.onion", 1003),
                                new Address("5wzt2sx53vzozoifkvntrl7rycegmker57aofb6qxgbunbw2p435fnyd.onion", 1004),
                                new Address("5ekgw2qowowt7ncgo2jnnwyehqfvj7gqj75cogt3apuyef32myj7byyd.onion", 1005),
                                new Address("cqvqm4ewvh2zdlrcz4whn6b5ltgg5hbef4ymp2pzk2hlrz4hjkqpmgyd.onion", 1006),
                                new Address("kctohowqiwkgeceag622c56kv5aqbeozm5ifjjldqo2x5hnzb5qkhfqd.onion", 1007),
                                new Address("hqvn6w7phvz6jifm2ak66flrdeieq24f653njkf4e7tsrqbramm3hpyd.onion", 1008),
                                new Address("onuyzrnp4zgnokmptj4krgwfydqh5snzoekhoo4hr3eo4qg7jhhziaad.onion", 1009),
                                new Address("zj6eiccp4irphbs4ihbgwcy3jo6unxap5nhac4r25mswrscuqqs5beqd.onion", 1010),
                                new Address("plj7e5vs67psw2jlryjrbuh4336zwrigcsiiwf4jeennkla5x2ly5qyd.onion", 1011)
                        )
                        .limit(numSeeds)
                        .collect(Collectors.toList());
            }
            case I2P -> {
                //todo
                return new ArrayList<>();
            }
            default -> {
                List<Address> seedAddresses = new ArrayList<>();
                for (int i = 0; i < numSeeds; i++) {
                    seedAddresses.add(Address.localHost(1000 + i));
                }
                return seedAddresses;
            }
        }
    }

    public List<Address> getNodeAddresses() {
        List<Address> addresses = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            addresses.add(Address.localHost(2000 + i));
        }
        return addresses;
    }

    public NetworkService createNetworkService(int port) {
        Map<Transport.Type, List<Address>> seedsByTransportType = Map.of(
                Transport.Type.TOR, getSeedAddresses(Transport.Type.TOR),
                Transport.Type.I2P, getSeedAddresses(Transport.Type.I2P),
                Transport.Type.CLEAR_NET, getSeedAddresses(Transport.Type.CLEAR_NET)
        );

        SeedNodeRepository seedNodeRepository = new SeedNodeRepository(seedsByTransportType);
        String dirPath = baseDirPath + File.separator + port;
        Transport.Config transportConfig = new Transport.Config(dirPath);

        KeepAliveService.Config keepAliveServiceConfig = new KeepAliveService.Config(TimeUnit.SECONDS.toMillis(180), TimeUnit.SECONDS.toMillis(90));
        PeerGroupService.Config defaultConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                TimeUnit.SECONDS.toMillis(60),   //bootstrapTime
                TimeUnit.SECONDS.toMillis(30),  //interval
                TimeUnit.SECONDS.toMillis(10),  //timeout
                100,                        //maxReported
                100,                        //maxPersisted
                2);                           //maxSeeds

        Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,                           //maxSeeds
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR_NET, new PeerGroupService.Config(peerGroupConfig,
                        peerExchangeStrategyConfig,
                        keepAliveServiceConfig,
                        TimeUnit.SECONDS.toMillis(5),   //bootstrapTime
                        TimeUnit.SECONDS.toMillis(5),  //interval
                        TimeUnit.SECONDS.toMillis(10),  //timeout
                        100,                        //maxReported
                        100,                        //maxPersisted
                        4)                           //maxSeeds
        );
        NetworkService.Config networkServiceConfig = new NetworkService.Config(dirPath,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                seedNodeRepository,
                Optional.empty());

        return new NetworkService(networkServiceConfig, keyPairRepository);
    }
}