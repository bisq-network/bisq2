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

import javafx.scene.layout.AnchorPane;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.containers.MisqGridPane;
import network.misq.desktop.components.table.MisqTableColumn;
import network.misq.desktop.components.table.MisqTableView;
import network.misq.desktop.layout.Layout;
import network.misq.i18n.Res;

public class TransportTypeView extends View<AnchorPane, TransportTypeModel, TransportTypeController> {
    private final MisqTableView<ConnectionListItem> connectionsTableView;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(new AnchorPane(), model, controller);

        MisqGridPane misqGridPane = new MisqGridPane();
        root.getChildren().add(misqGridPane);
        Layout.pinToAnchorPane(misqGridPane, 0, 0, 0, 0);
        misqGridPane.startSection(Res.network.get("nodeInfo.title"));
        misqGridPane.addTextField(Res.network.get("nodeInfo.myAddress"), model.getMyDefaultNodeAddress());
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("table.connections.title"));
        connectionsTableView = new MisqTableView<>(model.getSortedConnectionListItems());
        connectionsTableView.setMinHeight(200);
        misqGridPane.addTableView(connectionsTableView);
        configConnectionsTableView();
        misqGridPane.endSection();
    }

    private void configConnectionsTableView() {
        var dateColumn = new MisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.established"))
                .minWidth(180)
                .maxWidth(180)
                .valuePropertySupplier(ConnectionListItem::getDate)
                .comparator(ConnectionListItem::compareDate)
                .build();
        connectionsTableView.getColumns().add(dateColumn);
        connectionsTableView.getSortOrder().add(dateColumn);

        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.address"))
                .minWidth(220)
                .valuePropertySupplier(ConnectionListItem::getAddress)
                .comparator(ConnectionListItem::compareAddress)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.nodeId"))
                .valuePropertySupplier(ConnectionListItem::getNodeId)
                .comparator(ConnectionListItem::compareNodeId)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.connectionDirection"))
                .valuePropertySupplier(ConnectionListItem::getDirection)
                .comparator(ConnectionListItem::compareDirection)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.sentHeader"))
                .valuePropertySupplier(ConnectionListItem::getSent)
                .comparator(ConnectionListItem::compareSent)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.receivedHeader"))
                .valuePropertySupplier(ConnectionListItem::getReceived)
                .comparator(ConnectionListItem::compareReceived)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.rtt"))
                .valuePropertySupplier(ConnectionListItem::getRtt)
                .comparator(ConnectionListItem::compareRtt)
                .build());
    }

    @Override
    public void activate() {
    }

    @Override
    protected void deactivate() {
    }
}
