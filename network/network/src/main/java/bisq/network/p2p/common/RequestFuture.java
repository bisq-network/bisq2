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

@Getter
@Slf4j
public class RequestFuture<T extends Request, R extends Response> extends CompletableFuture<R> {
    private final Node node;
    private final Connection connection;
    private final String requestId;
    private final long requestTs;

    public RequestFuture(Node node, Connection connection, T request) {
        this.node = node;
        this.connection = connection;
        requestId = request.getRequestId();

        log.info("Send {} to {}", StringUtils.truncate(request), connection.getPeerAddress());
        requestTs = System.currentTimeMillis();
        node.sendAsync(request, connection)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.warn("Sending {} to {} failed. {}",
                                StringUtils.truncate(request),
                                connection.getPeerAddress(),
                                ExceptionUtil.getRootCauseMessage(throwable));
                        completeExceptionally(throwable);
                    }
                });
    }

    void handleResponse(R response) {
        if (response.getRequestId().equals(requestId)) {
            String passed = MathUtils.roundDouble((System.currentTimeMillis() - requestTs) / 1000d, 2) + " sec.";
            log.info("Received {} after {} from {}",
                    StringUtils.truncate(response),
                    passed,
                    connection.getPeerAddress());
            complete(response);
        } else {
            log.warn("Received response from {} with invalid requestId {}. RequestId was {}. Connection={}. response={}",
                    connection.getPeerAddress(), response.getRequestId(), requestId, connection.getId(), StringUtils.truncate(response));
        }
    }
}