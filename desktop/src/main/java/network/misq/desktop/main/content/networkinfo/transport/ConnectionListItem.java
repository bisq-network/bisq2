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

package network.misq.desktop.main.content.networkinfo.transport;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.StringUtils;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.components.table.TableItem;
import network.misq.i18n.Res;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.CloseReason;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Metrics;
import network.misq.presentation.formatters.DateFormatter;
import network.misq.presentation.formatters.TimeFormatter;

import java.util.Objects;

@Slf4j
public class ConnectionListItem implements TableItem {
    @Getter
    private final Connection connection;
    private final Connection.Listener listener;

    private final String id;
    private final Metrics metrics;
    @Getter
    private final StringProperty date = new SimpleStringProperty();
    @Getter
    private final StringProperty address = new SimpleStringProperty();
    @Getter
    private final StringProperty nodeId = new SimpleStringProperty();
    @Getter
    private final StringProperty pubKey = new SimpleStringProperty();
    @Getter
    private final StringProperty direction = new SimpleStringProperty();
    @Getter
    private final StringProperty sent = new SimpleStringProperty();
    @Getter
    private final StringProperty received = new SimpleStringProperty();
    @Getter
    private final StringProperty rtt = new SimpleStringProperty();

    public ConnectionListItem(Connection connection, String nodeId) {
        this.connection = connection;
        id = connection.getId();
        metrics = connection.getMetrics();
        this.nodeId.set(nodeId);

        date.set(DateFormatter.formatDateTime(metrics.getCreationDate()));
        address.set(connection.getPeerAddress().getFullAddress());

        direction.set(connection.isOutboundConnection() ? Res.network.get("table.connections.value.outbound") : Res.network.get("table.connections.value.inbound"));
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
        rtt.set(TimeFormatter.formatTime(Math.round(metrics.getAverageRtt())));
    }

    private void updateSent() {
        sent.set(Res.network.get("table.connections.value.ioData",
                StringUtils.fromBytes(metrics.getSentBytes().get()),
                metrics.getNumMessagesSent().get()));
    }

    private void updateReceived() {
        received.set(Res.network.get("table.connections.value.ioData",
                StringUtils.fromBytes(metrics.getReceivedBytes().get()),
                metrics.getNumMessagesReceived().get()));
    }

    public int compareDate(ConnectionListItem other) {
        return metrics.getCreationDate().compareTo(other.metrics.getCreationDate());
    }

    public int compareAddress(ConnectionListItem other) {
        return metrics.getCreationDate().compareTo(other.metrics.getCreationDate());
    }

    public int compareNodeId(ConnectionListItem other) {
        return nodeId.get().compareTo(other.getNodeId().get());
    }

    public int compareDirection(ConnectionListItem other) {
        return direction.get().compareTo(other.connection.toString());
    }

    public int compareSent(ConnectionListItem other) {
        return 0;
    }

    public int compareReceived(ConnectionListItem other) {
        return 0;
    }

    public int compareRtt(ConnectionListItem other) {
        return 0;
    }

    @Override
    public void activate() {
        connection.addListener(listener);
    }

    @Override
    public void deactivate() {
        connection.removeListener(listener);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionListItem that = (ConnectionListItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
