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

import bisq.common.util.ByteUnit;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.filter.DataFilter;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
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

@Slf4j
public class InventoryService implements Node.Listener {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    @Getter
    public static final class Config {
        private final int maxSizeInKb;

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getInt("maxSizeInKb"));
        }

        public Config(int maxSizeInKb) {
            this.maxSizeInKb = maxSizeInKb;
        }
    }

    private final Node node;
    private final PeerGroupService peerGroupService;
    private final InventoryProvider inventoryProvider;
    private final int maxSize;
    private final Map<String, InventoryHandler> requestHandlerMap = new ConcurrentHashMap<>();

    public InventoryService(Config config, Node node, PeerGroupService peerGroupService, InventoryProvider inventoryProvider) {
        this.node = node;
        this.peerGroupService = peerGroupService;
        this.inventoryProvider = inventoryProvider;
        maxSize = (int) Math.round(ByteUnit.KB.toBytes(config.getMaxSizeInKb()));

        node.addListener(this);
    }

    public List<CompletableFuture<Inventory>> request(DataFilter dataFilter) {
        int maxRequests = 400;
        return peerGroupService.getAllConnections()
                .filter(connection -> !requestHandlerMap.containsKey(connection.getId()))
                .limit(maxRequests)
                .map(connection -> {
                    String key = connection.getId();
                    InventoryHandler handler = new InventoryHandler(node, connection);
                    requestHandlerMap.put(key, handler);
                    return handler.request(dataFilter)
                            .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                            .whenComplete((inventory, throwable) -> requestHandlerMap.remove(key));
                })
                .collect(Collectors.toList());
    }

    public void shutdown() {
        requestHandlerMap.values().forEach(InventoryHandler::dispose);
        requestHandlerMap.clear();
    }

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
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            requestHandlerMap.get(key).dispose();
            requestHandlerMap.remove(key);
        }
    }

    public Inventory getInventory(DataFilter dataFilter) {
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
        inventoryProvider.getAuthenticatedDataStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (!dataFilter.getFilterEntries().contains(inventoryProvider.getFilterEntry(mapEntry))) {
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
        inventoryProvider.getMailboxStoreMaps().flatMap(map -> map.entrySet().stream())
                .forEach(mapEntry -> {
                    if (!dataFilter.getFilterEntries().contains(inventoryProvider.getFilterEntry(mapEntry))) {
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
        return inventoryProvider.getAddAppendOnlyDataStoreMaps().flatMap(map -> map.entrySet().stream())
                .filter(mapEntry -> dataFilter.getFilterEntries().contains(inventoryProvider.getFilterEntry(mapEntry)))
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