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
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
class RequestHandlerDelegate<T extends Request, R extends Response> implements HandlerLifecycle {

    interface Callback<T extends Request, R extends Response> {
        R createResponse(Connection connection, T request);

        default void onRequest(Connection connection, T request) {
        }
    }

    private final Node node;
    private final Class<T> requestClass;
    private final Callback<T, R> callback;

    RequestHandlerDelegate(Node node, Class<T> requestClass, Callback<T, R> callback) {
        this.node = node;
        this.requestClass = requestClass;
        this.callback = callback;
    }

    protected Optional<T> resolveRequest(EnvelopePayloadMessage message) {
        return requestClass.isInstance(message)
                ? Optional.of(requestClass.cast(message))
                : Optional.empty();
    }

    protected void processRequest(Connection connection, T request) {
        log.info("Received {} from {}", StringUtils.truncate(request), connection.getPeerAddress());
        callback.onRequest(connection, request);
        R response = callback.createResponse(connection, request);
        node.sendAsync(response, connection)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Sending {} to {} failed. {}", StringUtils.truncate(response), connection.getPeerAddress(), ExceptionUtil.getRootCauseMessage(throwable));
                    } else {
                        log.info("Sent {} to {}", StringUtils.truncate(response), connection.getPeerAddress());
                    }
                });
    }

    /* --------------------------------------------------------------------- */
    // HandlerLifecycle implementation
    /* --------------------------------------------------------------------- */

    @Override
    public void initialize() {
    }

    @Override
    public void shutdown() {
    }


}
