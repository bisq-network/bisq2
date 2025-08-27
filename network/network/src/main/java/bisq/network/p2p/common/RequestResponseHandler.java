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

import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class RequestResponseHandler<T extends Request, R extends Response> extends BaseHandler implements Node.Listener, HandlerLifecycle {
    protected final Node node;
    private final RequestHandlerDelegate<T, R> requestHandlerDelegate;
    private final ResponseHandlerDelegate<T, R> responseHandlerDelegate;

    public RequestResponseHandler(Node node, long timeout) {
        this.node = node;

        this.requestHandlerDelegate = new RequestHandlerDelegate<>(node, getRequestClass(), new RequestHandlerDelegate.Callback<>() {
            @Override
            public R createResponse(Connection connection, T request) {
                return RequestResponseHandler.this.createResponse(connection, request);
            }

            @Override
            public void onRequest(Connection connection, T request) {
                RequestResponseHandler.this.onRequest(connection, request);
            }
        });
        this.responseHandlerDelegate = new ResponseHandlerDelegate<>(node, timeout, getResponseClass());
    }

    /* --------------------------------------------------------------------- */
    // HandlerLifecycle implementation
    /* --------------------------------------------------------------------- */

    public void initialize() {
        node.addListener(this);
        requestHandlerDelegate.initialize();
        responseHandlerDelegate.initialize();
    }

    public void shutdown() {
        node.removeListener(this);
        requestHandlerDelegate.shutdown();
        responseHandlerDelegate.shutdown();
    }

    /* --------------------------------------------------------------------- */
    // ResponseHandler implementation
    /* --------------------------------------------------------------------- */

    protected Map<String, RequestFuture<T, R>> getRequestFuturesByConnectionId() {
        return responseHandlerDelegate.getRequestFuturesByConnectionId();
    }

    protected CompletableFuture<R> request(Connection connection, T request) {
        return responseHandlerDelegate.request(connection, request);
    }

    protected abstract Class<R> getResponseClass();

    protected void processResponse(Connection connection, R response) {
        responseHandlerDelegate.processResponse(connection, response);
    }

    /* --------------------------------------------------------------------- */
    // RequestHandler implementation
    /* --------------------------------------------------------------------- */

    protected abstract R createResponse(Connection connection, T request);

    protected abstract Class<T> getRequestClass();

    protected void onRequest(Connection connection, T request) {
    }

    protected void processRequest(Connection connection, T request) {
        requestHandlerDelegate.processRequest(connection, request);
    }

    /* --------------------------------------------------------------------- */
    // Node.Listener implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        try {
            responseHandlerDelegate.resolveResponse(envelopePayloadMessage)
                    .ifPresent(response -> processResponse(connection, response));
            requestHandlerDelegate.resolveRequest(envelopePayloadMessage)
                    .ifPresent(request -> processRequest(connection, request));
        } catch (Throwable t) {
            log.error("Error handling message: {}", connection, t);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        responseHandlerDelegate.processOnDisconnect(connection, closeReason);
    }
}