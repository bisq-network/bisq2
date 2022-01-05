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

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import network.misq.common.data.Pair;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.containers.MisqGridPane;
import network.misq.desktop.components.controls.MisqTextArea;
import network.misq.desktop.components.table.MisqTableColumn;
import network.misq.desktop.components.table.MisqTableView;
import network.misq.i18n.Res;

import java.util.UUID;

public class TransportTypeView extends View<ScrollPane, TransportTypeModel, TransportTypeController> {
    private final MisqTextArea receivedMessagesTextArea;
    private MisqTableView<ConnectionListItem> connectionsTableView;
    private final MisqTableView<DataListItem> dataTableView;
    private final TextField messageReceiverTextField, nodeIdTextField;
    private ChangeListener<DataListItem> dataTableSelectedItemListener;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(new ScrollPane(), model, controller);

        MisqGridPane misqGridPane = new MisqGridPane();
        root.setFitToWidth(true);
        root.setFitToHeight(true);
        root.setContent(misqGridPane);

        misqGridPane.startSection(Res.network.get("nodeInfo.title"));
        misqGridPane.addTextField(Res.network.get("nodeInfo.myAddress"), model.getMyDefaultNodeAddress());
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("table.connections.title"));
        connectionsTableView = new MisqTableView<>(model.getSortedConnectionListItems());
        connectionsTableView.setMinHeight(200);
        misqGridPane.addTableView(connectionsTableView);
        configConnectionsTableView();
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("addData.title"));
        TextField dataContentTextField = misqGridPane.addTextField(Res.network.get("addData.content"), "Test data");
        TextField idTextField = misqGridPane.addTextField(Res.network.get("addData.id"), UUID.randomUUID().toString().substring(0, 8));
        Pair<Button, Label> addDataButtonPair = misqGridPane.addButton(Res.network.get("addData.add"));
        Button addDataButton = addDataButtonPair.first();
        Label label = addDataButtonPair.second();
        addDataButton.setOnAction(e -> {
            addDataButton.setDisable(true);
            label.textProperty().unbind();
            label.setText("...");
            addDataButton.setDisable(false);
            StringProperty result = controller.addData(dataContentTextField.getText(), idTextField.getText());
            label.textProperty().bind(result);
        });
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("table.data.title"));
        dataTableView = new MisqTableView<>(model.getSortedDataListItems());
        dataTableView.setMinHeight(200);
        misqGridPane.addTableView(dataTableView);
        configDataTableView();
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("sendMessages.title"));
        messageReceiverTextField = misqGridPane.addTextField(Res.network.get("sendMessages.to"), "localhost:8000");
        messageReceiverTextField.setEditable(false);
        nodeIdTextField = misqGridPane.addTextField(Res.network.get("sendMessages.nodeId"), "");
        nodeIdTextField.setEditable(false);
        TextField msgTextField = misqGridPane.addTextField(Res.network.get("sendMessages.text"), "Test message");
        Pair<Button, Label> sendButtonPair = misqGridPane.addButton(Res.network.get("sendMessages.send"));
        Button sendButton = sendButtonPair.first();
        sendButton.setOnAction(e -> {
            String msg = msgTextField.getText();
            sendButton.setDisable(true);
            sendButtonPair.second().setText("...");
            controller.sendMessage(msg).whenComplete((result, throwable) -> {
                UIThread.run(() -> {
                    if (throwable == null) {
                        sendButtonPair.second().setText(result);
                    } else {
                        sendButtonPair.second().setText(throwable.toString());
                    }
                    sendButton.setDisable(false);
                });
            });
        });
        misqGridPane.addHSpacer();
        receivedMessagesTextArea = misqGridPane.addTextArea(Res.network.get("sendMessages.receivedMessage"), model.getReceivedMessages());
        receivedMessagesTextArea.setMinHeight(100);
        misqGridPane.endSection();

        dataTableSelectedItemListener = (observable, oldValue, newValue) -> {
            controller.onSelectNetworkId(newValue.getNetworkId());
        };
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

    private void configDataTableView() {
        var dateColumn = new MisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.received"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(DataListItem::getReceived)
                .comparator(DataListItem::compareDate)
                .build();
        dataTableView.getColumns().add(dateColumn);
        dataTableView.getSortOrder().add(dateColumn);

        dataTableView.getColumns().add(new MisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.content"))
                .minWidth(220)
                .valueSupplier(DataListItem::getContent)
                .build());
        dataTableView.getColumns().add(new MisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.nodeId"))
                .valueSupplier(DataListItem::getNodeId)
                .build());
    }

    @Override
    public void activate() {
        nodeIdTextField.textProperty().bind(model.getNodeIdString());
        messageReceiverTextField.textProperty().bind(model.getMessageReceiver());
        dataTableView.getSelectionModel().selectedItemProperty().addListener(dataTableSelectedItemListener);
    }

    @Override
    protected void deactivate() {
        nodeIdTextField.textProperty().unbind();
        messageReceiverTextField.textProperty().unbind();
        dataTableView.getSelectionModel().selectedItemProperty().removeListener(dataTableSelectedItemListener);
    }
}
