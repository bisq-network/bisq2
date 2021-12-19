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
import lombok.extern.slf4j.Slf4j;
import network.misq.common.data.Pair;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.common.util.OsUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.peergroup.PeerGroup;
import network.misq.network.p2p.services.peergroup.PeerGroupService;
import network.misq.network.p2p.services.peergroup.SeedNodeRepository;
import network.misq.network.p2p.services.peergroup.exchange.PeerExchangeStrategy;
import network.misq.network.p2p.services.peergroup.keepalive.KeepAliveService;
import network.misq.security.KeyPairRepository;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MultiNodesNetworkMonitor {
    private final Set<Transport.Type> supportedTransportTypes;
    private final boolean bootstrapAll;
    private final Optional<List<Address>> addressesToBootstrap;

    private final ServiceNode.Config serviceNodeConfig;
    private final String baseDirPath;
    private final KeyPairRepository keyPairRepository;

    @Getter
    private final Transport.Type transportType;
    private final List<Address> seedAddresses;
    private final Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport;
    private final SeedNodeRepository seedNodeRepository;
    @Setter
    @Getter
    private int numSeeds = 8;
    @Setter
    @Getter
    private int numNodes = 20;
    private final Map<Address, NetworkService> networkServicesByAddress = new HashMap<>();
    private final Map<Address, String> connectionInfoByAddress = new HashMap<>();

    private Optional<Consumer<Pair<Address, String>>> networkInfoConsumer = Optional.empty();

    public MultiNodesNetworkMonitor(Transport.Type transportType,
                                    boolean bootstrapAll,
                                    Optional<List<Address>> addressesToBootstrap) {
        this.transportType = transportType;
        supportedTransportTypes = Set.of(transportType);
        this.bootstrapAll = bootstrapAll;
        this.addressesToBootstrap = addressesToBootstrap;
        seedAddresses = getSeedAddresses(transportType);
        baseDirPath = OsUtils.getUserDataDir() + File.separator + this.getClass().getSimpleName();

        serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.CONFIDENTIAL,
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.RELAY,
                ServiceNode.Service.MONITOR));

        KeyPairRepository.Conf keyPairRepositoryConf = new KeyPairRepository.Conf(baseDirPath);
        keyPairRepository = new KeyPairRepository(keyPairRepositoryConf);
        Map<Transport.Type, List<Address>> seedsByTransportType = Map.of(
                Transport.Type.TOR, getSeedAddresses(Transport.Type.TOR),
                Transport.Type.I2P, getSeedAddresses(Transport.Type.I2P),
                Transport.Type.CLEAR_NET, getSeedAddresses(Transport.Type.CLEAR_NET)
        );
        seedNodeRepository = new SeedNodeRepository(seedsByTransportType);
        KeepAliveService.Config keepAliveServiceConfig = new KeepAliveService.Config(TimeUnit.SECONDS.toMillis(180), TimeUnit.SECONDS.toMillis(90));
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = new PeerExchangeStrategy.Config(2, 10, 10);
        PeerGroup.Config peerGroupConfig = new PeerGroup.Config(8, 20, 1);
        PeerGroupService.Config defaultConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                TimeUnit.SECONDS.toMillis(60),  //bootstrapTime
                TimeUnit.SECONDS.toMillis(30),  //interval
                TimeUnit.SECONDS.toMillis(10),  //timeout
                TimeUnit.MINUTES.toMillis(60),  //maxAge
                100,                        //maxReported
                100,                        //maxPersisted
                2                              //maxSeeds
        );
        peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,                           //maxSeeds
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR_NET, new PeerGroupService.Config(peerGroupConfig,
                        peerExchangeStrategyConfig,
                        keepAliveServiceConfig,
                        TimeUnit.SECONDS.toMillis(2),   //bootstrapTime
                        TimeUnit.SECONDS.toMillis(2),   //interval
                        TimeUnit.SECONDS.toMillis(5),  //timeout
                        TimeUnit.SECONDS.toMillis(10),  //maxAge
                        100,                        //maxReported
                        100,                        //maxPersisted
                        4                              //maxSeeds
                )
        );
    }

    public List<Address> bootstrapNetworkServices() {
        List<Address> addresses = new ArrayList<>(seedAddresses);
        addresses.addAll(getNodeAddresses());

        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            int port = address.getPort();
            NetworkService networkService = createNetworkService(port);
            setupConnectionListener(networkService);
            networkServicesByAddress.put(address, networkService);
            long minDelayMs = (i + 1) * 10L;
            long maxDelayMs = (i + 1) * 1000L;
            if (bootstrapAll || isInBootstrapList(address)) {
                CompletableFutureUtils.pause(minDelayMs, maxDelayMs)
                        .thenCompose(__ -> networkService.bootstrap(port));
            }
        }
        return addresses;
    }

    private boolean isInBootstrapList(Address address) {
        return addressesToBootstrap.stream().anyMatch(list -> list.contains(address));
    }

    private NetworkService createNetworkService(int port) {
        String dirPath = baseDirPath + File.separator + port;
        Transport.Config transportConfig = new Transport.Config(dirPath);
        NetworkService.Config networkServiceConfig = new NetworkService.Config(dirPath,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                seedNodeRepository,
                Optional.empty());

        return new NetworkService(networkServiceConfig, keyPairRepository);
    }

    public void addNetworkInfoConsumer(Consumer<Pair<Address, String>> handler) {
        networkInfoConsumer = Optional.of(handler);
    }

    private void setupConnectionListener(NetworkService networkService) {
        networkService.findDefaultNode(getTransportType()).ifPresent(node -> {
            node.addListener(new Node.Listener() {

                @Override
                public void onMessage(Message message, Connection connection, String nodeId) {
                }

                @Override
                public void onConnection(Connection connection) {
                    onConnectionStateChanged(connection, node, Optional.empty());
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                    onConnectionStateChanged(connection, node, Optional.of(closeReason));
                }
            });
        });
    }

    private void onConnectionStateChanged(Connection connection, Node node, Optional<CloseReason> closeReason) {
        node.findMyAddress().ifPresent(address -> {
            String now = " at " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            String dir = connection.isOutboundConnection() ? " --> " : " <-- ";
            String peerAddressVerified = connection.isPeerAddressVerified() ? " !]" : " ?]";
            String peerAddress = connection.getPeerAddress().toString().replace("]", peerAddressVerified);
            String tag = closeReason.isPresent() ? "\n- onDisconnect " : "\n+ onConnection ";
            String reason = " " + closeReason.map(r -> r.toString()).orElse("");
            String networkInfo = tag + node + dir + peerAddress + now + reason;

            String prev = connectionInfoByAddress.get(address);
            if (prev == null) {
                prev = "";
            }
            connectionInfoByAddress.put(address, prev + networkInfo);
            networkInfoConsumer.ifPresent(c -> c.accept(new Pair<>(address, networkInfo)));
        });
    }

    private List<Address> getSeedAddresses(Transport.Type type) {
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

    private List<Address> getNodeAddresses() {
        List<Address> addresses = new ArrayList<>();
        for (int i = 0; i < numNodes; i++) {
            addresses.add(Address.localHost(2000 + i));
        }
        return addresses;
    }


    public boolean isSeed(Address address) {
        return seedAddresses.contains(address);
    }

    public Optional<NetworkService> findNetworkService(Address address) {
        return Optional.ofNullable(networkServicesByAddress.get(address));
    }

    public void bootstrap(Address address) {
        int port = address.getPort();
        NetworkService networkService = createNetworkService(port);
        setupConnectionListener(networkService);
        networkServicesByAddress.put(address, networkService);
        networkService.bootstrap(port);
    }

    public void shutdown(Address address) {
        findNetworkService(address)
                .ifPresent(service -> service.shutdown()
                        .whenComplete((__, t) -> networkServicesByAddress.remove(address)));
    }

    public String getNodeInfo(Address address) {
        return findNetworkService(address)
                .flatMap(networkService ->
                        networkService.findServiceNode(transportType)
                                .filter(serviceNode -> serviceNode.getMonitorService().isPresent())
                                .filter(serviceNode -> serviceNode.findMyDefaultAddresses().isPresent()))
                .map(serviceNode -> {
                    String peerGroupInfo = serviceNode.getMonitorService().get().getPeerGroupInfo();
                    String connectionInfo = connectionInfoByAddress.get(serviceNode.findMyDefaultAddresses().get());
                    return peerGroupInfo + connectionInfo;
                })
                .orElse("");
    }
}