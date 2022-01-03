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
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import network.misq.common.data.Pair;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.containers.Form;
import network.misq.desktop.components.table.MisqTableView;
import network.misq.desktop.layout.Layout;
import network.misq.i18n.Res;

import java.util.Optional;
import java.util.UUID;

public class TransportTypeView extends View<ScrollPane, TransportTypeModel, TransportTypeController> {
    private final MisqTableView<ConnectionListItem> tableView;
    private final TextField pubKeyTextField;
    private ChangeListener<ConnectionListItem> tableSelectedItemListener;

    public TransportTypeView(TransportTypeModel model, TransportTypeController controller) {
        super(new ScrollPane(), model, controller);

        Form nodeInfoForm = new Form(Res.network.get("nodeInfo.title"));
        nodeInfoForm.addTextField(Res.network.get("nodeInfo.myAddress"), model.getMyDefaultNodeAddress());

        tableView = new MisqTableView<>(model.getSorted());

        VBox.setVgrow(tableView, Priority.ALWAYS);

        var dateColumn = tableView.getPropertyColumn(model.getDateHeader(),
                180,
                ConnectionListItem::getDate,
                Optional.of(ConnectionListItem::compareDate));
        tableView.getColumns().add(dateColumn);
        tableView.getSortOrder().add(dateColumn);

        tableView.getColumns().add(tableView.getPropertyColumn(model.getAddressHeader(),
                220,
                ConnectionListItem::getAddress,
                Optional.of(ConnectionListItem::compareAddress)));
        tableView.getColumns().add(tableView.getPropertyColumn(model.getNodeIdHeader(),
                80,
                ConnectionListItem::getNodeId,
                Optional.of(ConnectionListItem::compareNodeId)));
        tableView.getColumns().add(tableView.getPropertyColumn(model.getDirectionHeader(),
                80,
                ConnectionListItem::getDirection,
                Optional.of(ConnectionListItem::compareDirection)));
        tableView.getColumns().add(tableView.getPropertyColumn(model.getRttHeader(),
                80,
                ConnectionListItem::getRtt,
                Optional.of(ConnectionListItem::compareRtt)));
        tableView.getColumns().add(tableView.getPropertyColumn(model.getSentHeader(),
                80,
                ConnectionListItem::getSent,
                Optional.of(ConnectionListItem::compareSent)));
        tableView.getColumns().add(tableView.getPropertyColumn(model.getReceivedHeader(),
                80,
                ConnectionListItem::getReceived,
                Optional.of(ConnectionListItem::compareReceived)));

        Form addDataForm = new Form(Res.network.get("addData.title"));
        TextField dataContentTextField = addDataForm.addTextField(Res.network.get("addData.content"), "Test data");
        TextField idTextField = addDataForm.addTextField(Res.network.get("addData.id"), UUID.randomUUID().toString().substring(0, 8));
        Pair<Button, Label> addDataButtonPair = addDataForm.addButton(Res.network.get("addData.add"));
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


        Form sendMessagesForm = new Form(Res.network.get("sendMessages.title"));
        TextField addressTextField = sendMessagesForm.addTextField(Res.network.get("sendMessages.to"), "localhost:8000");
        pubKeyTextField = sendMessagesForm.addTextField(Res.network.get("sendMessages.pubKey"), "");
        pubKeyTextField.setPromptText(Res.network.get("sendMessages.pubKey.prompt"));
        TextField msgTextField = sendMessagesForm.addTextField(Res.network.get("sendMessages.text"), "Test message");
        Pair<Button, Label> sendButtonPair = sendMessagesForm.addButton(Res.network.get("sendMessages.send"));
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


        VBox vBox = new VBox();
        vBox.setSpacing(30);
        vBox.setPadding(Layout.INSETS);
        vBox.getChildren().addAll(nodeInfoForm, tableView, addDataForm, sendMessagesForm);

        root.setFitToWidth(true);
        root.setFitToHeight(true);
        root.setContent(vBox);

        tableSelectedItemListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends ConnectionListItem> observable, ConnectionListItem oldValue, ConnectionListItem newValue) {
                //newValue.getConnection().getPeersCapability().
            }
        };
    }

    @Override
    public void activate() {
        pubKeyTextField.textProperty().bind(model.getPubKey());
        tableView.getSelectionModel().selectedItemProperty().addListener(tableSelectedItemListener);
    }

    @Override
    protected void deactivate() {
        pubKeyTextField.textProperty().unbind();
        tableView.getSelectionModel().selectedItemProperty().removeListener(tableSelectedItemListener);
    }
}
