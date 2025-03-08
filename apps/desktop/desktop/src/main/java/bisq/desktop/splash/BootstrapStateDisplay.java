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

import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.node.transport.BootstrapInfo;
import bisq.common.network.TransportType;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

public class BootstrapStateDisplay {
    private final Controller controller;

    public BootstrapStateDisplay(TransportType transportType, ServiceProvider serviceProvider) {
        controller = new Controller(transportType, serviceProvider);
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
        private final SettingsService settingsService;
        private Pin bootstrapStatePin, progressPin, detailsPin;

        private Controller(TransportType transportType, ServiceProvider serviceProvider) {
            this.transportType = transportType;
            this.networkService = serviceProvider.getNetworkService();
            settingsService = serviceProvider.getSettingsService();

            model = new Model(getIconId(transportType));
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            BootstrapInfo bootstrapInfo = networkService.getBootstrapInfoByTransportType().get(transportType);
            bootstrapStatePin = bootstrapInfo.getBootstrapState().addObserver(bootstrapState ->
                    UIThread.run(() -> {
                        switch (bootstrapState) {
                            case BOOTSTRAP_TO_NETWORK:
                                String network = Res.get("splash.bootstrapState.network." + transportType.name());
                                model.getBootstrapState().set(Res.get("splash.bootstrapState." + bootstrapState.name(), network));
                                break;
                            case START_PUBLISH_SERVICE:
                            case SERVICE_PUBLISHED:
                                String service = Res.get("splash.bootstrapState.service." + transportType.name());
                                model.getBootstrapState().set(Res.get("splash.bootstrapState." + bootstrapState.name(), service));
                                break;
                            case CONNECTED_TO_PEERS:
                                model.getBootstrapState().set(Res.get("splash.bootstrapState." + bootstrapState.name()));
                                break;
                        }
                    })
            );
            progressPin = bootstrapInfo.getBootstrapProgress().addObserver(progress ->
                    UIThread.run(() -> model.getProgress().set(PercentageFormatter.formatToPercentWithSymbol(progress))));
            detailsPin = bootstrapInfo.getBootstrapDetails().addObserver(details ->
                    UIThread.run(() -> model.getDetails().set(details)));

            model.getDetailsVisible().set(settingsService.getCookie().asBoolean(CookieKey.SHOW_NETWORK_BOOTSTRAP_DETAILS).orElse(false));
        }

        @Override
        public void onDeactivate() {
            bootstrapStatePin.unbind();
            progressPin.unbind();
            detailsPin.unbind();
        }

        private String getIconId(TransportType transportType) {
            return switch (transportType) {
                case TOR -> "tor";
                case I2P -> "i2p";
                default -> "clearnet";
            };
        }

        void onToggleDetails() {
            boolean newValue = !model.getDetailsVisible().get();
            settingsService.setCookie(CookieKey.SHOW_NETWORK_BOOTSTRAP_DETAILS, newValue);
            model.getDetailsVisible().set(newValue);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final String iconId;
        private final StringProperty bootstrapState = new SimpleStringProperty();
        private final StringProperty details = new SimpleStringProperty();
        private final StringProperty progress = new SimpleStringProperty();
        private final BooleanProperty detailsVisible = new SimpleBooleanProperty();

        public Model(String iconId) {
            this.iconId = iconId;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<GridPane, Model, Controller> {
        private final Label bootstrapState;
        private final Label progress;
        private final Label details;

        private View(Model model, Controller controller) {
            super(new GridPane(), model, controller);

            root.setAlignment(Pos.CENTER);
            root.setHgap(7.5);
            root.setVgap(0);

            ColumnConstraints columnConstraints1 = new ColumnConstraints();
            columnConstraints1.setHalignment(HPos.RIGHT);
            columnConstraints1.setPercentWidth(35);
            ColumnConstraints columnConstraints2 = new ColumnConstraints();
            columnConstraints2.setPercentWidth(65);
            root.getColumnConstraints().addAll(columnConstraints1, columnConstraints2);

            ImageView icon = ImageUtil.getImageViewById(model.getIconId());
            icon.setMouseTransparent(true);
            root.add(icon, 0, 0);

            bootstrapState = new Label();
            bootstrapState.getStyleClass().add("splash-bootstrap-state");
            bootstrapState.setMouseTransparent(true);

            progress = new Label();
            progress.getStyleClass().add("splash-bootstrap-progress");
            progress.setMouseTransparent(true);

            HBox hBox = new HBox(7.5, bootstrapState, progress);
            root.add(hBox, 1, 0);

            details = new Label();
            details.getStyleClass().add("splash-bootstrap-details");
            root.add(details, 1, 1);

            Tooltip.install(root, new Tooltip(Res.get("splash.details.tooltip")));
        }

        @Override
        protected void onViewAttached() {
            bootstrapState.textProperty().bind(model.getBootstrapState());
            progress.textProperty().bind(model.getProgress());
            details.textProperty().bind(model.getDetails());
            details.visibleProperty().bind(model.getDetailsVisible());
            details.managedProperty().bind(model.getDetailsVisible());

            root.setOnMouseClicked(e -> controller.onToggleDetails());
        }

        @Override
        protected void onViewDetached() {
            bootstrapState.textProperty().unbind();
            progress.textProperty().unbind();
            details.textProperty().unbind();
            details.visibleProperty().unbind();
            details.managedProperty().unbind();

            root.setOnMouseClicked(null);
        }
    }
}