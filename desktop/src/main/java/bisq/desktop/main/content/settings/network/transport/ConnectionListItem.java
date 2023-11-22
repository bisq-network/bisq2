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

package bisq.desktop.main.content.settings.network.transport;

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.network_load.ConnectionMetrics;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.TimeFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
@Getter
public class ConnectionListItem implements TableItem {
    @EqualsAndHashCode.Include
    private final String connectionId;
    private final Connection connection;
    private final Connection.Listener listener;
    private final ConnectionMetrics connectionMetrics;
    private final String date;
    private final String address;
    private final String keyId;
    private final String direction;
    private final StringProperty sent = new SimpleStringProperty();
    private final StringProperty received = new SimpleStringProperty();
    private final StringProperty rtt = new SimpleStringProperty("-");
    private final String nodeTag;

    public ConnectionListItem(Connection connection, Node node, IdentityService identityService) {
        this.connection = connection;
        this.keyId = node.getNetworkId().getKeyId();
        connectionId = connection.getId();
        connectionMetrics = connection.getConnectionMetrics();

        date = DateFormatter.formatDateTime(connectionMetrics.getCreationDate());
        address = connection.getPeerAddress().getFullAddress();

        direction = connection.isOutboundConnection() ?
                Res.get("settings.network.connections.value.outbound") :
                Res.get("settings.network.connections.value.inbound");

        nodeTag = identityService.findAnyIdentityByNetworkId(node.getNetworkId())
                .map(Identity::getTag)
                .map(tag -> {
                    if (tag.contains("-")) {
                        String[] tokens = tag.split("-");
                        return tokens[0];
                    } else {
                        return tag;
                    }
                })
                .orElse(Res.get("data.na"));

        updateSent();
        updateReceived();
        updateRtt();

        listener = new Connection.Listener() {
            @Override
            public void onNetworkMessage(EnvelopePayloadMessage envelopePayloadMessage) {
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
        long rrt = Math.round(connectionMetrics.getAverageRtt());
        if (rrt > 0) {
            rtt.set(TimeFormatter.formatDuration(rrt));
        }
    }

    private void updateSent() {
        sent.set(Res.get("settings.network.connections.value.ioData",
                StringUtils.fromBytes(connectionMetrics.getSentBytes()),
                connectionMetrics.getNumMessagesSent()));
    }

    private void updateReceived() {
        received.set(Res.get("settings.network.connections.value.ioData",
                StringUtils.fromBytes(connectionMetrics.getReceivedBytes()),
                connectionMetrics.getNumMessagesReceived()));
    }

    public int compareDate(ConnectionListItem other) {
        return connectionMetrics.getCreationDate().compareTo(other.connectionMetrics.getCreationDate());
    }

    public int compareAddress(ConnectionListItem other) {
        return address.compareTo(other.getAddress());
    }

    public int compareKeyId(ConnectionListItem other) {
        return keyId.compareTo(other.getKeyId());
    }

    public int compareNodeTag(ConnectionListItem other) {
        return nodeTag.compareTo(other.getNodeTag());
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
