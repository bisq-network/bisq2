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

package bisq.api.dto.mappings.network;

import bisq.api.dto.network.ConnectionMetricsDto;
import bisq.network.p2p.node.network_load.ConnectionMetrics;

public class ConnectionMetricsDtoMapping {
    public static ConnectionMetricsDto fromBisq2Model(ConnectionMetrics value) {
        // averageRtt is 0 until a round-trip is measured (handshake / request-response); treat that as "unmeasured".
        // Null-check the raw average, not the rounded value: a measured sub-millisecond rtt still rounds to 0.
        double rttRaw = value.getAverageRtt();
        return new ConnectionMetricsDto(
                rttRaw > 0 ? Math.round(rttRaw) : null,
                value.getSentBytes(),
                value.getNumMessagesSent(),
                value.getReceivedBytes(),
                value.getNumMessagesReceived()
        );
    }
}
