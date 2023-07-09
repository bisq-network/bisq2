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

package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import bisq.support.alert.AlertType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SecurityManagerView extends View<VBox, SecurityManagerModel, SecurityManagerController> {
    private final Button actionButton;
    private final MaterialTextArea message;
    private final MaterialTextField minVersion, bannedRoleProfileId;
    private final AutoCompleteComboBox<AlertType> alertTypeSelection;
    private final CheckBox haltTradingCheckBox, requireVersionForTradingCheckBox;
    private final HBox requireVersionForTradingHBox;
    private Subscription selectedAlertTypePin;


    public SecurityManagerView(SecurityManagerModel model, SecurityManagerController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("authorizedRole.securityManager.alert.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        alertTypeSelection = new AutoCompleteComboBox<>(model.getAlertTypes(), Res.get("authorizedRole.securityManager.selectAlertType"));
        alertTypeSelection.setPrefWidth(300);
        alertTypeSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(AlertType alertType) {
                return alertType != null ? Res.get("authorizedRole.securityManager.alertType." + alertType.name()) : "";
            }

            @Override
            public AlertType fromString(String string) {
                return null;
            }
        });

        message = new MaterialTextArea(Res.get("authorizedRole.securityManager.alert.message"));

        haltTradingCheckBox = new CheckBox(Res.get("authorizedRole.securityManager.emergency.haltTrading"));
        requireVersionForTradingCheckBox = new CheckBox(Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading"));
        minVersion = new MaterialTextField(Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading.version"),
                Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading.version.prompt"));
        minVersion.setMinWidth(200);
        HBox.setMargin(requireVersionForTradingCheckBox, new Insets(35, 0, 0, 0));
        requireVersionForTradingHBox = new HBox(20, requireVersionForTradingCheckBox, minVersion);

        bannedRoleProfileId = new MaterialTextField(Res.get("authorizedRole.securityManager.ban.profileId"));

        actionButton = new Button();
        actionButton.setDefaultButton(true);
        actionButton.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(actionButton, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headline, alertTypeSelection, message,
                haltTradingCheckBox, requireVersionForTradingHBox,
                bannedRoleProfileId,
                actionButton);
    }

    @Override
    protected void onViewAttached() {
        haltTradingCheckBox.visibleProperty().bind(model.getSelectedAlertType().isEqualTo(AlertType.EMERGENCY));
        haltTradingCheckBox.managedProperty().bind(haltTradingCheckBox.visibleProperty());
        requireVersionForTradingHBox.visibleProperty().bind(haltTradingCheckBox.visibleProperty());
        requireVersionForTradingHBox.managedProperty().bind(haltTradingCheckBox.visibleProperty());
        minVersion.textProperty().bindBidirectional(model.getMinVersion());
        minVersion.disableProperty().bind(requireVersionForTradingCheckBox.selectedProperty().not());

        bannedRoleProfileId.textProperty().bindBidirectional(model.getBannedRoleProfileId());
        bannedRoleProfileId.visibleProperty().bind(model.getSelectedAlertType().isEqualTo(AlertType.BAN));
        bannedRoleProfileId.managedProperty().bind(bannedRoleProfileId.visibleProperty());

        message.textProperty().bindBidirectional(model.getMessage());
        message.visibleProperty().bind(bannedRoleProfileId.visibleProperty().not());
        message.managedProperty().bind(message.visibleProperty());

        actionButton.textProperty().bind(model.getActionButtonText());
        actionButton.disableProperty().bind(model.getActionButtonDisabled());

        alertTypeSelection.setOnChangeConfirmed(e -> {
            if (alertTypeSelection.getSelectionModel().getSelectedItem() == null) {
                alertTypeSelection.getSelectionModel().select(model.getSelectedAlertType().get());
                return;
            }
            controller.onSelectAlertType(alertTypeSelection.getSelectionModel().getSelectedItem());
        });
        actionButton.setOnAction(e -> controller.onSendAlert());
        haltTradingCheckBox.selectedProperty().bindBidirectional(model.getHaltTrading());
        requireVersionForTradingCheckBox.selectedProperty().bindBidirectional(model.getRequireVersionForTrading());

        selectedAlertTypePin = EasyBind.subscribe(model.getSelectedAlertType(),
                alertType -> alertTypeSelection.getSelectionModel().select(alertType));
    }

    @Override
    protected void onViewDetached() {
        haltTradingCheckBox.visibleProperty().unbind();
        haltTradingCheckBox.managedProperty().unbind();
        requireVersionForTradingHBox.visibleProperty().unbind();
        requireVersionForTradingHBox.managedProperty().unbind();
        minVersion.textProperty().unbindBidirectional(model.getMinVersion());
        minVersion.disableProperty().unbind();

        bannedRoleProfileId.textProperty().unbindBidirectional(model.getBannedRoleProfileId());
        bannedRoleProfileId.visibleProperty().unbind();
        bannedRoleProfileId.managedProperty().unbind();

        message.textProperty().unbindBidirectional(model.getMessage());
        message.visibleProperty().unbind();
        message.managedProperty().unbind();

        actionButton.textProperty().unbind();
        actionButton.disableProperty().unbind();
        haltTradingCheckBox.selectedProperty().unbindBidirectional(model.getHaltTrading());
        requireVersionForTradingCheckBox.selectedProperty().unbindBidirectional(model.getRequireVersionForTrading());

        alertTypeSelection.setOnChangeConfirmed(null);
        actionButton.setOnAction(null);

        selectedAlertTypePin.unsubscribe();
    }
}
