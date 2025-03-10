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

package bisq.desktop.splash;

import bisq.common.network.TransportType;
import bisq.common.observable.Pin;
import bisq.common.util.MathUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIClock;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BootstrapElement {

    public enum BootstrapElementType {
        TRANSPORT,
        SERVER_SOCKET,
        CONNECT_TO_PEER_GROUP
    }

    private final Controller controller;

    public BootstrapElement(ServiceProvider serviceProvider,
                            TransportType transportType,
                            BootstrapElementType bootstrapElementType,
                            Optional<String> identityTag) {
        controller = new Controller(serviceProvider, transportType, bootstrapElementType, identityTag);
    }

    public View getView() {
        return controller.getView();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final TransportType transportType;
        private final NetworkService networkService;
        private final BootstrapElementType bootstrapElementType;
        private final IdentityService identityService;
        private final Optional<String> identityTag;
        private final Set<Pin> pins = new HashSet<>();
        private Node.Listener nodeListener;
        private Node defaultNode;
        private Runnable onSecondTickHandler;

        private Controller(ServiceProvider serviceProvider,
                           TransportType transportType,
                           BootstrapElementType bootstrapElementType,
                           Optional<String> identityTag) {
            this.transportType = transportType;
            this.networkService = serviceProvider.getNetworkService();
            this.bootstrapElementType = bootstrapElementType;
            this.identityTag = identityTag;
            identityService = serviceProvider.getIdentityService();

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            networkService.getServiceNodesByTransport().findServiceNode(transportType)
                    .ifPresent(serviceNode -> {
                        String bootstrapElementTypeName = bootstrapElementType.name();
                        String initializingKey = "splash.stateInfo.initializing." + bootstrapElementTypeName;
                        String initializedKey = "splash.stateInfo.initialized." + bootstrapElementTypeName;
                        switch (bootstrapElementType) {
                            case TRANSPORT -> {
                                String transportTypeName = Res.get("splash.bootstrapState.network." + transportType.name());
                                model.getIconId().set(getIconId(transportType));
                                pins.add(serviceNode.getTransportService().getTimestampByTransportState().addObserver((state, timestamp) -> {
                                    UIThread.run(() -> {
                                        switch (state) {
                                            case INITIALIZE -> {
                                                model.getVisible().set(true);
                                                model.getInfo().set(Res.get(initializingKey, transportTypeName));
                                                model.setTimestamp(timestamp);
                                                applyDurationWhileInitialize();
                                                onSecondTickHandler = this::applyDurationWhileInitialize;
                                                UIClock.addOnSecondTickListener(onSecondTickHandler);
                                            }
                                            case INITIALIZED -> {
                                                model.getInitialized().set(true);
                                                model.getInfo().set(Res.get(initializedKey, transportTypeName));
                                                applyDurationWhenInitialized();
                                                UIClock.removeOnSecondTickListener(onSecondTickHandler);
                                            }
                                        }
                                    });
                                }));
                            }
                            case SERVER_SOCKET -> {
                                String transportSpecificInitializingKey = initializingKey + "." + transportType.name();
                                String transportSpecificInitializedKey = initializedKey + "." + transportType.name();
                                model.getIconId().set("rocket");
                                pins.add(serviceNode.getTransportService().getInitializeServerSocketTimestampByNetworkId().addObserver((networkId, timestamp) -> {
                                    UIThread.run(() -> {
                                        String tag = identityService.findAnyIdentityByNetworkId(networkId).map(Identity::getTag).orElse(Res.get("data.na"));
                                        if (identityTag.isPresent() && identityTag.get().equals(tag)) {
                                            model.getVisible().set(true);
                                            model.getInfo().set(Res.get(transportSpecificInitializingKey, resolveUsername(tag)));
                                            model.setTimestamp(timestamp);
                                            applyDurationWhileInitialize();
                                            onSecondTickHandler = this::applyDurationWhileInitialize;
                                            UIClock.addOnSecondTickListener(onSecondTickHandler);
                                        }
                                    });
                                }));
                                pins.add(serviceNode.getTransportService().getInitializedServerSocketTimestampByNetworkId().addObserver((networkId, timestamp) -> {
                                    UIThread.run(() -> {
                                        String tag = identityService.findAnyIdentityByNetworkId(networkId).map(Identity::getTag).orElse(Res.get("data.na"));
                                        if (identityTag.isPresent() && identityTag.get().equals(tag)) {
                                            model.getInitialized().set(true);
                                            model.getInfo().set(Res.get(transportSpecificInitializedKey, resolveUsername(tag)));
                                            applyDurationWhenInitialized();
                                            UIClock.removeOnSecondTickListener(onSecondTickHandler);
                                        }
                                    });
                                }));
                            }
                            case CONNECT_TO_PEER_GROUP -> {
                                model.getIconId().set("peer-group");
                                pins.add(serviceNode.getState().addObserver(serviceNodeState -> {
                                    if (serviceNodeState == ServiceNode.State.INITIALIZING) {
                                        defaultNode = serviceNode.getDefaultNode();
                                        serviceNode.getPeerGroupManager().ifPresent(peerGroupManager -> {
                                            int targetNumConnectedPeers = peerGroupManager.getPeerGroupService().getTargetNumConnectedPeers();
                                            pins.add(peerGroupManager.getState().addObserver(peerGroupState -> {
                                                UIThread.run(() -> {
                                                    switch (peerGroupState) {
                                                        case INITIALIZING -> {
                                                            model.getVisible().set(true);
                                                            model.getInfo().set(Res.get(initializingKey));
                                                            model.setTimestamp(System.currentTimeMillis());
                                                            model.getHighLightInfo().set(Res.get("splash.stateInfo.peergroup.initializing.highLight", setAndGetDuration(), model.getNumConnections()));
                                                            onSecondTickHandler = () -> updatePeerGroupState(targetNumConnectedPeers, defaultNode);
                                                            UIClock.addOnSecondTickListener(onSecondTickHandler);

                                                            updatePeerGroupState(targetNumConnectedPeers, defaultNode);
                                                            nodeListener = new Node.Listener() {
                                                                @Override
                                                                public void onMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                                                                      Connection connection,
                                                                                      NetworkId networkId) {
                                                                }

                                                                @Override
                                                                public void onConnection(Connection connection) {
                                                                    UIThread.run(() -> updatePeerGroupState(targetNumConnectedPeers, defaultNode));
                                                                }

                                                                @Override
                                                                public void onDisconnect(Connection connection,
                                                                                         CloseReason closeReason) {
                                                                    UIThread.run(() -> updatePeerGroupState(targetNumConnectedPeers, defaultNode));
                                                                }
                                                            };
                                                            defaultNode.addListener(nodeListener);
                                                        }
                                                        case INITIALIZED -> {
                                                            model.getInitialized().set(true);
                                                            // We do not set the initialized state and stop the scheduler as we want to keep
                                                            // the number of connections getting updated.
                                                        }
                                                    }
                                                });
                                            }));
                                        });
                                    }
                                }));
                            }
                        }
                    });
        }


        @Override
        public void onDeactivate() {
            pins.forEach(Pin::unbind);
            UIClock.removeOnSecondTickListener(onSecondTickHandler);
            if (defaultNode != null) {
                defaultNode.removeListener(nodeListener);
            }
        }

        private static String resolveUsername(String tag) {
            // The tag is username + '-' + profileId
            // We want to extract the username but if the username contains a '-' we cannot do it.
            // In any case we truncate to max 15 chars
            String[] parts = tag.split("-");
            if (parts.length == 2) {
                String userName = parts[0];
                return StringUtils.truncate(userName, 20);
            } else {
                return StringUtils.truncate(tag, 20);
            }
        }

        private void updatePeerGroupState(int targetNumConnectedPeers, Node defaultNode) {
            if (targetNumConnectedPeers > 0) {
                int numConnections = defaultNode.getNumConnections();
                model.setNumConnections(numConnections);
                double value = Math.max(0.01, Math.min(1, numConnections / (double) targetNumConnectedPeers));
                if (model.getInitialized().get()) {
                    model.getHighLightInfo().set(Res.get("splash.stateInfo.peergroup.initializing.highLight", model.getDuration(), numConnections));
                } else {
                    model.getHighLightInfo().set(Res.get("splash.stateInfo.peergroup.initializing.highLight", setAndGetDuration(), numConnections));
                }
            }
        }

        private void applyDurationWhileInitialize() {
            model.getHighLightInfo().set(setAndGetDuration());
        }

        private void applyDurationWhenInitialized() {
            model.getHighLightInfo().set(setAndGetDuration());
        }

        private String setAndGetDuration() {
            String duration = MathUtils.roundDoubleToInt((System.currentTimeMillis() - model.getTimestamp()) / 1000d) + " sec.";
            model.setDuration(duration);
            return duration;
        }

        private String getIconId(TransportType transportType) {
            return switch (transportType) {
                case TOR -> "tor";
                case I2P -> "i2p";
                default -> "clearnet";
            };
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty info = new SimpleStringProperty();
        private final StringProperty highLightInfo = new SimpleStringProperty();
        private final StringProperty iconId = new SimpleStringProperty();
        private final BooleanProperty visible = new SimpleBooleanProperty();
        private final BooleanProperty initialized = new SimpleBooleanProperty();
        @Setter
        private long timestamp;
        @Setter
        private String duration;
        @Setter
        private int numConnections;

        public Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label info, separator, duration;
        private final ImageView icon;
        private Subscription initializedPin;

        private View(Model model, Controller controller) {
            super(new HBox(5), model, controller);

            root.setAlignment(Pos.CENTER_LEFT);

            icon = new ImageView();
            icon.setMouseTransparent(true);
            info = new Label();
            separator = new Label("|");
            separator.setOpacity(0.75);
            duration = new Label();
            HBox.setMargin(separator, new Insets(0, 5, 0, 5));
            root.getChildren().addAll(icon, info, separator, duration);
        }

        @Override
        protected void onViewAttached() {
            root.managedProperty().bind(model.getVisible());
            root.visibleProperty().bind(model.getVisible());
            info.textProperty().bind(model.getInfo());
            duration.textProperty().bind(model.getHighLightInfo());
            icon.idProperty().bind(model.getIconId());
            initializedPin = EasyBind.subscribe(model.getInitialized(), initialized -> {
                icon.setOpacity(initialized ? 0.5 : 1);
                if (initialized) {
                    info.getStyleClass().remove("splash-initializing-info");
                    info.getStyleClass().add("splash-initialized-info");
                    separator.getStyleClass().remove("splash-initializing-info");
                    separator.getStyleClass().add("splash-initialized-info");
                    duration.getStyleClass().remove("splash-initializing-duration");
                    duration.getStyleClass().add("splash-initialized-duration");
                } else {
                    info.getStyleClass().remove("splash-initialized-info");
                    info.getStyleClass().add("splash-initializing-info");
                    separator.getStyleClass().remove("splash-initialized-info");
                    separator.getStyleClass().add("splash-initializing-info");
                    duration.getStyleClass().remove("splash-initialized-duration");
                    duration.getStyleClass().add("splash-initializing-duration");
                }
            });
        }

        @Override
        protected void onViewDetached() {
            root.managedProperty().unbind();
            root.visibleProperty().unbind();
            info.textProperty().unbind();
            duration.textProperty().unbind();
            icon.idProperty().unbind();
            initializedPin.unsubscribe();
        }
    }
}