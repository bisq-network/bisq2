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

package bisq.desktop.main.content.settings.bisq_connect.api_config;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.api.access.transport.ApiAccessTransportType;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ApiConfigView extends View<VBox, ApiConfigModel, ApiConfigController> {
    private final Switch websocketEnabled;
    private final AutoCompleteComboBox<ApiAccessTransportType> apiAccessTransportTypes;
    private final MaterialTextField bindHost, bindPort, serverUrl,
            onionServiceUrl,
            detectedLanHost;
    private final Button applyDetectedLanHostButton, applyButton;
    private final VBox onionServiceVBox, detectedLanHostVbox;
    private final Set<Subscription> subscriptions = new HashSet<>();

    ApiConfigView(ApiConfigModel model, ApiConfigController controller) {
        super(new VBox(25), model, controller);

        Label headline = SettingsViewUtils.getHeadline(Res.get("settings.bisqConnect.apiConfig.headline"));
        Label pairingInfo = getInfoLabel(Res.get("settings.bisqConnect.apiConfig.info"));

        websocketEnabled = new Switch(Res.get("settings.bisqConnect.apiConfig.enable"));

        apiAccessTransportTypes = new AutoCompleteComboBox<>(model.getApiTransportTypes(),
                Res.get("settings.bisqConnect.apiConfig.transport"));
        apiAccessTransportTypes.setPrefWidth(SettingsViewUtils.TEXT_FIELD_WIDTH);
        apiAccessTransportTypes.setConverter(new StringConverter<>() {
            @Override
            public String toString(ApiAccessTransportType type) {
                if (type == null) return "";
                return type.getDisplayString();
            }

            @Override
            public ApiAccessTransportType fromString(String string) {
                return null;
            }
        });


        Label serverHeadline = getHeadline(Res.get("settings.bisqConnect.apiConfig.server"));

        // server
        bindHost = getHostField(Res.get("settings.bisqConnect.apiConfig.server.host"));
        bindHost.setValidator(model.getBindHostValidator());

        bindPort = getPortField(Res.get("settings.bisqConnect.apiConfig.server.port"));
        bindPort.setValidator(model.getBindPortValidator());

        serverUrl = getUrlField(Res.get("settings.bisqConnect.apiConfig.server.url"));

        VBox serverBox = new VBox(10, serverHeadline, new HBox(10, bindHost, bindPort, serverUrl));

        // onionService
        Label onionServiceHeadline = getHeadline(Res.get("settings.bisqConnect.apiConfig.onionService"));

        onionServiceUrl = getUrlField(Res.get("settings.bisqConnect.apiConfig.onionService.url"));

        onionServiceVBox = new VBox(5, onionServiceHeadline, onionServiceUrl);

        // detectedLanHost
        Label detectedLanHostHeadline = getHeadline(Res.get("settings.bisqConnect.apiConfig.lan.detection"));

        detectedLanHost = getHostField(Res.get("settings.bisqConnect.apiConfig.lan.detectedHost"));
        detectedLanHost.setEditable(false);
        applyDetectedLanHostButton = new Button(Res.get("settings.bisqConnect.apiConfig.lan.apply"));
        applyDetectedLanHostButton.setDefaultButton(true);

        HBox detectedLanHostHbox = new HBox(10, detectedLanHost, applyDetectedLanHostButton);
        detectedLanHostHbox.setAlignment(Pos.CENTER_LEFT);
        detectedLanHostVbox = new VBox(5, detectedLanHostHeadline, detectedLanHostHbox);

        applyButton = new Button(Res.get("settings.bisqConnect.applyButton"));
        applyButton.setDefaultButton(true);

        root.getChildren().addAll(
                headline, SettingsViewUtils.getLineAfterHeadline(25), pairingInfo,

                websocketEnabled,

                apiAccessTransportTypes,

                serverBox,
                onionServiceVBox,
                detectedLanHostVbox,

                applyButton);
    }

    @Override
    protected void onViewAttached() {
        websocketEnabled.selectedProperty().bindBidirectional(model.getWebsocketEnabled());
        websocketEnabled.setOnAction(e -> controller.onToggleEnabled(websocketEnabled.isSelected()));

        apiAccessTransportTypes.getSelectionModel().select(model.getApiAccessTransportType().get());
        apiAccessTransportTypes.setOnChangeConfirmed(e -> {
            ApiAccessTransportType selected = apiAccessTransportTypes.getSelectionModel().getSelectedItem();
            if (selected == null) {
                apiAccessTransportTypes.getSelectionModel().select(model.getApiAccessTransportType().get());
                return;
            }
            controller.onApiTransportType(selected);
        });
        subscriptions.add(EasyBind.subscribe(model.getApiAccessTransportType(), type -> {
            onionServiceVBox.setVisible(type == ApiAccessTransportType.TOR);
            onionServiceVBox.setManaged(type == ApiAccessTransportType.TOR);
            detectedLanHostVbox.setVisible(type == ApiAccessTransportType.CLEARNET);
            detectedLanHostVbox.setManaged(type == ApiAccessTransportType.CLEARNET);
        }));

        // server
        bindHost.textProperty().bindBidirectional(model.getBindHost());
        bindHost.validate();

        Bindings.bindBidirectional(bindPort.textProperty(), model.getBindPort(), model.getBindPortConverter());
        bindPort.validate();

        serverUrl.textProperty().bind(model.getServerUrl());

        // onionService
        onionServiceUrl.textProperty().bind(model.getOnionServiceUrl());
        onionServiceUrl.promptTextProperty().bind(model.getOnionServiceUrlPrompt());
        onionServiceUrl.helpProperty().bind(model.getOnionServiceUrlHelp());

        // detectedLanHost
        detectedLanHost.textProperty().bind(model.getDetectedLanHost());
        applyDetectedLanHostButton.setOnAction(e -> controller.onApplyDetectedLanHost());
        applyDetectedLanHostButton.disableProperty().bind(model.getDetectedLanHostApplied());

        applyButton.disableProperty().bind(model.getApplyButtonDisabled());
        applyButton.setOnAction(e -> controller.onApply());
    }

    @Override
    protected void onViewDetached() {
        websocketEnabled.selectedProperty().unbindBidirectional(model.getWebsocketEnabled());
        websocketEnabled.setOnAction(null);

        apiAccessTransportTypes.setOnChangeConfirmed(null);

        bindHost.textProperty().unbindBidirectional(model.getBindHost());
        bindHost.resetValidation();

        Bindings.unbindBidirectional(bindPort.textProperty(), model.getBindPort());
        bindPort.resetValidation();

        serverUrl.textProperty().unbind();

        onionServiceUrl.textProperty().unbind();
        onionServiceUrl.promptTextProperty().unbind();
        onionServiceUrl.helpProperty().unbind();

        detectedLanHost.textProperty().unbind();
        applyDetectedLanHostButton.disableProperty().unbind();
        applyDetectedLanHostButton.setOnAction(null);

        applyButton.disableProperty().unbind();
        applyButton.setOnAction(null);

        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }


    private static Label getHeadline(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-sub-headline");
        return label;
    }

    private static Label getInfoLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");
        VBox.setMargin(label, new Insets(5, 0, 10, 0));
        return label;
    }

    private static MaterialTextField getHostField(String description) {
        MaterialTextField field = new MaterialTextField(description);
        field.showCopyIcon();
        field.setMinWidth(300);
        return field;
    }

    private static MaterialTextField getPortField(String description) {
        MaterialTextField field = new MaterialTextField(description);
        field.showCopyIcon();
        field.setMinWidth(100);
        return field;
    }

    private static MaterialTextField getUrlField(String description) {
        MaterialTextField field = new MaterialTextField(description);
        field.showCopyIcon();
        field.setEditable(false);
        field.setMinWidth(300);
        return field;
    }
}
