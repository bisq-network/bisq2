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

package bisq.node_monitor;

import bisq.network.p2p.services.reporting.Report;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.TreeMap;

@Schema(name = "Report")
public record ReportDto(TreeMap<String, Integer> authorizedDataPerClassName,
                        TreeMap<String, Integer> authenticatedDataPerClassName,
                        TreeMap<String, Integer> mailboxDataPerClassName,
                        int numConnections,
                        int memoryUsed,
                        int numThreads,
                        double nodeLoad) {
    public static ReportDto from(Report report) {
        return new ReportDto(report.getAuthorizedDataPerClassName(),
                report.getAuthenticatedDataPerClassName(),
                report.getMailboxDataPerClassName(),
                report.getNumConnections(),
                report.getMemoryUsed(),
                report.getNumThreads(),
                report.getNodeLoad());
    }
}
