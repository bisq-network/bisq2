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
import javafx.application.Platform;
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
import network.misq.common.data.Pair;
import network.misq.common.util.StringUtils;
import network.misq.desktop.common.threading.UIThread;
import network.misq.network.p2p.State;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.monitor.MultiNodesNetworkMonitor;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MultiNodesNetworkMonitorUI extends Application implements MultiNodesNetworkMonitor.Handler {
    private MultiNodesNetworkMonitor multiNodesNetworkMonitor;
    private FlowPane clearSeedButtonsPane, torSeedButtonsPane, i2pSeedButtonsPane, clearNodeButtonsPane, torNodeButtonsPane, i2pNodeButtonsPane;
    private TextArea nodeInfoTextArea, networkInfoTextArea;
    private Optional<Address> selected = Optional.empty();
    private Map<Address, Pair<Button, Transport.Type>> buttonsByAddress = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        setupUi(primaryStage);
        setupMultiNodesNetworkMonitor();
    }

    private void setupUi(Stage stage) {
        String bgStyle = "-fx-background-color: #dadada";
        String seedStyle = "-fx-background-color: #80afa1";

        Insets padding = new Insets(10, 10, 10, 10);
        int nodeWidth = 50;
        double stageWidth = 2400;
        double stageHeight = 1000;
        double availableWidth = stageWidth - 2 * padding.getLeft() - 2 * padding.getRight();
        int nodesPerRow = (int) (availableWidth / nodeWidth);
        stageWidth = nodesPerRow * nodeWidth + 2 * padding.getLeft() + 2 * padding.getRight();

        VBox vBoxSeeds = new VBox(5);
        clearSeedButtonsPane = getFlowPane(seedStyle, padding);
        torSeedButtonsPane = getFlowPane(seedStyle, padding);
        i2pSeedButtonsPane = getFlowPane(seedStyle, padding);
        vBoxSeeds.getChildren().addAll(clearSeedButtonsPane, torSeedButtonsPane, i2pSeedButtonsPane);

        VBox vBoxNodes = new VBox(5);
        clearNodeButtonsPane = getFlowPane(bgStyle, padding);
        torNodeButtonsPane = getFlowPane(bgStyle, padding);
        i2pNodeButtonsPane = getFlowPane(bgStyle, padding);
        vBoxNodes.getChildren().addAll(clearNodeButtonsPane, torNodeButtonsPane, i2pNodeButtonsPane);

        nodeInfoTextArea = new TextArea();
        nodeInfoTextArea.setEditable(false);
        nodeInfoTextArea.setMinHeight(stageHeight - 200);
        nodeInfoTextArea.setMinWidth(stageWidth / 2 - 100);
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

        HBox infoBox = new HBox(10);
        infoBox.setStyle(bgStyle);
        infoBox.setPadding(padding);
        infoBox.getChildren().addAll(nodeInfoTextArea, networkInfoTextArea);

        VBox vBox = new VBox(10);
        vBox.setPadding(padding);
        vBox.getChildren().addAll(vBoxSeeds, vBoxNodes, infoBox);

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, stageWidth, stageHeight);
        stage.setTitle("Network monitor");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> {
            multiNodesNetworkMonitor.shutdown().whenComplete((r, t) -> Platform.exit());
        });
    }

    private FlowPane getFlowPane(String style, Insets bgPadding) {
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(5);
        flowPane.setStyle(style);
        flowPane.setPadding(bgPadding);
        return flowPane;
    }

    private void setupMultiNodesNetworkMonitor() {
        Map<String, String> params = getParameters().getNamed();
        Optional<String> myAddress = Optional.ofNullable(params.get("myAddress"));
        Set<Transport.Type> transports = Optional.ofNullable(params.get("transports"))
                .map(StringUtils::trimWhitespace)
                .map(str -> List.of(str.split(",")))
                .map(list -> list.stream().map(Transport.Type::valueOf).collect(Collectors.toSet()))
                .orElse(Set.of(Transport.Type.CLEAR));
        String addressesToBootstrap1 = params.get("addressesToBootstrap");
        Optional<List<Address>> addressesToBootstrap = Optional.ofNullable(addressesToBootstrap1)
                .map(StringUtils::trimWhitespace)
                .map(str -> List.of(str.split(",")))
                .map(list -> list.stream().map(Address::new).collect(Collectors.toList()));
        boolean bootstrapAll = Optional.ofNullable(params.get("bootstrapAll"))
                .map(e -> e.equalsIgnoreCase("true"))
                .orElse(addressesToBootstrap.isEmpty() && myAddress.isEmpty());
        multiNodesNetworkMonitor = new MultiNodesNetworkMonitor(transports, bootstrapAll, addressesToBootstrap);
        multiNodesNetworkMonitor.addNetworkInfoConsumer(this);
        multiNodesNetworkMonitor.bootstrap()
                .forEach((transportType, addresses) -> addresses.forEach(address -> addButton(address, transportType)));
    }

    @Override
    public void onConnectionStateChange(Transport.Type transportType, Address address, String networkInfo) {
        UIThread.run(() -> {
            if (networkInfoTextArea.isFocused()) {
                double pos = networkInfoTextArea.getScrollTop();
                int anchor = networkInfoTextArea.getAnchor();
                int caret = networkInfoTextArea.getCaretPosition();
                networkInfoTextArea.appendText(networkInfo);
                networkInfoTextArea.setScrollTop(pos);
                networkInfoTextArea.selectRange(anchor, caret);
            } else {
                networkInfoTextArea.appendText(networkInfo);
            }

            if (selected.isEmpty() || selected.get().equals(address)) {
                nodeInfoTextArea.setText(multiNodesNetworkMonitor.getNodeInfo(address, transportType));
                if (!nodeInfoTextArea.isFocused()) {
                    nodeInfoTextArea.positionCaret(0);
                }
            }
        });
    }

    @Override
    public void onStateChange(Address address, State networkServiceState) {
        UIThread.run(() -> Optional.ofNullable(buttonsByAddress.get(address)).ifPresent(buttonInfo -> {
            Button button = buttonInfo.first();
            button.setText(getTitle(address, buttonInfo.second()) + " [" + networkServiceState.name() + "]");
            button.setDefaultButton(networkServiceState == State.BOOTSTRAPPED);
        }));
    }

    private void addButton(Address address, Transport.Type transportType) {
        UIThread.run(() -> {
            Button button = new Button(getTitle(address, transportType));
            button.setUserData(address);
            button.setOnAction(e -> onButtonClicked(address, transportType));
            buttonsByAddress.put(address, new Pair<>(button, transportType));
            if (multiNodesNetworkMonitor.isSeed(address, transportType)) {
                switch (transportType) {
                    case TOR -> torSeedButtonsPane.getChildren().add(button);
                    case I2P -> i2pSeedButtonsPane.getChildren().add(button);
                    case CLEAR -> clearSeedButtonsPane.getChildren().add(button);
                }
            } else {
                switch (transportType) {
                    case TOR -> torNodeButtonsPane.getChildren().add(button);
                    case I2P -> i2pNodeButtonsPane.getChildren().add(button);
                    case CLEAR -> clearNodeButtonsPane.getChildren().add(button);
                }
            }
        });
    }

    private void onButtonClicked(Address address, Transport.Type transportType) {
        selected = Optional.of(address);
        nodeInfoTextArea.setText(multiNodesNetworkMonitor.getNodeInfo(address, transportType));
        nodeInfoTextArea.positionCaret(0);

        MenuItem start = new MenuItem("Start");
        MenuItem stop = new MenuItem("Stop");

        start.setDisable(multiNodesNetworkMonitor.findNetworkService(address).isPresent());
        stop.setDisable(multiNodesNetworkMonitor.findNetworkService(address).isEmpty());

        start.setOnAction((event) -> {
            multiNodesNetworkMonitor.bootstrap(address, transportType);
            start.setDisable(true);
            stop.setDisable(false);
        });
        stop.setOnAction((event) -> {
            multiNodesNetworkMonitor.shutdown(address);
            stop.setDisable(true);
            start.setDisable(false);
        });

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(start, stop);
        nodeInfoTextArea.setContextMenu(contextMenu);
    }

    private String getTitle(Address address, Transport.Type transportType) {
        String name = multiNodesNetworkMonitor.isSeed(address, transportType) ? "-Seed: " : "-Node: ";
        return transportType.name() + name + address;
    }
}
