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

package bisq.desktop.primary.main.content.settings.networkInfo.transport;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;

public class TransportTypeView extends View<BisqGridPane, TransportTypeModel, TransportTypeController> {
    private final BisqTableView<ConnectionListItem> connectionsTableView;
    private final BisqTableView<NodeListItem> nodesTableView;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(new BisqGridPane(), model, controller);

        root.setPadding(new Insets(20, 20, 20, 0));

        root.startSection(Res.get("nodeInfo.title"));
        root.addTextField(Res.get("nodeInfo.myAddress"), model.getMyDefaultNodeAddress());
        root.endSection();

        root.startSection(Res.get("table.connections.title"));
        connectionsTableView = new BisqTableView<>(model.getSortedConnectionListItems());
        connectionsTableView.setPadding(new Insets(-15, 0, 0, 0));
        connectionsTableView.setMinHeight(150);
        root.addTableView(connectionsTableView);
        configConnectionsTableView();
        root.endSection();

        root.startSection(Res.get("table.nodes.title"));
        nodesTableView = new BisqTableView<>(model.getSortedNodeListItems());
        nodesTableView.setPadding(new Insets(-15, 0, 0, 0));
        nodesTableView.setMinHeight(100);
        root.addTableView(nodesTableView);
        configNodesTableView();
        root.endSection();
    }

    private void configConnectionsTableView() {
        var dateColumn = new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("table.connections.header.established"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(ConnectionListItem::getDate)
                .comparator(ConnectionListItem::compareDate)
                .build();
        connectionsTableView.getColumns().add(dateColumn);
        connectionsTableView.getSortOrder().add(dateColumn);

        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("address"))
                .minWidth(250)
                .valueSupplier(ConnectionListItem::getAddress)
                .comparator(ConnectionListItem::compareAddress)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("table.connections.header.nodeId"))
                .minWidth(220)
                .valueSupplier(ConnectionListItem::getNodeId)
                .comparator(ConnectionListItem::compareNodeId)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("table.connections.header.connectionDirection"))
                .minWidth(100)
                .valueSupplier(ConnectionListItem::getDirection)
                .comparator(ConnectionListItem::compareDirection)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("table.connections.header.sentHeader"))
                .minWidth(120)
                .valuePropertySupplier(ConnectionListItem::getSent)
                .comparator(ConnectionListItem::compareSent)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("table.connections.header.receivedHeader"))
                .minWidth(120)
                .valuePropertySupplier(ConnectionListItem::getReceived)
                .comparator(ConnectionListItem::compareReceived)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("table.connections.header.rtt"))
                .minWidth(100)
                .valuePropertySupplier(ConnectionListItem::getRtt)
                .comparator(ConnectionListItem::compareRtt)
                .build());
    }

    private void configNodesTableView() {
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("table.nodes.header.type"))
                .minWidth(100)
                .valueSupplier(NodeListItem::getType)
                .comparator(NodeListItem::compareType)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("table.nodes.header.domainId"))
                .minWidth(100)
                .valueSupplier(NodeListItem::getDomainId)
                .comparator(NodeListItem::compareDomainId)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("table.nodes.header.nodeId"))
                .minWidth(250)
                .valueSupplier(NodeListItem::getNodeId)
                .comparator(NodeListItem::compareNodeId)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("table.nodes.header.address"))
                .minWidth(220)
                .valueSupplier(NodeListItem::getAddress)
                .comparator(NodeListItem::compareAddress)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("table.nodes.header.numConnections"))
                .minWidth(120)
                .valuePropertySupplier(NodeListItem::getNumConnections)
                .comparator(NodeListItem::compareNumConnections)
                .build());
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
