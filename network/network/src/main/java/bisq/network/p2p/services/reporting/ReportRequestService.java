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

package bisq.network.p2p.services.reporting;

import bisq.common.network.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Sends a request for NetworkLoad to our peers. We add our own NetworkLoad in the request.
 * We do not user a config here as we want to have the same behaviour in the network to avoid stale networkLoad states.
 */
@Slf4j
public class ReportRequestService implements Node.Listener {
    private static final long TIMEOUT_SEC = 60;

    private final Node node;
    private final Map<String, ReportHandler> requestHandlerMap = new ConcurrentHashMap<>();

    public ReportRequestService(Node node) {
        this.node = node;

        initialize();
    }

    public void initialize() {
        node.addListener(this);
    }

    public void shutdown() {
        node.removeListener(this);
        requestHandlerMap.values().forEach(ReportHandler::dispose);
        requestHandlerMap.clear();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Report> request(Address address) {
        Connection connection = node.getConnection(address);
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            log.info("requestHandlerMap contains {}. " +
                            "This is expected if the connection is still pending the response or the peer is not available " +
                            "but the timeout has not triggered an exception yet. We skip that request. Connection={}",
                    key, connection);
            return null;
        }
        ReportHandler handler = new ReportHandler(node, connection);
        requestHandlerMap.put(key, handler);
        return handler.request()
                .orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .whenComplete((report, throwable) -> {
                    log.error("storageReporting {}", report);
                    requestHandlerMap.remove(key);
                });
    }
}