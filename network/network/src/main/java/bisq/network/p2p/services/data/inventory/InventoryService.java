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

import bisq.common.data.ByteArray;
import bisq.common.timer.Scheduler;
import bisq.common.util.ByteUnit;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.peergroup.Peer;
import bisq.network.p2p.services.peergroup.PeerGroupManager;
import bisq.network.p2p.services.peergroup.PeerGroupService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages Inventory data requests and response and apply it to the data service.
 * We have InventoryServices for each supported transport. The data service though is a single instance getting services
 * by all transport specific services.
 *
 * TODO Find better solution for getting the diff of the missing data. See https://github.com/bisq-network/bisq2/issues/1602
 */
@Slf4j
public class InventoryService implements Node.Listener, PeerGroupManager.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    @Getter
    public static final class Config {
        // Default config value is 2000 (about 2MB)
        private final int maxSizeInKb;

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getInt("maxSizeInKb"));
        }

        public Config(int maxSizeInKb) {
            this.maxSizeInKb = maxSizeInKb;
        }
    }

    private final int maxSize;
    private final Node node;
    private final PeerGroupManager peerGroupManager;
    private final PeerGroupService peerGroupService;
    private final StorageService storageService;
    private final DataService dataService;
    private final Map<String, InventoryHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private boolean requestsPending;

    public InventoryService(Config config,
                            Node node,
                            PeerGroupManager peerGroupManager,
                            DataService dataService) {
        maxSize = (int) Math.round(ByteUnit.KB.toBytes(config.getMaxSizeInKb()));
        this.node = node;
        this.peerGroupManager = peerGroupManager;
        peerGroupService = peerGroupManager.getPeerGroupService();
        this.dataService = dataService;
        storageService = dataService.getStorageService();

        node.addListener(this);
        peerGroupManager.addListener(this);

        Inventory.setMaxSize(maxSize);
    }

    public void shutdown() {
        node.removeListener(this);
        peerGroupManager.removeListener(this);
        requestHandlerMap.values().forEach(InventoryHandler::dispose);
        requestHandlerMap.clear();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof InventoryRequest) {
            InventoryRequest request = (InventoryRequest) envelopePayloadMessage;

            log.info("Received a InventoryRequest with {} filter entries and {} kb from peer {}",
                    request.getDataFilter().getFilterEntries().size(),
                    ByteUnit.BYTE.toKB(request.getDataFilter().toProto().getSerializedSize()), connection.getPeerAddress());
            Inventory inventory = getInventory(request.getDataFilter());
            NetworkService.NETWORK_IO_POOL.submit(() -> node.send(new InventoryResponse(inventory, request.getNonce()), connection));
        }
    }

    @Override
    public void onConnection(Connection connection) {
        if (sufficientConnections()) {
            log.info("We are sufficiently connected to start the inventory request. numConnections={}",
                    node.getNumConnections());
            doRequest();
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = connection.getId();
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
            log.info("PeerGroupManager is ready. We start the inventory request.");
            doRequest();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Request inventory
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void doRequest() {
        if (!requestsPending && peerGroupManager.getState().get() == PeerGroupManager.State.RUNNING) {
            requestsPending = true;
            List<FilterEntry> filterEntries = storageService.getAllDataRequestMapEntries()
                    .map(this::toFilterEntry)
                    .collect(Collectors.toList());
            if (filterEntries.size() > DataFilter.MAX_ENTRIES) {
                filterEntries = filterEntries.stream().limit(DataFilter.MAX_ENTRIES).collect(Collectors.toList());
                // TODO Find better solution for getting the diff of the missing data. See https://github.com/bisq-network/bisq2/issues/1602
                log.warn("We limited the number of filter entries we send in our inventory request to {}",
                        DataFilter.MAX_ENTRIES);
            }
            DataFilter dataFilter = new DataFilter(filterEntries);
            CompletableFutureUtils.allOf(request(dataFilter))
                    .whenComplete((list, throwable) -> {
                        if (list != null) {
                            // Repeat requests until we have received all data
                            if (list.stream().noneMatch(Inventory::noDataMissing)) {
                                requestsPending = false;
                                Scheduler.run(this::doRequest).after(1000);
                            }
                        }
                    });
        } else {
            Scheduler.run(this::doRequest).after(5000);
        }
    }

    private List<CompletableFuture<Inventory>> request(DataFilter dataFilter) {
        int maxSeeds = 2;
        int maxCandidates = 4;
        List<Address> candidates = peerGroupService.getAllConnectedPeers(node)
                .filter(peerGroupService::isSeed)
                .limit(maxSeeds)
                .map(Peer::getAddress)
                .collect(Collectors.toList());
        candidates.addAll(peerGroupService.getAllConnectedPeers(node)
                .filter(peerGroupService::notASeed)
                .map(Peer::getAddress)
                .collect(Collectors.toList()));
        return node.getAllActiveConnections()
                .filter(connection -> !requestHandlerMap.containsKey(connection.getId()))
                .filter(connection -> candidates.contains(connection.getPeerAddress()))
                .limit(maxCandidates)
                .map(connection -> {
                    String key = connection.getId();
                    InventoryHandler handler = new InventoryHandler(node, connection);
                    requestHandlerMap.put(key, handler);
                    return handler.request(dataFilter)
                            .orTimeout(TIMEOUT, TimeUnit.SECONDS)
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
                            });
                })
                .collect(Collectors.toList());
    }

    private boolean sufficientConnections() {
        return node.getNumConnections() > peerGroupService.getTargetNumConnectedPeers() / 2;
    }

    private FilterEntry toFilterEntry(Map.Entry<ByteArray, ? extends DataRequest> mapEntry) {
        DataRequest dataRequest = mapEntry.getValue();
        int sequenceNumber = 0;
        byte[] hash = mapEntry.getKey().getBytes();
        if (dataRequest instanceof AddAppendOnlyDataRequest) {
            // AddAppendOnlyDataRequest does not use a seq nr.
            return new FilterEntry(hash, 0);
        } else if (dataRequest instanceof AddAuthenticatedDataRequest) {
            // AddMailboxRequest extends AddAuthenticatedDataRequest so its covered here as well
            sequenceNumber = ((AddAuthenticatedDataRequest) dataRequest).getAuthenticatedSequentialData().getSequenceNumber();
        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest) {
            // RemoveMailboxRequest extends RemoveAuthenticatedDataRequest so its covered here as well
            sequenceNumber = ((RemoveAuthenticatedDataRequest) dataRequest).getSequenceNumber();
        }
        return new FilterEntry(hash, sequenceNumber);
    }


    private Inventory getInventory(DataFilter dataFilter) {
        final AtomicInteger accumulatedSize = new AtomicInteger();
        final AtomicBoolean maxSizeReached = new AtomicBoolean();
        List<DataRequest> dataRequests = getAuthenticatedDataRequests(dataFilter, accumulatedSize, maxSizeReached);

        if (!maxSizeReached.get()) {
            dataRequests.addAll(getMailboxRequests(dataFilter, accumulatedSize, maxSizeReached));
        }

        if (!maxSizeReached.get()) {
            dataRequests.addAll(getAppendOnlyDataRequests(dataFilter, accumulatedSize, maxSizeReached));
        }

        log.info("Inventory with {} items and accumulatedSize of {} kb. maxSizeReached={}",
                dataRequests.size(), ByteUnit.BYTE.toKB(accumulatedSize.get()), maxSizeReached.get());
        return new Inventory(dataRequests, maxSizeReached.get());
    }

    private List<DataRequest> getAuthenticatedDataRequests(DataFilter dataFilter,
                                                           AtomicInteger accumulatedSize,
                                                           AtomicBoolean maxSizeReached) {
        List<AddAuthenticatedDataRequest> addRequests = new ArrayList<>();
        List<RemoveAuthenticatedDataRequest> removeRequests = new ArrayList<>();
        storageService.getAuthenticatedDataStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (!dataFilter.getFilterEntries().contains(toFilterEntry(mapEntry))) {
                        AuthenticatedDataRequest dataRequest = mapEntry.getValue();
                        if (dataRequest instanceof AddAuthenticatedDataRequest) {
                            addRequests.add((AddAuthenticatedDataRequest) dataRequest);
                        } else if (dataRequest instanceof RemoveAuthenticatedDataRequest) {
                            removeRequests.add((RemoveAuthenticatedDataRequest) dataRequest);
                        }
                        // Refresh is ignored
                    }
                });

        List<DataRequest> sortedAndFilteredRequests = addRequests.stream()
                .sorted((o1, o2) -> Integer.compare(o2.getAuthenticatedSequentialData().getAuthenticatedData().getDistributedData().getMetaData().getPriority(),
                        o1.getAuthenticatedSequentialData().getAuthenticatedData().getDistributedData().getMetaData().getPriority()))
                .filter(request -> {
                    if (!maxSizeReached.get()) {
                        maxSizeReached.set(accumulatedSize.addAndGet(request.toProto().getSerializedSize()) > maxSize);
                    }
                    return !maxSizeReached.get();
                })
                .collect(Collectors.toList());


        if (!maxSizeReached.get()) {
            sortedAndFilteredRequests.addAll(removeRequests.stream()
                    .sorted((o1, o2) -> Integer.compare(o2.getMetaData().getPriority(), o1.getMetaData().getPriority()))
                    .filter(request -> {
                        if (!maxSizeReached.get()) {
                            maxSizeReached.set(accumulatedSize.addAndGet(request.toProto().getSerializedSize()) > maxSize);
                        }
                        return !maxSizeReached.get();
                    })
                    .collect(Collectors.toList()));
        }
        return sortedAndFilteredRequests;
    }

    private List<DataRequest> getMailboxRequests(DataFilter dataFilter,
                                                 AtomicInteger accumulatedSize,
                                                 AtomicBoolean maxSizeReached) {
        List<AddMailboxRequest> addRequests = new ArrayList<>();
        List<RemoveMailboxRequest> removeRequests = new ArrayList<>();
        storageService.getMailboxStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (!dataFilter.getFilterEntries().contains(toFilterEntry(mapEntry))) {
                        MailboxRequest dataRequest = mapEntry.getValue();
                        if (dataRequest instanceof AddMailboxRequest) {
                            addRequests.add((AddMailboxRequest) dataRequest);
                        } else if (dataRequest instanceof RemoveMailboxRequest) {
                            removeRequests.add((RemoveMailboxRequest) dataRequest);
                        }
                    }
                });
        List<DataRequest> sortedAndFilteredRequests = addRequests.stream()
                .sorted((o1, o2) -> Integer.compare(o2.getMailboxSequentialData().getMailboxData().getMetaData().getPriority(),
                        o1.getMailboxSequentialData().getMailboxData().getMetaData().getPriority()))
                .filter(request -> {
                    if (!maxSizeReached.get()) {
                        maxSizeReached.set(accumulatedSize.addAndGet(request.toProto().getSerializedSize()) > maxSize);
                    }
                    return !maxSizeReached.get();
                })
                .collect(Collectors.toList());

        if (!maxSizeReached.get()) {
            sortedAndFilteredRequests.addAll(removeRequests.stream()
                    .sorted((o1, o2) -> Integer.compare(o2.getMetaData().getPriority(), o1.getMetaData().getPriority()))
                    .filter(request -> {
                        if (!maxSizeReached.get()) {
                            maxSizeReached.set(accumulatedSize.addAndGet(request.toProto().getSerializedSize()) > maxSize);
                        }
                        return !maxSizeReached.get();
                    })
                    .collect(Collectors.toList()));
        }
        return sortedAndFilteredRequests;
    }

    private List<DataRequest> getAppendOnlyDataRequests(DataFilter dataFilter,
                                                        AtomicInteger accumulatedSize,
                                                        AtomicBoolean maxSizeReached) {
        return storageService.getAddAppendOnlyDataStoreMaps().flatMap(map -> map.entrySet().stream())
                .filter(mapEntry -> dataFilter.getFilterEntries().contains(toFilterEntry(mapEntry)))
                .map(Map.Entry::getValue)
                .sorted((o1, o2) -> Integer.compare(o2.getAppendOnlyData().getMetaData().getPriority(),
                        o1.getAppendOnlyData().getMetaData().getPriority()))
                .filter(request -> {
                    if (!maxSizeReached.get()) {
                        maxSizeReached.set(accumulatedSize.addAndGet(request.toProto().getSerializedSize()) > maxSize);
                    }
                    return !maxSizeReached.get();
                })
                .collect(Collectors.toList());
    }
}