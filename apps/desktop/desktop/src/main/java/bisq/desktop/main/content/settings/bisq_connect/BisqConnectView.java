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
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.IndexColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states.FormUtils;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.http_api.web_socket.BisqConnectClientInfo;
import bisq.i18n.Res;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqConnectView extends View<VBox, BisqConnectModel, BisqConnectController> {
    private final Switch enableSwitch;
    private final AutoCompleteComboBox<BisqConnectExposureMode> exposureModeComboBox;
    private final MaterialPasswordField passwordField;
    private Subscription passwordSubscription;


    private final MaterialTextField connectionUrlField;
    private final ImageView qrImageView;
    private final Label qrPlaceholder;
    private final Button saveChangesButton;

    private Subscription websocketInitErrorSub;

    BisqConnectView(BisqConnectModel model, BisqConnectController controller) {
        super(new VBox(35), model, controller);
        root.setPadding(new Insets(0, 40, 20, 40));
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = SettingsViewUtils.getHeadline(Res.get("settings.bisqConnect.headline"));
        Label description = new Label(Res.get("settings.bisqConnect.description"));
        description.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");
        VBox.setMargin(description, new Insets(5, 0, 10, 0));

        enableSwitch = new Switch(Res.get("settings.bisqConnect.enable"));
        passwordField = new MaterialPasswordField(Res.get("settings.bisqConnect.password.label"));
        passwordField.setHelpText(null);
        passwordField.setPrefWidth(SettingsViewUtils.TEXT_FIELD_WIDTH);
        passwordField.setValidators(model.getPasswordRequiredValidator(), model.getPasswordMinimumLengthValidator());
        saveChangesButton = new Button(Res.get("settings.bisqConnect.saveChanges"));
        saveChangesButton.setDefaultButton(true);

        exposureModeComboBox = new AutoCompleteComboBox<>(model.getExposureModes(), Res.get("settings.bisqConnect.exposureMode.label"));
        exposureModeComboBox.setPrefWidth(SettingsViewUtils.TEXT_FIELD_WIDTH);
        exposureModeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BisqConnectExposureMode mode) {
                if (mode == null) return "";
                return mode.displayString();
            }

            @Override
            public BisqConnectExposureMode fromString(String string) {
                return null;
            }
        });
        VBox modeBox = new VBox(10, exposureModeComboBox);

        connectionUrlField = FormUtils.getTextField(Res.get("settings.bisqConnect.url.label"), "", false);

        Label qrHeadline = SettingsViewUtils.getHeadline(Res.get("settings.bisqConnect.connectionDetails.headline"));
        qrImageView = new ImageView();
        qrImageView.fitWidthProperty().bind(model.getQrCodeSize().multiply(1.0));
        qrImageView.fitHeightProperty().bind(model.getQrCodeSize().multiply(1.0));
        qrImageView.setPreserveRatio(true);

        qrPlaceholder = new Label();
        qrPlaceholder.setWrapText(true);
        qrPlaceholder.textAlignmentProperty().set(TextAlignment.CENTER);
        qrPlaceholder.setAlignment(Pos.CENTER);
        qrPlaceholder.getStyleClass().addAll("normal-text", "text-fill-grey-dimmed", "wrap-text");

        StackPane qrPane = new StackPane(qrImageView, qrPlaceholder);
        qrPane.setAlignment(Pos.CENTER);
        qrPane.minWidthProperty().bind(model.getQrCodeSize().add(40));
        qrPane.minHeightProperty().bind(model.getQrCodeSize().add(40));
        qrPane.prefWidthProperty().bind(model.getQrCodeSize().add(40));
        qrPane.prefHeightProperty().bind(model.getQrCodeSize().add(40));

        VBox contentBox = new VBox(25,
                headline,
                SettingsViewUtils.getLineAfterHeadline(30),
                description,
                enableSwitch,
                modeBox,
                passwordField,
                saveChangesButton,
                qrHeadline,
                SettingsViewUtils.getLineAfterHeadline(30),
                qrPane,
                connectionUrlField);

        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setPadding(new Insets(0, 5, 0, 5));

        root.getChildren().add(contentBox);

        SortedList<BisqConnectClientInfo> sortedClients = new SortedList<>(model.getConnectedClients());
        RichTableView<BisqConnectClientInfo> clientsTable = new RichTableView<>(sortedClients, Res.get("settings.bisqConnect.clients.headline"));
        BisqTableColumn<BisqConnectClientInfo> indexCol = IndexColumnUtil.getIndexColumn(sortedClients);
        BisqTableColumn<BisqConnectClientInfo> ipCol = new BisqTableColumn.Builder<BisqConnectClientInfo>()
                .title(Res.get("settings.bisqConnect.clients.ip"))
                .valueSupplier(info -> info.getAddress().orElse(Res.get("data.na")))
                .left()
                .minWidth(200)
                .build();
        BisqTableColumn<BisqConnectClientInfo> uaCol = new BisqTableColumn.Builder<BisqConnectClientInfo>()
                .title(Res.get("settings.bisqConnect.clients.userAgent"))
                .valueSupplier(info -> info.getUserAgent().orElse(Res.get("data.na")))
                .left()
                .minWidth(300)
                .build();
        clientsTable.getColumns().add(indexCol);
        clientsTable.getColumns().add(ipCol);
        clientsTable.getColumns().add(uaCol);
        root.getChildren().add(clientsTable);
    }

    @Override
    protected void onViewAttached() {
        enableSwitch.selectedProperty().bindBidirectional(model.getEnabled());
        enableSwitch.setOnAction(e -> controller.onToggleEnabled(enableSwitch.isSelected()));

        exposureModeComboBox.getSelectionModel().select(model.getSelectedMode().get());
        exposureModeComboBox.setOnChangeConfirmed(e -> {
            BisqConnectExposureMode selected = exposureModeComboBox.getSelectionModel().getSelectedItem();
            if (selected == null) {
                exposureModeComboBox.getSelectionModel().select(model.getSelectedMode().get());
                return;
            }
            controller.onSelectMode(selected);
        });

        passwordField.setText(model.getPassword().get());
        passwordField.isValidProperty().bindBidirectional(model.getPasswordIsValid());
        passwordSubscription = EasyBind.subscribe(passwordField.textProperty(), newVal -> {
            passwordField.validate();
            controller.onPasswordChanged(newVal);
        });
        passwordField.resetValidation(); // to reset error message set by current value of the password

        qrPlaceholder.textProperty().bind(model.getQrPlaceholder());

        websocketInitErrorSub = EasyBind.subscribe(model.getWebsocketInitError(), newVal -> {
            boolean hasError = newVal != null && !newVal.isEmpty();
            if (hasError) {
                qrPlaceholder.getStyleClass().remove("text-fill-grey-dimmed");
                if (!qrPlaceholder.getStyleClass().contains("text-fill-error")) {
                    qrPlaceholder.getStyleClass().add("text-fill-error");
                }
            } else {
                qrPlaceholder.getStyleClass().remove("text-fill-error");
                if (!qrPlaceholder.getStyleClass().contains("text-fill-grey-dimmed")) {
                    qrPlaceholder.getStyleClass().add("text-fill-grey-dimmed");
                }
            }
        });
        connectionUrlField.textProperty().bind(model.getApiUrl());

        qrImageView.imageProperty().bind(model.getQrCodeImage());

        qrImageView.visibleProperty().bind(model.getQrCodeImage().isNotNull().and(model.getWebsocketRunning()));
        qrImageView.managedProperty().bind(model.getQrCodeImage().isNotNull().and(model.getWebsocketRunning()));

        qrPlaceholder.visibleProperty().bind(model.getQrCodeImage().isNull().or(model.getWebsocketRunning().not()));
        qrPlaceholder.managedProperty().bind(model.getQrCodeImage().isNull().or(model.getWebsocketRunning().not()));

        connectionUrlField.visibleProperty().bind(qrImageView.visibleProperty());
        connectionUrlField.managedProperty().bind(qrImageView.managedProperty());

        saveChangesButton.disableProperty().bind(model.getIsChangeDetected().not());
        saveChangesButton.setOnAction(e -> {
            passwordField.validate();
            controller.onSaveChanges();
        });
    }

    @Override
    protected void onViewDetached() {
        enableSwitch.selectedProperty().unbindBidirectional(model.getEnabled());
        enableSwitch.setOnAction(null);

        exposureModeComboBox.setOnChangeConfirmed(null);

        if (passwordSubscription != null) {
            passwordSubscription.unsubscribe();
            passwordSubscription = null;
        }
        passwordField.isValidProperty().unbindBidirectional(model.getPasswordIsValid());

        connectionUrlField.textProperty().unbind();
        connectionUrlField.visibleProperty().unbind();
        connectionUrlField.managedProperty().unbind();

        qrImageView.imageProperty().unbind();
        qrPlaceholder.textProperty().unbind();
        if (websocketInitErrorSub != null) {
            websocketInitErrorSub.unsubscribe();
            websocketInitErrorSub = null;
        }
        qrPlaceholder.visibleProperty().unbind();
        qrPlaceholder.managedProperty().unbind();
        qrImageView.visibleProperty().unbind();
        qrImageView.managedProperty().unbind();

        saveChangesButton.disableProperty().unbind();
        saveChangesButton.setOnAction(null);
    }
}
