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

package network.misq.tools.network.monitor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.timer.Scheduler;
import network.misq.common.util.ByteUnit;
import network.misq.common.util.CompletableFutureUtils;
import network.misq.common.util.MathUtils;
import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.ServiceNode;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.NetworkPayload;
import network.misq.network.p2p.services.data.storage.MetaData;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import network.misq.persistence.PersistenceService;
import network.misq.security.KeyPairService;
import network.misq.security.PubKey;

import java.io.File;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class MultiNodesModel {

    public interface Handler {
        void onConnectionStateChange(Transport.Type transportType, Address address, String networkInfo);

        void onStateChange(Address address, ServiceNode.State networkServiceState);

        void onMessage(Address address);

        void onData(Address address, NetworkPayload networkPayload);
    }

    public record MockMailBoxMessage(String message) implements MailboxMessage {
        @Override
        public MetaData getMetaData() {
            double v = ByteUnit.KIB.toBytes(1);
            return new MetaData(TimeUnit.MINUTES.toMillis(1), (int) v, "MockMailBoxMessage");
        }
    }

    private final NetworkService.Config networkServiceConfig;
    private final Set<Transport.Type> supportedTransportTypes;
    private final boolean bootstrapAll;
    private Optional<List<Address>> addressesToBootstrap = Optional.empty();
    private final int numSeeds = 8;
    private final int numNodes = 20;
    @Getter
    private final Map<Address, NetworkService> networkServicesByAddress = new ConcurrentHashMap<>();
    private final Map<Address, KeyPairService> keyPairServicesByAddress = new ConcurrentHashMap<>();
    private final Map<Address, String> logHistoryByAddress = new ConcurrentHashMap<>();
    private final Map<Transport.Type, List<Address>> seedAddressesByTransport;

    private Optional<Handler> handler = Optional.empty();
    private Node.Listener sendMsgListener;

    public MultiNodesModel(NetworkService.Config networkServiceConfig, Set<Transport.Type> supportedTransportTypes,
                           boolean bootstrapAll) {
        this.networkServiceConfig = cloneWithLimitedSeeds(networkServiceConfig, numSeeds, supportedTransportTypes);
        this.supportedTransportTypes = supportedTransportTypes;
        this.bootstrapAll = bootstrapAll;

        seedAddressesByTransport = networkServiceConfig.seedAddressesByTransport();
    }


    public Map<Transport.Type, List<Address>> bootstrap(Optional<List<Address>> addressesToBootstrap) {
        int delay = supportedTransportTypes.contains(Transport.Type.TOR) || supportedTransportTypes.contains(Transport.Type.I2P) ?
                1000 :
                20;
        this.addressesToBootstrap = addressesToBootstrap;
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


    public List<Address> getSeedAddresses(Transport.Type transportType, int numSeeds) {
        return seedAddressesByTransport.get(transportType).stream().limit(numSeeds).collect(Collectors.toList());
    }

    public void bootstrap(Address address, Transport.Type transportType) {
        NetworkService networkService = createNetworkService(address, transportType);
        networkService.bootstrap(address.getPort())
                .whenComplete((r, t) -> handler.ifPresent(handler ->
                        handler.onStateChange(address, networkService.getStateByTransportType().get(transportType))));
    }

    public CompletableFuture<List<Void>> shutdown() {
        Set<Address> addresses = new HashSet<>(networkServicesByAddress.keySet());
        return CompletableFutureUtils.allOf(addresses.stream().map(this::shutdown));
    }

    public CompletableFuture<Void> shutdown(Address address) {
        handler.ifPresent(handler -> handler.onStateChange(address, ServiceNode.State.SHUTDOWN_STARTED));
        return findNetworkService(address)
                .map(networkService -> networkService.shutdown()
                        .whenComplete((__, t) -> {
                            networkServicesByAddress.remove(address);
                            handler.ifPresent(handler -> {
                                ServiceNode.State networkServiceState = supportedTransportTypes.stream()
                                        .map(e -> networkService.getStateByTransportType().get(e)).findAny().orElseThrow();
                                handler.onStateChange(address, networkServiceState);
                            });
                        }))
                .orElse(CompletableFuture.completedFuture(null));
    }

    public void send(Address senderAddress, Address receiverAddress, String nodeId, String message) {
        KeyPairService sendersKeyPairService = keyPairServicesByAddress.get(senderAddress);
        KeyPairService receiversKeyPairService = keyPairServicesByAddress.get(senderAddress);
        String senderKeyId = senderAddress + nodeId;
        KeyPair senderKeyPair = sendersKeyPairService.getOrCreateKeyPair(senderKeyId);
        String receiverKeyId = receiverAddress + nodeId;
        KeyPair receiverKeyPair = receiversKeyPairService.getOrCreateKeyPair(receiverKeyId);
        NetworkId receiverNetworkId = new NetworkId(Map.of(Transport.Type.from(receiverAddress), receiverAddress),
                new PubKey(receiverKeyPair.getPublic(), receiverKeyId),
                nodeId);
        NetworkId senderNetworkId = new NetworkId(Map.of(Transport.Type.from(senderAddress), senderAddress),
                new PubKey(senderKeyPair.getPublic(), senderKeyId),
                nodeId);
        send(senderNetworkId, receiverNetworkId, senderKeyPair, message);
    }

    private void send(NetworkId senderNetworkId, NetworkId receiverNetworkId, KeyPair senderKeyPair, String message) {
        NetworkService senderNetworkService = senderNetworkId.addressByNetworkType().entrySet().stream()
                .map(e -> getOrCreateNetworkService(e.getValue(), e.getKey()))
                .findAny()
                .get();
        NetworkService receiverNetworkService = receiverNetworkId.addressByNetworkType().entrySet().stream()
                .map(e -> getOrCreateNetworkService(e.getValue(), e.getKey()))
                .findAny()
                .get();
        receiverNetworkService.findMyAddresses().forEach((type, value) -> {
            Address receiverAddress = value.get(receiverNetworkId.getNodeId());
            sendMsgListener = new Node.Listener() {
                @Override
                public void onMessage(Message message, Connection connection, String nodeId) {
                    String newLine = "\n" + getTimestamp() + " " +
                            type.toString().substring(0, 3) + "  onReceived   " +
                            connection.getPeerAddress() + " --> " + receiverAddress + " " + message.toString();
                    appendToHistory(receiverAddress, newLine);
                    handler.ifPresent(handler -> handler.onMessage(receiverAddress));
                    receiverNetworkService.removeMessageListener(sendMsgListener);
                }

                @Override
                public void onConnection(Connection connection) {
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                }
            };
            receiverNetworkService.addMessageListener(sendMsgListener);
        });

        MockMailBoxMessage mailBoxMessage = new MockMailBoxMessage(message);
        senderNetworkService.confidentialSendAsync(mailBoxMessage,
                        receiverNetworkId,
                        senderKeyPair,
                        senderNetworkId.getNodeId())
                .whenComplete((result, throwable) -> {
                    senderNetworkService.findMyAddresses().forEach((type, value) -> {
                        Address senderAddress = value.get(senderNetworkId.getNodeId());
                        String newLine = "\n" + getTimestamp() + " " +
                                type.toString().substring(0, 3) + "  onSent       " +
                                senderAddress + " --> " + receiverNetworkId.addressByNetworkType().get(type) + " " +
                                mailBoxMessage + ", Result: " + result.get(type);
                        appendToHistory(senderAddress, newLine);
                        handler.ifPresent(handler -> handler.onMessage(senderAddress));
                    });
                });
    }

    private void appendToHistory(Address address, String newLine) {
        String prev = logHistoryByAddress.get(address);
        if (prev == null) {
            prev = "";
        }
        logHistoryByAddress.put(address, prev + newLine);
    }

    private NetworkService getOrCreateNetworkService(Address address, Transport.Type transportType) {
        if (networkServicesByAddress.containsKey(address)) {
            return networkServicesByAddress.get(address);
        } else {
            return createNetworkService(address, transportType);
        }
    }

    private NetworkService createNetworkService(Address address, Transport.Type transportType) {
        // We use a different dirPath per node so we have to recreate the NetworkService.Config
        String dirPath = networkServiceConfig.baseDir() + File.separator + address.getPort();
        Transport.Config transportConfig = new Transport.Config(dirPath);
        NetworkService.Config specificNetworkServiceConfig = new NetworkService.Config(dirPath,
                transportConfig,
                networkServiceConfig.supportedTransportTypes(),
                networkServiceConfig.serviceNodeConfig(),
                networkServiceConfig.peerGroupServiceConfigByTransport(),
                networkServiceConfig.seedAddressesByTransport(),
                Optional.empty());

        PersistenceService persistenceService = new PersistenceService(specificNetworkServiceConfig.baseDir());
        KeyPairService keyPairService = new KeyPairService(persistenceService);
        keyPairServicesByAddress.put(address, keyPairService);
        keyPairService.initialize().join();
        NetworkService networkService = new NetworkService(specificNetworkServiceConfig, keyPairService, persistenceService);
        handler.ifPresent(handler -> handler.onStateChange(address, networkService.getStateByTransportType().get(transportType)));
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

        networkService.addDataServiceListener(new DataService.Listener() {
            @Override
            public void onNetworkDataAdded(NetworkPayload networkPayload) {
                onNetworkDataChanged(networkService, networkPayload, transportType, true);
            }

            @Override
            public void onNetworkDataRemoved(NetworkPayload networkPayload) {
                onNetworkDataChanged(networkService, networkPayload, transportType, false);
            }
        });

    }

    private void onNetworkDataChanged(NetworkService networkService,
                                      NetworkPayload networkPayload,
                                      Transport.Type transportType,
                                      boolean wasAdded) {
        StringBuilder sb = new StringBuilder("\n");
        Address address = networkService.findMyDefaultAddress(transportType).get();
        sb.append(getTimestamp())
                .append(" ").append(transportType.name(), 0, 3)
                .append(wasAdded ? " +onDataAdded " : " -onDataRemoved ")
                .append(address)
                .append(" networkPayload=").append(networkPayload);
        String newLine = sb.toString();
        appendToHistory(address, newLine);
        handler.ifPresent(handler -> handler.onData(address, networkPayload));
    }

    private void onConnectionStateChanged(Transport.Type transportType, Connection connection, Node node, Optional<CloseReason> closeReason) {
        node.findMyAddress().ifPresent(address -> {
            StringBuilder sb = new StringBuilder("\n");
            sb.append(getTimestamp())
                    .append(" ").append(transportType.name(), 0, 3)
                    .append(closeReason.isPresent() ? " -onDisconnect " : " +onConnection ")
                    .append(node)
                    .append(connection.isOutboundConnection() ? " --> " : " <-- ")
                    .append(connection.getPeerAddress().toString().replace("]", connection.isPeerAddressVerified() ? " !]" : " ?]"))
                    .append(closeReason.map(r -> ", " + r).orElse(""));
            String newLine = sb.toString();
            appendToHistory(address, newLine);
            handler.ifPresent(handler -> handler.onConnectionStateChange(transportType, address, newLine));
        });
    }

    private String getTimestamp() {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
    }

    public String getNodeInfo(Address address, Transport.Type transportType) {
        return findNetworkService(address)
                .flatMap(networkService -> networkService.findServiceNode(transportType))
                .filter(serviceNode -> serviceNode.getMonitorService().isPresent())
                .map(serviceNode -> {
                    String peerGroupInfo = serviceNode.getMonitorService().get().getPeerGroupInfo();
                    String connectionHistory = Optional.ofNullable(logHistoryByAddress.get(address))
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

    private NetworkService.Config cloneWithLimitedSeeds(NetworkService.Config networkServiceConfig,
                                                        int numSeeds,
                                                        Set<Transport.Type> supportedTransportTypes) {

        Map<Transport.Type, List<Address>> seeds = networkServiceConfig.seedAddressesByTransport().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream().limit(numSeeds).collect(Collectors.toList())));
        return new NetworkService.Config(networkServiceConfig.baseDir(),
                networkServiceConfig.transportConfig(),
                supportedTransportTypes,
                networkServiceConfig.serviceNodeConfig(),
                networkServiceConfig.peerGroupServiceConfigByTransport(),
                seeds,
                networkServiceConfig.socks5ProxyAddress());
    }

    public List<Address> getNodeAddresses(Transport.Type transportType, int numNodes) {
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