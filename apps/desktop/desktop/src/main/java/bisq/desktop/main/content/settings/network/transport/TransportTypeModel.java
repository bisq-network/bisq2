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

import bisq.common.observable.collection.ObservableSet;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.table.TableList;
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TransportTypeModel implements Model {
    private final TransportType transportType;
    private final ServiceNode serviceNode;
    private final Node defaultNode;
    private final ObservableSet<Connection> connections = new ObservableSet<>();
    private final TableList<ConnectionListItem> connectionListItems = new TableList<>();
    private final TableList<NodeListItem> nodeListItems = new TableList<>();
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty();

    public TransportTypeModel(TransportType transportType, ServiceNode serviceNode, Node defaultNode) {
        this.transportType = transportType;
        this.serviceNode = serviceNode;
        this.defaultNode = defaultNode;
    }
}
