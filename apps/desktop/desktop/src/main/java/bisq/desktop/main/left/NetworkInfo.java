package bisq.desktop.main.left;

import bisq.bisq_easy.NavigationTarget;
import bisq.common.data.Pair;
import bisq.common.data.Triple;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.data.inventory.InventoryRequestService;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.function.Consumer;

@Slf4j
public class NetworkInfo {
    private final Controller controller;

    public NetworkInfo(ServiceProvider serviceProvider, Consumer<NavigationTarget> onNavigationTargetSelectedHandler) {
        controller = new Controller(serviceProvider, onNavigationTargetSelectedHandler);
    }

    public VBox getRoot() {
        return controller.getView().getRoot();
    }

    public static class Controller implements bisq.desktop.common.view.Controller {

        private final Model model;
        @Getter
        private final View view;
        private final Consumer<NavigationTarget> onNavigationTargetSelectedHandler;
        private final NetworkService networkService;
        private Pin numPendingRequestsPin, allDataReceivedPin;
        private UIScheduler inventoryRequestAnimation;

        public Controller(ServiceProvider serviceProvider,
                          Consumer<NavigationTarget> onNavigationTargetSelectedHandler) {
            this.onNavigationTargetSelectedHandler = onNavigationTargetSelectedHandler;
            networkService = serviceProvider.getNetworkService();
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.setClearNetEnabled(networkService.isTransportTypeSupported(TransportType.CLEAR));
            model.setTorEnabled(networkService.isTransportTypeSupported(TransportType.TOR));
            model.setI2pEnabled(networkService.isTransportTypeSupported(TransportType.I2P));

            networkService.getSupportedTransportTypes().forEach(type ->
                    networkService.getServiceNodesByTransport().findServiceNode(type).ifPresent(serviceNode -> {
                        serviceNode.getPeerGroupManager().ifPresent(peerGroupManager -> {
                            PeerGroupService peerGroupService = peerGroupManager.getPeerGroupService();
                            Node node = peerGroupManager.getNode();
                            String numTargetConnections = String.valueOf(peerGroupService.getTargetNumConnectedPeers());
                            switch (type) {
                                case CLEAR:
                                    model.setClearNetNumTargetConnections(numTargetConnections);
                                    break;
                                case TOR:
                                    model.setTorNumTargetConnections(numTargetConnections);
                                    break;
                                case I2P:
                                    model.setI2pNumTargetConnections(numTargetConnections);
                                    break;
                            }

                            serviceNode.getInventoryService().ifPresent(inventoryService -> {
                                InventoryRequestService inventoryRequestService = inventoryService.getInventoryRequestService();
                                numPendingRequestsPin = inventoryRequestService.getNumPendingRequests().addObserver(numPendingRequests -> {
                                            if (numPendingRequests != null) {
                                                UIThread.run(() -> {
                                                    model.setPendingInventoryRequests(String.valueOf(numPendingRequests));
                                                    model.inventoryDataChangeFlag.set(!model.inventoryDataChangeFlag.get());
                                                });
                                            }
                                        }
                                );
                                allDataReceivedPin = inventoryRequestService.getAllDataReceived().addObserver(allDataReceived -> {
                                    if (allDataReceived != null) {
                                        UIThread.run(() -> {
                                            model.getAllInventoryDataReceived().set(allDataReceived);

                                            if (allDataReceived) {
                                                if (inventoryRequestAnimation != null) {
                                                    inventoryRequestAnimation.stop();
                                                }

                                                model.setInventoryRequestsInfo(Res.get("navigation.network.info.inventoryRequest.completed"));
                                            }
                                            model.inventoryDataChangeFlag.set(!model.inventoryDataChangeFlag.get());
                                        });
                                    }
                                });
                                inventoryRequestAnimation = UIScheduler.run(() -> {
                                                    String dots = "";
                                                    long numDots = inventoryRequestAnimation.getCounter() % 6;
                                                    for (long l = 0; l < numDots; l++) {
                                                        dots += ".";
                                                    }
                                                    if (!inventoryRequestService.getAllDataReceived().get()) {
                                                        model.setInventoryRequestsInfo(Res.get("navigation.network.info.inventoryRequest.requesting") + dots);
                                                        model.inventoryDataChangeFlag.set(!model.inventoryDataChangeFlag.get());
                                                    }
                                                }
                                        )
                                        .periodically(250);
                                model.setMaxInventoryRequests(String.valueOf(inventoryService.getConfig().getMaxPendingRequestsAtStartup()));

                                model.inventoryDataChangeFlag.set(!model.inventoryDataChangeFlag.get());
                            });

                            Node defaultNode = serviceNode.getDefaultNode();
                            defaultNode.addListener(new Node.Listener() {
                                @Override
                                public void onMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                                      Connection connection,
                                                      NetworkId networkId) {
                                }

                                @Override
                                public void onConnection(Connection connection) {
                                    onNumConnectionsChanged(type, node);
                                }

                                @Override
                                public void onDisconnect(Connection connection, CloseReason closeReason) {
                                    onNumConnectionsChanged(type, node);
                                }
                            });

                            onNumConnectionsChanged(type, node);
                        });
                    })
            );
        }


