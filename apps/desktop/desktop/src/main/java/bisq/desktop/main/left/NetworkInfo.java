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

package bisq.desktop.main.left;

import bisq.common.data.Triple;
import bisq.common.network.TransportType;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.TorTransportService;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.network.p2p.services.peer_group.PeerGroupService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
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
        private Optional<Pin> useExternalTorPin = Optional.empty();

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

            useExternalTorPin = networkService.getServiceNodesByTransport()
                    .findServiceNode(TransportType.TOR)
                    .map(serviceNode -> (TorTransportService) serviceNode.getTransportService())
                    .map(TorTransportService::getUseExternalTor)
                    .map(useExternalTor -> useExternalTor.addObserver(model.getUseExternalTor()::set));

            networkService.getSupportedTransportTypes().forEach(type ->
                    networkService.getServiceNodesByTransport().findServiceNode(type)
                            .ifPresent(serviceNode -> serviceNode.getPeerGroupManager()
                                    .ifPresent(peerGroupManager -> {
                                        applyNumTargetConnections(type, peerGroupManager);
                                        applyNumConnections(type, serviceNode);
                                    })));
        }

        @Override
        public void onDeactivate() {
            useExternalTorPin.ifPresent(Pin::unbind);
            useExternalTorPin = Optional.empty();
        }

        private void applyNumTargetConnections(TransportType type, PeerGroupManager peerGroupManager) {
            PeerGroupService peerGroupService = peerGroupManager.getPeerGroupService();
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
        }

        private void applyNumConnections(TransportType type, ServiceNode serviceNode) {
            Node defaultNode = serviceNode.getDefaultNode();
            defaultNode.addListener(new Node.Listener() {
                @Override
                public void onMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                      Connection connection,
                                      NetworkId networkId) {
                }

                @Override
                public void onConnection(Connection connection) {
                    UIThread.run(() -> onNumConnectionsChanged(type, defaultNode));
                }

                @Override
                public void onDisconnect(Connection connection, CloseReason closeReason) {
                    UIThread.run(() -> onNumConnectionsChanged(type, defaultNode));
                }
            });

            onNumConnectionsChanged(type, defaultNode);
        }

        private void onNavigateToNetworkInfo() {
            onNavigationTargetSelectedHandler.accept(NavigationTarget.NETWORK_P2P);
        }

        private void onNumConnectionsChanged(TransportType transportType, Node node) {
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
        private final StringProperty clearNetNumConnections = new SimpleStringProperty("0");
        private final StringProperty torNumConnections = new SimpleStringProperty("0");
        private final StringProperty i2pNumConnections = new SimpleStringProperty("0");
        private final BooleanProperty useExternalTor = new SimpleBooleanProperty();
    }

    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final HBox clearNetHBox, torHBox, i2pHBox;
        private final Triple<Label, Label, ImageView> clearNetTriple, torTriple, i2pTriple;
        private final BisqTooltip clearNetTooltip, torTooltip, i2pTooltip;
        private Subscription clearNetNumConnectionsPin, torNumConnectionsPin, useExternalTorPin,
                i2pNumConnectionsPin;

        public View(Model model, Controller controller) {
            super(new VBox(8), model, controller);

            root.setMinHeight(VBox.USE_PREF_SIZE);
            root.setPadding(new Insets(26, 24, 0, 24));

            Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> clearNet = getNetworkBox(Res.get("navigation.network.info.clearNet"), "clearnet-grey");
            clearNetHBox = clearNet.getFirst();
            clearNetTooltip = clearNet.getSecond();
            clearNetTriple = clearNet.getThird();

            Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> tor = getNetworkBox(Res.get("navigation.network.info.tor"), "tor-grey");
            torHBox = tor.getFirst();
            torTooltip = tor.getSecond();
            torTriple = tor.getThird();

            Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> i2p = getNetworkBox(Res.get("navigation.network.info.i2p"), "i2p-grey");
            i2pHBox = i2p.getFirst();
            i2pTooltip = i2p.getSecond();
            i2pTriple = i2p.getThird();

            root.getChildren().addAll(clearNetHBox, torHBox, i2pHBox);
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
                    String torInfo = Res.get("navigation.network.info.tor");
                    String postFixInfo = model.useExternalTor.get() ? Res.get("navigation.network.info.externalTor") : "";
                    torTooltip.setText(Res.get("navigation.network.info.tooltip.tor",
                            torInfo, numConnections, model.getTorNumTargetConnections(), postFixInfo));
                }
            });

            useExternalTorPin = EasyBind.subscribe(model.getUseExternalTor(), useExternalTor -> {
                if (useExternalTor != null) {
                    if (useExternalTor) {
                        torHBox.getChildren().stream()
                                .filter(ImageView.class::isInstance)
                                .map(ImageView.class::cast)
                                .forEach(imageView -> {
                                    imageView.setId("tor-green");
                                    imageView.setOpacity(0.75);
                                });
                    }
                }
            });

            i2pNumConnectionsPin = EasyBind.subscribe(model.getI2pNumConnections(), numConnections -> {
                if (numConnections != null) {
                    i2pTriple.getFirst().setText(numConnections);
                    i2pTooltip.setText(Res.get("navigation.network.info.tooltip",
                            Res.get("navigation.network.info.i2p"), numConnections, model.getI2pNumTargetConnections()));
                }
            });

            root.setOnMouseClicked(e -> controller.onNavigateToNetworkInfo());
        }

        @Override
        protected void onViewDetached() {
            clearNetNumConnectionsPin.unsubscribe();
            torNumConnectionsPin.unsubscribe();
            useExternalTorPin.unsubscribe();
            i2pNumConnectionsPin.unsubscribe();

            root.setOnMouseClicked(null);
        }

        private Triple<HBox, BisqTooltip, Triple<Label, Label, ImageView>> getNetworkBox(String title, String imageId) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("bisq-smaller-dimmed-label");
            titleLabel.setMinWidth(Label.USE_PREF_SIZE);

            Label numConnectionsLabel = new Label();
            numConnectionsLabel.getStyleClass().add("bisq-smaller-dimmed-label");
            numConnectionsLabel.setMinWidth(Label.USE_PREF_SIZE);

            Label separator = new Label("|");
            separator.getStyleClass().add("bisq-smaller-dimmed-label");

            Label numTargetConnectionsLabel = new Label();
            numTargetConnectionsLabel.getStyleClass().add("bisq-smaller-dimmed-label");
            numTargetConnectionsLabel.setMinWidth(Label.USE_PREF_SIZE);

            ImageView icon = ImageUtil.getImageViewById(imageId);

            HBox hBox = new HBox(5, icon, titleLabel, numConnectionsLabel, separator, numTargetConnectionsLabel);
            hBox.setAlignment(Pos.CENTER_LEFT);

            BisqTooltip tooltip = new BisqTooltip();
            Tooltip.install(hBox, tooltip);
            Triple<Label, Label, ImageView> triple = new Triple<>(numConnectionsLabel, numTargetConnectionsLabel, icon);
            return new Triple<>(hBox, tooltip, triple);
        }
    }
}
