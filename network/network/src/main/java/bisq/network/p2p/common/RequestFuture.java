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
import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.network.p2p.message.Request;
import bisq.network.p2p.message.Response;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Slf4j
public class RequestFuture<T extends Request, R extends Response> extends CompletableFuture<R> {
    private final Node node;
    private final Connection connection;
    private final T request;
    // We use that to execute code in the caller prior to the main future gets completed, to ensure that
    // cleanup code is executed before any client can react to completion.
    private final CompletableFuture<Void> priorityFuture = new CompletableFuture<>();
    private long requestTs;

    public RequestFuture(Node node,
                         Connection connection,
                         T request,
                         long timeout) {
        this.node = node;
        this.connection = connection;
        this.request = request;
        orTimeout(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean completeExceptionally(Throwable throwable) {
        if (!priorityFuture.isDone()) {
            priorityFuture.completeExceptionally(throwable);
        }
        return super.completeExceptionally(throwable);
    }

    CompletableFuture<Void> sendRequest() {
        log.info("Send {} to {}", StringUtils.truncate(request), connection.getPeerAddress());
        requestTs = System.currentTimeMillis();
        node.sendAsync(request, connection)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Sending failed.\n" +
                                        "Request: {}\n" +
                                        "Peer: {}\n" +
                                        "Error: {}",
                                StringUtils.truncate(request),
                                connection.getPeerAddress(),
                                ExceptionUtil.getRootCauseMessage(throwable));
                        if (!priorityFuture.isDone()) {
                            priorityFuture.completeExceptionally(throwable);
                        }
                        if (isDone()) {
                            log.warn("We got a failed sendAsync but the response handler had already completed our future. This should never happen");
                        } else {
                            completeExceptionally(throwable);
                        }
                    }
                });
        return priorityFuture;
    }

    void handleResponse(R response) {
        String requestId = request.getRequestId();
        if (!response.getRequestId().equals(requestId)) {
            log.warn("Received response from {} with invalid requestId {}. RequestId was {}. Connection={}. response={}",
                    connection.getPeerAddress(), response.getRequestId(), requestId, connection.getId(), StringUtils.truncate(response));
            return;
        }

        String passed = MathUtils.roundDouble((System.currentTimeMillis() - requestTs) / 1000d, 2) + " sec.";
        log.info("Received {} after {} from {}",
                StringUtils.truncate(response, 100),
                passed,
                connection.getPeerAddress());
        if (!priorityFuture.isDone()) {
            priorityFuture.complete(null);
        }
        if (isDone()) {
            log.info("Discarded response for requestId {} (already completed by error or timeout)", request.getRequestId());
        } else {
            complete(response);
        }
    }
}