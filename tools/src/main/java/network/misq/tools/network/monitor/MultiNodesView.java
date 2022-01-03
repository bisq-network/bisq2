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

package network.misq.tools.network.monitor;

import com.typesafe.config.Config;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.util.ConfigUtil;
import network.misq.common.util.OsUtils;
import network.misq.common.util.StringUtils;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.utils.KeyCodeUtils;
import network.misq.network.NetworkService;
import network.misq.network.NetworkServiceConfigFactory;
import network.misq.network.p2p.State;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.network.p2p.services.data.NetworkPayload;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MultiNodesView extends Application implements MultiNodesModel.Handler {
    private MultiNodesModel multiNodesModel;
    private FlowPane clearSeedButtonsPane, torSeedButtonsPane, i2pSeedButtonsPane, clearNodeButtonsPane, torNodeButtonsPane, i2pNodeButtonsPane;
    private TextArea nodeInfoTextArea, networkInfoTextArea;
    private Optional<Address> selected = Optional.empty();
    private final Map<Address, NodeInfoBox> nodeInfoBoxByAddress = new HashMap<>();
    private TextField fromTf;

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
        double stageHeight = 1300;
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
        nodeInfoTextArea.setMinHeight(stageHeight - 500);
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


        fromTf = new TextField("localhost:9000");
        TextField toTf = new TextField("localhost:9999");
        // TextField toTf = new TextField("l2takiyfs5d7nou7wwjomx3a4jxpn4fabtxfclgobrucnokms6j6liid.onion:2000");
        TextField nodeIdTf = new TextField(Node.DEFAULT_NODE_ID);
        TextField msgTf = new TextField("Test message");
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> multiNodesModel.send(new Address(fromTf.getText()),
                new Address(toTf.getText()),
                nodeIdTf.getText(),
                msgTf.getText()));

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> nodeInfoBoxByAddress.values().forEach(NodeInfoBox::reset));

        HBox sendMsgBox = new HBox(10);
        sendMsgBox.setStyle(bgStyle);
        sendMsgBox.setPadding(padding);
        sendMsgBox.getChildren().addAll(new Label("From: "), fromTf,
                new Label("To: "), toTf,
                new Label("Node ID: "), nodeIdTf,
                new Label("Message: "), msgTf,
                sendButton, resetButton);


        VBox vBox = new VBox(10);
        vBox.setPadding(padding);
        vBox.getChildren().addAll(vBoxSeeds, vBoxNodes, sendMsgBox, infoBox);

        ScrollPane scrollPane = new ScrollPane(vBox);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, stageWidth, stageHeight);
        stage.setTitle("Network monitor");
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(event -> quit());
        scene.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
            if (KeyCodeUtils.isCtrlPressed(KeyCode.W, keyEvent) ||
                    KeyCodeUtils.isCtrlPressed(KeyCode.Q, keyEvent)) {
                quit();
            }
        });
    }

    private void quit() {
        multiNodesModel.shutdown().whenComplete((r, t) -> Platform.exit());
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


        String appDir = OsUtils.getUserDataDir() + File.separator + "misq_MultiNodes";
        Config typesafeConfig = ConfigUtil.load("Misq", "misq.networkServiceConfig");
        NetworkService.Config networkServiceConfigFactory = NetworkServiceConfigFactory.getConfig(appDir, typesafeConfig);
        multiNodesModel = new MultiNodesModel(networkServiceConfigFactory, transports, bootstrapAll);
        multiNodesModel.addNetworkInfoConsumer(this);
        multiNodesModel.bootstrap(addressesToBootstrap)
                .forEach((transportType, addresses) -> addresses.forEach(address -> addNodeInfoBox(address, transportType)));
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
                nodeInfoTextArea.setText(multiNodesModel.getNodeInfo(address, transportType));
                if (!nodeInfoTextArea.isFocused()) {
                    nodeInfoTextArea.positionCaret(0);
                }
            }
        });
    }

    @Override
    public void onStateChange(Address address, State networkServiceState) {
        UIThread.run(() -> Optional.ofNullable(nodeInfoBoxByAddress.get(address)).ifPresent(nodeInfoBox -> {
            nodeInfoBox.onStateChange(networkServiceState);
            nodeInfoBox.setDefaultButton(networkServiceState == State.PEER_GROUP_INITIALIZED);
        }));
    }

    @Override
    public void onMessage(Address address) {
        UIThread.run(() -> Optional.ofNullable(nodeInfoBoxByAddress.get(address))
                .flatMap(buttonInfo -> selected.filter(addr -> addr.equals(address)))
                .ifPresent(addr -> updateNodeInfo(address, Transport.Type.from(address))));
    }


    @Override
    public void onData(Address address, NetworkPayload networkPayload) {
        UIThread.run(() -> Optional.ofNullable(nodeInfoBoxByAddress.get(address)).ifPresent(buttonInfo -> {
            selected.filter(addr -> addr.equals(address))
                    .ifPresent(addr -> updateNodeInfo(address, Transport.Type.from(address)));
            NodeInfoBox nodeInfoBox = nodeInfoBoxByAddress.get(address);
            nodeInfoBox.onData(networkPayload);
        }));
    }

    private void addNodeInfoBox(Address address, Transport.Type transportType) {
        UIThread.run(() -> {
            boolean isSeed = multiNodesModel.isSeed(address, transportType);
            NodeInfoBox nodeInfoBox = new NodeInfoBox(address, transportType, isSeed);
            nodeInfoBox.setOnAction(e -> onButtonClicked(address, transportType));
            nodeInfoBoxByAddress.put(address, nodeInfoBox);
            if (isSeed) {
                switch (transportType) {
                    case TOR -> torSeedButtonsPane.getChildren().add(nodeInfoBox);
                    case I2P -> i2pSeedButtonsPane.getChildren().add(nodeInfoBox);
                    case CLEAR -> clearSeedButtonsPane.getChildren().add(nodeInfoBox);
                }
            } else {
                switch (transportType) {
                    case TOR -> torNodeButtonsPane.getChildren().add(nodeInfoBox);
                    case I2P -> i2pNodeButtonsPane.getChildren().add(nodeInfoBox);
                    case CLEAR -> clearNodeButtonsPane.getChildren().add(nodeInfoBox);
                }
            }
        });
    }

    private void onButtonClicked(Address address, Transport.Type transportType) {
        selected = Optional.of(address);
        fromTf.setText(address.getFullAddress());
        updateNodeInfo(address, transportType);

        MenuItem start = new MenuItem("Start");
        MenuItem stop = new MenuItem("Stop");

        start.setDisable(multiNodesModel.findNetworkService(address).isPresent());
        stop.setDisable(multiNodesModel.findNetworkService(address).isEmpty());

        start.setOnAction((event) -> {
            multiNodesModel.bootstrap(address, transportType);
            start.setDisable(true);
            stop.setDisable(false);
        });
        stop.setOnAction((event) -> {
            multiNodesModel.shutdown(address);
            stop.setDisable(true);
            start.setDisable(false);
        });

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(start, stop);
        nodeInfoTextArea.setContextMenu(contextMenu);
    }

    private void updateNodeInfo(Address address, Transport.Type transportType) {
        nodeInfoTextArea.setText(multiNodesModel.getNodeInfo(address, transportType));
        nodeInfoTextArea.positionCaret(0);
    }

    private String getTitle(Address address, Transport.Type transportType) {
        String name = multiNodesModel.isSeed(address, transportType) ? "-Seed: " : "-Node: ";
        return transportType.name() + name + address;
    }
}
