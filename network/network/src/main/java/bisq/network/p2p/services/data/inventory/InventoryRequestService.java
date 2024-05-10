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

import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
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
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class InventoryRequestService implements Node.Listener, PeerGroupManager.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(120);

    private final Node node;
    private final PeerGroupManager peerGroupManager;
    private final PeerGroupService peerGroupService;
    private final DataService dataService;
    private final Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices;
    private final List<InventoryFilterType> myPreferredInventoryFilterTypes;
    private final long repeatRequestInterval;
    private final int maxSeedsForRequest;
    private final int maxPeersForRequest;
    private final Map<String, InventoryHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean requestsPending = new AtomicBoolean();
    private final AtomicBoolean allDataReceived = new AtomicBoolean();
    private Optional<Scheduler> scheduler = Optional.empty();
    private Optional<Scheduler> periodicScheduler = Optional.empty();

    public InventoryRequestService(Node node,
                                   PeerGroupManager peerGroupManager,
                                   DataService dataService,
                                   Map<InventoryFilterType, FilterService<? extends InventoryFilter>> supportedFilterServices,
                                   InventoryService.Config config) {
        this.node = node;
        this.peerGroupManager = peerGroupManager;
        peerGroupService = peerGroupManager.getPeerGroupService();
        this.dataService = dataService;
        this.supportedFilterServices = supportedFilterServices;
        myPreferredInventoryFilterTypes = config.getMyPreferredFilterTypes();
        repeatRequestInterval = config.getRepeatRequestInterval();
        maxSeedsForRequest = config.getMaxSeedsForRequest();
        maxPeersForRequest = config.getMaxPeersForRequest();
        node.addListener(this);
        peerGroupManager.addListener(this);
    }

    public void shutdown() {
        node.removeListener(this);
        peerGroupManager.removeListener(this);
        requestHandlerMap.values().forEach(InventoryHandler::dispose);
        requestHandlerMap.clear();
        scheduler.ifPresent(Scheduler::stop);
        scheduler = Optional.empty();
        periodicScheduler.ifPresent(Scheduler::stop);
        periodicScheduler = Optional.empty();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
    }

    @Override
    public void onConnection(Connection connection) {
        if (sufficientConnections()) {
            maybeRequestInventory();
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = getRequestHandlerMapKey(connection);
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PeerGroupManager.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onStateChanged(PeerGroupManager.State state) {
        if (state == PeerGroupManager.State.RUNNING) {
            maybeRequestInventory();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Request inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void maybeRequestInventory() {
        if (allDataReceived.get() || requestsPending.get()) {
            return;
        }

        if (peerGroupManager.getState().get() != PeerGroupManager.State.RUNNING) {
            // Not ready yet, lets try again later
            scheduler.ifPresent(Scheduler::stop);
            scheduler = Optional.of(Scheduler.run(this::maybeRequestInventory).after(5000));
            return;
        }

        log.info("Start inventory requests");
        requestsPending.set(true);
        CompletableFutureUtils.allOf(requestFromPeers())
                .whenComplete((list, throwable) -> {
                    requestsPending.set(false);
                    if (throwable != null) {
                        if (throwable instanceof CompletionException &&
                                throwable.getCause() instanceof CancellationException) {
                            log.debug("requestFromPeers failed", throwable);
                        } else {
                            log.error("requestFromPeers failed", throwable);
                        }
                    } else if (list == null) {
                        log.error("requestFromPeers completed with result list = null");
                    } else {
                        // Repeat requests until we have received all data
                        if (list.isEmpty()) {
                            log.info("No matching peers for request have been found. We try again in 10 sec.");
                            scheduler.ifPresent(Scheduler::stop);
                            scheduler = Optional.of(Scheduler.run(this::maybeRequestInventory).after(10000));
                        } else if (list.stream().noneMatch(Inventory::noDataMissing)) {
                            // We still miss data, so repeat requests
                            scheduler.ifPresent(Scheduler::stop);
                            scheduler = Optional.of(Scheduler.run(this::maybeRequestInventory).after(1000));
                        } else {
                            // We got all data
                            allDataReceived.set(true);

                            // We request again in 10 minutes to be sure that potentially missed data gets received.
                            if (periodicScheduler.isEmpty()) {
                                periodicScheduler = Optional.of(Scheduler.run(() -> {
                                    allDataReceived.set(false);
                                    maybeRequestInventory();
                                }).periodically(repeatRequestInterval));
                            }
                        }
                    }
                });
    }

    private List<CompletableFuture<Inventory>> requestFromPeers() {
        return getCandidates().stream()
                .map(connection -> {
                    // We need to handle requests from ourselves and those from our peer separate in case they happen on the same connection
                    // therefor we add the peer address
                    String key = getRequestHandlerMapKey(connection);
                    InventoryHandler handler = new InventoryHandler(node, connection);
                    requestHandlerMap.put(key, handler);
                    List<Feature> peersFeatures = connection.getPeersCapability().getFeatures();
                    InventoryFilterType inventoryFilterType = getPreferredFilterType(peersFeatures).orElseThrow(); // we filtered above for presence
                    var filterService = supportedFilterServices.get(inventoryFilterType);
                    return handler.request(filterService.getFilter())
                            .orTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                            .whenComplete((inventory, throwable) -> {
                                requestHandlerMap.remove(key);
                                if (inventory != null) {
                                    inventory.getEntries().forEach(dataRequest -> {
                                        if (dataRequest instanceof AddDataRequest) {
                                            dataService.processAddDataRequest((AddDataRequest) dataRequest, false);
                                        } else if (dataRequest instanceof RemoveDataRequest) {
                                            dataService.processRemoveDataRequest((RemoveDataRequest) dataRequest, false);
                                        }
                                    });
                                }
                                if (throwable != null) {
                                    if (throwable instanceof CancellationException) {
                                        log.debug("Inventory request failed.", throwable);
                                    } else {
                                        log.info("Inventory request failed.", throwable);
                                    }
                                }
                            });
                })
                .collect(Collectors.toList());
    }

    private List<Connection> getCandidates() {
        Stream<Connection> seeds = peerGroupService.getShuffledSeedConnections(node)
                .filter(connection -> !requestHandlerMap.containsKey(getRequestHandlerMapKey(connection)))
                .limit(maxSeedsForRequest);
        Stream<Connection> peers = peerGroupService.getShuffledNonSeedConnections(node)
                .filter(connection -> !requestHandlerMap.containsKey(getRequestHandlerMapKey(connection)))
                .limit(maxPeersForRequest);
        List<Connection> allConnections = Stream.concat(seeds, peers).collect(Collectors.toList());
        List<Connection> matchingConnections = allConnections.stream()
                .filter(connection -> getPreferredFilterType(connection.getPeersCapability().getFeatures()).isPresent())
                .collect(Collectors.toList());
        if (matchingConnections.isEmpty() && !allConnections.isEmpty()) {
            log.warn("We did not find any peer which matches our inventory filter type settings");
        }
        return matchingConnections;
    }

    private boolean sufficientConnections() {
        return node.getNumConnections() > peerGroupService.getTargetNumConnectedPeers() / 2;
    }

    // Get first match with peers feature based on order of myPreferredFilterTypes
    private Optional<InventoryFilterType> getPreferredFilterType(List<Feature> peersFeatures) {
        List<InventoryFilterType> peersInventoryFilterTypes = toFilterTypes(peersFeatures);
        return myPreferredInventoryFilterTypes.stream()
                .filter(peersInventoryFilterTypes::contains)
                .findFirst();
    }

    private List<InventoryFilterType> toFilterTypes(List<Feature> features) {
        return features.stream()
                .flatMap(feature -> InventoryFilterType.fromFeature(feature).stream())
                .collect(Collectors.toList());
    }

    private static String getRequestHandlerMapKey(Connection connection) {
        return connection.getPeerAddress().getFullAddress();
    }
}