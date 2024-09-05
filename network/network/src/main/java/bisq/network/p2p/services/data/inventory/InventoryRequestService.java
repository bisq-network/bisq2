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

package bisq.network.p2p.services.data.inventory;

import bisq.common.observable.Observable;
import bisq.common.timer.Scheduler;
import bisq.common.util.CollectionUtil;
import bisq.common.util.ExceptionUtil;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.inventory.filter.FilterService;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class InventoryRequestService implements Node.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(180);

    private final Node node;
    private final PeerGroupService peerGroupService;
    private final DataService dataService;
    private final Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices;
    private final InventoryService.Config config;
    @Getter
    private final Observable<Integer> numPendingRequests = new Observable<>(0);
    @Getter
    private final Observable<Boolean> allDataReceived = new Observable<>(false);
    private final Map<String, InventoryHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private Optional<Scheduler> periodicRequestScheduler = Optional.empty();
    private volatile boolean shutdownInProgress;

    public InventoryRequestService(Node node,
                                   PeerGroupManager peerGroupManager,
                                   DataService dataService,
                                   Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices,
                                   InventoryService.Config config) {
        this.node = node;
        peerGroupService = peerGroupManager.getPeerGroupService();
        this.dataService = dataService;
        this.supportedFilterServices = supportedFilterServices;
        this.config = config;

        node.addListener(this);
    }

    public void shutdown() {
        shutdownInProgress = true;
        node.removeListener(this);
        requestHandlerMap.values().forEach(InventoryHandler::dispose);
        periodicRequestScheduler.ifPresent(Scheduler::stop);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
    }

    @Override
    public void onConnection(Connection connection) {
        if (!allDataReceived.get() &&
                canUseCandidate(connection) &&
                requestHandlerMap.size() < config.getMaxPendingRequestsAtStartup()) {
            requestInventoryFromFreshConnection(connection);
        }
    }

    private void requestInventoryFromFreshConnection(Connection connection) {
        requestInventory(connection)
                .whenComplete((inventory, throwable) -> {
                    if (throwable != null) {
                        if (!shutdownInProgress) {
                            log.info("Exception at inventory request to peer {}: {}",
                                    connection.getPeerAddress().getFullAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                        }
                    } else {
                        if (!allDataReceived.get()) {
                            if (inventory.allDataReceived()) {
                                allDataReceived.set(true);
                                node.removeListener(this);
                                startPeriodicRequests(config.getRepeatRequestInterval());
                            } else {
                                // We use same connection for repeated request until we have all data
                                if (canUseCandidate(connection) &&
                                        requestHandlerMap.size() < config.getMaxPendingRequestsAtStartup()) {
                                    requestInventoryFromFreshConnection(connection);
                                }
                            }
                        }
                    }

                    // In case of an error or if we completed without all data received and no other request is
                    // open (unlikely) we request using 3 existing peers.
                    if (!allDataReceived.get() && requestHandlerMap.isEmpty()) {
                        getCandidatesForPeriodicRequests().stream().limit(3).forEach(this::requestInventory);
                    }
                });
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = getKey(connection);
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
            numPendingRequests.set(requestHandlerMap.size());
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Request inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Inventory> requestInventory(Connection connection) {
        return requestFromPeer(connection)
                .thenApply(inventory -> {
                    checkNotNull(inventory);
                    inventory.getEntries().forEach(dataRequest -> {
                        if (dataRequest instanceof AddDataRequest) {
                            dataService.processAddDataRequest((AddDataRequest) dataRequest, false);
                        } else if (dataRequest instanceof RemoveDataRequest) {
                            dataService.processRemoveDataRequest((RemoveDataRequest) dataRequest, false);
                        }
                    });
                    return inventory;
                });
    }

    private CompletableFuture<Inventory> requestFromPeer(Connection connection) {
        String key = getKey(connection);
        InventoryHandler handler = new InventoryHandler(node, connection);
        requestHandlerMap.put(key, handler);
        numPendingRequests.set(requestHandlerMap.size());
        List<Feature> peersFeatures = connection.getPeersCapability().getFeatures();
        InventoryFilterType inventoryFilterType = getPreferredFilterType(peersFeatures).orElseThrow(); // we filtered before for presence
        var filterService = supportedFilterServices.get(inventoryFilterType);
        return handler.request(filterService.getFilter())
                .orTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .whenComplete((inventory, throwable) -> {
                    if (throwable != null) {
                        handler.dispose();
                    }
                    requestHandlerMap.remove(key);
                    numPendingRequests.set(requestHandlerMap.size());
                });
    }

    private void startPeriodicRequests(long interval) {
        periodicRequestScheduler.ifPresent(Scheduler::stop);
        periodicRequestScheduler = Optional.of(Scheduler.run(this::periodicRequest)
                .host(this)
                .runnableName("periodicRequest")
                .after(interval));
    }

    private void periodicRequest() {
        List<Connection> candidatesForPeriodicRequests = getCandidatesForPeriodicRequests();
        int numCandidates = candidatesForPeriodicRequests.size();
        AtomicBoolean allDataReceived = new AtomicBoolean();
        AtomicInteger numCompleted = new AtomicInteger();
        candidatesForPeriodicRequests.forEach(connection -> requestInventory(connection)
                .whenComplete((inventory, throwable) -> {
                    if (throwable != null) {
                        log.info("Exception at periodic inventory request to peer {}: {}",
                                connection.getPeerAddress().getFullAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                    } else if (inventory.allDataReceived()) {
                        allDataReceived.set(true);
                    }
                    if (numCompleted.incrementAndGet() == numCandidates) {
                        if (allDataReceived.get()) {
                            long repeatRequestInterval = config.getRepeatRequestInterval();
                            log.info("We got {} requests completed and have all data received. " +
                                            "We repeat requests in {} seconds",
                                    numCandidates, repeatRequestInterval / 1000);
                            startPeriodicRequests(repeatRequestInterval);
                        } else {
                            log.info("We got {} requests completed but still data missing. " +
                                    "We repeat requests in 1 second", numCandidates);
                            startPeriodicRequests(1000);
                        }
                    }
                }));
    }

    private List<Connection> getCandidatesForPeriodicRequests() {
        Stream<Connection> seeds = peerGroupService.getShuffledSeedConnections(node)
                .limit(config.getMaxSeedsForRequest());
        Stream<Connection> peers = peerGroupService.getShuffledNonSeedConnections(node)
                .limit(config.getMaxPeersForRequest());
        int limit = config.getMaxPendingRequestsAtPeriodicRequests() - requestHandlerMap.size();
        return CollectionUtil.toShuffledList(Stream.concat(seeds, peers)).stream()
                .filter(this::canUseCandidate)
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Get first match with peers feature based on order of myPreferredFilterTypes
    private Optional<InventoryFilterType> getPreferredFilterType(List<Feature> peersFeatures) {
        List<InventoryFilterType> peersInventoryFilterTypes = toFilterTypes(peersFeatures);
        return config.getMyPreferredFilterTypes().stream()
                .filter(peersInventoryFilterTypes::contains)
                .findFirst();
    }

    private List<InventoryFilterType> toFilterTypes(List<Feature> features) {
        return features.stream()
                .flatMap(feature -> InventoryFilterType.fromFeature(feature).stream())
                .collect(Collectors.toList());
    }

    private boolean canUseCandidate(Connection connection) {
        return !requestHandlerMap.containsKey(getKey(connection)) &&
                getPreferredFilterType(connection.getPeersCapability().getFeatures()).isPresent();
    }

    private static String getKey(Connection connection) {
        return connection.getPeerAddress().getFullAddress();
    }
}