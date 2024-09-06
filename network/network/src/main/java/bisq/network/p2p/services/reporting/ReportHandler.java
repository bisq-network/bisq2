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

import bisq.common.threading.ThreadName;
import bisq.common.util.MathUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Getter
@Slf4j
class ReportHandler implements Connection.Listener {
    private final Node node;
    private final Connection connection;
    private final CompletableFuture<Report> future = new CompletableFuture<>();
    private final String requestId;
    private long requestTs;

    ReportHandler(Node node, Connection connection) {
        this.node = node;
        this.connection = connection;

        requestId = UUID.randomUUID().toString();
        this.connection.addListener(this);
    }

    CompletableFuture<Report> request() {
        requestTs = System.currentTimeMillis();
        supplyAsync(() -> {
            ThreadName.set(this, "request");
            return node.send(new ReportRequest(requestId), connection);
        }, NetworkService.NETWORK_IO_POOL)
                .whenComplete((c, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        dispose();
                    }
                });
        return future;
    }

    @Override
    public void onNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof ReportResponse response) {
            if (response.getRequestId().equals(requestId)) {
                Report report = response.getReport();
                String passed = MathUtils.roundDouble((System.currentTimeMillis() - requestTs) / 1000d, 2) + " sec.";
                log.info("Received StorageReportingResponse after {} from {} with requestId {}.Connection={}\nStorageReporting={}",
                        passed, connection.getPeerAddress(), response.getRequestId(), connection.getId(), report);
                removeListeners();
                future.complete(report);
            } else {
                log.warn("Received StorageReportingResponse from {} with invalid requestId {}. RequestId {}. Connection={}",
                        connection.getPeerAddress(), response.getRequestId(), requestId, connection.getId());
            }
        }
    }

    @Override
    public void onConnectionClosed(CloseReason closeReason) {
        dispose();
    }

    void dispose() {
        removeListeners();
        if (!future.isDone()) {
            future.cancel(true);
        }
    }

    private void removeListeners() {
        connection.removeListener(this);
    }
}