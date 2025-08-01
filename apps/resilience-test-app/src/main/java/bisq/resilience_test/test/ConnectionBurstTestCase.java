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

package bisq.resilience_test.test;

import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.node.transport.TransportService;
import bisq.network.p2p.services.peer_group.BanList;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
public class ConnectionBurstTestCase extends BaseTestCase {

    private final NetworkService networkService;
    private final IdentityService identityService;
    private int connectionCount = 1000;
    private int socketSilenceInSecs = 10;
    private boolean doHandshake = false;
    private int silenceAfterHandshakeInSecs = 0;

    public ConnectionBurstTestCase(Optional<Config> optionalConfig,
                                   NetworkService networkService,
                                   IdentityService identityService) {
        super(optionalConfig);
        this.networkService = networkService;
        this.identityService = identityService;
        optionalConfig.ifPresent(config -> {
            if (config.hasPath("connectionCount")) {
                connectionCount = config.getInt("connectionCount");
            }
            if (config.hasPath("socketSilenceInSecs")) {
                socketSilenceInSecs = config.getInt("socketSilenceInSecs");
            }
            if (config.hasPath("doHandshake")) {
                doHandshake = config.getBoolean("doHandshake");
            }
            if (config.hasPath("silenceAfterHandshakeInSecs")) {
                silenceAfterHandshakeInSecs = config.getInt("silenceAfterHandshakeInSecs");
            }
        });
    }

    protected void run() {
        var capsMap = getAllTransportCapabilities();
        if (!isCapabilitiesPresent(capsMap)) {
            log.info("ConnectionBurst: our Capabilities for default nodes are not present yet");
            return;
        }
        // Convert to HashMap<TransportType, Capability>
        HashMap<TransportType, Capability> capsMapPresent = new HashMap<>();
        capsMap.forEach((key, value) ->
                capsMapPresent.put(key, value.get()));

        HashMap<TransportService, HashSet<Address>> serviceMap = new HashMap<>();
        try {
            networkService.getServiceNodesByTransport()
                    .getAllServiceNodes().stream().forEach(node -> {
                        TransportService transportService = node.getTransportService();
                        List<Address> address = node.getDefaultNode()
                                .getAllConnections()
                                .parallel()
                                .map(Connection::getPeerAddress).toList();
                        synchronized (serviceMap) {
                            serviceMap.computeIfAbsent(transportService, k -> new HashSet<>()).addAll(address);
                        }
                    });
        } catch (Exception e) {
            log.error("ConnectionBurst: Error", e);
            return;
        }
        serviceMap.entrySet().stream().parallel().forEach(entry -> {
            var transportService = entry.getKey();
            var addressList = entry.getValue();

            addressList.stream().parallel().forEach(address -> {
                IntStream.range(0, connectionCount).parallel().forEach(i -> {
                    Socket socket;
                    try {
                        socket = transportService.getSocket(address); // blocking, and we are in ForkJoinPool.
                    } catch (IOException e) {
                        log.info("ConnectionBurst: failed to getSocket of `{}`", address.toString(), e);
                        return;
                    }

                    if (socketSilenceInSecs > 0) {
                        CompletableFuture<Void> delay = CompletableFuture.runAsync(() -> {
                                },
                                CompletableFuture.delayedExecutor(socketSilenceInSecs, TimeUnit.SECONDS)
                        );
                        try {
                            delay.join();
                        } catch (Exception e) {
                            log.error("ConnectionBurst: interrupted while waiting", e.getCause());
                            return;
                        }
                    }

                    if (doHandshake) {
                        try {
                            var myCapability = capsMapPresent.get(transportService.getTransportType());

                            ConnectionHandshake connectionHandshake = new ConnectionHandshake(socket,
                                    new BanList(),
                                    myCapability,
                                    networkService.getServiceNodesByTransport().getAuthorizationService(),
                                    identityService.getOrCreateDefaultIdentity().getKeyBundle());
                            log.debug("Outbound handshake started: Initiated by {} to {}", myCapability.getAddress(), address);
                            connectionHandshake.start(new NetworkLoadSnapshot().getCurrentNetworkLoad(), address); // Blocking call
                        } catch (Exception e) {
                            log.error("ConnectionBurst: Error at performing handshake", e);
                        }
                        if (silenceAfterHandshakeInSecs > 0) {
                            try {
                                Thread.sleep(silenceAfterHandshakeInSecs * 1000L);
                            } catch (InterruptedException e) {
                                log.error("ConnectionBurst: we got interrupted while sleeping for handshake silence");
                            }
                        }
                    }

                    try {
                        socket.close();
                    } catch (IOException e) {
                        //ignore
                    }
                });
            });
        });
    }

    private HashMap<TransportType, Optional<Capability>> getAllTransportCapabilities() {
        return networkService.getServiceNodesByTransport()
                .getAllServiceNodes().stream()
                .collect(HashMap::new,
                        (map, sn) ->
                                map.put(sn.getTransportType(), sn.getDefaultNode().getMyCapability()),
                        HashMap::putAll);
    }

    private boolean isCapabilitiesPresent(HashMap<TransportType, Optional<Capability>> typeCapabilities) {
        return typeCapabilities.values().stream().allMatch(Optional::isPresent);
    }
}