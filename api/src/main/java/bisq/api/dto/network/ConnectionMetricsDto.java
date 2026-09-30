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

package bisq.api.dto.network;

import javax.annotation.Nullable;

/**
 * Per-peer traffic metrics for one connection, read off {@code Connection.getConnectionMetrics()}.
 * {@code rttMillis} is {@code null} until a round-trip has actually been measured (handshake /
 * request-response); byte and message counts are lifetime totals for the connection.
 */
public record ConnectionMetricsDto(@Nullable Long rttMillis,
                                   long sentBytes,
                                   long sentMessageCount,
                                   long receivedBytes,
                                   long receivedMessageCount) {
}
