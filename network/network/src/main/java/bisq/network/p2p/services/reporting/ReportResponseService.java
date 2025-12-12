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
import bisq.network.p2p.common.RequestHandler;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.NetworkLoadSnapshot;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import lombok.extern.slf4j.Slf4j;

import java.util.TreeMap;

@Slf4j
public class ReportResponseService extends RequestHandler<ReportRequest, ReportResponse> {

    private final DataService dataService;
    private final NetworkLoadSnapshot networkLoadSnapshot;
    private final MemoryReportService memoryReportService;

    public ReportResponseService(Node node,
                                 DataService dataService,
                                 NetworkLoadSnapshot networkLoadSnapshot,
                                 MemoryReportService memoryReportService) {
        super(node);
        this.dataService = dataService;
        this.networkLoadSnapshot = networkLoadSnapshot;
        this.memoryReportService = memoryReportService;

        initialize();
    }

    @Override
    protected ReportResponse createResponse(Connection connection, ReportRequest request) {
        Report report = createStorageReport();
        return new ReportResponse(request.getRequestId(), report);
    }

    @Override
    protected Class<ReportRequest> getRequestClass() {
        return ReportRequest.class;
    }

    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

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