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

import lombok.extern.slf4j.Slf4j;
import network.misq.common.timer.Scheduler;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.common.util.MathUtils;
import network.misq.common.util.OsUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.State;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MultiNodesNetworkMonitor {
    public interface Handler {
        void onConnectionStateChange(Transport.Type transportType, Address address, String networkInfo);

        void onStateChange(Address address, State networkServiceState);
    }

    private final Set<Transport.Type> supportedTransportTypes;
    private final boolean bootstrapAll;
    private final Optional<List<Address>> addressesToBootstrap;
    private final ServiceNode.Config serviceNodeConfig;
    private final String baseDirPath;
    private final KeyPairRepository keyPairRepository;
    private final Map<Transport.Type, PeerGroupService.Config> peerGroupServiceConfigByTransport;
    private final SeedNodeRepository seedNodeRepository;
    private final int numSeeds = 8;
    private final int numNodes = 20;
    private final Map<Address, NetworkService> networkServicesByAddress = new HashMap<>();
    private final Map<Address, String> connectionHistoryByAddress = new HashMap<>();

    private Optional<Handler> handler = Optional.empty();

    public MultiNodesNetworkMonitor(Set<Transport.Type> supportedTransportTypes,
                                    boolean bootstrapAll,
                                    Optional<List<Address>> addressesToBootstrap) {
        this.supportedTransportTypes = supportedTransportTypes;
        this.bootstrapAll = bootstrapAll;
        this.addressesToBootstrap = addressesToBootstrap;

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
        PeerGroup.Config peerGroupConfig = new PeerGroup.Config(8, 16, 1);
        PeerGroupService.Config defaultConf = new PeerGroupService.Config(peerGroupConfig,
                peerExchangeStrategyConfig,
                keepAliveServiceConfig,
                TimeUnit.SECONDS.toMillis(30),  //bootstrapTime
                TimeUnit.SECONDS.toMillis(30),  //interval
                TimeUnit.SECONDS.toMillis(60),  //timeout
                TimeUnit.MINUTES.toMillis(60),  //maxAge
                100,                        //maxReported
                100,                        //maxPersisted
                8                              //maxSeeds
        );
        peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,                           //maxSeeds
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR_NET, new PeerGroupService.Config(peerGroupConfig,
                        peerExchangeStrategyConfig,
                        keepAliveServiceConfig,
                        TimeUnit.SECONDS.toMillis(10),   //bootstrapTime
                        TimeUnit.SECONDS.toMillis(10),   //interval
                        TimeUnit.SECONDS.toMillis(50),  //timeout
                        TimeUnit.MINUTES.toMillis(10),  //maxAge
                        100,                        //maxReported
                        100,                        //maxPersisted
                        4                              //maxSeeds
                )
        );
    }

    public Map<Transport.Type, List<Address>> bootstrap() {
        return supportedTransportTypes.stream()
                .collect(Collectors.toMap(transportType -> transportType, this::bootstrap));
    }

    private List<Address> bootstrap(Transport.Type transportType) {
        List<Address> addresses = new ArrayList<>(getSeedAddresses(transportType));
        addresses.addAll(getNodeAddresses(transportType));

        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            if (bootstrapAll || isInBootstrapList(address)) {
                int delayMs = (i + 1) * 1000;
                long randDelay = new Random().nextInt(delayMs);
                Scheduler.run(() -> bootstrap(address, transportType)).after(randDelay);
            }
        }
        return addresses;
    }

    public void bootstrap(Address address, Transport.Type transportType) {
        NetworkService networkService = createNetworkService(address, transportType);
        networkService.findServiceNode(transportType).ifPresent(serviceNode ->
                serviceNode.bootstrap(Node.DEFAULT_NODE_ID, address.getPort())
                        .whenComplete((r, t) -> handler.ifPresent(handler -> handler.onStateChange(address, serviceNode.getState()))));
    }

    public CompletableFuture<List<Void>> shutdown() {
        return CompletableFutureUtils.allOf(networkServicesByAddress.keySet().stream().map(this::shutdown));
    }

    public CompletableFuture<Void> shutdown(Address address) {
        handler.ifPresent(handler -> handler.onStateChange(address, State.SHUTDOWN_STARTED));
        return findNetworkService(address)
                .map(networkService -> networkService.shutdown()
                        .whenComplete((__, t) -> {
                            networkServicesByAddress.remove(address);
                            handler.ifPresent(handler -> handler.onStateChange(address, networkService.state));
                        }))
                .orElse(CompletableFuture.completedFuture(null));
    }

    private NetworkService createNetworkService(Address address, Transport.Type transportType) {
        String dirPath = baseDirPath + File.separator + address.getPort();
        Transport.Config transportConfig = new Transport.Config(dirPath);
        NetworkService.Config networkServiceConfig = new NetworkService.Config(dirPath,
                transportConfig,
                supportedTransportTypes,
                serviceNodeConfig,
                peerGroupServiceConfigByTransport,
                seedNodeRepository,
                Optional.empty());

        NetworkService networkService = new NetworkService(networkServiceConfig, keyPairRepository);
        handler.ifPresent(handler -> handler.onStateChange(address, networkService.state));
        networkServicesByAddress.put(address, networkService);
        setupConnectionListener(networkService, transportType);
        return networkService;
    }

    private void setupConnectionListener(NetworkService networkService, Transport.Type transportType) {
        networkService.findDefaultNode(transportType).ifPresent(node -> {
            node.addListener(new Node.Listener() {

                @Override
                public void onMessage(Message message, Connection connection, String nodeId) {
                }

                @Override
                public void onConnection(Connection connection) {
                    onConnectionStateChanged(transportType, connection, node, Optional.empty());
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                    onConnectionStateChanged(transportType, connection, node, Optional.of(closeReason));
                }
            });
        });
    }

    private void onConnectionStateChanged(Transport.Type transportType, Connection connection, Node node, Optional<CloseReason> closeReason) {
        node.findMyAddress().ifPresent(address -> {
            String now = " at " + new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            String dir = connection.isOutboundConnection() ? " --> " : " <-- ";
            String peerAddressVerified = connection.isPeerAddressVerified() ? " !]" : " ?]";
            String peerAddress = connection.getPeerAddress().toString().replace("]", peerAddressVerified);
            String tag = closeReason.isPresent() ? "\n- onDisconnect " : "\n+ onConnection ";
            String reason = closeReason.map(r -> ", " + r).orElse("");
            String info = tag + node + dir + peerAddress + now + reason;

            String prev = connectionHistoryByAddress.get(address);
            if (prev == null) {
                prev = "";
            }
            connectionHistoryByAddress.put(address, prev + info);
            handler.ifPresent(handler -> handler.onConnectionStateChange(transportType, address, info));
        });
    }

    public String getNodeInfo(Address address, Transport.Type transportType) {
        return findNetworkService(address)
                .flatMap(networkService -> networkService.findServiceNode(transportType))
                .filter(serviceNode -> serviceNode.getMonitorService().isPresent())
                .map(serviceNode -> {
                    String peerGroupInfo = serviceNode.getMonitorService().get().getPeerGroupInfo();
                    String connectionHistory = Optional.ofNullable(connectionHistoryByAddress.get(address))
                            .map(history -> {
                                long nunOnConnection = Stream.of(history.split("\\n"))
                                        .filter(e -> e.startsWith("+ onConnection"))
                                        .count();
                                long numOnDisconnect = Stream.of(history.split("\\n"))
                                        .filter(e -> e.startsWith("- onDisconnect"))
                                        .count();
                                long open = nunOnConnection - numOnDisconnect;
                                double churnRate = nunOnConnection != 0 ?
                                        MathUtils.roundDouble(numOnDisconnect / (double) nunOnConnection * 100, 2) :
                                        0;
                                return "\nChurn rate=" + churnRate +
                                        "%; Open connections=" + open +
                                        "; Num OnConnection=" + nunOnConnection +
                                        "; Num OnDisconnect=" + numOnDisconnect +
                                        "\n\nConnection History:\n" + history;
                            }).orElse("");
                    return peerGroupInfo + connectionHistory;
                })
                .orElse("");
    }

    public boolean isSeed(Address address, Transport.Type transportType) {
        return getSeedAddresses(transportType).contains(address);
    }

    public Optional<NetworkService> findNetworkService(Address address) {
        return Optional.ofNullable(networkServicesByAddress.get(address));
    }

    private boolean isInBootstrapList(Address address) {
        return addressesToBootstrap.stream().anyMatch(list -> list.contains(address));
    }

    public void addNetworkInfoConsumer(Handler handler) {
        this.handler = Optional.of(handler);
    }

    private List<Address> getSeedAddresses(Transport.Type transportType) {
        switch (transportType) {
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
                    seedAddresses.add(Address.localHost(8000 + i));
                }
                return seedAddresses;
            }
        }
    }

    private List<Address> getNodeAddresses(Transport.Type transportType) {
        switch (transportType) {
            case TOR -> {
                return Stream.of(
                                new Address("l2takiyfs4d7nou7wwjomx3a4jxpn4fabtxfclgobrucnokms6j6liid.onion", 2000),
                                new Address("nsnos6boshp2iznnqkrgoqutr2sxgysyufx72ikd2e2ik2g4pdhmjsyd.onion", 2001),
                                new Address("tbzmnqea2lris25z5ljtkzqi5euut2k33rpsnjv3izzgfpxfh3xwbfyd.onion", 2002),
                                new Address("q7tixbmhuvivqttcuhl7a42lyaoelu4h27b4l7tdbwaw7qifbxxa2yqd.onion", 2003),
                                new Address("epgfefrpwqdaat2kz2v6g5sdup3me32wotfhzv275zluhbrsssg3e5yd.onion", 2004),
                                new Address("y2fu3dmmpdetteqwizubc7ivrgaheis3w2ynqg3yvpe36pg7z7zu6mqd.onion", 2005),
                                new Address("6ydkwmxndkedl2tw3qfvxqv6nlgscj5uviaapgmh2h6hu4xggr2hqdqd.onion", 2006),
                                new Address("pno7acyqteamsdj4mfjropdqbvgppia6k4hsxisdzcwywftq3h453oyd.onion", 2007),
                                new Address("mi3x6vebpanoqjhrhyyve7zt6bqrh4erz4wmcqfyn6vqi4xgz4a5tfqd.onion", 2008),
                                new Address("nympb7ltiv4el43cr3gi6klp7xvh4jyvbj23zbhpdoxntikqd2hjmtyd.onion", 2009),
                                new Address("e7pt5zkibm2rkeof4o4erxdyir4mweprezobwzclmpl6xf54nx6byvqd.onion", 2010),
                                new Address("sgyb2vlk4xafsupbycatsmtusgbi2gsh63ralvkizdpgas5rx2mq2lad.onion", 2011),
                                new Address("3cllqgogu7tao7o5hnhp22erlq7gjncplcqoybytcbtpa7nj7xwtdvqd.onion", 2012),
                                new Address("umzgiazybhoh4wg2w2bncjzuiwgs6xhsxrxeo56wv4xxhwpbqqrixhad.onion", 2013),
                                new Address("pfzyun73mhmywoyq5vadatlegujfszplksity3a2shkhyqhjatgvp3ad.onion", 2014),
                                new Address("hoxfbsde72pe3wawkwqtnch4577r2p2ttygvdubl7ew53c6hmqlqk4qd.onion", 2015),
                                new Address("reftygswljdhzipgscqck36zpfbg7c2lucgwj2l352jm5lhrduhwfqqd.onion", 2016),
                                new Address("gtxarnkuf5cwe2qzinpygx7brfj2ruh5mkw3rp6c5clawhvijwmkp5id.onion", 2017),
                                new Address("yy63g2w6rfrjcvrgrunewchjprw3lmt7pbyf3yitwvlv3kuz3djom7yd.onion", 2018),
                                new Address("mfopr77pvffhdpvysxbyrywqnp6goqwgbazjlchxjm3quzksm2zij3ad.onion", 2019)
                        )
                        .limit(numNodes)
                        .collect(Collectors.toList());
            }
            case I2P -> {
                //todo
                return new ArrayList<>();
            }
            default -> {
                List<Address> addresses = new ArrayList<>();
                for (int i = 0; i < numNodes; i++) {
                    addresses.add(Address.localHost(9000 + i));
                }
                return addresses;
            }
        }
    }
}