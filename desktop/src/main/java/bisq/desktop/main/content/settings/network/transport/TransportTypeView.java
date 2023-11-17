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

import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class TransportTypeView extends View<GridPane, TransportTypeModel, TransportTypeController> {
    private final BisqTableView<ConnectionListItem> connectionsTableView;
    private final BisqTableView<NodeListItem> nodesTableView;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(GridPaneUtil.getGridPane(5, 20, new Insets(0)), model, controller);

        MaterialTextField myAddress = new MaterialTextField(Res.get("settings.network.nodeInfo.myAddress"), "", "", model.getMyDefaultNodeAddress().get());
        myAddress.setEditable(false);
        myAddress.showCopyIcon();
        root.add(myAddress, 0, root.getRowCount(), 2, 1);

        Label connectionsLabel = GridPaneUtil.getHeadline(
                Res.get("settings.network.connections.title"),
                "bisq-sub-title-label",
                "",
                0);
        GridPane.setMargin(connectionsLabel, new Insets(0, 0, -15, 10));
        root.add(connectionsLabel, 0, root.getRowCount());

        connectionsTableView = new BisqTableView<>(model.getSortedConnectionListItems());
        connectionsTableView.setPadding(new Insets(-15, 0, 0, 0));
        connectionsTableView.setMinHeight(150);
        connectionsTableView.setPrefHeight(250);
        configConnectionsTableView();

        VBox vBoxConnections = new VBox(16, connectionsTableView);
        vBoxConnections.getStyleClass().add("bisq-grey-2-bg");
        vBoxConnections.setPadding(new Insets(20, 0, 0, 0));
        vBoxConnections.setAlignment(Pos.TOP_LEFT);
        root.add(vBoxConnections, 0, root.getRowCount(), 2, 1);

        Label nodesLabel = GridPaneUtil.getHeadline(Res.get("settings.network.nodes.title"),
                "bisq-sub-title-label",
                "",
                0);
        GridPane.setMargin(nodesLabel, new Insets(0, 0, -15, 10));
        root.add(nodesLabel, 0, root.getRowCount());

        nodesTableView = new BisqTableView<>(model.getSortedNodeListItems());
        nodesTableView.setPadding(new Insets(-15, 0, 0, 0));
        nodesTableView.setMinHeight(100);
        nodesTableView.setPrefHeight(200);
        configNodesTableView();

        VBox vBoxNodes = new VBox(16, nodesTableView);
        vBoxNodes.getStyleClass().add("bisq-grey-2-bg");
        vBoxNodes.setPadding(new Insets(20, 0, 0, 0));
        vBoxNodes.setAlignment(Pos.TOP_LEFT);
        root.add(vBoxNodes, 0, root.getRowCount(), 2, 1);

    }

    private void configConnectionsTableView() {
        var dateColumn = new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.established"))
                .minWidth(180)
                .maxWidth(180)
                .left()
                .valueSupplier(ConnectionListItem::getDate)
                .comparator(ConnectionListItem::compareDate)
                .build();
        connectionsTableView.getColumns().add(dateColumn);
        connectionsTableView.getSortOrder().add(dateColumn);

        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.address"))
                .minWidth(250)
                .valueSupplier(ConnectionListItem::getAddress)
                .comparator(ConnectionListItem::compareAddress)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.nodeId"))
                .minWidth(220)
                .valueSupplier(ConnectionListItem::getNodeId)
                .comparator(ConnectionListItem::compareNodeId)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.connectionDirection"))
                .minWidth(100)
                .valueSupplier(ConnectionListItem::getDirection)
                .comparator(ConnectionListItem::compareDirection)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.sentHeader"))
                .minWidth(120)
                .valuePropertySupplier(ConnectionListItem::getSent)
                .comparator(ConnectionListItem::compareSent)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.receivedHeader"))
                .minWidth(120)
                .valuePropertySupplier(ConnectionListItem::getReceived)
                .comparator(ConnectionListItem::compareReceived)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.rtt"))
                .minWidth(100)
                .valuePropertySupplier(ConnectionListItem::getRtt)
                .comparator(ConnectionListItem::compareRtt)
                .build());
    }

    private void configNodesTableView() {
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.type"))
                .minWidth(100)
                .left()
                .valueSupplier(NodeListItem::getType)
                .comparator(NodeListItem::compareType)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.domainId"))
                .minWidth(100)
                .valueSupplier(NodeListItem::getDomainId)
                .comparator(NodeListItem::compareDomainId)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.nodeId"))
                .minWidth(250)
                .valueSupplier(NodeListItem::getNodeId)
                .comparator(NodeListItem::compareNodeId)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.address"))
                .minWidth(220)
                .valueSupplier(NodeListItem::getAddress)
                .comparator(NodeListItem::compareAddress)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.numConnections"))
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
