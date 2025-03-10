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
import bisq.desktop.ServiceProvider;
import bisq.identity.IdentityService;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static bisq.desktop.splash.BootstrapElement.BootstrapElementType.*;
import static bisq.identity.IdentityService.DEFAULT_IDENTITY_TAG;

public class BootstrapElementsPerTransport {
    private final Controller controller;

    public BootstrapElementsPerTransport(TransportType transportType, ServiceProvider serviceProvider) {
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
        private final ServiceProvider serviceProvider;
        private final IdentityService identityService;

        private Controller(TransportType transportType, ServiceProvider serviceProvider) {
            this.transportType = transportType;
            this.serviceProvider = serviceProvider;
            identityService = serviceProvider.getIdentityService();

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            List<BootstrapElement> bootstrapElementList = model.getBootstrapElementList();
            bootstrapElementList.add(new BootstrapElement(serviceProvider, transportType, TRANSPORT, Optional.empty()));
            bootstrapElementList.add(new BootstrapElement(serviceProvider, transportType, SERVER_SOCKET, Optional.of(DEFAULT_IDENTITY_TAG)));
            bootstrapElementList.add(new BootstrapElement(serviceProvider, transportType, CONNECT_TO_PEER_GROUP, Optional.empty()));
            identityService.getActiveIdentityByTag().keySet().forEach(tag ->
                    bootstrapElementList.add(new BootstrapElement(serviceProvider, transportType, SERVER_SOCKET, Optional.of(tag))));
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final List<BootstrapElement> bootstrapElementList = new ArrayList<>();

        public Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private View(Model model, Controller controller) {
            super(new VBox(5), model, controller);

            root.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void onViewAttached() {
            root.getChildren().addAll(model.getBootstrapElementList().stream()
                    .map(bootstrapElement -> bootstrapElement.getView().getRoot())
                    .collect(Collectors.toList()));
        }

        @Override
        protected void onViewDetached() {
            root.getChildren().clear();
        }
    }
}