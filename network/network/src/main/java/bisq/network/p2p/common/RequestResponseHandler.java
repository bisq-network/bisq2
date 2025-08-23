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

package bisq.network.p2p.common;

import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public abstract class RequestResponseHandler<T extends Request, R extends Response> implements Node.Listener {
    protected final Node node;
    protected final long timeout;
    protected final Map<String, RequestFuture<T, R>> requestFuturesByConnectionId = new ConcurrentHashMap<>();

    public RequestResponseHandler(Node node, long timeout) {
        this.node = node;
        this.timeout = timeout;
    }

    public void initialize() {
        node.addListener(this);
    }

    public void shutdown() {
        node.removeListener(this);
        requestFuturesByConnectionId.values().forEach(handler -> handler.cancel(true));
        requestFuturesByConnectionId.clear();
    }

    protected abstract R createResponse(Connection connection, T request);

    protected abstract Class<T> getRequestClass();

    protected abstract Class<R> getResponseClass();

    protected Optional<T> resolveRequest(EnvelopePayloadMessage message) {
        Class<T> requestClass = getRequestClass();
        return requestClass.isInstance(message)
                ? Optional.of(requestClass.cast(message))
                : Optional.empty();
    }

    protected Optional<R> resolveResponse(EnvelopePayloadMessage message) {
        Class<R> responseClass = getResponseClass();
        return responseClass.isInstance(message)
                ? Optional.of(responseClass.cast(message))
                : Optional.empty();
    }

    // If we get called on a connection we have already assigned a request for, we ignore the new request and return
    // the future from the pending request.
    protected CompletableFuture<R> request(Connection connection, T request) {
        return requestFuturesByConnectionId.compute(connection.getId(), (k, existing) -> {
            if (existing != null && !existing.isDone()) {
                log.warn("[{}]Reusing pending request future for {}", this.getClass().getSimpleName(), connection.getPeerAddress());
                return existing;
            }
            RequestFuture<T, R> requestFuture = new RequestFuture<>(node, connection, request);
            requestFuture.orTimeout(timeout, TimeUnit.MILLISECONDS)
                    .whenComplete((response, throwable) -> {
                        requestFuturesByConnectionId.remove(k);
                        if (throwable instanceof TimeoutException) {
                            log.warn("[{}]Request to {} timed out after {} ms", this.getClass().getSimpleName(), connection.getPeerAddress(), timeout);
                        } else if (throwable != null) {
                            log.warn("[{}]Request to {} failed: {}", this.getClass().getSimpleName(), connection.getPeerAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                        } else {
                            log.debug("[{}]Request to {} completed", this.getClass().getSimpleName(), connection.getPeerAddress());
                        }
                    });
            return requestFuture;
        });
    }

    /* --------------------------------------------------------------------- */
    // Node.Listener implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        resolveResponse(envelopePayloadMessage).ifPresent(response -> processResponse(connection, response));
        resolveRequest(envelopePayloadMessage).ifPresent(request -> processRequest(connection, request));
    }

    protected void processResponse(Connection connection, R response) {
        Optional.ofNullable(requestFuturesByConnectionId.get(connection.getId())).ifPresent(handler -> handler.handleResponse(response));
    }

    protected void processRequest(Connection connection, T request) {
        log.info("Received {} from {}", StringUtils.truncate(request), connection.getPeerAddress());
        onRequest(connection, request);
        R response = createResponse(connection, request);
        node.sendAsync(response, connection)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Sending {} to {} failed. {}", StringUtils.truncate(response), connection.getPeerAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                    } else {
                        log.info("Sent {} to {}", StringUtils.truncate(response), connection.getPeerAddress());
                    }
                });
    }

    protected void onRequest(Connection connection, T request) {
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        requestFuturesByConnectionId.computeIfPresent(connection.getId(), (key, handler) -> {
            handler.cancel(true);
            return null; // removes entry
        });
    }

    protected String createRequestId() {
        return String.valueOf(createNonce());
    }

    protected int createNonce() {
        return new Random().nextInt();
    }
}