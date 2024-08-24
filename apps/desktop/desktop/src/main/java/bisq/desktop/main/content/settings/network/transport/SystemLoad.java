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

package bisq.desktop.main.content.settings.network.transport;

import bisq.common.formatter.DataSizeFormatter;
import bisq.desktop.ServiceProvider;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.authorization.token.hash_cash.HashCashTokenService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.TimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

public class SystemLoad {
    private final Controller controller;

    public SystemLoad(ServiceProvider serviceProvider, TransportType transportType) {
        controller = new Controller(serviceProvider, transportType);
    }

    public Pane getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final Model model;
        @Getter
        private final View view;
        @Getter
        private final Optional<TransportController> clearNetController = Optional.empty();
        @Getter
        private final Optional<TransportController> torController = Optional.empty();
        @Getter
        private final Optional<TransportController> i2pController = Optional.empty();
        private final Optional<StorageService> storageService;
        private final Optional<NetworkLoadService> networkLoadService;
        private final Optional<HashCashTokenService.Metrics> metrics;

        private Controller(ServiceProvider serviceProvider, TransportType transportType) {
            NetworkService networkService = serviceProvider.getNetworkService();
            storageService = networkService.getDataService().map(DataService::getStorageService);
            networkLoadService = networkService.findServiceNode(transportType)
                    .flatMap(ServiceNode::getNetworkLoadService);
            metrics = networkService.getServiceNodesByTransport().getAuthorizationService().getSupportedServices().values().stream()
                    .filter(authorizationTokenService -> authorizationTokenService instanceof HashCashTokenService)
                    .map(authorizationTokenService -> (HashCashTokenService) authorizationTokenService)
                    .map(HashCashTokenService::getMetrics)
                    .findAny();

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.setNetworkLoad(networkLoadService.map(NetworkLoadService::updateNetworkLoad)
                    .map(NetworkLoad::getLoad)
                    .map(PercentageFormatter::formatToPercentWithSymbol)
                    .orElse(Res.get("data.na")));

            model.setDbSize(storageService.map(StorageService::getNetworkDatabaseSize)
                    .map(DataSizeFormatter::formatMB)
                    .orElse(Res.get("data.na")));

            metrics.ifPresent(metrics -> {
                model.setAccumulatedPoWDuration(TimeFormatter.formatDuration(metrics.getAccumulatedPoWDuration()));
                model.setAveragePowTimePerMessage(TimeFormatter.formatDuration(metrics.getAveragePowTimePerMessage()));
                model.setAverageNetworkLoad(PercentageFormatter.formatToPercentWithSymbol(metrics.getAverageNetworkLoad()));
                model.setNumPowTokensCreated(String.valueOf(metrics.getNumPowTokensCreated()));
            });
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private String dbSize;
        @Setter
        private String networkLoad;
        @Setter
        private String accumulatedPoWDuration = Res.get("data.na");
        @Setter
        private String averagePowTimePerMessage = Res.get("data.na");
        @Setter
        private String averageNetworkLoad = Res.get("data.na");
        @Setter
        private String numPowTokensCreated = Res.get("data.na");

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Label systemLoad;
        private final Label pow;

        private View(Model model, Controller controller) {
            super(new VBox(10), model, controller);

            Label powHeadline = new Label(Res.get("settings.network.transport.pow.headline"));
            powHeadline.getStyleClass().add("standard-table-headline");

            pow = new Label();
            pow.getStyleClass().add("standard-table-view");
            pow.setPadding(new Insets(10));
            pow.setAlignment(Pos.TOP_LEFT);
            pow.setPrefWidth(4000);

            Label systemLoadHeadline = new Label(Res.get("settings.network.transport.systemLoad.headline"));
            systemLoadHeadline.getStyleClass().add("standard-table-headline");

            systemLoad = new Label();
            systemLoad.getStyleClass().add("standard-table-view");
            systemLoad.setPadding(new Insets(10));
            systemLoad.setAlignment(Pos.TOP_LEFT);
            systemLoad.setPrefWidth(4000);

            VBox.setMargin(powHeadline, new Insets(0, 0, 0, 10));
            VBox.setMargin(systemLoadHeadline, new Insets(15, 0, 0, 10));
            root.getChildren().addAll(powHeadline, pow, systemLoadHeadline, systemLoad);
        }

        @Override
        protected void onViewAttached() {
            pow.setText(Res.get("settings.network.transport.pow.details",
                    model.getAccumulatedPoWDuration(),
                    model.getAveragePowTimePerMessage(),
                    model.getNumPowTokensCreated()));
            systemLoad.setText(Res.get("settings.network.transport.systemLoad.details",
                    model.getDbSize(),
                    model.getNetworkLoad(),
                    model.getAverageNetworkLoad()));
        }

        @Override
        protected void onViewDetached() {
        }
    }
}
