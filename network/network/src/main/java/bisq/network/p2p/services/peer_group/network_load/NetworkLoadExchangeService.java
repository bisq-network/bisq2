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

package bisq.network.p2p.services.peer_group.network_load;

import bisq.common.threading.ThreadName;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.NetworkLoad;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Sends a request for NetworkLoad to our peers. We add our own NetworkLoad in the request.
 * We do not user a config here as we want to have the same behaviour in the network to avoid stale networkLoad states.
 */
@Slf4j
public class NetworkLoadExchangeService implements Node.Listener {
    private static final long TIMEOUT_SEC = 120;
    private static final long INITIAL_DELAY = TimeUnit.SECONDS.toMillis(10);
    private static final long INTERVAL = TimeUnit.MINUTES.toMillis(3);
    private static final long MAX_IDLE = TimeUnit.MINUTES.toMillis(5);

    private final Node node;
    private final Map<String, NetworkLoadExchangeHandler> requestHandlerMap = new ConcurrentHashMap<>();
    private Optional<Scheduler> scheduler = Optional.empty();

    public NetworkLoadExchangeService(Node node) {
        this.node = node;
        this.node.addListener(this);
    }

    public void initialize() {
        scheduler = Optional.of(Scheduler.run(this::requestFromAll)
                .host(this)
                .runnableName("requestFromAll")
                .periodically(INITIAL_DELAY, INTERVAL, TimeUnit.MILLISECONDS));
    }

    public void shutdown() {
        scheduler.ifPresent(Scheduler::stop);
        requestHandlerMap.values().forEach(NetworkLoadExchangeHandler::dispose);
        requestHandlerMap.clear();
    }

    private void requestFromAll() {
        node.getAllActiveConnections()
                .filter(this::needsUpdate)
                .forEach(this::request);
    }

    public void request(Connection connection) {
        String key = connection.getId();
        if (requestHandlerMap.containsKey(key)) {
            log.info("requestHandlerMap contains {}. " +
                            "This is expected if the connection is still pending the response or the peer is not available " +
                            "but the timeout has not triggered an exception yet. We skip that request. Connection={}",
                    key, connection);
            return;
        }
        NetworkLoadExchangeHandler handler = new NetworkLoadExchangeHandler(node, connection);
        requestHandlerMap.put(key, handler);
        handler.request()
                .orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .whenComplete((nil, throwable) -> requestHandlerMap.remove(key));
    }

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof NetworkLoadExchangeRequest request) {
            NetworkLoad peersNetworkLoad = request.getNetworkLoad();
            log.debug("Received NetworkLoadRequest with nonce {} and peers networkLoad {} from {}",
                    request.getNonce(), peersNetworkLoad, connection.getPeerAddress());
            connection.getPeersNetworkLoadSnapshot().updateNetworkLoad(peersNetworkLoad);
            NetworkLoad myNetworkLoad = node.getNetworkLoadSnapshot().getCurrentNetworkLoad();
            NetworkLoadExchangeResponse response = new NetworkLoadExchangeResponse(request.getNonce(),
                    myNetworkLoad);
            NetworkService.NETWORK_IO_POOL.submit(() -> {
                ThreadName.set(this, "response");
                node.send(response, connection);
            });
            log.debug("Sent NetworkLoadResponse with nonce {} and my networkLoad {} to {}. Connection={}",
                    request.getNonce(), myNetworkLoad, connection.getPeerAddress(), connection.getId());
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

    private boolean needsUpdate(Connection connection) {
        return System.currentTimeMillis() - connection.getPeersNetworkLoadSnapshot().getLastUpdated() > MAX_IDLE;
    }
}