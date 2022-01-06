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

package bisq.desktop.main.content.networkinfo.transport;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.scene.layout.AnchorPane;

public class TransportTypeView extends View<AnchorPane, TransportTypeModel, TransportTypeController> {
    private final BisqTableView<ConnectionListItem> connectionsTableView;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(new AnchorPane(), model, controller);

        BisqGridPane bisqGridPane = new BisqGridPane();
        root.getChildren().add(bisqGridPane);
        Layout.pinToAnchorPane(bisqGridPane, 0, 0, 0, 0);
        bisqGridPane.startSection(Res.network.get("nodeInfo.title"));
        bisqGridPane.addTextField(Res.network.get("nodeInfo.myAddress"), model.getMyDefaultNodeAddress());
        bisqGridPane.endSection();

        bisqGridPane.startSection(Res.network.get("table.connections.title"));
        connectionsTableView = new BisqTableView<>(model.getSortedConnectionListItems());
        connectionsTableView.setMinHeight(200);
        bisqGridPane.addTableView(connectionsTableView);
        configConnectionsTableView();
        bisqGridPane.endSection();
    }

    private void configConnectionsTableView() {
        var dateColumn = new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.established"))
                .minWidth(180)
                .maxWidth(180)
                .valuePropertySupplier(ConnectionListItem::getDate)
                .comparator(ConnectionListItem::compareDate)
                .build();
        connectionsTableView.getColumns().add(dateColumn);
        connectionsTableView.getSortOrder().add(dateColumn);

        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.address"))
                .minWidth(220)
                .valuePropertySupplier(ConnectionListItem::getAddress)
                .comparator(ConnectionListItem::compareAddress)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.nodeId"))
                .valuePropertySupplier(ConnectionListItem::getNodeId)
                .comparator(ConnectionListItem::compareNodeId)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.connectionDirection"))
                .valuePropertySupplier(ConnectionListItem::getDirection)
                .comparator(ConnectionListItem::compareDirection)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.sentHeader"))
                .valuePropertySupplier(ConnectionListItem::getSent)
                .comparator(ConnectionListItem::compareSent)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
                .title(Res.network.get("table.connections.header.receivedHeader"))
                .valuePropertySupplier(ConnectionListItem::getReceived)
                .comparator(ConnectionListItem::compareReceived)
                .build());
        connectionsTableView.getColumns().add(new BisqTableColumn.Builder<ConnectionListItem>()
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
