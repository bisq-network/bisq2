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

package bisq.desktop.main.content.settings.network;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NetworkSettingsView extends View<VBox, NetworkSettingsModel, NetworkSettingsController> {
    private final ToggleGroup transportOptionToggleGroup = new ToggleGroup();
    private final RadioButton torAndI2P, torOnly, i2pOnly, clearOnly;
    private final Switch useEmbedded;
    private final MaterialTextField i2cpAddress, bi2pGrpcAddress;
    private final Button resetToDefaultsButton, shutdownButton;
    private Subscription selectedTransportOptionPin;

    NetworkSettingsView(NetworkSettingsModel model, NetworkSettingsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        // Transport options
        Label transportOptionsHeadline = SettingsViewUtils.getHeadline(Res.get("settings.network.transport.headline"));
        ImageView infoIcon = ImageUtil.getImageViewById("info");
        infoIcon.setOpacity(0.6);
        Tooltip.install(infoIcon, new BisqTooltip(Res.get("settings.network.transport.options.info")));
        HBox.setMargin(infoIcon, new Insets(5, 0, 0, 0));
        HBox transportOptionsHeadlineHBox = new HBox(10, transportOptionsHeadline, infoIcon);

        torAndI2P = new RadioButton(Res.get("settings.network.transport.options.torAndI2P"));
        torAndI2P.setToggleGroup(transportOptionToggleGroup);
        torAndI2P.setUserData(TransportOption.TOR_AND_I2P);
        torOnly = new RadioButton(Res.get("settings.network.transport.options.torOnly"));
        torOnly.setToggleGroup(transportOptionToggleGroup);
        torOnly.setUserData(TransportOption.TOR);
        i2pOnly = new RadioButton(Res.get("settings.network.transport.options.i2pOnly"));
        i2pOnly.setToggleGroup(transportOptionToggleGroup);
        i2pOnly.setUserData(TransportOption.I2P);
        clearOnly = new RadioButton(Res.get("settings.network.transport.options.clearOnly"));
        clearOnly.setToggleGroup(transportOptionToggleGroup);
        clearOnly.setUserData(TransportOption.CLEAR);

        VBox transportOptionsVBox = new VBox(10, torAndI2P, torOnly, i2pOnly, clearOnly);

        // I2P options
        Label i2pOptionsHeadline = SettingsViewUtils.getHeadline(Res.get("settings.network.i2p.headline"));

        useEmbedded = new Switch(Res.get("settings.network.i2p.useEmbedded"));
        ImageView useEmbeddedInfoIcon = ImageUtil.getImageViewById("info");
        useEmbeddedInfoIcon.setOpacity(0.6);
        Tooltip.install(useEmbeddedInfoIcon, new BisqTooltip(Res.get("settings.network.i2p.useEmbedded.info")));
        // HBox.setMargin(useEmbeddedInfoIcon, new Insets(5,0,0,0));
        HBox useEmbeddedHBox = new HBox(10, useEmbedded, useEmbeddedInfoIcon);
        useEmbeddedHBox.setAlignment(Pos.CENTER_LEFT);

        i2cpAddress = new MaterialTextField(Res.get("settings.network.i2p.i2cpAddress"),
                null,
                Res.get("settings.network.i2p.i2cpAddress.help"));
        i2cpAddress.setStringConverter(model.getI2cpAddressConverter());
        i2cpAddress.setValidator(model.getI2cpAddressValidator());
        i2cpAddress.setMaxWidth(SettingsViewUtils.TEXT_FIELD_WIDTH);

        bi2pGrpcAddress = new MaterialTextField(Res.get("settings.network.i2p.bi2pGrpcAddress"),
                null,
                Res.get("settings.network.i2p.bi2pGrpcAddress.help"));
        bi2pGrpcAddress.setStringConverter(model.getBi2pGrpcAddressConverter());
        bi2pGrpcAddress.setValidator(model.getBi2pGrpcAddressValidator());
        bi2pGrpcAddress.setMaxWidth(SettingsViewUtils.TEXT_FIELD_WIDTH);

        VBox.setMargin(i2cpAddress, new Insets(15, 0, 15, 0));
        VBox i2pOptionsVBox = new VBox(10, useEmbeddedHBox, i2cpAddress, bi2pGrpcAddress);

        resetToDefaultsButton = new Button(Res.get("settings.network.resetToDefaults"));
        resetToDefaultsButton.getStyleClass().add("grey-transparent-outlined-button");

        shutdownButton = new Button(Res.get("settings.network.shutdown"));
        shutdownButton.setDefaultButton(true);

        HBox buttons = new HBox(10, resetToDefaultsButton, shutdownButton);

        VBox.setMargin(useEmbedded, new Insets(10, 0, 0, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox.setMargin(transportOptionsVBox, new Insets(0, 5, 0, 5));
        VBox.setMargin(i2pOptionsVBox, new Insets(0, 5, 0, 5));
        VBox contentBox = new VBox(50);

        contentBox.getChildren().addAll(
                transportOptionsHeadlineHBox, separator(contentBox), transportOptionsVBox,
                i2pOptionsHeadline, separator(contentBox), i2pOptionsVBox,
                buttons
        );

        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }


    @Override
    protected void onViewAttached() {
        i2cpAddress.textProperty().bindBidirectional(model.getI2cpAddress(), model.getI2cpAddressConverter());
        bi2pGrpcAddress.textProperty().bindBidirectional(model.getBi2pGrpcAddress(), model.getBi2pGrpcAddressConverter());
        useEmbedded.selectedProperty().bindBidirectional(model.getUseEmbeddedI2PRouter());
        shutdownButton.visibleProperty().bind(model.getShutdownButtonVisible());
        shutdownButton.managedProperty().bind(model.getShutdownButtonVisible());
        clearOnly.visibleProperty().bind(model.getClearOnlyVisible());
        clearOnly.managedProperty().bind(model.getClearOnlyVisible());

        selectedTransportOptionPin = EasyBind.subscribe(model.getSelectedTransportOption(), selected -> applyTransportOption());

        torAndI2P.setOnAction(e -> controller.onSetTransport(TransportOption.TOR_AND_I2P));
        torOnly.setOnAction(e -> {
            new Popup().backgroundInfo(Res.get("settings.network.transport.options.torOnly.warn"))
                    .closeButtonText(Res.get("settings.network.transport.options.torOnly.accept"))
                    .onClose(() -> controller.onSetTransport(TransportOption.TOR))
                    .actionButtonText(Res.get("settings.network.transport.options.torOnly.cancel"))
                    .onAction(() -> UIThread.runOnNextRenderFrame(() -> transportOptionToggleGroup.selectToggle(torAndI2P)))
                    .show();
        });
        i2pOnly.setOnAction(e -> {
            new Popup().backgroundInfo(Res.get("settings.network.transport.options.i2pOnly.warn"))
                    .closeButtonText(Res.get("settings.network.transport.options.i2pOnly.accept"))
                    .onClose(() -> controller.onSetTransport(TransportOption.I2P))
                    .actionButtonText(Res.get("settings.network.transport.options.i2pOnly.cancel"))
                    .onAction(() -> UIThread.runOnNextRenderFrame(() -> transportOptionToggleGroup.selectToggle(torAndI2P)))
                    .show();
        });
        clearOnly.setOnAction(e -> controller.onSetTransport(TransportOption.CLEAR));

        useEmbedded.setSelected(model.getUseEmbeddedI2PRouter().get());
        useEmbedded.setOnAction(e -> {
            boolean selected = useEmbedded.isSelected();
            if (selected) {
                new Popup().backgroundInfo(Res.get("settings.network.i2p.useEmbedded.warn"))
                        .closeButtonText(Res.get("settings.network.i2p.useEmbedded.warn.accept"))
                        .onClose(() -> controller.onToggleUseEmbedded(true))
                        .actionButtonText(Res.get("settings.network.i2p.useEmbedded.warn.cancel"))
                        .onAction(() -> UIThread.runOnNextRenderFrame(() -> useEmbedded.setSelected(false)))
                        .show();
            } else {
                controller.onToggleUseEmbedded(false);
            }
        });

        resetToDefaultsButton.setOnAction(e -> {
            controller.onResetToDefaults();
            i2cpAddress.resetValidation();
            bi2pGrpcAddress.resetValidation();
        });

        shutdownButton.setOnAction(e -> controller.onApplyAndShutdown());

        i2cpAddress.validate();
        bi2pGrpcAddress.validate();
    }

    @Override
    protected void onViewDetached() {
        i2cpAddress.textProperty().unbindBidirectional(model.getI2cpAddress());
        bi2pGrpcAddress.textProperty().unbindBidirectional(model.getBi2pGrpcAddress());
        useEmbedded.selectedProperty().unbindBidirectional(model.getUseEmbeddedI2PRouter());
        shutdownButton.visibleProperty().unbind();
        shutdownButton.managedProperty().unbind();
        clearOnly.visibleProperty().unbind();
        clearOnly.managedProperty().unbind();

        selectedTransportOptionPin.unsubscribe();

        torAndI2P.setOnAction(null);
        torOnly.setOnAction(null);
        i2pOnly.setOnAction(null);
        clearOnly.setOnAction(null);
        useEmbedded.setOnAction(null);
        resetToDefaultsButton.setOnAction(null);
        shutdownButton.setOnAction(null);

        i2cpAddress.resetValidation();
        bi2pGrpcAddress.resetValidation();
    }

    private void applyTransportOption() {
        switch (model.getSelectedTransportOption().get()) {
            case TOR_AND_I2P: {
                transportOptionToggleGroup.selectToggle(torAndI2P);
                break;
            }
            case TOR: {
                transportOptionToggleGroup.selectToggle(torOnly);
                break;
            }
            case I2P: {
                transportOptionToggleGroup.selectToggle(i2pOnly);
                break;
            }
            case CLEAR: {
                transportOptionToggleGroup.selectToggle(clearOnly);
                break;
            }
        }
    }

    private static Region separator(VBox contentBox) {
        return SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing());
    }
}
