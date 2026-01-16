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

package bisq.desktop.main.content.settings.bisq_connect;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.api.web_socket.WebsocketClient1;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqConnectView extends View<VBox, BisqConnectModel, BisqConnectController> {
    private final Label webSocketServerStateLabel;
    private final Circle webSocketServerState;
    private final MaterialTextField qrCode;
    private final ImageView qrImageView;
    private final VBox pairingVBox;
    private final RichTableView<WebsocketClient1> clientsTable;
    private final MaterialTextField webSocketServerUrl;
    private Subscription webSocketServiceStatePin;

    BisqConnectView(BisqConnectModel model, BisqConnectController controller, VBox apiConfigVbox) {
        super(new VBox(35), model, controller);

        root.setPadding(new Insets(0, 40, 20, 40));
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = SettingsViewUtils.getHeadline(Res.get("settings.bisqConnect.headline"));
        Label info = getInfoLabel(Res.get("settings.bisqConnect.info"));

        webSocketServerState = new Circle(8);
        webSocketServerState.setFill(Color.GRAY);
        webSocketServerStateLabel = new Label();
        webSocketServerStateLabel.getStyleClass().addAll("normal-text", "text-fill-grey-dimmed");

        webSocketServerUrl = new MaterialTextField(Res.get("settings.bisqConnect.webSocketServerUrl.description"));
        webSocketServerUrl.showCopyIcon();
        webSocketServerUrl.setEditable(false);

        HBox websocketServerStateBox = new HBox(10, webSocketServerState, webSocketServerStateLabel);
        websocketServerStateBox.setAlignment(Pos.CENTER_LEFT);

        // Pairing
        Label pairingHeadline = SettingsViewUtils.getHeadline(Res.get("settings.bisqConnect.pairing.headline"));
        Label pairingInfo = getInfoLabel(Res.get("settings.bisqConnect.pairing.info"));

        qrImageView = new ImageView();
        qrImageView.setFitHeight(model.getQrCodeSize());
        qrImageView.setFitWidth(model.getQrCodeSize());
        qrImageView.setPreserveRatio(true);

        Label qrCodeInfo = getInfoLabel(Res.get("settings.bisqConnect.qrCode.info"));
        qrCode = new MaterialTextField(Res.get("settings.bisqConnect.qrCode.description"));
        qrCode.showCopyIcon();

        VBox.setMargin(qrCodeInfo, new Insets(10, 0, -15, 0));
        pairingVBox = new VBox(25, pairingHeadline, SettingsViewUtils.getLineAfterHeadline(30), pairingInfo,
                qrImageView,
                qrCodeInfo, qrCode);

        VBox.setMargin(websocketServerStateBox, new Insets(-10, 0, 25, 0));
        VBox.setMargin(pairingVBox, new Insets(25, 0, 0, 0));

        VBox contentBox = new VBox(25,
                headline,
                SettingsViewUtils.getLineAfterHeadline(30),
                info,
                webSocketServerUrl, websocketServerStateBox,

                apiConfigVbox,

                pairingVBox);

        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setPadding(new Insets(0, 5, 0, 5));

        clientsTable = new RichTableView<>(model.getConnectedClients(), Res.get("settings.bisqConnect.clients.headline"));
        clientsTable.setMaxHeight(100);
        configTable();

        this.root.getChildren().addAll(contentBox, clientsTable);
    }

    private static Label getInfoLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");
        VBox.setMargin(label, new Insets(5, 0, 10, 0));
        return label;
    }

    @Override
    protected void onViewAttached() {
        webSocketServerUrl.setText(model.getWebSocketServerUrl());

        webSocketServiceStatePin = EasyBind.subscribe(model.getWebSocketServiceState(), state -> {
            if (state != null) {
                webSocketServerStateLabel.setText(Res.get("settings.bisqConnect.webSocketServerState." + state.name()));
                switch (state) {
                    case NEW -> webSocketServerState.setFill(Color.RED);
                    case STARTING -> webSocketServerState.setFill(Color.YELLOW);
                    case RUNNING -> webSocketServerState.setFill(Color.GREEN);
                    case STOPPING -> webSocketServerState.setFill(Color.ORANGE);
                    case TERMINATED -> webSocketServerState.setFill(Color.RED);
                }
            }
        });
        pairingVBox.visibleProperty().bind(model.getIsPairingVisible());
        pairingVBox.managedProperty().bind(model.getIsPairingVisible());

        qrImageView.imageProperty().bind(model.getQrCodeImage());
        qrCode.textProperty().bind(model.getQrCode());
    }

    @Override
    protected void onViewDetached() {
        webSocketServiceStatePin.unsubscribe();

        pairingVBox.visibleProperty().unbind();
        pairingVBox.managedProperty().unbind();

        qrImageView.imageProperty().unbind();
        qrCode.textProperty().unbind();
    }

    private void configTable() {
        clientsTable.getColumns().add(new BisqTableColumn.Builder<WebsocketClient1>()
                .title(Res.get("settings.bisqConnect.clients.address"))
                .valueSupplier(info -> info.getAddress().orElse(Res.get("data.na")))
                .left()
                .minWidth(200)
                .build());
        clientsTable.getColumns().add(new BisqTableColumn.Builder<WebsocketClient1>()
                .title(Res.get("settings.bisqConnect.clients.userAgent"))
                .valueSupplier(info -> info.getUserAgent().orElse(Res.get("data.na")))
                .left()
                .minWidth(300)
                .build());
    }

}
