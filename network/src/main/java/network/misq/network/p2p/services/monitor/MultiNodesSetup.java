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
import lombok.extern.slf4j.Slf4j;
import network.misq.common.timer.Scheduler;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.common.util.MathUtils;
import network.misq.common.util.OsUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.State;
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
public class MultiNodesSetup {
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
    @Getter
    private final Map<Address, NetworkService> networkServicesByAddress = new HashMap<>();
    private final Map<Address, String> connectionHistoryByAddress = new HashMap<>();

    private Optional<Handler> handler = Optional.empty();

    public MultiNodesSetup(Set<Transport.Type> supportedTransportTypes,
                           boolean bootstrapAll,
                           Optional<List<Address>> addressesToBootstrap) {
        this.supportedTransportTypes = supportedTransportTypes;
        this.bootstrapAll = bootstrapAll;
        this.addressesToBootstrap = addressesToBootstrap;

        baseDirPath = OsUtils.getUserDataDir() + File.separator + this.getClass().getSimpleName();

        serviceNodeConfig = new ServiceNode.Config(Set.of(
                ServiceNode.Service.PEER_GROUP,
                ServiceNode.Service.DATA,
                ServiceNode.Service.MONITOR));

        KeyPairRepository.Conf keyPairRepositoryConf = new KeyPairRepository.Conf(baseDirPath);
        keyPairRepository = new KeyPairRepository(keyPairRepositoryConf);
        Map<Transport.Type, List<Address>> seedsByTransportType = Map.of(
                Transport.Type.TOR, getSeedAddresses(Transport.Type.TOR, numSeeds),
                Transport.Type.I2P, getSeedAddresses(Transport.Type.I2P, numSeeds),
                Transport.Type.CLEAR, getSeedAddresses(Transport.Type.CLEAR, numSeeds)
        );
        seedNodeRepository = new SeedNodeRepository(seedsByTransportType);
        KeepAliveService.Config keepAliveServiceConfig = new KeepAliveService.Config(TimeUnit.SECONDS.toMillis(180), TimeUnit.SECONDS.toMillis(90));
        PeerExchangeStrategy.Config peerExchangeStrategyConfig = new PeerExchangeStrategy.Config(2, 10, 10);
        PeerGroup.Config peerGroupConfig = new PeerGroup.Config(8, 16, 1);
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
        peerGroupServiceConfigByTransport = Map.of(
                Transport.Type.TOR, defaultConf,
                Transport.Type.I2P, defaultConf,
                Transport.Type.CLEAR, clearNetConf
        );
    }

    public Map<Transport.Type, List<Address>> bootstrap(int delay) {
        return supportedTransportTypes.stream()
                .collect(Collectors.toMap(transportType -> transportType, transportType -> bootstrap(transportType, delay)));
    }

