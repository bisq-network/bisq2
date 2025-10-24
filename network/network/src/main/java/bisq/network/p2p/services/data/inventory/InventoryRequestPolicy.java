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

import bisq.common.network.Address;
import bisq.common.util.CollectionUtil;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.inventory.InventoryRequestPolicy.NextTaskAfterRequestCompleted.DO_NOTHING;
import static bisq.network.p2p.services.data.inventory.InventoryRequestPolicy.NextTaskAfterRequestCompleted.RETRY_REQUEST_WITH_NEW_CONNECTION;
import static bisq.network.p2p.services.data.inventory.InventoryRequestPolicy.NextTaskAfterRequestCompleted.RETRY_REQUEST_WITH_SAME_CONNECTION;
import static bisq.network.p2p.services.data.inventory.InventoryRequestPolicy.NextTaskAfterRequestCompleted.START_PERIODIC_REQUESTS;

/**
 * Encapsulates the policy logic for managing inventory requests in the InventoryService.
 * <p>
 * This class defines when and how inventory requests should be made, retried, or scheduled periodically.
 * It maintains references to configuration, current request state, and network services needed to make decisions.
 * </p>
 *
 * <h2>Policy Overview:</h2>
 * <ul>
 *   <li><b>When to initiate requests:</b> A request should be sent on a new connection only if
 *       all data has not yet been received, the connection is eligible, and the number of pending requests
 *       is below the configured threshold.</li>
 *   <li><b>Which connections to use:</b> Candidates are selected from shuffled seed and non-seed connections,
 *       limited by configured maximums. A connection is eligible if it is not currently processing a request
 *       and supports a preferred filter type.</li>
 *   <li><b>Retry behavior:</b> After a request completes, this class decides whether to retry using the same
 *       connection, a different connection, start periodic requests, or do nothing based on success, error state,
 *       and data completeness.</li>
 *   <li><b>Periodic request scheduling:</b> Periodic requests repeat at a configurable interval if all data
 *       has been received; otherwise, retries happen quickly (every 1 second) until completion.</li>
 *   <li><b>Request concurrency limits:</b> Ensures the number of concurrent pending requests stays below configured limits.</li>
 *   <li><b>Filter type preferences:</b> Requests are made only to peers supporting preferred inventory filter types,
 *       ordered by preference.</li>
 * </ul>
 *
 * <h2>State and Dependencies:</h2>
 * <ul>
 *   <li>{@link InventoryService.Config} is used for retrieving configuration parameters such as request intervals,
 *       max pending requests, and preferred filter types.</li>
 *   <li>{@link InventoryRequestModel} tracks current request futures, pending request counts, and all-data-received status.</li>
 *   <li>{@link Node} and {@link PeerGroupService} provide network context for selecting peer connections.</li>
 * </ul>
 *
 * <h2>Key Methods:</h2>
 * <ul>
 *   <li>{@link #shouldRequestOnNewConnection(Connection)} - Determines if a request should be initiated on a new connection.</li>
 *   <li>{@link #getCandidatesForPeriodicRequests()} - Retrieves a filtered and shuffled list of connections suitable for periodic requests.</li>
 *   <li>{@link #getFreshCandidate(Connection)} - Finds a candidate connection excluding a given connection.</li>
 *   <li>{@link #onRequestCompleted(Connection, Inventory, Throwable)} - Determines next action after a request completes based on outcome.</li>
 *   <li>{@link #getDelayForNextPeriodicRequests(List, Throwable, List)} - Computes delay before next periodic request based on completion status.</li>
 * </ul>
 */
@Slf4j
class InventoryRequestPolicy {
    enum NextTaskAfterRequestCompleted {
        START_PERIODIC_REQUESTS,
        RETRY_REQUEST_WITH_SAME_CONNECTION,
        RETRY_REQUEST_WITH_NEW_CONNECTION,
        DO_NOTHING
    }

    private final InventoryService.Config config;
    private final InventoryRequestModel inventoryRequestModel;
    private final InventoryFilterFactory inventoryFilterFactory;
    private final Node node;
    private final PeerGroupService peerGroupService;
    private final Set<Address> ignoredAddresses = new CopyOnWriteArraySet<>();

    InventoryRequestPolicy(InventoryService.Config config,
                           InventoryRequestModel inventoryRequestModel,
                           InventoryFilterFactory inventoryFilterFactory,
                           Node node,
                           PeerGroupService peerGroupService) {
        this.config = config;
        this.inventoryRequestModel = inventoryRequestModel;
        this.inventoryFilterFactory = inventoryFilterFactory;
        this.node = node;
        this.peerGroupService = peerGroupService;
    }

    boolean shouldRequestOnNewConnection(Connection connection) {
        return !inventoryRequestModel.getInitialInventoryRequestsCompleted().get() &&
                canUseCandidate(connection) &&
                isBelowMaxPendingRequests();
    }

    public void onAllConnectionsLost() {
        log.warn("We have lost all connections. " +
                "We reset initialInventoryRequestsCompleted to false so that we trigger " +
                "a new inventory request once we are reconnected.");
        inventoryRequestModel.getNumInventoryRequestsCompleted().set(0);
        inventoryRequestModel.getInitialInventoryRequestsCompleted().set(false);
    }


