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
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.node.network_load.NetworkLoadService;
import bisq.presentation.formatters.TimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public class Traffic {
    private final Controller controller;

    public Traffic(ServiceProvider serviceProvider, TransportType transportType) {
        controller = new Controller(serviceProvider, transportType);
    }

    public Pane getViewRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
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
        private final Optional<NetworkLoadService> networkLoadService;

        public Controller(ServiceProvider serviceProvider, TransportType transportType) {
            networkLoadService = serviceProvider.getNetworkService().findServiceNode(transportType)
                    .flatMap(ServiceNode::getNetworkLoadService);

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            networkLoadService
                    .ifPresent(networkLoadService -> {
                        NetworkLoad networkLoad = networkLoadService.updateNetworkLoad();

                        model.setDataSent(DataSizeFormatter.formatMB(networkLoadService.getSentBytesOfLastHour()));
                        model.setTimeSpentSending(TimeFormatter.formatDuration(networkLoadService.getSpentSendMessageTimeOfLastHour()));
                        model.setNumMessagesSent(String.valueOf(networkLoadService.getNumMessagesSentOfLastHour()));
                        model.setSentMessagesDetails(getNumMessagesByMessageClassName(networkLoadService.getNumSentMessagesByMessageClassName()));

                        model.setDataReceived(DataSizeFormatter.formatMB(networkLoadService.getReceivedBytesOfLastHour()));
                        model.setTimeSpentDeserializing(TimeFormatter.formatDuration(networkLoadService.getDeserializeTimeOfLastHour()));
                        model.setNumMessagesReceived(String.valueOf(networkLoadService.getNumMessagesReceivedOfLastHour()));
                        model.setReceivedMessagesDetails(getNumMessagesByMessageClassName(networkLoadService.getNumReceivedMessagesByMessageClassName()));
                    });
        }

        @Override
        public void onDeactivate() {
        }

        private String getNumMessagesByMessageClassName(TreeMap<String, AtomicLong> map) {
            StringBuilder numSentMsgPerClassName = new StringBuilder();
            map.forEach((key, value) -> {
                numSentMsgPerClassName.append("\n        - ");
                numSentMsgPerClassName.append(key);
                numSentMsgPerClassName.append(": ");
                numSentMsgPerClassName.append(value.get());
            });
            return numSentMsgPerClassName.toString();
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private String dataSent = Res.get("data.na");
        @Setter
        private String timeSpentSending = Res.get("data.na");
        @Setter
        private String numMessagesSent = Res.get("data.na");
        @Setter
        private String sentMessagesDetails = Res.get("data.na");

        @Setter
        private String dataReceived = Res.get("data.na");
        @Setter
        private String timeSpentDeserializing = Res.get("data.na");
        @Setter
        private String numMessagesReceived = Res.get("data.na");
        @Setter
        private String receivedMessagesDetails = Res.get("data.na");

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final Label sentDetails, receivedDetails;

        private View(Model model, Controller controller) {
            super(new HBox(30), model, controller);

            Label sentHeadline = new Label(Res.get("settings.network.transport.traffic.sent.headline"));
            sentDetails = new Label();
            VBox sentBox = getVBox(sentHeadline, sentDetails);
            sentBox.setFillWidth(true);

            Label receivedHeadline = new Label(Res.get("settings.network.transport.traffic.received.headline"));
            receivedDetails = new Label();
            VBox receivedBox = getVBox(receivedHeadline, receivedDetails);
            receivedBox.setFillWidth(true);

            root.getChildren().addAll(sentBox, receivedBox);
        }

        @Override
        protected void onViewAttached() {
            sentDetails.setText(Res.get("settings.network.transport.traffic.sent.details",
                    model.getDataSent(),
                    model.getTimeSpentSending(),
                    model.getNumMessagesSent(),
                    model.getSentMessagesDetails()));
            receivedDetails.setText(Res.get("settings.network.transport.traffic.received.details",
                    model.getDataReceived(),
                    model.getTimeSpentDeserializing(),
                    model.getNumMessagesReceived(),
                    model.getReceivedMessagesDetails()));


            sentDetails.minHeightProperty().bind(receivedDetails.heightProperty());
            receivedDetails.minHeightProperty().bind(sentDetails.heightProperty());
        }

        @Override
        protected void onViewDetached() {
            sentDetails.minHeightProperty().unbind();
            receivedDetails.minHeightProperty().unbind();
        }

        private VBox getVBox(Label headline, Label details) {
            headline.getStyleClass().add("standard-table-headline");
            VBox.setMargin(headline, new Insets(0, 0, 0, 10));
            VBox vBox = new VBox(10, headline, details);
            vBox.setAlignment(Pos.TOP_LEFT);
            details.getStyleClass().add("standard-table-view");
            details.setPadding(new Insets(10, 10, 15, 10));
            details.setAlignment(Pos.TOP_LEFT);
            details.setPrefWidth(4000);
            return vBox;
        }
    }
}
