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

import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import network.misq.common.data.Pair;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.containers.MisqGridPane;
import network.misq.desktop.components.table.MisqTableColumn;
import network.misq.desktop.components.table.MisqTableView;
import network.misq.desktop.layout.Layout;
import network.misq.i18n.Res;

import java.util.UUID;

public class TransportTypeView extends View<ScrollPane, TransportTypeModel, TransportTypeController> {
    private MisqTableView<ConnectionListItem> connectionsTableView;
    private TextField pubKeyTextField;
    private ChangeListener<ConnectionListItem> tableSelectedItemListener;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(new ScrollPane(), model, controller);
        AnchorPane anchorPane = new AnchorPane();
        root.setContent(anchorPane);

        MisqGridPane misqGridPane = new MisqGridPane();
        root.setFitToWidth(true);
        root.setFitToHeight(true);
        anchorPane.getChildren().add(misqGridPane);
        Layout.pinToAnchorPane(misqGridPane, 0, 0, 0, 0);


        misqGridPane.startSection(Res.network.get("nodeInfo.title"));
        misqGridPane.addTextField(Res.network.get("nodeInfo.myAddress"), model.getMyDefaultNodeAddress());
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("table.connections.title"));
        connectionsTableView = new MisqTableView<>(model.getSorted());
        misqGridPane.addTableView(connectionsTableView);
        configConnectionsTableView();
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("addData.title"));
        TextField dataContentTextField = misqGridPane.addTextField(Res.network.get("addData.content"), "Test data");
        TextField idTextField = misqGridPane.addTextField(Res.network.get("addData.id"), UUID.randomUUID().toString().substring(0, 8));
        Pair<Button, Label> addDataButtonPair = misqGridPane.addButton(Res.network.get("addData.add"));
        Button addDataButton = addDataButtonPair.first();
        addDataButton.setOnAction(e -> {
            addDataButton.setDisable(true);
            addDataButtonPair.second().setText("...");
            controller.addData(dataContentTextField.getText(), idTextField.getText()).whenComplete((result, throwable) -> {
                UIThread.run(() -> {
                    if (throwable == null) {
                        addDataButtonPair.second().setText(result);
                    } else {
                        addDataButtonPair.second().setText(throwable.toString());
                    }
                    addDataButton.setDisable(false);
                });
            });
        });
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("sendMessages.title"));
        TextField addressTextField = misqGridPane.addTextField(Res.network.get("sendMessages.to"), "localhost:8000");
        pubKeyTextField = misqGridPane.addTextField(Res.network.get("sendMessages.pubKey"), "");
        pubKeyTextField.setPromptText(Res.network.get("sendMessages.pubKey.prompt"));
        TextField msgTextField = misqGridPane.addTextField(Res.network.get("sendMessages.text"), "Test message");
        Pair<Button, Label> sendButtonPair = misqGridPane.addButton(Res.network.get("sendMessages.send"));
        Button sendButton = sendButtonPair.first();
        sendButton.setOnAction(e -> {
            String to = addressTextField.getText();
            String pubKey = pubKeyTextField.getText();
            String msg = msgTextField.getText();
            sendButton.setDisable(true);
            sendButtonPair.second().setText("...");
            controller.sendMessage(to, pubKey, msg).whenComplete((result, throwable) -> {
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
        misqGridPane.endSection();

        tableSelectedItemListener = (observable, oldValue, newValue) -> {
            //newValue.getConnection().getPeersCapability().
        };
    }

    private void configConnectionsTableView() {
        var dateColumn = new MisqTableColumn.Builder<ConnectionListItem>()
                .title(model.getDateHeader())
                .minWidth(180)
                .maxWidth(180)
                .valuePropertySupplier(ConnectionListItem::getDate)
                .comparator(ConnectionListItem::compareDate)
                .build();
        connectionsTableView.getColumns().add(dateColumn);
        connectionsTableView.getSortOrder().add(dateColumn);

        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(model.getAddressHeader())
                .minWidth(220)
                .valuePropertySupplier(ConnectionListItem::getAddress)
                .comparator(ConnectionListItem::compareAddress)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(model.getNodeIdHeader())
                .valuePropertySupplier(ConnectionListItem::getNodeId)
                .comparator(ConnectionListItem::compareNodeId)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(model.getDirectionHeader())
                .valuePropertySupplier(ConnectionListItem::getDirection)
                .comparator(ConnectionListItem::compareDirection)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(model.getRttHeader())
                .valuePropertySupplier(ConnectionListItem::getRtt)
                .comparator(ConnectionListItem::compareRtt)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(model.getSentHeader())
                .valuePropertySupplier(ConnectionListItem::getSent)
                .comparator(ConnectionListItem::compareSent)
                .build());
        connectionsTableView.getColumns().add(new MisqTableColumn.Builder<ConnectionListItem>()
                .title(model.getReceivedHeader())
                .valuePropertySupplier(ConnectionListItem::getReceived)
                .comparator(ConnectionListItem::compareReceived)
                .build());
    }

    @Override
    public void activate() {
        pubKeyTextField.textProperty().bind(model.getPubKey());
        connectionsTableView.getSelectionModel().selectedItemProperty().addListener(tableSelectedItemListener);
    }

    @Override
    protected void deactivate() {
        pubKeyTextField.textProperty().unbind();
        connectionsTableView.getSelectionModel().selectedItemProperty().removeListener(tableSelectedItemListener);
    }
}