    List<Connection> getCandidatesForPeriodicRequests() {
        List<Connection> candidates = peerGroupService.getShuffledNonSeedConnections(node)
                .filter(this::canUseCandidate)
                .limit(config.getMaxPeersForRequest())
                .collect(Collectors.toCollection(ArrayList::new));

        List<Connection> seeds = peerGroupService.getShuffledSeedConnections(node)
                .filter(this::canUseCandidate)
                .limit(config.getMaxSeedsForRequest())
                .collect(Collectors.toCollection(ArrayList::new));
        if (!seeds.isEmpty()) {
            Connection selectedSeed = seeds.removeFirst();
            candidates.addAll(seeds);

            candidates = CollectionUtil.toShuffledList(candidates);
            candidates.addFirst(selectedSeed); // ensure seed at front
        }

        int limit = config.getMaxPendingRequestsAtPeriodicRequests();
        return candidates.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    Optional<Connection> getFreshCandidate(Connection excludedConnection) {
        return getCandidatesForPeriodicRequests().stream()
                .filter(connection -> !connection.getId().equals(excludedConnection.getId()))
                .findAny();
    }

    NextTaskAfterRequestCompleted onRequestCompleted(Connection connection, Inventory inventory, Throwable throwable) {
        boolean initialInventoryRequestsCompleted = inventoryRequestModel.getInitialInventoryRequestsCompleted().get();
        if (initialInventoryRequestsCompleted) {
            return DO_NOTHING;
        }

        boolean belowMaxPendingRequests = isBelowMaxPendingRequests();
        if (throwable != null) {
            return belowMaxPendingRequests ? RETRY_REQUEST_WITH_NEW_CONNECTION : DO_NOTHING;
        }

        boolean finalDataDelivered = inventory.finalDataDelivered();
        if (finalDataDelivered) {
            int numInventoryRequestsCompleted = inventoryRequestModel.getNumInventoryRequestsCompleted().incrementAndGet();
            inventoryRequestModel.getNumInventoryRequestsCompletedObservable().set(numInventoryRequestsCompleted);
            if (numInventoryRequestsCompleted >= config.getMinCompletedRequests()) {
                inventoryRequestModel.getInitialInventoryRequestsCompleted().set(true);
                // We consider initial inventory request completed.
                // There is no guarantee though that we really got all data. In case our peers had incomplete data
                // themselves, we are also left in an incomplete state, though with a healthy network that is rather
                // unlikely. Also with periodic requests we have good chances to pick up missing data at a bit later.
                return START_PERIODIC_REQUESTS;
            }
        }

        if (!belowMaxPendingRequests) {
            // Too many open requests...
            return DO_NOTHING;
        }

        if (inventory.getEntries().isEmpty() || !inventory.isMaxSizeReached()) {
            // Peers which deliver no entries might be bootstrapping, or the peer has no data which we have already.
            // Peers which have isMaxSizeReached=false have sent all data.
            // In all those cases we ignore them.
            ignoredAddresses.add(connection.getPeerAddress());
        }

        boolean canUseCandidate = canUseCandidate(connection);
        if (canUseCandidate && inventory.isMaxSizeReached() && !inventory.getEntries().isEmpty()) {
            // Peer has more data, we stick to that
            return RETRY_REQUEST_WITH_SAME_CONNECTION;
        } else {
            // Try new peer
            return RETRY_REQUEST_WITH_NEW_CONNECTION;
        }
    }

    long getDelayForNextPeriodicRequests(List<Inventory> results,
                                         Throwable throwable,
                                         List<Connection> candidates) {
        if (throwable != null || results == null) {
            log.info("Periodic batch request failed – retrying in 1 second");
            return 1000;
        }
        boolean finalDataDelivered = results.stream().anyMatch(Inventory::finalDataDelivered);
        if (finalDataDelivered) {
            long delay = config.getRepeatRequestInterval();
            log.info("We got {} requests completed and have all data received. " +
                            "We repeat requests in {} seconds",
                    candidates.size(), delay / 1000);
            return delay;
        } else if (!candidates.isEmpty()) {
            log.info("We got {} requests completed but data is still missing. " +
                    "We repeat requests in 1 second", candidates.size());
            return 1000;
        } else {
            log.info("We got no requests completed and data is still missing. " +
                    "We repeat requests in 1 minute");
            return 60_000;
        }
    }

    private boolean canUseCandidate(Connection connection) {
        return !ignoredAddresses.contains(connection.getPeerAddress()) &&
                !inventoryRequestModel.getRequestResponseHandler().hasPendingRequest(connection.getId()) &&
                inventoryFilterFactory.getPreferredFilterType(connection.getPeersCapability().getFeatures()).isPresent();
    }

    private boolean isBelowMaxPendingRequests() {
        int numPendingRequests = inventoryRequestModel.getRequestResponseHandler().getNumPendingRequests();
        inventoryRequestModel.getNumPendingRequestsObservable().set(numPendingRequests);
        return numPendingRequests < config.getMaxPendingRequests();
    }
}
