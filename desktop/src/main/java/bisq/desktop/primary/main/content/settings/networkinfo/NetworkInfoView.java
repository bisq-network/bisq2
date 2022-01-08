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

package bisq.desktop.primary.main.content.settings.networkinfo;

import bisq.common.data.Pair;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.TabView;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.primary.main.content.settings.networkinfo.transport.TransportTypeView;
import bisq.i18n.Res;
import bisq.network.p2p.node.transport.Transport;
import com.jfoenix.controls.JFXTabPane;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class NetworkInfoView extends TabView<JFXTabPane, NetworkInfoModel, NetworkInfoController> {
    private final Map<Transport.Type, Tab> tabByTransportType = new HashMap<>();
    private final ChangeListener<Optional<TransportTypeView>> transportTypeViewChangeListener;
    private final ChangeListener<Tab> tabChangeListener;
    private final BisqTextArea receivedMessagesTextArea;
    private final BisqTableView<DataListItem> dataTableView;
    private final TextField messageReceiverTextField, nodeIdTextField;
    private ChangeListener<DataListItem> dataTableSelectedItemListener;

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller) {
        super(new JFXTabPane(), model, controller);

        root.setPadding(new Insets(20,20,20,0));

        BisqGridPane bisqGridPane = new BisqGridPane();

        tabChangeListener = (observable, oldValue, newValue) -> {
            controller.onTabSelected(Optional.ofNullable(newValue).map(tab -> Transport.Type.valueOf(tab.getId())));
        };

        transportTypeViewChangeListener = (observable, oldValue, transportTypeViewOptional) -> {
            Optional<Tab> tabOptional = model.getSelectedTransportType().flatMap(e -> Optional.ofNullable(tabByTransportType.get(e)));
            tabOptional.ifPresent(tab -> tab.setContent(transportTypeViewOptional.map(View::getRoot).orElse(null)));
            root.getSelectionModel().select(tabOptional.orElse(null));
            root.requestFocus();
        };

        bisqGridPane.startSection(Res.network.get("addData.title"));
        TextField dataContentTextField = bisqGridPane.addTextField(Res.network.get("addData.content"), "Test data");
        TextField idTextField = bisqGridPane.addTextField(Res.network.get("addData.id"), UUID.randomUUID().toString().substring(0, 8));
        Pair<Button, Label> addDataButtonPair = bisqGridPane.addButton(Res.network.get("addData.add"));
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
        bisqGridPane.endSection();

        bisqGridPane.startSection(Res.network.get("table.data.title"));
        dataTableView = new BisqTableView<>(model.getSortedDataListItems());
        dataTableView.setMinHeight(200);
        bisqGridPane.addTableView(dataTableView);
        configDataTableView();
        bisqGridPane.endSection();

        bisqGridPane.startSection(Res.network.get("sendMessages.title"));
        messageReceiverTextField = bisqGridPane.addTextField(Res.network.get("sendMessages.to"), "localhost:8000");
        messageReceiverTextField.setEditable(false);
        nodeIdTextField = bisqGridPane.addTextField(Res.network.get("sendMessages.nodeId"), "");
        nodeIdTextField.setEditable(false);
        TextField msgTextField = bisqGridPane.addTextField(Res.network.get("sendMessages.text"), "Test proto");
        Pair<Button, Label> sendButtonPair = bisqGridPane.addButton(Res.network.get("sendMessages.send"));
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
        bisqGridPane.addHSpacer();
        receivedMessagesTextArea = bisqGridPane.addTextArea(Res.network.get("sendMessages.receivedMessage"), model.getReceivedMessages());
        receivedMessagesTextArea.setMinHeight(100);
        bisqGridPane.endSection();

        dataTableSelectedItemListener = (observable, oldValue, newValue) -> {
            controller.onSelectNetworkId(newValue.getNetworkId());
        };
    }

    @Override
    protected void createAndAddTabs() {
        Tab clearNetTab = createTab(Transport.Type.CLEAR, Res.network.get("clearNet"));
        Tab torTab = createTab(Transport.Type.TOR, "Tor");
        Tab i2pTab = createTab(Transport.Type.I2P, "I2P");
        root.getTabs().setAll(clearNetTab, torTab, i2pTab);
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();

        model.getTransportTypeView().addListener(transportTypeViewChangeListener);
        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);

        Tab clearNetTab = tabByTransportType.get(Transport.Type.CLEAR);
        clearNetTab.disableProperty().bind(model.getClearNetDisabled());
        Tab torTab = tabByTransportType.get(Transport.Type.TOR);
        torTab.disableProperty().bind(model.getTorDisabled());
        Tab i2pTab = tabByTransportType.get(Transport.Type.I2P);
        i2pTab.disableProperty().bind(model.getI2pDisabled());

        if (!model.getClearNetDisabled().get()) {
            root.getSelectionModel().select(clearNetTab);
        } else if (!model.getTorDisabled().get()) {
            root.getSelectionModel().select(torTab);
        } else if (!model.getI2pDisabled().get()) {
            root.getSelectionModel().select(i2pTab);
        }

        nodeIdTextField.textProperty().bind(model.getNodeIdString());
        messageReceiverTextField.textProperty().bind(model.getMessageReceiver());
        dataTableView.getSelectionModel().selectedItemProperty().addListener(dataTableSelectedItemListener);
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        
        model.getTransportTypeView().removeListener(transportTypeViewChangeListener);
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
        tabByTransportType.values().forEach(tab -> tab.disableProperty().unbind());

        nodeIdTextField.textProperty().unbind();
        messageReceiverTextField.textProperty().unbind();
        dataTableView.getSelectionModel().selectedItemProperty().removeListener(dataTableSelectedItemListener);
    }

    private Tab createTab(Transport.Type transportType, String title) {
        Tab tab = new Tab(title.toUpperCase());
        tab.setClosable(false);
        tab.setId(model.getNavigationTargetFromTransportType(transportType).name()); 
        tabByTransportType.put(transportType, tab);
        return tab;
    }

    private void configDataTableView() {
        var dateColumn = new BisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.received"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(DataListItem::getReceived)
                .comparator(DataListItem::compareDate)
                .build();
        dataTableView.getColumns().add(dateColumn);
        dateColumn.setSortType(TableColumn.SortType.DESCENDING);
        dataTableView.getSortOrder().add(dateColumn);

        dataTableView.getColumns().add(new BisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.content"))
                .minWidth(320)
                .valueSupplier(DataListItem::getContent)
                .build());
        dataTableView.getColumns().add(new BisqTableColumn.Builder<DataListItem>()
                .minWidth(320)
                .title(Res.network.get("table.data.header.nodeId"))
                .valueSupplier(DataListItem::getNodeId)
                .build());
    }

}
