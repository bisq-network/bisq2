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

package network.misq.monitor;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.StringUtils;
import network.misq.desktop.common.threading.UIThread;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.monitor.MultiNodesNetworkMonitor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class NetworkMonitorUI extends Application {
    private MultiNodesNetworkMonitor multiNodesNetworkMonitor;
    private FlowPane seedsPane, nodesPane;
    private TextArea nodeInfoTextArea, networkInfoTextArea;
    private Optional<Address> selected = Optional.empty();

    @Override
    public void start(Stage primaryStage) {
        setupUi(primaryStage);
        setupMultiNodesNetworkMonitor();
    }

    private void setupUi(Stage primaryStage) {
        String bgStyle = "-fx-background-color: #dadada";
        String seedStyle = "-fx-background-color: #80afa1";

        Insets bgPadding = new Insets(10, 10, 10, 10);
        Insets labelPadding = new Insets(4, -20, 0, 0);
        int nodeWidth = 50;
        double stageWidth = 2000;
        double availableWidth = stageWidth - 2 * bgPadding.getLeft() - 2 * bgPadding.getRight();
        int nodesPerRow = (int) (availableWidth / nodeWidth);
        stageWidth = nodesPerRow * nodeWidth + 2 * bgPadding.getLeft() + 2 * bgPadding.getRight();

        seedsPane = new FlowPane();
        seedsPane.setStyle(seedStyle);
        seedsPane.setPadding(bgPadding);

        nodesPane = new FlowPane();
        nodesPane.setStyle(bgStyle);
        nodesPane.setPadding(bgPadding);

        HBox modesBox = new HBox(20);
        modesBox.setStyle(bgStyle);
        modesBox.setPadding(bgPadding);

        Label modeLabel = new Label("Mode: ");
        modeLabel.setPadding(labelPadding);
        modesBox.getChildren().add(modeLabel);

        HBox actionBox = new HBox(20);
        actionBox.setStyle(bgStyle);
        actionBox.setPadding(bgPadding);

        Label actionLabel = new Label("Info: ");
        actionLabel.setPadding(labelPadding);
        actionBox.getChildren().add(actionLabel);


        HBox infoBox = new HBox(20);
        infoBox.setStyle(bgStyle);
        infoBox.setPadding(bgPadding);

        Label infoLabel = new Label("Node info: ");
        infoLabel.setPadding(labelPadding);
        infoBox.getChildren().add(actionLabel);

        nodeInfoTextArea = new TextArea();
        nodeInfoTextArea.setEditable(false);
        nodeInfoTextArea.setMinHeight(900);
        nodeInfoTextArea.setMinWidth(900);
        nodeInfoTextArea.setFont(Font.font("Courier", FontWeight.NORMAL, FontPosture.REGULAR, 13));
        nodeInfoTextArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                nodeInfoTextArea.clear();
            }
        });

        networkInfoTextArea = new TextArea();
        networkInfoTextArea.setEditable(false);
        networkInfoTextArea.minHeightProperty().bind(nodeInfoTextArea.minHeightProperty());
        networkInfoTextArea.minWidthProperty().bind(nodeInfoTextArea.minWidthProperty());
        networkInfoTextArea.setFont(Font.font("Courier", FontWeight.NORMAL, FontPosture.REGULAR, 13));

        ContextMenu contextMenu = new ContextMenu();
        MenuItem clear = new MenuItem("Clear");
        clear.setOnAction((event) -> {
            networkInfoTextArea.clear();
        });
        contextMenu.getItems().add(clear);
        networkInfoTextArea.setContextMenu(contextMenu);

        infoBox.getChildren().addAll(nodeInfoTextArea, networkInfoTextArea);

        VBox vBox = new VBox(20);
        vBox.setPadding(bgPadding);
        vBox.getChildren().addAll(seedsPane, nodesPane, infoBox);

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, stageWidth, 1400);
        primaryStage.setTitle("Network simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupMultiNodesNetworkMonitor() {
        Map<String, String> params = getParameters().getNamed();
        Optional<String> myAddress = Optional.ofNullable(params.get("myAddress"));
        Optional<List<Address>> addressesToBootstrap = Optional.ofNullable(params.get("addressesToBootstrap"))
                .map(StringUtils::trimWhitespace)
                .map(str -> List.of(str.split(",")))
                .map(list -> list.stream().map(Address::new).collect(Collectors.toList()));
        boolean bootstrapAll = Optional.ofNullable(params.get("bootstrapAll"))
                .map(e -> e.equalsIgnoreCase("true"))
                .orElse(addressesToBootstrap.isEmpty() && myAddress.isEmpty());
        multiNodesNetworkMonitor = new MultiNodesNetworkMonitor(Transport.Type.CLEAR_NET, bootstrapAll, addressesToBootstrap);
        multiNodesNetworkMonitor.addNetworkInfoConsumer(pair -> {
            UIThread.run(() -> {
                if (networkInfoTextArea.isFocused()) {
                    double pos = networkInfoTextArea.getScrollTop();
                    int anchor = networkInfoTextArea.getAnchor();
                    int caret = networkInfoTextArea.getCaretPosition();
                    networkInfoTextArea.appendText(pair.second());
                    networkInfoTextArea.setScrollTop(pos);
                    networkInfoTextArea.selectRange(anchor, caret);
                } else {
                    networkInfoTextArea.appendText(pair.second());
                }

                Address address = pair.first();
                if (selected.isEmpty() || selected.get().equals(address)) {
                    nodeInfoTextArea.setText(multiNodesNetworkMonitor.getNodeInfo(address));
                    if (!nodeInfoTextArea.isFocused()) {
                        nodeInfoTextArea.positionCaret(0);
                    }
                }
            });
        });
        List<Address> addresses = multiNodesNetworkMonitor.bootstrapNetworkServices();
        UIThread.run(() -> addresses.forEach(this::addButton));
    }

    private void addButton(Address address) {
        int port = address.getPort();
        String name = multiNodesNetworkMonitor.isSeed(address) ? "Seed " : "Node ";
        Button button = new Button(name + port);
        button.setMinWidth(100);
        button.setMaxWidth(button.getMinWidth());
        button.setOnAction(e -> onButtonClicked(address));
        if (multiNodesNetworkMonitor.isSeed(address)) {
            seedsPane.getChildren().add(button);
        } else {
            nodesPane.getChildren().add(button);
        }
    }

    private void onButtonClicked(Address address) {
        selected = Optional.of(address);
        nodeInfoTextArea.setText(multiNodesNetworkMonitor.getNodeInfo(address));
        nodeInfoTextArea.positionCaret(0);

        ContextMenu contextMenu = new ContextMenu();
        MenuItem start = new MenuItem("Start");
        start.setOnAction((event) -> multiNodesNetworkMonitor.bootstrap(address));
        MenuItem stop = new MenuItem("Stop");
        stop.setOnAction((event) -> multiNodesNetworkMonitor.shutdown(address));
        contextMenu.getItems().addAll(start, stop);
        nodeInfoTextArea.setContextMenu(contextMenu);
    }
}
