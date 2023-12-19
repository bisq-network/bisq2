package bisq.desktop.main.left;

import bisq.bisq_easy.NavigationTarget;
import bisq.common.data.Triple;
import bisq.desktop.ServiceProvider;
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
import bisq.network.p2p.services.peergroup.PeerGroupService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.Setter;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.function.Consumer;

public class NetworkInfo {
    private final Controller controller;

    public NetworkInfo(ServiceProvider serviceProvider, Consumer<NavigationTarget> onNavigationTargetSelectedHandler) {
        controller = new Controller(serviceProvider, onNavigationTargetSelectedHandler);
    }

    public HBox getRoot() {
        return controller.getView().getRoot();
    }

    public static class Controller implements bisq.desktop.common.view.Controller {

        private final Model model;
        @Getter
        private final View view;
        private final Consumer<NavigationTarget> onNavigationTargetSelectedHandler;
        private final NetworkService networkService;

        public Controller(ServiceProvider serviceProvider, Consumer<NavigationTarget> onNavigationTargetSelectedHandler) {
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

                            Node defaultNode = serviceNode.getDefaultNode();
                            defaultNode.addListener(new Node.Listener() {
                                @Override
                                public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
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
        private final StringProperty clearNetNumConnections = new SimpleStringProperty("0");
        private final StringProperty torNumConnections = new SimpleStringProperty("0");
        private final StringProperty i2pNumConnections = new SimpleStringProperty("0");
    }

    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final HBox clearNetHBox, torHBox, i2pHBox;
        private final Triple<Label, Label, ImageView> clearNetTriple, torTriple, i2pTriple;
        private final BisqTooltip clearNetTooltip, torTooltip, i2pTooltip;
        private Subscription clearNetNumConnectionsPin, torNumConnectionsPin, i2pNumConnectionsPin;

        public View(Model model, Controller controller) {
            super(new HBox(), model, controller);

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

            root.getChildren().addAll(clearNetHBox, torHBox, Spacer.fillHBox(), i2pHBox);
        }

        @Override
        protected void onViewAttached() {
            clearNetHBox.setVisible(model.isClearNetEnabled());
            clearNetHBox.setManaged(model.isClearNetEnabled());
            clearNetTriple.getThird().setOpacity(model.isClearNetEnabled() ? 1 : 0.5);
            clearNetTriple.getSecond().setText(model.getClearNetNumTargetConnections());

            torHBox.setVisible(model.isTorEnabled());
            torHBox.setManaged(model.isTorEnabled());
            torTriple.getThird().setOpacity(model.isTorEnabled() ? 1 : 0.5);
            torTriple.getSecond().setText(model.getTorNumTargetConnections());

            i2pHBox.setVisible(model.isI2pEnabled());
            i2pHBox.setManaged(model.isI2pEnabled());
            i2pTriple.getThird().setOpacity(model.isI2pEnabled() ? 1 : 0.5);
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

            root.setOnMouseClicked(e -> controller.onNavigateToNetworkInfo());
        }

        @Override
        protected void onViewDetached() {
            clearNetNumConnectionsPin.unsubscribe();
            torNumConnectionsPin.unsubscribe();
            i2pNumConnectionsPin.unsubscribe();

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
    }
}
