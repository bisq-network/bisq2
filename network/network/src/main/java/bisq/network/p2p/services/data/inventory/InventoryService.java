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

import bisq.common.data.ByteUnit;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.timer.Scheduler;
import bisq.common.util.CompletableFutureUtils;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.network.p2p.common.RequestResponseHandler;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Manages Inventory data requests and response and apply it to the data service.
 * We have InventoryServices for each supported transport. The data service though is a single instance getting services
 * by all transport specific services.
 * <p>
 * <p>
 * We request at startup from all new connections up to maxPendingRequests (5 by default).
 */
@Slf4j
public class InventoryService extends RequestResponseHandler<InventoryRequest, InventoryResponse> {
    private static final long TIMEOUT = SECONDS.toMillis(180);


    @Getter
    public static final class Config {
        private final int maxSizeInKb;  // Default config value is 2000 (about 2MB)
        private final long repeatRequestInterval; // Default 10 min
        private final int maxSeedsForRequest;
        private final int maxPeersForRequest;
        private final int maxPendingRequests; // Default 5
        private final int maxPendingRequestsAtPeriodicRequests; // Default 2
        private final List<InventoryFilterType> myPreferredFilterTypes; // Lower list index means higher preference

        public static Config from(com.typesafe.config.Config config) {
            return new Config(config.getInt("maxSizeInKb"),
                    SECONDS.toMillis(config.getLong("repeatRequestIntervalInSeconds")),
                    config.getInt("maxSeedsForRequest"),
                    config.getInt("maxPeersForRequest"),
                    config.getInt("maxPendingRequests"),
                    config.getInt("maxPendingRequestsAtPeriodicRequests"),
                    new ArrayList<>(config.getEnumList(InventoryFilterType.class, "myPreferredFilterTypes")));
        }

        public Config(int maxSizeInKb,
                      long repeatRequestInterval,
                      int maxSeedsForRequest,
                      int maxPeersForRequest,
                      int maxPendingRequests,
                      int maxPendingRequestsAtPeriodicRequests,
                      List<InventoryFilterType> myPreferredFilterTypes) {
            this.maxSizeInKb = maxSizeInKb;
            this.repeatRequestInterval = repeatRequestInterval;
            this.maxSeedsForRequest = maxSeedsForRequest;
            this.maxPeersForRequest = maxPeersForRequest;
            this.maxPendingRequests = maxPendingRequests;
            this.maxPendingRequestsAtPeriodicRequests = maxPendingRequestsAtPeriodicRequests;
            this.myPreferredFilterTypes = myPreferredFilterTypes;
        }
    }

    private final DataService dataService;
    private final InventoryFilterFactory inventoryFilterFactory;
    private final InventoryRequestModel model;
    private final InventoryRequestPolicy policy;

    private Optional<Scheduler> periodicRequestScheduler = Optional.empty();
    private volatile boolean shutdownInProgress;
    @Getter
    private final Config config;

    public InventoryService(Config config,
                            Node node,
                            PeerGroupManager peerGroupManager,
                            DataService dataService,
                            Set<Feature> myFeatures) {

        super(node, TIMEOUT);
        this.dataService = dataService;
        this.config = config;

        inventoryFilterFactory = new InventoryFilterFactory(myFeatures, dataService, config);
        model = new InventoryRequestModel(getRequestFuturesByConnectionId());
        policy = new InventoryRequestPolicy(config, model, inventoryFilterFactory, node, peerGroupManager.getPeerGroupService());
        initialize();
    }

    public void shutdown() {
        if (shutdownInProgress) {
            return;
        }
        shutdownInProgress = true;
        super.shutdown();
        periodicRequestScheduler.ifPresent(Scheduler::stop);
        periodicRequestScheduler = Optional.empty();
    }