        @Override
        public void onDeactivate() {
            if (numPendingRequestsPin != null) {
                numPendingRequestsPin.unbind();
            }
            if (allDataReceivedPin != null) {
                allDataReceivedPin.unbind();
            }
            if (inventoryRequestAnimation != null) {
                inventoryRequestAnimation.stop();
            }

        }

        private void onNavigateToNetworkInfo() {
            onNavigationTargetSelectedHandler.accept(NavigationTarget.NETWORK_INFO);
        }

        private void onNumConnectionsChanged(TransportType transportType, Node node) {
            UIThread.run(() -> {
                String value = String.valueOf(node.getNumConnections());
                switch (transportType) {
                    case CLEAR:
                        model.getClearNetNumConnections().set(value);
                        break;
                    case TOR:
                        model.getTorNumConnections().set(value);
                        break;
                    case I2P:
                        model.getI2pNumConnections().set(value);
                        break;
                }
            });
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private boolean clearNetEnabled;
        @Setter
        private boolean torEnabled;
        @Setter
        private boolean i2pEnabled;
        @Setter
        private String clearNetNumTargetConnections;
        @Setter
        private String torNumTargetConnections;
        @Setter
        private String i2pNumTargetConnections;
        @Setter
        private String pendingInventoryRequests;
        @Setter
        private String inventoryRequestsInfo;
        @Setter
        private String maxInventoryRequests;
        private final StringProperty clearNetNumConnections = new SimpleStringProperty("0");
        private final StringProperty torNumConnections = new SimpleStringProperty("0");
        private final StringProperty i2pNumConnections = new SimpleStringProperty("0");
        private final BooleanProperty inventoryDataChangeFlag = new SimpleBooleanProperty();
        private final BooleanProperty allInventoryDataReceived = new SimpleBooleanProperty();
    }

    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final HBox clearNetHBox, torHBox, i2pHBox;
        private final Triple<Label, Label, ImageView> clearNetTriple, torTriple, i2pTriple;
        private final Pair<Label, ImageView> inventoryRequestsPair;
        private final BisqTooltip clearNetTooltip, torTooltip, i2pTooltip, inventoryRequestsTooltip;
        private Subscription clearNetNumConnectionsPin, torNumConnectionsPin, i2pNumConnectionsPin, allInventoryDataReceivedPin, inventoryDataChangeFlagPin;

        public View(Model model, Controller controller) {
            super(new VBox(8), model, controller);

            root.setMinHeight(53);
            root.setMaxHeight(53);
            root.setPadding(new Insets(26, 24, 0, 24));

            Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> clearNet = getNetworkBox(Res.get("navigation.network.info.clearNet"), "clearnet");
            clearNetHBox = clearNet.getFirst();
            clearNetTooltip = clearNet.getSecond();
            clearNetTriple = clearNet.getThird();

            Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> tor = getNetworkBox(Res.get("navigation.network.info.tor"), "tor");
            torHBox = tor.getFirst();
            torTooltip = tor.getSecond();
            torTriple = tor.getThird();

            Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> i2p = getNetworkBox(Res.get("navigation.network.info.i2p"), "i2p");
            i2pHBox = i2p.getFirst();
            i2pTooltip = i2p.getSecond();
            i2pTriple = i2p.getThird();

            HBox hBox = new HBox(clearNetHBox, torHBox, Spacer.fillHBox(), i2pHBox);

            Triple<HBox, BisqTooltip, Pair<Label, ImageView>> inventoryRequests = getInventoryRequestBox();
            HBox inventoryRequestsHBox = inventoryRequests.getFirst();
            inventoryRequestsTooltip = inventoryRequests.getSecond();
            inventoryRequestsPair = inventoryRequests.getThird();

            root.getChildren().addAll(hBox, inventoryRequestsHBox);
        }

