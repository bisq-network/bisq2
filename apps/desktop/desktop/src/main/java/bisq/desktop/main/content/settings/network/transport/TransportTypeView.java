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
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;

public class TransportTypeView extends View<GridPane, TransportTypeModel, TransportTypeController> {
    private final RichTableView<ConnectionListItem> connectionsTableView;
    private final RichTableView<NodeListItem> nodesTableView;
    private final MaterialTextField myAddress;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(GridPaneUtil.getGridPane(5, 20, new Insets(0)), model, controller);

        myAddress = new MaterialTextField(Res.get("settings.network.nodeInfo.myAddress"));
        myAddress.setEditable(false);
        myAddress.showCopyIcon();
        root.add(myAddress, 0, root.getRowCount(), 2, 1);

        connectionsTableView = new RichTableView<>(model.getConnectionListItems().getSortedList(),
                Res.get("settings.network.connections.title"),
                controller::applyConnectionListSearchPredicate);
        // Fill available width
        connectionsTableView.setPrefWidth(2000);
        configConnectionsTableView();
        root.add(connectionsTableView, 0, root.getRowCount(), 2, 1);

        nodesTableView = new RichTableView<>(model.getNodeListItems().getSortedList(),
                Res.get("settings.network.nodes.title"),
                controller::applyNodeListSearchPredicate);
        nodesTableView.setPrefHeight(200);
        configNodesTableView();
        root.add(nodesTableView, 0, root.getRowCount(), 2, 1);
    }

    @Override
    protected void onViewAttached() {
        connectionsTableView.initialize();
        nodesTableView.initialize();
        myAddress.textProperty().bind(model.getMyDefaultNodeAddress());
    }

    @Override
    protected void onViewDetached() {
        connectionsTableView.dispose();
        nodesTableView.dispose();
        myAddress.textProperty().unbind();
    }

    private void configConnectionsTableView() {
        connectionsTableView.getColumns().add(DateColumnUtil.getDateColumn(connectionsTableView.getSortOrder()));

        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.peer"))
                .minWidth(130)
                .valueSupplier(ConnectionListItem::getPeer)
                .tooltipSupplier(ConnectionListItem::getPeer)
                .comparator(ConnectionListItem::comparePeer)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.connections.header.address"))
                .minWidth(130)
                .valueSupplier(ConnectionListItem::getAddress)
                .tooltipSupplier(ConnectionListItem::getAddress)
                .comparator(ConnectionListItem::compareAddress)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.get("settings.network.header.nodeTag"))
                .minWidth(120)
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