    /* --------------------------------------------------------------------- */
    // Node.Listener implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void onConnection(Connection connection) {
        if (policy.shouldRequestOnNewConnection(connection)) {
            requestWithRetry(connection);
        }
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        // Might let fail a pending request
        updateNumPendingRequests();
    }


    /* --------------------------------------------------------------------- */
    // RequestResponseHandler implementation
    /* --------------------------------------------------------------------- */

    @Override
    protected Class<InventoryRequest> getRequestClass() {
        return InventoryRequest.class;
    }

    @Override
    protected Class<InventoryResponse> getResponseClass() {
        return InventoryResponse.class;
    }

    @Override
    protected InventoryResponse createResponse(Connection connection, InventoryRequest request) {
        Inventory inventory = inventoryFilterFactory.createInventoryForResponse(request);
        return new InventoryResponse(request.getVersion(), inventory, request.getNonce());
    }

    @Override
    protected void processRequest(Connection connection, InventoryRequest request) {
        InventoryFilter inventoryFilter = request.getInventoryFilter();
        double size = ByteUnit.BYTE.toKB(inventoryFilter.getSerializedSize());
        log.info("Received an InventoryRequest from peer {}. Size: {} kb. Filter details: {}",
                connection.getPeerAddress(), size, inventoryFilter.getDetails());

        onRequest(connection, request);
        long ts = System.currentTimeMillis();
        InventoryResponse response = createResponse(connection, request);
        node.sendAsync(response, connection)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Successfully sent an InventoryResponse to peer {} with {} kb. Took {} ms",
                                connection.getPeerAddress(),
                                ByteUnit.BYTE.toKB(response.getInventory().getSerializedSize()),
                                System.currentTimeMillis() - ts);
                    } else {
                        log.warn("Sending {} to {} failed. {}", StringUtils.truncate(response), connection.getPeerAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                    }
                });
    }

    @Override
    protected void processResponse(Connection connection, InventoryResponse response) {
        super.processResponse(connection, response);
        InventoryPrinter.print(response, connection, model.getRequestTimestampByConnectionId());
        model.getRequestTimestampByConnectionId().remove(connection.getId());
    }


    /* --------------------------------------------------------------------- */
    // Delegate
    /* --------------------------------------------------------------------- */

    public ReadOnlyObservable<Boolean> getAllDataReceived() {
        return model.getAllDataReceived();
    }

    public ReadOnlyObservable<Integer> getNumPendingRequests() {
        return model.getNumPendingRequests();
    }


    /* --------------------------------------------------------------------- */
    // Request inventory
    /* --------------------------------------------------------------------- */

    private void requestWithRetry(Connection connection) {
        request(connection)
                .whenComplete((inventory, throwable) -> {
                    if (shutdownInProgress) {
                        return;
                    }
                    if (throwable != null) {
                        log.info("Exception at inventory request to peer {}: {}",
                                connection.getPeerAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                    }

                    InventoryRequestPolicy.NextTaskAfterRequestCompleted nextTask = policy.onRequestCompleted(connection, inventory, throwable);
                    switch (nextTask) {
                        case START_PERIODIC_REQUESTS -> startPeriodicRequests(config.getRepeatRequestInterval());
                        case RETRY_REQUEST_WITH_SAME_CONNECTION -> requestWithRetry(connection);
                        case RETRY_REQUEST_WITH_NEW_CONNECTION ->
                                policy.getFreshCandidate(connection).ifPresent(this::requestWithRetry);
                        case DO_NOTHING -> {
                        }
                    }
                });
    }

    private CompletableFuture<Inventory> request(Connection connection) {
        InventoryFilter inventoryFilter = inventoryFilterFactory.createInventoryFilterForRequest(connection);
        InventoryRequest request = new InventoryRequest(inventoryFilter, createNonce());
        model.getRequestTimestampByConnectionId().put(connection.getId(), System.currentTimeMillis());
        CompletableFuture<InventoryResponse> requestFuture = request(connection, request);
        updateNumPendingRequests();
        return requestFuture
                .whenComplete((r, throwable) -> {
                    updateNumPendingRequests();
                    // On exceptional completion we won't reach processResponse; ensure cleanup
                    if (throwable != null) {
                        model.getRequestTimestampByConnectionId().remove(connection.getId());
                    }
                })
                .thenApply(response -> {
                    Inventory inventory = response.getInventory();
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

    private void startPeriodicRequests(long interval) {
        if (shutdownInProgress) {
            return;
        }
        periodicRequestScheduler.ifPresent(Scheduler::stop);
        periodicRequestScheduler = Optional.of(Scheduler.run(this::periodicRequest)
                .host(this)
                .runnableName("periodicRequest")
                .after(interval));
    }

    private void periodicRequest() {
        List<Connection> candidates = policy.getCandidatesForPeriodicRequests();
        Stream<CompletableFuture<Inventory>> futures = candidates.stream().map(this::request);
        CompletableFutureUtils.allOf(futures)
                .whenComplete((results, throwable) -> {
                    if (shutdownInProgress) {
                        return;
                    }
                    long delay = policy.getDelayForPeriodicRequests(results, throwable, candidates);
                    startPeriodicRequests(delay);
                });
    }

    private void updateNumPendingRequests() {
        model.getNumPendingRequests().set(getRequestFuturesByConnectionId().size());
    }
}