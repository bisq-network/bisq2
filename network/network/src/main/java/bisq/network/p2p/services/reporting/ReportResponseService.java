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

import bisq.common.platform.MemoryReportService;
import bisq.common.threading.ThreadName;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.extern.slf4j.Slf4j;

import java.util.TreeMap;

/**
 * Sends a request for NetworkLoad to our peers. We add our own NetworkLoad in the request.
 * We do not user a config here as we want to have the same behaviour in the network to avoid stale networkLoad states.
 */
@Slf4j
public class ReportResponseService implements Node.Listener {
    private static final long TIMEOUT_SEC = 120;

    private final Node node;
    private final DataService dataService;
    private final NetworkLoadSnapshot networkLoadSnapshot;
    private final MemoryReportService memoryReportService;

    public ReportResponseService(Node node,
                                 DataService dataService,
                                 NetworkLoadSnapshot networkLoadSnapshot,
                                 MemoryReportService memoryReportService) {
        this.node = node;
        this.dataService = dataService;
        this.networkLoadSnapshot = networkLoadSnapshot;
        this.memoryReportService = memoryReportService;

        node.addListener(this);
    }

    public void shutdown() {
        node.removeListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof ReportRequest request) {
            Report report = createStorageReport();
            ReportResponse response = new ReportResponse(request.getRequestId(), report);
            log.info("Received a ReportRequest from {}", connection.getPeerAddress());
            NetworkService.NETWORK_IO_POOL.submit(() -> {
                ThreadName.set(this, "response");
                node.send(response, connection);
            });
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Report createStorageReport() {
        TreeMap<String, Integer> authorizedDataPerClassName = new TreeMap<>();
        dataService.getAuthorizedData().forEach(data -> {
            String className = data.getClassName();
            authorizedDataPerClassName.put(className, authorizedDataPerClassName.getOrDefault(className, 0) + 1);
        });

        TreeMap<String, Integer> authenticatedDataPerClassName = new TreeMap<>();
        dataService.getAuthenticatedData()
                .filter(data -> !(data instanceof AuthorizedData))
                .forEach(data -> {
                    String className = data.getClassName();
                    authenticatedDataPerClassName.put(className, authenticatedDataPerClassName.getOrDefault(className, 0) + 1);
                });

        TreeMap<String, Integer> mailboxDataPerClassName = new TreeMap<>();
        dataService.getMailboxData().forEach(data -> {
            String className = data.getClassName();
            mailboxDataPerClassName.put(className, mailboxDataPerClassName.getOrDefault(className, 0) + 1);
        });


        int numConnections = node.getNumConnections();
        int memoryUsed = (int) memoryReportService.getUsedMemoryInMB();
        int numThreads = Thread.activeCount();
        double nodeLoad = networkLoadSnapshot.getCurrentNetworkLoad().getLoad();

        return new Report(authorizedDataPerClassName,
                authenticatedDataPerClassName,
                mailboxDataPerClassName, numConnections, memoryUsed, numThreads, nodeLoad);
    }
}