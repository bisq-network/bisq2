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

import bisq.bonded_roles.alert.AlertType;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.presentation.formatters.BooleanFormatter;
import bisq.presentation.formatters.DateFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class SecurityManagerView extends View<VBox, SecurityManagerModel, SecurityManagerController> {
    private final Button actionButton;
    private final MaterialTextArea message;
    private final MaterialTextField minVersion;
    private final AutoCompleteComboBox<AlertType> alertTypeSelection;
    private final AutoCompleteComboBox<BondedRoleListItem> bondedRoleSelection;
    private final CheckBox haltTradingCheckBox, requireVersionForTradingCheckBox;
    private final HBox requireVersionForTradingHBox;
    private final BisqTableView<AlertListItem> tableView;
    private Subscription selectedAlertTypePin, selectedBondedRolListItemePin;

    public SecurityManagerView(SecurityManagerModel model, SecurityManagerController controller, Pane roleInfo) {
        super(new VBox(10), model, controller);

        this.root.setAlignment(Pos.TOP_LEFT);

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

        bondedRoleSelection = new AutoCompleteComboBox<>(model.getBondedRoleListItems(), Res.get("authorizedRole.securityManager.selectBondedRole"));
        bondedRoleSelection.setPrefWidth(800);
        bondedRoleSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(BondedRoleListItem listItem) {
                return listItem != null ? listItem.getDisplayString() : "";
            }

            @Override
            public BondedRoleListItem fromString(String string) {
                return null;
            }
        });

        message = new MaterialTextArea(Res.get("authorizedRole.securityManager.alert.message"));

        haltTradingCheckBox = new CheckBox(Res.get("authorizedRole.securityManager.emergency.haltTrading"));
        requireVersionForTradingCheckBox = new CheckBox(Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading"));
        minVersion = new MaterialTextField(Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading.version"),
                Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading.version.prompt"));
        minVersion.setMinWidth(300);

        requireVersionForTradingHBox = new HBox(20, requireVersionForTradingCheckBox, minVersion);
        requireVersionForTradingHBox.setAlignment(Pos.CENTER_LEFT);

        actionButton = new Button();
        actionButton.setDefaultButton(true);
        actionButton.setAlignment(Pos.BOTTOM_RIGHT);

        Label tableHeadline = new Label(Res.get("authorizedRole.securityManager.alert.table.headline"));
        tableHeadline.getStyleClass().add("bisq-text-headline-2");

        tableView = new BisqTableView<>(model.getSortedAlertListItems());
        tableView.setMinHeight(200);
        tableView.getStyleClass().add("user-bonded-roles-table-view");
        configTableView();

        VBox.setMargin(headline, new Insets(30, 0, 10, 0));
        VBox.setMargin(actionButton, new Insets(10, 0, 0, 0));
        VBox.setMargin(haltTradingCheckBox, new Insets(10, 0, 0, 0));
        VBox.setMargin(tableHeadline, new Insets(30, 0, 10, 0));
        VBox.setMargin(roleInfo, new Insets(20, 0, 0, 0));
        this.root.getChildren().addAll(headline,
                alertTypeSelection, message,
                haltTradingCheckBox, requireVersionForTradingHBox,
                bondedRoleSelection,
                actionButton,
                tableHeadline, tableView,
                roleInfo);
    }

    @Override
    protected void onViewAttached() {
        haltTradingCheckBox.visibleProperty().bind(model.getSelectedAlertType().isEqualTo(AlertType.EMERGENCY));
        haltTradingCheckBox.managedProperty().bind(haltTradingCheckBox.visibleProperty());
        requireVersionForTradingHBox.visibleProperty().bind(haltTradingCheckBox.visibleProperty());
        requireVersionForTradingHBox.managedProperty().bind(haltTradingCheckBox.visibleProperty());
        minVersion.textProperty().bindBidirectional(model.getMinVersion());
        minVersion.disableProperty().bind(requireVersionForTradingCheckBox.selectedProperty().not());

        bondedRoleSelection.visibleProperty().bind(model.getSelectedAlertType().isEqualTo(AlertType.BAN));
        bondedRoleSelection.managedProperty().bind(bondedRoleSelection.visibleProperty());

        message.textProperty().bindBidirectional(model.getMessage());
        message.visibleProperty().bind(bondedRoleSelection.visibleProperty().not());
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
        bondedRoleSelection.setOnChangeConfirmed(e -> {
            if (bondedRoleSelection.getSelectionModel().getSelectedItem() == null) {
                bondedRoleSelection.getSelectionModel().select(model.getSelectedBondedRoleListItem().get());
                return;
            }
            controller.onBondedRoleListItem(bondedRoleSelection.getSelectionModel().getSelectedItem());
        });
        actionButton.setOnAction(e -> controller.onSendAlert());
        haltTradingCheckBox.selectedProperty().bindBidirectional(model.getHaltTrading());
        requireVersionForTradingCheckBox.selectedProperty().bindBidirectional(model.getRequireVersionForTrading());

        selectedAlertTypePin = EasyBind.subscribe(model.getSelectedAlertType(),
                alertType -> alertTypeSelection.getSelectionModel().select(alertType));
        selectedBondedRolListItemePin = EasyBind.subscribe(model.getSelectedBondedRoleListItem(),
                bondedRole -> bondedRoleSelection.getSelectionModel().select(bondedRole));
    }

    @Override
    protected void onViewDetached() {
        haltTradingCheckBox.visibleProperty().unbind();
        haltTradingCheckBox.managedProperty().unbind();
        requireVersionForTradingHBox.visibleProperty().unbind();
        requireVersionForTradingHBox.managedProperty().unbind();
        minVersion.textProperty().unbindBidirectional(model.getMinVersion());
        minVersion.disableProperty().unbind();

        bondedRoleSelection.visibleProperty().unbind();
        bondedRoleSelection.managedProperty().unbind();


        message.textProperty().unbindBidirectional(model.getMessage());
        message.visibleProperty().unbind();
        message.managedProperty().unbind();

        actionButton.textProperty().unbind();
        actionButton.disableProperty().unbind();
        haltTradingCheckBox.selectedProperty().unbindBidirectional(model.getHaltTrading());
        requireVersionForTradingCheckBox.selectedProperty().unbindBidirectional(model.getRequireVersionForTrading());

        alertTypeSelection.setOnChangeConfirmed(null);
        bondedRoleSelection.setOnChangeConfirmed(null);

        actionButton.setOnAction(null);

        selectedAlertTypePin.unsubscribe();
        selectedBondedRolListItemePin.unsubscribe();
    }

    protected void configTableView() {
        BisqTableColumn<AlertListItem> date = new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.date"))
                .isFirst()
                .minWidth(180)
                .comparator(Comparator.comparing(AlertListItem::getDate).reversed())
                .valueSupplier(AlertListItem::getDateString)
                .build();
        tableView.getColumns().add(date);
        tableView.getSortOrder().add(date);

        tableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.alertType"))
                .minWidth(150)
                .comparator(Comparator.comparing(AlertListItem::getAlertType))
                .valueSupplier(AlertListItem::getAlertType)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.message"))
                .minWidth(200)
                .comparator(Comparator.comparing(AlertListItem::getMessage))
                .valueSupplier(AlertListItem::getMessage)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.haltTrading"))
                .minWidth(120)
                .comparator(Comparator.comparing(AlertListItem::getHaltTrading))
                .valueSupplier(AlertListItem::getHaltTrading)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.requireVersionForTrading"))
                .minWidth(120)
                .comparator(Comparator.comparing(AlertListItem::getRequireVersionForTrading))
                .valueSupplier(AlertListItem::getRequireVersionForTrading)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.minVersion"))
                .minWidth(120)
                .comparator(Comparator.comparing(AlertListItem::getMinVersion))
                .valueSupplier(AlertListItem::getMinVersion)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.bannedRole"))
                .minWidth(150)
                .comparator(Comparator.comparing(AlertListItem::getBondedRoleDisplayString))
                .valueSupplier(AlertListItem::getBondedRoleDisplayString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .isSortable(false)
                .minWidth(200)
                .isLast()
                .setCellFactory(getRemoveAlertCellFactory())
                .build());

    }

    private Callback<TableColumn<AlertListItem, AlertListItem>, TableCell<AlertListItem, AlertListItem>> getRemoveAlertCellFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button(Res.get("authorizedRole.securityManager.alert.table.remove"));

            @Override
            public void updateItem(final AlertListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty && controller.isRemoveButtonVisible(item.getAuthorizedAlertData())) {
                    button.setOnAction(e -> controller.onRemoveAlert(item.getAuthorizedAlertData()));
                    setGraphic(button);
                } else {
                    button.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }

    @EqualsAndHashCode
    @Getter
    @ToString
    public static class AlertListItem implements TableItem {
        private final AuthorizedAlertData authorizedAlertData;
        private final String dateString;
        private final String alertType;
        private final String message;
        private final String haltTrading;
        private final String requireVersionForTrading;
        private final String minVersion;
        private final long date;
        private final String bondedRoleDisplayString;

        public AlertListItem(AuthorizedAlertData authorizedAlertData, SecurityManagerController controller) {
            this.authorizedAlertData = authorizedAlertData;
            date = this.authorizedAlertData.getDate();
            dateString = DateFormatter.formatDateTime(date);
            alertType = Res.get("authorizedRole.securityManager.alertType." + this.authorizedAlertData.getAlertType().name());
            message = this.authorizedAlertData.getMessage().orElse("");
            minVersion = this.authorizedAlertData.getMinVersion().orElse("");
            haltTrading = BooleanFormatter.toYesNo(this.authorizedAlertData.isHaltTrading());
            requireVersionForTrading = BooleanFormatter.toYesNo(this.authorizedAlertData.isRequireVersionForTrading());
            bondedRoleDisplayString = authorizedAlertData.getBannedRole().map(controller::getBondedRoleDisplayString).orElse("");
        }
    }

    @Getter
    @EqualsAndHashCode
    public static class BondedRoleListItem {
        private final BondedRole bondedRole;
        private final String displayString;

        public BondedRoleListItem(BondedRole bondedRole, SecurityManagerController controller) {
            this.bondedRole = bondedRole;
            displayString = controller.getBondedRoleShortDisplayString(bondedRole);
        }
    }
}