        @Override
        protected void onViewAttached() {
            clearNetHBox.setVisible(model.isClearNetEnabled());
            clearNetHBox.setManaged(model.isClearNetEnabled());
            clearNetTriple.getSecond().setText(model.getClearNetNumTargetConnections());

            torHBox.setVisible(model.isTorEnabled());
            torHBox.setManaged(model.isTorEnabled());
            torTriple.getSecond().setText(model.getTorNumTargetConnections());

            i2pHBox.setVisible(model.isI2pEnabled());
            i2pHBox.setManaged(model.isI2pEnabled());
            i2pTriple.getSecond().setText(model.getI2pNumTargetConnections());

            Label inventoryRequestsLabel = inventoryRequestsPair.getFirst();
            inventoryRequestsLabel.setText(model.getMaxInventoryRequests());

            clearNetNumConnectionsPin = EasyBind.subscribe(model.getClearNetNumConnections(), numConnections -> {
                if (numConnections != null) {
                    clearNetTriple.getFirst().setText(numConnections);
                    clearNetTooltip.setText(Res.get("navigation.network.info.tooltip",
                            Res.get("navigation.network.info.clearNet"), numConnections, model.getClearNetNumTargetConnections()));
                }
            });

            torNumConnectionsPin = EasyBind.subscribe(model.getTorNumConnections(), numConnections -> {
                if (numConnections != null) {
                    torTriple.getFirst().setText(numConnections);
                    torTooltip.setText(Res.get("navigation.network.info.tooltip",
                            Res.get("navigation.network.info.tor"), numConnections, model.getTorNumTargetConnections()));
                }
            });

            i2pNumConnectionsPin = EasyBind.subscribe(model.getI2pNumConnections(), numConnections -> {
                if (numConnections != null) {
                    i2pTriple.getFirst().setText(numConnections);
                    i2pTooltip.setText(Res.get("navigation.network.info.tooltip",
                            Res.get("navigation.network.info.i2p"), numConnections, model.getI2pNumTargetConnections()));
                }
            });

            allInventoryDataReceivedPin = EasyBind.subscribe(model.getAllInventoryDataReceived(), allInventoryDataReceived -> {
                log.error("allInventoryDataReceived {}", allInventoryDataReceived);
                if (allInventoryDataReceived) {
                    inventoryRequestsLabel.getStyleClass().remove("bisq-text-yellow-dim");
                    inventoryRequestsLabel.getStyleClass().add("bisq-text-green");

                    ImageView inventoryRequestsIcon = inventoryRequestsPair.getSecond();
                    inventoryRequestsIcon.setId("check-green");

                    // We use the parent hBox
                    Parent node = inventoryRequestsIcon.getParent();
                    node.setOpacity(1);
                    Timeline fadeIn = new Timeline();
                    ObservableList<KeyFrame> fadeInKeyFrames = fadeIn.getKeyFrames();
                    fadeInKeyFrames.add(new KeyFrame(Duration.millis(0), new KeyValue(node.opacityProperty(), 1, Interpolator.LINEAR)));
                    fadeInKeyFrames.add(new KeyFrame(Duration.millis(4000), new KeyValue(node.opacityProperty(), 1, Interpolator.LINEAR)));
                    fadeInKeyFrames.add(new KeyFrame(Duration.millis(5000), new KeyValue(node.opacityProperty(), 0, Interpolator.EASE_OUT)));
                    fadeIn.setOnFinished(e -> {
                        inventoryRequestsLabel.getStyleClass().remove("bisq-text-green");
                        inventoryRequestsLabel.getStyleClass().add("bisq-text-grey-9");
                        inventoryRequestsIcon.setId("check-grey");

                        Timeline fadeOut = new Timeline();
                        ObservableList<KeyFrame> fadeOutKeyFrames = fadeOut.getKeyFrames();
                        fadeOutKeyFrames.add(new KeyFrame(Duration.millis(0), new KeyValue(node.opacityProperty(), 0, Interpolator.LINEAR)));
                        fadeOutKeyFrames.add(new KeyFrame(Duration.millis(1000), new KeyValue(node.opacityProperty(), 0, Interpolator.LINEAR)));
                        fadeOutKeyFrames.add(new KeyFrame(Duration.millis(3000), new KeyValue(node.opacityProperty(), 1, Interpolator.EASE_OUT)));
                        fadeOut.play();
                    });
                    fadeIn.play();
                } else {
                    inventoryRequestsLabel.getStyleClass().add("bisq-text-yellow-dim");
                }
            });
            inventoryDataChangeFlagPin = EasyBind.subscribe(model.getInventoryDataChangeFlag(), inventoryDataChangeFlag -> {
                if (inventoryDataChangeFlag != null) {
                    inventoryRequestsLabel.setText(model.getInventoryRequestsInfo());
                    boolean allInventoryDataReceived = model.getAllInventoryDataReceived().get();
                    String allReceived = allInventoryDataReceived ? Res.get("confirmation.yes") : Res.get("confirmation.no");
                    inventoryRequestsTooltip.setText(
                            Res.get("navigation.network.info.inventoryRequests.tooltip",
                                    model.getPendingInventoryRequests(),
                                    model.getMaxInventoryRequests(),
                                    allReceived));
                    ImageView inventoryRequestsIcon = inventoryRequestsPair.getSecond();
                    inventoryRequestsIcon.setVisible(allInventoryDataReceived);
                    inventoryRequestsIcon.setManaged(allInventoryDataReceived);
                }
            });

            root.setOnMouseClicked(e -> controller.onNavigateToNetworkInfo());
        }