    private List<Address> bootstrap(Transport.Type transportType, int delay) {
        List<Address> addresses = new ArrayList<>(getSeedAddresses(transportType, numSeeds));
        addresses.addAll(getNodeAddresses(transportType, numNodes));

        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            if (bootstrapAll || isInBootstrapList(address)) {
                if (delay > 0) {
                    int delayMs = (i + 1) * delay;
                    long randDelay = new Random().nextInt(delayMs) + 1;
                    Scheduler.run(() -> bootstrap(address, transportType)).name("monitor-bootstrap-" + address).after(randDelay);
                } else {
                    bootstrap(address, transportType);
                }
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
        Set<Address> addresses = new HashSet<>(networkServicesByAddress.keySet());
        return CompletableFutureUtils.allOf(addresses.stream().map(this::shutdown));
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
           /* String now = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            String dir = connection.isOutboundConnection() ? " --> " : " <-- ";
            String peerAddressVerified = connection.isPeerAddressVerified() ? " !]" : " ?]";
            String peerAddress = connection.getPeerAddress().toString().replace("]", peerAddressVerified);
            String tag = closeReason.isPresent() ? " - onDisconnect " : " + onConnection ";
            String reason = closeReason.map(r -> ", " + r).orElse("");
            String info = "\n" + now + transportType.name().substring(0, 3) + tag + node + dir + peerAddress + now + reason;*/

            StringBuilder sb = new StringBuilder("\n");
            sb.append(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()))
                    .append(" ").append(transportType.name(), 0, 3)
                    .append(closeReason.isPresent() ? " -onDisconnect " : " +onConnection ")
                    .append(node)
                    .append(connection.isOutboundConnection() ? " --> " : " <-- ")
                    .append(connection.getPeerAddress().toString().replace("]", connection.isPeerAddressVerified() ? " !]" : " ?]"))
                    .append(closeReason.map(r -> ", " + r).orElse(""));
            String info = sb.toString();

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
                                        .filter(e -> e.contains("+onConnection"))
                                        .count();
                                long numOnDisconnect = Stream.of(history.split("\\n"))
                                        .filter(e -> e.contains("-onDisconnect"))
                                        .count();
                                long open = nunOnConnection - numOnDisconnect;
                                double churnRate = nunOnConnection != 0 ?
                                        MathUtils.roundDouble(numOnDisconnect / (double) nunOnConnection * 100, 2) :
                                        0;
                                return "\nChurn rate=" + churnRate +
                                        "% [Open connections=" + open +
                                        ", Num onConnection=" + nunOnConnection +
                                        ", Num onDisconnect=" + numOnDisconnect +
                                        "]\n\nConnection History:\n" + history;
                            }).orElse("");
                    return peerGroupInfo + connectionHistory;
                })
                .orElse("");
    }

    public boolean isSeed(Address address, Transport.Type transportType) {
        return getSeedAddresses(transportType, numSeeds).contains(address);
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

    public static List<Address> getSeedAddresses(Transport.Type transportType, int numSeeds) {
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
                return Stream.of(
                                new Address("KvQVpgFzxw7jvwdxLAjywlc9Y4hLPT49on2XPYzcRoHmQa-UAkydPKZfRiY40Dh5DHjr6~jeOuLqGXk-~qz3afeiOjEBp2Ev~qdT8Xg55jPLs8ObG-20Fsa1M3PpVF2gCeDPe~lY8UMgxpJuH~yVMz13rtyqM9wIlpCck3YFtIhe97Pm1fkb2~88z6Oo3tZGWixI0-MEPGWh8hRwdVju5Un6NXpterWdTLWkM7A3kHPh0qCJn9WaoH~wX5oiIL7JtP0Sn8F852JdmJJHxupgosLJ1L63uvbvb0pT3RtOoG~drdfbATv~jqQGc2GaEV2v8xbEYhp7usXAukJeQTLiWFxFCHlRlIjmhM-u10J8cKrqAp2OXrDwLzyX7phDEm58N21rQXdvQ8MiSfm4VPlgYxie6oo5Fu8RTAkK-8SKRUA0wx7QiJUVPLm4h1-6lIHUbethGfDpCsW-z2M3qwLKbn~DAkvyxitNylCTR-UNZ4rbuDSH38nLRbDYug2gVRjiBQAEAAcAAA==", 5000),
                                new Address("m~1WfaZNz1x9HCOFdotg~G9m~YSMowWvE3jeqAmc-xsFNJZKNPcOub4yWc4uhoSu6yL0WuRIH7B4skPvlDe1BtEnPVJXyTGQX3wepcL3aekY0Gc3kB5gcMy48pUHNcxdznPNDNFVCqrmOpthGDksukJIlYxfh-M~S~3K-2gxYrDiJsT16o59E3bOEwArVpLg~C4NtaU6~KyUFvPfcD9SKA8PrQ4nu7OjyCrzhnO0BNhNv2t1c~5gLlu3gsRviWBl6hxppystHuDDCE~6ERufsvr0DFSrRetxkY0eHqL9l8--YbDgceTPtoWiEfmgpfLrznHnaWdn9J~CMQ~0dIbi7hPhGh8z5rBp5h2RRBzumNF5~A60Fr4WSIsCbSGeaQo0SZJsGpysJdmws5ExcxQaqTiCDUuef0zbl2Su3THlipNOTkZaA6wQv-TbJjfaJPnVhnpIBsnyK8Dd8GzG3P6eYvrA2QFN2XzxS4rQ~KK5oNqQr4MHRJBBFUM1QmGLU6wmBQAEAAcAAA==", 5001),
                                new Address("fZDdLw9o1eWz69BQSLTYD7TvYnDA9QLFyEykh7OJR7d4qs88H5xFJ11IRQzup56NZVeZEqh-fgaG-JT0LKHgUx3G5qlYM5N1QlidKWDzpmuLwqnXg25n5V3wK0EpomYslsBej1~tYReK5qw6ASw5N~1qVZDKhYktKrCoD7SLmnPejFmOA~Gx2YWoV4eU35Y0PYHq~ysWwEs9BPuuhxbN9PUA2IFsXu5NmXsyuvmHABjMOyj9h5uMN0F0Dj~2slxEqadWBCqzcbmE5EkdjAO1vcdnMRh2nTs71J7MEX7w1sXi6lU0xLLEv~JCBLs8qIRT32v2j7a3FvnGhmZju7XAUbx3LNgFUyCqReUFCZnmNIKdmTmNdg~MLlI50hFsErykxlMIjaf5bn3fE~E839J0fJP-OW~3sBg7t8Z68aOQhZEydIOjmiiGI~Tp1JFvT6Dnwkldk1SLstQMNrOKNNfIbK43aNIWehbiU4nXXvVR-0vxNPAFyTPRqNVpTCZIobrdBQAEAAcAAA==", 5002),
                                new Address("lZrqdVrgGByiwTr5NHjO8PUVjTxjxcpJowKyxfXtfj0wBJIYDTQM88fiUkAYCLGuJSZhiavmysEa6n2lYxn3EwM1jq0hSA0mclYOj6fIqO7dupBKHHRppRcr-E4ir07pIV23edi1wnW0oOhXGNjS7K5RXaQngu0KOkCgvjSaBPYrUkbPgT5zxTkZMly2FUkntGQ-qEBDktUr72a2TCrg2i6geBqo5E~hSN-SrLFFViri2pz9wS52AkBOscu6DDPas2LWpTwXYBg-qkUZtzM38c3UCYUjAvNHooI68Z7nj6Fo0zs3Em6r63o6ztQfAvn~PJ8LmJboFxGzGhgGk4AL4zbgY8ZF0Fyrr3OqCjOWBwaClFPfTsWhYFzQcFw06QEd0N-MQnkCmwI6gcu6BTz-Z6m7HktjJHJHhpRMkXsXUOVBTBFp~QyfijGvJO9sll91rcJ6cNl09R9jsLSnJ~82rWBQagBdW02TNyS4UgVlpMr4QN35S1zRu5rYSDtOnrS4BQAEAAcAAA==", 5003),
                                new Address("eay7qLrWoMp~C2Kid4aOfzTVaYAJIvT-xtSSHw4EDr6R7-K4I2bcRyt2I4XIZaRta-QKgFC2MBfQ9rdv6~BlbpN4EaSAfaTnqvBNb~00eSDdvdlzKDTgo0OsAlSHYx0KriEZk-qc3LA6T-xFmFCsqADxweHSprNG0UP87O0jZUldJjJc-9~Ls76~C1NEpHWp0W8GJ47TkCkqXNoA7nvNGMfIxBznP0HlnlMgnbWtUDeWcuPLaH8Na5Yh3Y8XMwHtwofkPJeAaX51~74muB2t5U9xkD9oPAKZQSIA1Ykt8XAoFRfRrsqKrqeGwtbrtsGFopcUu4Zry3QCbsS76czKxlW7WpSIbWwcaj~XOkUR3WycfsGuMkE7rijuuYTqwR5508pmYA1wIm6ThY8sjfqTKvpVZUFx5BUYKB6KRXUy9tvbBGVjrG~DFhEWeo~mcAkMu-MpnqrqMB0iG2-dwUKpjhsOar0KSycjtURmYBvLchh6L~4W5imlgemFkXWfumDJBQAEAAcAAA==", 5004),
                                new Address("ZKZfVE-s4Qw~My~xVXKSBMot4ms9~wEWruWX8398muZytxV8Hw~cgq410EoHRbq-HsWuzbFI-Acd3t-ja8JffE2KwEmzEc3GwZxyaMXccUbreUeM33ldEJZIWOm~~szhGlAJ4wdFOrjgdsCHM~cyDzPQujerN640Ghkr4rC~gkLL7P~-vTLFUEVuAzMLe3-fs2NJWG9jhfLeynZ845QTKifWs~0OK4zwu~iGwifOw2H1Ra9k~8ZmTW0UM44WowFE7djPtM2XDKfQGWKM2Z1oa2747rdQNa-MDbZcZJvGSO6DiSP7ia2XKSkn0YyvuGqul2q1~EJZYu9~ih1rd35U20YKC~AurXV8RPRc8yUl8Go4WWH-1n5EHkxXp~o8oKeUM2vACzJW0TZHXM6uDsEYy7s-3pRAuM5xQn2s-5HPA63XH47ki8GieiXfberMxdgYj-RIEWp3DxSu7TnSt1cZIRSmwMhUo0ialyr6oIqt83qtD9hdOxbfRI9t734rp8b3BQAEAAcAAA==", 5005),
                                new Address("7mUI1wGvc0SFXe95sSEBGc~FKeLTZ5Rm5DkP8KgM23uFIVyzafsJHrspXmI6nrO-zocRkRlh96pbo-Klj9vnxzCnlwOrcLtevCSH6Mb1i6dqez2OBZJPAWkwMl5rhlBNKZK-gnjHKzvGWi7p~JQVTD4DBXOREzl1Ja9KTh7DKI-LOr1IfmRvvgBJ~tMRwy~DhpPzrKnb3wQ7JOdbPov76LSrzZ4pJ0k0ZJCAwLLn-gLm0f1bCjZWuqDragzp4k1niNKYsDUS6yX3vAlybW-yqbCHIe-P0xojpru1YHaY0RRiz3F9M8h57lgX1bTIkE4XHZNAvTerC-2DP~Y32ZyVSuc14vaZ31WhqTfrXnl3h9RDZ8OfyjwL7zKQMznnMPbWFLv~KyTAcmI7wjGlmEXBjwk0jjVJhvrm4uEn648aNc3oPv4vryXH9u-~54J~gU3-wgii3zpYtpT9AhK2X0OTEvvnG0QZAL0gTwmyvlKSrKsRPfdl9AWWswmj8F3WxjK-BQAEAAcAAA==", 5006),
                                new Address("gFT7xEiKOYTOLOyJgFntwdGXpcGeq0XloyUXAqqQooqwWQC~UqpaFw5GANm-kjbNdkyymL8DJTw6HU~rXxPqVs6YKV~v80Rtrug2jQuIlPwXWEwRYnDl38bMMH4a6Qqq0--vh7JzhXt~fh4hiWPyiPkPZVN6F8PjLJ0myr54plzEz5ueMxW~bjOWshef8yWrvrXZ5QPMSJWwRhw1AClWZUjdjR-7yRZfT0XW4z7l3EE7ib~ZFzdRxcb~M3JFaudpIT5bowgPmByym-M5ZNZXws6~Nlu4OpflSsboAmXz-gdyMElpUOm8p0n1UyV0n20nTVRiuCVL0e~SSYVjNLSBdWfUSwYHG-TjDfFovAFd1Ns7sHZwOXFKZSWY6bUOuOW4rRwGKaXOvXGdvm881w-wIVgk6bah3fVIi-D6vjiKacFKQD6gxybneUci6KXi59cucBxzp3QPMlQLweAvX6cBKvKzleStJhH18MkJ1Q9MPNq8tl2WF4X-YPSnpkHsYtM6BQAEAAcAAA==", 5007)
                        )
                        .limit(numSeeds)
                        .collect(Collectors.toList());
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

    public static List<Address> getNodeAddresses(Transport.Type transportType, int numNodes) {
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
                return Stream.of(
                                new Address("z9py3WPq0xU5prjB3bGmYCGLfCrtu3~VHrrXxEQcYJFGCwJ5KblTKv6R6pGpKxD1dv4w1rtrwao-DqWhk-sY7RqXnVlPAtdmFxhwaGlIVfBqFYcbdBawcqdebkNXbCUwfZZWElyqkEShsn3pORAnkcIL7r87EQ6XoWgD7PkA74eCWJ-Eq5Y3N7eLGTetva9uE5nXl8HOUJalD3mwAkcs-u8hYDWOLkvbzgWR-xg0tV9s3~tLz2MchpS7bTsZcX3NRMkrGWWXgYwdYUCmIiMC1xVWn994dltrDD-k4RDhcglQtcfL~pv0MrACXxierlqc8ldWqZLwaLjodGAr5-lDTbL~b52SNW2I~qglr1wn18qcDTDUtXqsH3HpN1Y3q3pTpk~4d9Bx7YWThf8prYss63QIh3U9udQpLwsih1YAryzQBgaFpzuNWt4FMko-TYeMHdNfRcDRR1dpvzJRI~EYAnHuuzbv4SDErIlV~ydTL6Gn7wAo6DyWPwjDL0G2jzaSBQAEAAcAAA==", 6000),
                                new Address("0bNgtmgSfEgfFIPsoH2RXUrNC8-Ri535JrIHEy5df2DL824uR6a5GIn4Aj3UbUGQQcXep6n4iWljntL9Umh-aAv0RKdd3x91APLNuEDozXhbLJzU2LDGJBOY2laBSZBxWV9l4FvwpSFJ30msKVdKn5JlYaBiL66q8bBPzAo1AVvZdhScIKeZC9EkZ8CktkNSleTrjYGO9Zbm2cssQJDP27VXqR3oPl558cszCxKDleWtDNf1Rg7jZ1-4lrbc80QS5Y1xT-uAkOQomDzYfjVj9doILN4op-ONcsdPQQpOXgBOXQJXmkENWyWKL7XCWx7bPMnN4WRjsacB8lStoPIaqgoPhB9wBuihUU~zp8y6j8LLOxH85rBuGzw1wPyS0TNfCi7KgFqXzTvy3YWR8GzNOJ6xFfIL-MuultLZievE~aCTCILWxdrzwbydUAGUcZxhenY9gFiMd1Do2RrMNqzq-0vsohC5B0hdJg83wa~b9hLNDwvYZy1eG1CEtH4spnH2BQAEAAcAAA==", 6001),
                                new Address("ryxwDekPGV6eBCTlgjMT6jpOT1w3OeriYBIPDE02lkX1gSlBc5iGk1CzsNNnUgqZUeAUgxVQFVVWsILp~fpK8Zxprid6jTyZtKR46AlOH3AXnEc8cGDdgY~zaPAPDnOv42JpHFWn7bwgyy1I08cQHApvdRJSeZTTrTZbOlsmigigPBNhUARTTosEfJyjROC-Q1MypzHL91uuOC3cEvTZ-qpv1gcMoHMdhNjKF37LgWgkUiN4XbQfOQaCdM6Tg4BO0YZPKtt4mv-u7-qtY2U-wCdjfSEiGhtYHnG6Xw9muglRYfHfwX7iEinGRz2uBE9FkyFhz~1~snlRHI5fQRDYD8eUd0mDNqqOEjzAk7MbsbBJBXng90Aq7zWiqhMjjTihyVM~h4Bx-6VIQ4fy87dPuOlv~ZMjoall9doheJwdYiyelp-YnLEOMsFsyx9KxuAeDZprkgMEHSPDbWBMFDiZT8RO~eZ0j6J8lzJBqh9TbPmvxHlB3E9c3YbpCfjstn0TBQAEAAcAAA==", 6002),
                                new Address("UBsDzOCNLHceDvFuxuxPIAr3W6MUTk1DNwZVNaCtegIy1KpWtZ708EW31zG02nVe1TY6836wjCVeCOlxuvA3G8AMNM~iyMwrX2kBjIAtYwhehhsrLUvrNcrrfTvVd5KkG5gnRM6K-qUs0ffZNNpz8slqXkCt8G8buY6DmUOITaA1kHORuLVKEH4kysxjD2a-Xhjui26aw7R7MH~0K~FLcfSWocXNJHs1DeVLS675jyhjcoghyLuSPEH~BDMWN-lNZ9RvhTYc1hSJm8oyF9j5OC3qKGFgbMizns8WNc1zmmqkJQJmXFYXY7Cr6lBA~UqW3D7C4675KsceJPJNOAl5GZEalXm2hdifgWCFKyaEfhWBtlr15y1zUfcOqIlHolP617mT~hHXb6jBgt1UzU84Zs5QlKxKlEV6f6X5klRtIHCp~s2DP3bi5DfPZ2yqG--9xszVPYVbNjAAcg-qHuPyqR0WRfktO0N5Lhs-d0oPWYxAGIvTMxXvs9HeEwOPez22BQAEAAcAAA==", 6003),
                                new Address("cg-elwncJDQtrOjRmJjvtO7W3pq0xR90rOq6bBMp4hjrdJXJci21y5zMEFmzb4q0XASEpz~VA6MtNimIBPpCZCzykXufrlnWSC9FxVRz3gg6-5SiSHMW1xDYvhtYYZ2jgCsAeuU6abOgrC0km9GRPbrQN8fx06tqsg6PS4u8f1ptCE1EpblX5Uf4HRAqi-EdLPCm4GtueVXYnETX82cLa5Pk0g0pIgyFB-nYsCC4uMK-xhz-3NDrGJKF8J0Uuk7LTbg6OhcTAFRGoaP~iAindLK4gBgzLJQbIiji8X1JxRWncJ3~xplIQ3ADR0aeEdNlLFUGP8Yfh2Ctva37XT4ledr~GpFTUNFUh8zTxItxOJvdaQiov5s0Q324XNbbhJc5ZV8sztzJXeDFj81846~1TLT-2bns7PjTGQFvt7wJDNywRucDuA0legv9BfAKC2EDxAMvW2Aa~qb92zaOufgmPJBcSPn-kn6gxyRVQMJdeh6JLCSoG7qyRlBgI6q1OrwWBQAEAAcAAA==", 6004),
                                new Address("G~P7OKiwZWsAr4Xt56vJQlGFXWTt74Ou4Z2vzXbqGA6GfR9~jLuHzcJN3t534KlObA92EcYSfdaYAMQOH7Uzd2E~cTgOJiTPDFOBRqeMHopmRVO3rK6Lx9aHuyoMor0UTDXlZojmiQd88L0qCzVeFsIovvUAZTpxX4s25FQ8jfTNxYz21tBak0UiH44ERVnd0ton~aIqP7RcXbyxSJMbsMdwyT8Id939yuDzGOMo~mpHyoJqyiv3VlIF2YVsUtZ8Q1q6KCFwAVyzFR7hIitNEqx0ia1VuK~IkxZThcqzIM9n~-IFgIAZ6YKwwcBZxfFYTITmuqnheVZcEEJxJNpLPDFxdgXR3ytCplGAqZjihDoRS2QH0RNWllMAgjKa0UL0ShR2RY6O4W9uzQ07g4L-oFVdgR4FM2V7le51V6r-wE-Q1UzuUu9AIE6CDNAHCRAQ6ltb4J8eoJePWSsAUv-IbhxfshxJet9PsfGk4RrybNTswtb1mX-c4CKss6ee67rlBQAEAAcAAA==", 6005),
                                new Address("ZV2QUM1OpHX8BLiZdeyGHLJHUGaBR-InO-H2~El4gPXcyQHhz0HcLowq3NH7gKGWQqproJohPjdoCorTXptcLHu5EhVhjUn2s2vKhMy2uLtmOvunS1-gFkbsJ-Xd7xHmqRhcgxOmxGBgoufP1-rGp5-XTkmWwrxXrIzdEFfSAxeikTYCIVeYd~-lq0UCDqwCUOJ2je57fGJYsic0VT4YvAGbfEgUOk7GRUgp3Sd4VGuwiULhWOSslUnykz~FWrjtbp7~oHbrV8Ck-gC6IcYPz2ZFX4b8JzjyiW4bPls9gdzJS9Bc-3ktnKuLh3uNA-djOHljzXZXL6lobbwPU~R5xna8a-DGL9WqGgZgiD~KBFLtFbw9VALyTFGyXEKY57I4lLgS8s03VD6-RR5iG2zAw~V0zO1ZHAKI5q6Z4bqWOAu2GafPaBpprlbZ3iQ0TIev0iGXorl3yse6EF2yn92DeJhsf7usaxEilar6rq94wu-6nQHNIYCgQgmOC~EVA8URBQAEAAcAAA==", 6006),
                                new Address("5hBkq2JnyKhd71SPs5XSEou3OMZo5j2iQnr7a4VxW4QGSfrSGRlRe5gBvePjc39~bXx~~8p-OJxoq-4qrwhp9eQsq2HahdKvhg6BxdN709ScbvuT~eOnxVPTtLigaTbf1f2NLgS0QLVETQUrlNZjHNpvlLDVrQOpwjwIbZtNR4Is5mQxln8a5imCsifNs07JergYKwoFqjgA7Y2M~QolMs1qUSQiW-bujOxC5kidmAX2aPiytd1QuTuretWGHJnLsMX2eITPlg4gmFt0u6pXS65qZhWyjIv6T2URu3sVJ19FGPeSWCRvWAlEzkdddqz7Ob-0brgwUZxrvHdRadFhUsgkLy3OGvnogO4iNvXCNzuzFnXhOVPtFjvbdX0sME1HJaNZ1JGPbMekcr2-zDf6cYUdNPEZ9NMzhF4u6U4SdvOyU2okLwWY0SVEUVCJbGf6lBG2o~B53AgDu4sRr312sXu1SIWBJfKgwQA~ROknDO7mrpCbekfEigPw-~yWAB2OBQAEAAcAAA==", 6007),
                                new Address("Y4PAVfpyTXWPhLZYJ0iGrCoIqvDg774c~TGKvu0-vp5PMsEpExi3q3pn0CLmjrJWArul5hYci2apFopNatylPZMJvCzhZ1rHYMAMJKQtqx9WweQ-TAx33ecHWn4dBLt-0maMLpmUdvq69LU43k2nMsH4sjg2nxX0mIve5tANR3vuKhbQhMC1y~hTHw9-7ZLjX6KKsvf9MI27DLICqnObUdp7CRixUAi1hCLjgTJVwn7qX~LAfqhesdHT8Kkk2nj2Pg0B5gugLmiHVV1Anc8yMgvo4zhMThGYCU7nmQgaf-FXMG~eVQi0qou-KpU4zAJOmm3d3-ZSy92WKO5eWeSm5dKv-ktXWF9NhnTUtdrZ-U2bWLC1A-bAZ2MWXfHR0S-SMAchi-oH8nH-0quBEtn3EA9WEBLbr6qgm0BscTGMZe8oQJpGksdy1HKmpaLfsaF7eaxKXCbCHmhx-itzYvdUY0enHleAh9iYuUXmN2G8CqSV9TPgdkQPOEGeWP6h1~HBBQAEAAcAAA==", 6008),
                                new Address("mMo3G75l9GFm8~wWU-MB7LFMzKEkz-GC1clV6Ki0FTjuFQz0vCOPukZah1Io68qvJcwhbM35WCCVMKvrWT2n6zu~hdZCuj2gxa9wAs29vYOiYaCJpe0qrGyCTfWwLGVTwktiy1K3t4InEDraq9~b7bs4jLq~AHA-REtDTMxIO4S2HTijaJS6AoyBOaAYh9oIeWMt290Y8iBwbG3gGwAeoQQB-7uueOGz02krAV4jVjtOGtc1d~D0SLt~p7aDdKJ7eq2mQdT~0Skj52gwfPPZ~T4cqSR-Jl2T2q~9VihFzDd34PwuTEB-3v87ciUNPirAG7sg1vhbo0O2V3yWJAuAME7BmhG9shkKuHyPeOH5D4e8fCwdzWZG8w8szy1z6A5~2zFhhBCujPMjx-NHuW0EfimdokqVkkWtXXPddoo8i42VIBgufSt~4YNlCMEG5dMDWtGOmvAVZxq5vFWk6elzSjL3NbHMS0PJQsz66sTzcqkVDfbgxp1kxmCOqnawllFsBQAEAAcAAA==", 6009),
                                new Address("Ndb74rscsXbnF2MKtilI6QmT8lcQS~vNKvDHYx1UvMN5P3l1mnyI3a3a7N4wgL1Ts5N-uscYFacy2OXc7Pi7ZiWjHz5iICtmZpfKPl6-cx8tX1TwCoeTMl5u5WgmgVVX2poReyky3Q71Akr49hq45r7VYie2DIkQUx-vITO7lbUFoBvPUY3arPQ5pjilTvFEOKwt~IWMS4CY4Z4LbXBsZRTLaEXsGwedrGiVFe2VJddEdljWja6IdwfX1-JONlU88x794~n9x8qrwKCcrVJ9jt-Tw3rBDkc59xDp5Q8hGJGCIDmTPCldFAN3RK1BKBvroesWGHH4qe8EVQqQxOImQHk0k9v4wghkiQCOQjEcq-l7nz4Yv-9F-t2f0SrlTdTl3oHn-dow~FibmLO5DQLi9LFWVaDOMWcY7TCxVDjwVu8bC8s8Kgj1lUTZ7kb9KAQIzUKGLzmFfbCrkG-X4uyjZ8dvua62nhH9gLQfCa5NYCoJYWFdJJedawl0YEQKbcH9BQAEAAcAAA==", 6010),
                                new Address("hTRBFyvYJMRSZ4Im3lwD1~y9vfcPd0rV3qGfVEJ6bJxnKMDuYwUAAtu~c37J2GdX-6ThkOdQOLl1e8gSy9X3AXmSuHoU1eevyi13uA0jXCLAAQKWof2AvHYxTkGHwu5B~ZCv7~puklIFsKnE5tbhVqeI5VxazW6Es76vmXdM~z7znPZbBulFlpFdlKAkshKDR~cX3vR5h4ismkgdcxlxyr4mlsoOz0SUUQEy-jmNbuuFeBxe6cYu7etMv6jCadt2d1tSP2NpQjKnnRAMzIrWRjeccQ7m3DYRFywPLoO1DIRWs8d4Ei9UmVqgfmXOTcJ1vvZkIQZFKISolvmVx20L02n3rrOSIXxcqxhVXraSDQafzn-8MOj4OhYVOy0HvIdSwbTi2NBrVJrSneh1yFBfXMZC6lfRE9brP2O3AdbxXgrxMrysRb~GcDfSjORmdo0QL-5x4s-7KkoB8tWd7iQzhmjX3aGQ~PlFEwXoOvlYAgLPW7bpU3v1Q1CqBMgmJTAfBQAEAAcAAA==", 6011),
                                new Address("kxTrmG5i7bi-vOpB4~-kXUIGZitDArhXWSMzULq83qXUueFj9qd9LapO8LCFI8dxilFXj6xTXM~8tYE0vDy1I6-NS5oicwx4b6IUb5OkCti2opzlGIGGh3IuYUUnWSGtA-TXDcLiEd5POmxpcj4Bxgz-RlSKGVDYIWKq0JiTZ064wWpVFiCfjYsMG2JOlEhYijlaXRw2ArOzZU-UjNE13P0FtPJK3g5rs7oYeNGuaejdTAClE0JO~Us6wbPtUt-TKIcMrhvxOtQR1-4KdrrmIsic8LSDQefJqmVs99aIyBSv4jVmk2EfEFYftFxPlLImbV8XJ2TyQbih-t50MvZtB0gqQN~RsB63OHaKY3F0-wnfJR7FtZM3ZNvVH4S~11YCQBXRMXmUMN0RMfqnzaBbMgnFStMvEXU4C7mABopGG4a5za-4WpFmOgoPkyDMS94-Lcm6Dc2eVtEfBDqE5p-TkQOr3X1UoQk-BwiOS9-nqbFXDOS7caVvBsduG7YS7FNEBQAEAAcAAA==", 6012),
                                new Address("1x8cZ5PTOpTE43eH9F4chrxYS3KTMlNjS3PmHRkpOUrKHT-9xO6JCuDTbqfxbK-kKTLfU7uEp~FCrTozztzLXB~yPUuRQdM~qbZ5YLY1aRZmNsqWsX~ogsphOr5S7LgQGQlqnvB35Ggrv0WgVPbwGEYZzH7m218JbbcpYoQVeCy-soi2Wrsl6XKePJF9neU4TnUrBCiN2udccMKrc-gUMZZaXOLttFb-rAzaq2fuuTcGAIykfoKKXvcVKxooQwiymVz6DjAsxffip~Ld1wf8Ga3AeViV1GadjrITrEfAGkoa3QV652ZNij9k7uyba71aOXptPcPyATVG7LHDi6yB-pbJIyPCKDCRTTGKMcZNOthUJd~45-pXE~-meYuF1SfyJNYyBxWMx318WrL1ntSnWsKhabk2LQUf6yrknrZKLaOBWwqS~5VFOjMmZvP6ZasOJo5BONrisG18~NvmR1AsHWD8Vk-57BCNz6SrYnUZyCDQVjrCU8m9B7O3CQdsy~qvBQAEAAcAAA==", 6013),
                                new Address("cysmvxpIjXQhqvbTZhttXC-f-jm7zR0KANtDM0Z49uMP6P2~I74Duci8UXwV2BSmi89XBJAYTNzUy403DczeYlbvGng3ucO1fWdSS5SPGTT06ZIcvfxWgZYsDVNoujobIm7cPD7Bq6mVgIVXhF0FmSOjuajxlLY56rBMnsHFGf~hONIcBockYhHm3WXyxRxcHchBJs4mTjHc4VyqEWLizVNPyfWsb6omE2~l40KWHFdZDx5R8bVrfIrzBtdtOUBF0B3xSj1OcPlYdgTZ5XZpif1cyKEDMR-D6-nv-2K0SoVY4V6dx7Y1MHXiB2ANefWbLI~XQZvUyIbohNkbEqgrk6tI9YMslgW63vc9xfXz7hmXfHnHDApKPqaNULp3LYV0r27TapTOgn3~J8WkFu3Sov84ooiB6Rj75FXcAsOW2sYfsaGHR5gvrNol0lTjDDux12x85Ty9RitjMsT40Bzwc89CQ101OQ34edsCAey-kX~i~ojlY7n2ZPCNt-fBcktRBQAEAAcAAA==", 6014),
                                new Address("1Own3TU9MHC7kJwC9vIT~XpZwMYYfq0e4QC3RxwbRK0dogMYAhcRIyP5ox0zm9NHa8PDsYASgMmxHIzKlDhleuT-n0uBDvZ3M25qBFShdpAxBLU4y~SBBGjOPfnrCDIrCjUr-9OnH7VXWBKuizAhiNdQYWwj3ZajIpnLgR7AbsuMvHqBV-KoS5l3m4SU1FapOTaGoDvV4UYImTa18FyT5HBU3BnJVDiuspCSO8~~eOgeabBk11r-kXPIc9o37QzJhIA~kmHzBLh45fSwGrRFMCnoTFRqUC0ruigWTHpbI8CvwA3lJ5y-1X0x4cHHVQ3Qxcx4Mm7vfU-oCJ1bl6pG8w9KxXHcipOOQcK74RTUnl71rVmLTFPcZC9wE0lMSbnqtrFBfqFhE0mCQv4Vvh~JIwSQcjhhoPfV5gMDm0OU1cTsg8-s4Ze79ezgLby-Ajekbpkz1jJVQf1LaxyNCESbuzkis0sA4pDuGCX9a8NttIMYoCxk1aYx1-qDDy6NIrn3BQAEAAcAAA==", 6015),
                                new Address("qzBM1QBga-TNfaffMLHVN6f~sMLiD2Zreaek8MFABtj4BBdpq5oNURUVffrmlHXYCX6Q2fWbkBB1wULU~acfcXsfqsSGwE-LdvFrWG1FXO0ICmsbVw2EWJ6L7L26OEqnZhWkc0e8t7OkWXVFxhq6oKs6gLDlX5gSNJUrmcSTUqzFdMaGQHVXuL4pp-YP-Nx58vCbLtJtEkriNi10XEMFGJAosZ9u-R3IsHJcbhNCtDQXVdvGmtH4rYhwVX3Dgh7O0OWgNDyjOY9NcN0-3ZYvqUsu3p0mJut5cfjrxVmWAq1ym0yoAYlRUZbt3mpM43GqV2qSW6Bi~VofXy~ior0WPCryv2PESuwac56QDzDbLtouLQ~61dMr8wi2VXs~CJT9YO61MRiE133PC76hHPdo6HJ1-6GNNdIOnw~O4VlH-qKvbhnRAYtHuwcRpyJZ~Z20nSQma4i9~vh5qUkfky-0pT8APjz0O47~-zuPw9oqo4fqYv7QuSdfM9TJNMWl3Y~ZBQAEAAcAAA==", 6016),
                                new Address("iAyzEp63Gn07pOcBKgqaBenT2r2yDVQmLJLjjuOIHlJYwXw63yN46TcH8nMfM6kX1YG7jQKbTmmgXNsbOBD3~SztnFfQeAuvS008iKZa6V4GHgvwSz0ix4P2MDG9y2N-kCe-Y515gJPDVYjb964MBcPMz85ca9PMN6SYhvvq032tgcDIPGSERabwwfrzijgcwbg6~fHMb7lmJ6MKMumxNCo4OOvw8qo8hlOdmCuUsNQipy1yKN9Ut65nplIpbDV2nww7cJABCLR2txdRYzOgyMc0FwazQHZpun~921fppOG1PRid7BLszRRt0jrzhR6ALcQWh8Jlnyt72wBseaJpCWHPTLPSw1xtRncG~2Iduq96soXCIwsb~WcjUVaRD1aa87GJ2l1J1Jof70KCBgB7IbYQqcxZUCu3yeMCmCLEdJNB1hcV7bfcFcWyvTPNJLxRLCJqgvyK7yU6bRZzaalWwXe713dWl~m4AQVah9632ApjIFmK3O1z4XeNLLIXAtZLBQAEAAcAAA==", 6017),
                                new Address("lb8n1E9HedPMmXTiDUSJ9rje47uFQoX0nnlXSY4BsICLJVPqZsosLsv81ZAPWiZbHLCB8Cb4WrogCaI3TYs2SuMIO2AjxLfSfulPHWiPkQznPNdParhX~BjQRIqRajMeNsz9Ynzp2f8JNqEA5sRLzuAWlcVu3~NV38ftRo~2v4YIZZRrxHx-Gtkv6Im6eG5nzO-s74LVPpDl3bu-5bBcc18z5W69N79YmPSzWk7nn8BuEdGewJim5rYd9frmj1lM6aZe~Jvs6iJ7WP5BgCKbRlYPIN7e2ch9NMbZ~pAmLC2Im7WfgyOt5aJMASTBDriGf7IdNC8c45k1yXOSU9W1wGanvj4V8Jov1O0IqNklNU0GmPF5K4fm79pG46otMjPtBTciOhsPTcRy0p84t2cZnclU7Ut60-4krdEj2YzERE2X0RXk8D-kv3ZYQTekVIkqwIO8K5ZzjZHl7AvwNp9OTEGUqoJLfZnfsvMEifmPFj~nybIIGmaYPgvHwxDr4n9tBQAEAAcAAA==", 6018),
                                new Address("HU4rnXiXnLoFQKU0hSLrpIyv81Wo--IpDAKzTQiDsONQHG8jad4EYXxNMKOMrtZSfm4ap5GGbaPAA~Gyx2Tp~kHVI4kmxNIh8UKc3a8ZJ7rSvqJwky1x~jR8f7A5KTXPGxUkZf6qYLrOcXRM0lDxa9GYAlHeYvaSQRebN-3VGCq3PDvnUGQyPiD1UHpBEYQ4zN~39qG3s3nRzJjSdm5hYSMnQaWCd6UM5GzYOLwfm~xC3KsmK373ENX8-hXphrLL1TDcze3vJmyno8rEGscsFjwqoTEbgMTbcr5iWYAMyZKOQv94~T-~8D2xr1HE1lsqNSgOkLPIG644djVxPCPoSUL5QED9eSu-EkYLTBjYpA51Lz14WsIaqDFzmBkbbnSaPy0XnY-0qKBkoSmxs5Uoxi1gb-umzgAzoptR92n8ZU98B2~eTHZiz8LrnBHqkOoj2RRkNonluFYx1G3bM97amImPMdk1Vsq36Cp2VxjP3WHoPswW8werthAggKdkcap8BQAEAAcAAA==", 6019)
                        )
                        .limit(numNodes)
                        .collect(Collectors.toList());
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