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
import bisq.desktop.components.table.BisqTableColumns;
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
    private final MaterialTextField myAddress;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(GridPaneUtil.getGridPane(5, 20, new Insets(0)), model, controller);


        myAddress = new MaterialTextField(Res.get("settings.network.nodeInfo.myAddress"));
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

        connectionsTableView = new BisqTableView<>(model.getConnectionListItems().getSortedList());
        connectionsTableView.setPadding(new Insets(-15, 0, 0, 0));
        connectionsTableView.setMinHeight(150);
        connectionsTableView.setPrefHeight(250);
        // Fill available width
        connectionsTableView.setPrefWidth(2000);
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

        nodesTableView = new BisqTableView<>(model.getNodeListItems().getSortedList());
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

    @Override
    protected void onViewAttached() {
        myAddress.textProperty().bind(model.getMyDefaultNodeAddress());
    }

    @Override
    protected void onViewDetached() {
        myAddress.textProperty().unbind();
    }

    private void configConnectionsTableView() {
        connectionsTableView.getColumns().add(BisqTableColumns.getDateColumn(
                Res.get("settings.network.connections.header.established"),
                connectionsTableView.getSortOrder()));

        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.address"))
                .minWidth(130)
                .valueSupplier(ConnectionListItem::getAddress)
                .tooltipSupplier(ConnectionListItem::getAddress)
                .comparator(ConnectionListItem::compareAddress)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.header.nodeTag"))
                .minWidth(100)
                .valueSupplier(ConnectionListItem::getNodeTag)
                .tooltipSupplier(ConnectionListItem::getNodeTagTooltip)
                .comparator(ConnectionListItem::compareNodeTag)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.connectionDirection"))
                .minWidth(100)
                .valueSupplier(ConnectionListItem::getDirection)
                .comparator(ConnectionListItem::compareDirection)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.sentHeader"))
                .minWidth(160)
                .valuePropertySupplier(ConnectionListItem::getSent)
                .tooltipPropertySupplier(ConnectionListItem::getSent)
                .comparator(ConnectionListItem::compareSent)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.receivedHeader"))
                .minWidth(160)
                .valuePropertySupplier(ConnectionListItem::getReceived)
                .tooltipPropertySupplier(ConnectionListItem::getReceived)
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
                .minWidth(70)
                .left()
                .valueSupplier(NodeListItem::getType)
                .comparator(NodeListItem::compareType)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.header.nodeTag"))
                .minWidth(100)
                .valueSupplier(NodeListItem::getNodeTag)
                .tooltipSupplier(NodeListItem::getNodeTagTooltip)
                .comparator(NodeListItem::compareNodeTag)
                .build());
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.address"))
                .minWidth(130)
                .valueSupplier(NodeListItem::getAddress)
                .tooltipSupplier(NodeListItem::getAddress)
                .comparator(NodeListItem::compareAddress)
                .build());
        BisqTableColumn<NodeListItem> numConnections = new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.numConnections"))
                .minWidth(170)
                .valuePropertySupplier(NodeListItem::getNumConnections)
                .comparator(NodeListItem::compareNumConnections)
                .build();
        nodesTableView.getColumns().add(numConnections);
        nodesTableView.getSortOrder().add(numConnections);
        nodesTableView.getColumns().add(new BisqTableColumn.Builder<NodeListItem>()
                .title(Res.get("settings.network.nodes.header.keyId"))
                .minWidth(100)
                .valueSupplier(NodeListItem::getKeyId)
                .tooltipSupplier(NodeListItem::getKeyId)
                .comparator(NodeListItem::compareKeyId)
                .build());
    }
}