        @Override
        protected void onViewDetached() {
            clearNetNumConnectionsPin.unsubscribe();
            torNumConnectionsPin.unsubscribe();
            i2pNumConnectionsPin.unsubscribe();
            inventoryDataChangeFlagPin.unsubscribe();
            allInventoryDataReceivedPin.unsubscribe();

            root.setOnMouseClicked(null);
        }

        private Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> getNetworkBox(String title, String imageId) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("bisq-smaller-dimmed-label");

            Label numConnectionsLabel = new Label();
            numConnectionsLabel.getStyleClass().add("bisq-smaller-label");

            Label separator = new Label("|");
            separator.getStyleClass().add("bisq-smaller-dimmed-label");

            Label numTargetConnectionsLabel = new Label();
            numTargetConnectionsLabel.getStyleClass().add("bisq-smaller-label");

            ImageView icon = ImageUtil.getImageViewById(imageId);
            HBox.setMargin(icon, new Insets(0, 0, 0, 2));

            HBox hBox = new HBox(5, titleLabel, numConnectionsLabel, separator, numTargetConnectionsLabel, icon);

            BisqTooltip tooltip = new BisqTooltip();
            Tooltip.install(hBox, tooltip);
            Triple<Label, Label, ImageView> triple = new Triple<>(numConnectionsLabel, numTargetConnectionsLabel, icon);
            return new Triple<>(hBox, tooltip, triple);
        }

        private Triple<HBox, BisqTooltip, Pair<Label, ImageView>> getInventoryRequestBox() {
            Label info = new Label();
            info.getStyleClass().add("bisq-smaller-dimmed-label");

            ImageView icon = ImageUtil.getImageViewById("check-white");
            HBox.setMargin(icon, new Insets(0, 0, 0, 2));

            HBox hBox = new HBox(5, info, icon);
            BisqTooltip tooltip = new BisqTooltip();
            Tooltip.install(hBox, tooltip);
            Pair<Label, ImageView> pair = new Pair<>(info, icon);
            return new Triple<>(hBox, tooltip, pair);
        }
    }
}
