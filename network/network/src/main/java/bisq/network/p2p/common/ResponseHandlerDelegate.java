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
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
class ResponseHandlerDelegate<T extends Request, R extends Response> implements LifecycleHandler {
    private final Node node;
    private final long timeout;
    private final Class<R> responseClass;
    private final Map<String, RequestFuture<T, R>> requestFuturesByConnectionId = new ConcurrentHashMap<>();

    ResponseHandlerDelegate(Node node, long timeout, Class<R> responseClass) {
        this.node = node;
        this.timeout = timeout;
        this.responseClass = responseClass;
    }

    public Optional<R> resolveResponse(EnvelopePayloadMessage message) {
        return responseClass.isInstance(message)
                ? Optional.of(responseClass.cast(message))
                : Optional.empty();
    }


    // If we get called on a connection we have already assigned a request for, we ignore the new request and return
    // the future from the pending request.
    public CompletableFuture<R> request(Connection connection, T request) {
        return requestFuturesByConnectionId.compute(connection.getId(), (k, existing) -> {
            if (existing != null && !existing.isDone()) {
                log.warn("[{}] Reusing pending request future for {}", request.getClass().getSimpleName(), connection.getPeerAddress());
                return existing;
            }
            RequestFuture<T, R> requestFuture = new RequestFuture<>(node, connection, request);
            requestFuture.orTimeout(timeout, TimeUnit.MILLISECONDS)
                    .whenComplete((response, throwable) -> {
                        requestFuturesByConnectionId.remove(k);
                        if (throwable instanceof TimeoutException) {
                            log.warn("[{}] Request to {} timed out after {} ms", request.getClass().getSimpleName(), connection.getPeerAddress(), timeout);
                        } else if (throwable != null) {
                            log.warn("[{}] Request to {} failed: {}", request.getClass().getSimpleName(), connection.getPeerAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                        } else {
                            log.debug("[{}] Request to {} completed", request.getClass().getSimpleName(), connection.getPeerAddress());
                        }
                    });
            return requestFuture;
        });
    }

    protected void processResponse(Connection connection, R response) {
        Optional.ofNullable(requestFuturesByConnectionId.get(connection.getId())).ifPresent(handler -> handler.handleResponse(response));
    }

    public void processOnDisconnect(Connection connection, CloseReason closeReason) {
        requestFuturesByConnectionId.computeIfPresent(connection.getId(), (key, handler) -> {
            if (!handler.isDone()) {
                handler.cancel(true);
            }
            return null; // removes entry
        });
    }

    public Map<String, RequestFuture<T, R>> getRequestFuturesByConnectionId(){
        return requestFuturesByConnectionId;
    }

    /* --------------------------------------------------------------------- */
    // LifecycleHandler implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void initialize() {

    }

    @Override
    public void shutdown() {
        requestFuturesByConnectionId.values().forEach(handler -> handler.cancel(true));
        requestFuturesByConnectionId.clear();
    }
}
