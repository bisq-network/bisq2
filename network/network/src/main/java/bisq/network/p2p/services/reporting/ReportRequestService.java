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
import bisq.network.p2p.common.ResponseHandler;
import bisq.network.p2p.node.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class ReportRequestService extends ResponseHandler<ReportRequest, ReportResponse> {
    private static final long TIMEOUT = SECONDS.toMillis(60);

    public ReportRequestService(Node node) {
        super(node, TIMEOUT);

        initialize();
    }

    @Override
    protected Class<ReportResponse> getResponseClass() {
        return ReportResponse.class;
    }

    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public CompletableFuture<Report> request(Address address) {
        return node.getOrCreateConnectionAsync(address)
                .thenCompose(connection -> {
                    ReportRequest reportRequest = new ReportRequest(createRequestId());
                    return request(connection, reportRequest)
                            .thenApply(ReportResponse::getReport);
                });
    }
}