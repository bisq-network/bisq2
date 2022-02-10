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

package bisq.desktop.primary.main.content.settings.networkinfo.transport;

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Metrics;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
public class ConnectionListItem implements TableItem {
    @Getter
    private final Connection connection;
    private final Connection.Listener listener;

    private final Metrics metrics;
    @Getter
    private final String date;
    @Getter
    private final String address;

    @Getter
    private final String nodeId;
    @Getter
    private final String direction;
    @Getter
    private final StringProperty sent = new SimpleStringProperty();
    @Getter
    private final StringProperty received = new SimpleStringProperty();
    @Getter
    private final StringProperty rtt = new SimpleStringProperty("-");
    @EqualsAndHashCode.Include
    private final String connectionId;

    public ConnectionListItem(Connection connection, String nodeId) {
        this.connection = connection;
        this.nodeId = nodeId;
        connectionId = connection.getId();
        metrics = connection.getMetrics();

        date = DateFormatter.formatDateTime(metrics.getCreationDate());
        address = connection.getPeerAddress().getFullAddress();

        direction = connection.isOutboundConnection() ?
                Res.get("table.connections.value.outbound") :
                Res.get("table.connections.value.inbound");

        updateSent();
        updateReceived();
        updateRtt();

        listener = new Connection.Listener() {
            @Override
            public void onMessage(Message message) {
                UIThread.run(() -> {
                    updateSent();
                    updateReceived();
                    updateRtt();
                });
            }

            @Override
            public void onConnectionClosed(CloseReason closeReason) {
            }
        };
    }

    private void updateRtt() {
        long rrt = Math.round(metrics.getAverageRtt());
        if (rrt > 0) {
            rtt.set(TimeFormatter.formatDuration(rrt));
        }
    }

    private void updateSent() {
        sent.set(Res.get("table.connections.value.ioData",
                StringUtils.fromBytes(metrics.getSentBytes().get()),
                metrics.getNumMessagesSent().get()));
    }

    private void updateReceived() {
        received.set(Res.get("table.connections.value.ioData",
                StringUtils.fromBytes(metrics.getReceivedBytes().get()),
                metrics.getNumMessagesReceived().get()));
    }

    public int compareDate(ConnectionListItem other) {
        return metrics.getCreationDate().compareTo(other.metrics.getCreationDate());
    }

    public int compareAddress(ConnectionListItem other) {
        return address.compareTo(other.getAddress());
    }

    public int compareNodeId(ConnectionListItem other) {
        return nodeId.compareTo(other.getNodeId());
    }

    public int compareDirection(ConnectionListItem other) {
        return direction.compareTo(other.direction);
    }

    public int compareSent(ConnectionListItem other) {
        return 0; //todo
    }

    public int compareReceived(ConnectionListItem other) {
        return 0;//todo
    }

    public int compareRtt(ConnectionListItem other) {
        return 0;//todo
    }

    @Override
    public void activate() {
        connection.addListener(listener);
    }

    @Override
    public void deactivate() {
        connection.removeListener(listener);
    }
}
