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

package bisq.desktop.main.content.authorized_role.security_manager.tabs;

import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.util.StringUtils;
import bisq.desktop.common.converters.Converters;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.DateTableItem;
import bisq.desktop.components.table.RichTableView;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.presentation.formatters.BooleanFormatter;
import bisq.presentation.formatters.DateFormatter;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
    private static final ValidatorBase DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR =
            new NumberValidator(Res.get("authorizedRole.securityManager.difficultyAdjustment.invalid", NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT),
                    0, NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT, false);

    private final Button difficultyAdjustmentButton, sendAlertButton;
    private final MaterialTextArea message, bannedAccountData;
    private final MaterialTextField headline, minVersion, difficultyAdjustmentFactor;
    private final AutoCompleteComboBox<AlertType> alertTypeSelection;
    private final AutoCompleteComboBox<BondedRoleListItem> bondedRoleSelection;
    private final CheckBox haltTradingCheckBox, requireVersionForTradingCheckBox;
    private final HBox requireVersionForTradingHBox;
    private final RichTableView<AlertListItem> alertTableView;
    private final RichTableView<DifficultyAdjustmentListItem> difficultyAdjustmentTableView;

    private Subscription selectedAlertTypePin, selectedBondedRolListItemPin;

    public SecurityManagerView(SecurityManagerModel model, SecurityManagerController controller) {
        super(new VBox(10), model, controller);

        root.setPadding(new Insets(20, 0, 0, 0));
        root.setAlignment(Pos.TOP_LEFT);


        // alerts
        Label alertHeadline = new Label(Res.get("authorizedRole.securityManager.alert.headline"));
        alertHeadline.getStyleClass().add("large-thin-headline");

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

        bondedRoleSelection = new AutoCompleteComboBox<>(model.getBondedRoleSortedList(), Res.get("authorizedRole.securityManager.selectBondedRole"));
        bondedRoleSelection.setMaxWidth(Double.MAX_VALUE);
        bondedRoleSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(BondedRoleListItem listItem) {
                return listItem != null ? controller.getBondedRoleDisplayString(listItem.getBondedRole()) : "";
            }

            @Override
            public BondedRoleListItem fromString(String string) {
                return null;
            }
        });

        headline = new MaterialTextField(Res.get("authorizedRole.securityManager.alert.message.headline"));
        message = new MaterialTextArea(Res.get("authorizedRole.securityManager.alert.message"));

        haltTradingCheckBox = new CheckBox(Res.get("authorizedRole.securityManager.emergency.haltTrading"));
        requireVersionForTradingCheckBox = new CheckBox(Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading"));
        minVersion = new MaterialTextField(Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading.version"),
                Res.get("authorizedRole.securityManager.emergency.requireVersionForTrading.version.prompt"));
        minVersion.setMinWidth(300);

        requireVersionForTradingHBox = new HBox(20, requireVersionForTradingCheckBox, minVersion);
        requireVersionForTradingHBox.setAlignment(Pos.CENTER_LEFT);

        bannedAccountData = new MaterialTextArea(Res.get("authorizedRole.securityManager.bannedAccounts.data"),
                Res.get("authorizedRole.securityManager.bannedAccounts.data.prompt"));

        sendAlertButton = new Button();
        sendAlertButton.setDefaultButton(true);
        sendAlertButton.setAlignment(Pos.BOTTOM_RIGHT);

        alertTableView = new RichTableView<>(model.getSortedAlertListItems(),
                Res.get("authorizedRole.securityManager.alert.table.headline"));
        configAlertTableView();


        // difficultyAdjustment
        Label difficultyAdjustmentHeadline = new Label(Res.get("authorizedRole.securityManager.difficultyAdjustment.headline"));
        difficultyAdjustmentHeadline.getStyleClass().add("large-thin-headline");

        difficultyAdjustmentFactor = new MaterialTextField(Res.get("authorizedRole.securityManager.difficultyAdjustment.description"));
        difficultyAdjustmentFactor.setMaxWidth(400);
        difficultyAdjustmentFactor.setValidators(DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR);

        difficultyAdjustmentButton = new Button(Res.get("authorizedRole.securityManager.difficultyAdjustment.button"));
        difficultyAdjustmentButton.setDefaultButton(true);

        difficultyAdjustmentTableView = new RichTableView<>(model.getDifficultyAdjustmentListItems(),
                Res.get("authorizedRole.securityManager.difficultyAdjustment.table.headline"));
        configDifficultyAdjustmentTableView();
        difficultyAdjustmentTableView.setMaxHeight(50);


        VBox.setMargin(alertTableView, new Insets(40, 0, 0, 0));
        VBox.setMargin(difficultyAdjustmentButton, new Insets(0, 0, 10, 0));
        VBox.setMargin(sendAlertButton, new Insets(10, 0, 0, 0));
        VBox.setMargin(haltTradingCheckBox, new Insets(10, 0, 0, 0));
        VBox.setMargin(difficultyAdjustmentHeadline, new Insets(20, 0, 0, 0));
        VBox.setVgrow(difficultyAdjustmentTableView, Priority.NEVER);
        VBox.setVgrow(alertTableView, Priority.NEVER);
        this.root.getChildren().addAll(
                alertHeadline,
                alertTypeSelection, headline, message,
                haltTradingCheckBox, requireVersionForTradingHBox,
                bondedRoleSelection,
                bannedAccountData,
                sendAlertButton,
                alertTableView,
                difficultyAdjustmentHeadline, difficultyAdjustmentFactor, difficultyAdjustmentButton,
                difficultyAdjustmentTableView);
    }

    @Override
    protected void onViewAttached() {
        alertTableView.initialize();
        difficultyAdjustmentTableView.initialize();

        Bindings.bindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor(),
                Converters.DOUBLE_STRING_CONVERTER);

        haltTradingCheckBox.visibleProperty().bind(model.getSelectedAlertType().isEqualTo(AlertType.EMERGENCY));
        haltTradingCheckBox.managedProperty().bind(haltTradingCheckBox.visibleProperty());
        requireVersionForTradingHBox.visibleProperty().bind(haltTradingCheckBox.visibleProperty());
        requireVersionForTradingHBox.managedProperty().bind(haltTradingCheckBox.visibleProperty());
        minVersion.textProperty().bindBidirectional(model.getMinVersion());
        minVersion.disableProperty().bind(requireVersionForTradingCheckBox.selectedProperty().not());
        difficultyAdjustmentButton.disableProperty().bind(model.getDifficultyAdjustmentFactorButtonDisabled());
        bondedRoleSelection.visibleProperty().bind(model.getBondedRoleSelectionVisible());
        bondedRoleSelection.managedProperty().bind(model.getBondedRoleSelectionVisible());

        headline.textProperty().bindBidirectional(model.getHeadline());
        headline.visibleProperty().bind(model.getAlertsVisible());
        headline.managedProperty().bind(model.getAlertsVisible());

        message.textProperty().bindBidirectional(model.getMessage());
        message.visibleProperty().bind(model.getAlertsVisible());
        message.managedProperty().bind(model.getAlertsVisible());

        bannedAccountData.textProperty().bindBidirectional(model.getBannedAccountData());
        bannedAccountData.visibleProperty().bind(model.getBannedAccountDataVisible());
        bannedAccountData.managedProperty().bind(model.getBannedAccountDataVisible());


        sendAlertButton.textProperty().bind(model.getActionButtonText());
        sendAlertButton.disableProperty().bind(model.getActionButtonDisabled());

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

        difficultyAdjustmentButton.setOnAction(e -> controller.onPublishDifficultyAdjustmentFactor());
        sendAlertButton.setOnAction(e -> controller.onSendAlert());
        haltTradingCheckBox.selectedProperty().bindBidirectional(model.getHaltTrading());
        requireVersionForTradingCheckBox.selectedProperty().bindBidirectional(model.getRequireVersionForTrading());

        selectedAlertTypePin = EasyBind.subscribe(model.getSelectedAlertType(),
                alertType -> alertTypeSelection.getSelectionModel().select(alertType));
        selectedBondedRolListItemPin = EasyBind.subscribe(model.getSelectedBondedRoleListItem(),
                bondedRole -> {
                    if (bondedRole == null) {
                        // FIXME: selection does not get cleared. Probably a bug inside of AutoCompleteComboBox
                        bondedRoleSelection.getSelectionModel().clearSelection();
                    } else {
                        bondedRoleSelection.getSelectionModel().select(bondedRole);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        alertTableView.dispose();
        difficultyAdjustmentTableView.dispose();

        Bindings.unbindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor());

        haltTradingCheckBox.visibleProperty().unbind();
        haltTradingCheckBox.managedProperty().unbind();
        requireVersionForTradingHBox.visibleProperty().unbind();
        requireVersionForTradingHBox.managedProperty().unbind();
        minVersion.textProperty().unbindBidirectional(model.getMinVersion());
        minVersion.disableProperty().unbind();
        difficultyAdjustmentButton.disableProperty().unbind();

        bondedRoleSelection.visibleProperty().unbind();
        bondedRoleSelection.managedProperty().unbind();

        headline.textProperty().unbindBidirectional(model.getHeadline());
        headline.visibleProperty().unbind();
        headline.managedProperty().unbind();

        message.textProperty().unbindBidirectional(model.getMessage());
        message.visibleProperty().unbind();
        message.managedProperty().unbind();

        bannedAccountData.textProperty().unbindBidirectional(model.getBannedAccountData());
        bannedAccountData.visibleProperty().unbind();
        bannedAccountData.managedProperty().unbind();

        sendAlertButton.textProperty().unbind();
        sendAlertButton.disableProperty().unbind();
        haltTradingCheckBox.selectedProperty().unbindBidirectional(model.getHaltTrading());
        requireVersionForTradingCheckBox.selectedProperty().unbindBidirectional(model.getRequireVersionForTrading());

        difficultyAdjustmentButton.setOnAction(null);
        alertTypeSelection.setOnChangeConfirmed(null);
        bondedRoleSelection.setOnChangeConfirmed(null);

        sendAlertButton.setOnAction(null);

        selectedAlertTypePin.unsubscribe();
        selectedBondedRolListItemPin.unsubscribe();
    }

    private void configDifficultyAdjustmentTableView() {
        difficultyAdjustmentTableView.getColumns().add(DateColumnUtil.getDateColumn(difficultyAdjustmentTableView.getSortOrder()));
        difficultyAdjustmentTableView.getColumns().add(new BisqTableColumn.Builder<DifficultyAdjustmentListItem>()
                .title(Res.get("authorizedRole.securityManager.difficultyAdjustment.table.value"))
                .minWidth(150)
                .comparator(Comparator.comparing(DifficultyAdjustmentListItem::getDifficultyAdjustmentFactor))
                .valueSupplier(DifficultyAdjustmentListItem::getDifficultyAdjustmentFactorString)
                .build());
        difficultyAdjustmentTableView.getColumns().add(new BisqTableColumn.Builder<DifficultyAdjustmentListItem>()
                .isSortable(false)
                .minWidth(200)
                .right()
                .setCellFactory(getRemoveDifficultyAdjustmentCellFactory())
                .build());
    }

    private void configAlertTableView() {
        alertTableView.getColumns().add(DateColumnUtil.getDateColumn(alertTableView.getSortOrder()));

        alertTableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.alertType"))
                .minWidth(150)
                .comparator(Comparator.comparing(AlertListItem::getAlertTypeString))
                .valueSupplier(AlertListItem::getAlertTypeString)
                .build());
        alertTableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.data"))
                .minWidth(200)
                .comparator(Comparator.comparing(AlertListItem::getData))
                .valueSupplier(AlertListItem::getData)
                .setCellFactory(getDataCellFactory())
                .build());
        alertTableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.haltTrading"))
                .minWidth(120)
                .comparator(Comparator.comparing(AlertListItem::getHaltTrading))
                .valueSupplier(AlertListItem::getHaltTrading)
                .build());
        alertTableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.requireVersionForTrading"))
                .minWidth(120)
                .comparator(Comparator.comparing(AlertListItem::getRequireVersionForTrading))
                .valueSupplier(AlertListItem::getRequireVersionForTrading)
                .build());
        alertTableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.minVersion"))
                .minWidth(120)
                .comparator(Comparator.comparing(AlertListItem::getMinVersion))
                .valueSupplier(AlertListItem::getMinVersion)
                .build());

        BisqTableColumn<AlertListItem> bannedRoleColumn = new BisqTableColumn.Builder<AlertListItem>()
                .title(Res.get("authorizedRole.securityManager.alert.table.bannedRole"))
                .minWidth(150)
                .comparator(Comparator.comparing(AlertListItem::getBondedRoleDisplayString))
                .valueSupplier(AlertListItem::getBondedRoleDisplayString)
                .tooltipSupplier(AlertListItem::getBondedRoleDisplayString)
                .build();
        alertTableView.getColumns().add(bannedRoleColumn);
        alertTableView.getSortOrder().add(bannedRoleColumn);

        alertTableView.getColumns().add(new BisqTableColumn.Builder<AlertListItem>()
                .isSortable(false)
                .minWidth(200)
                .right()
                .setCellFactory(getRemoveAlertCellFactory())
                .includeForCsv(false)
                .build());
    }

    private Callback<TableColumn<AlertListItem, AlertListItem>, TableCell<AlertListItem, AlertListItem>> getDataCellFactory() {
        return column -> {

            return new TableCell<>() {
                private final Label label = new Label();
                private final BisqIconButton copyButton = new BisqIconButton();
                private final HBox hBox = new HBox(5, label, Spacer.fillHBox(), copyButton);
                private final BisqTooltip tooltip = new BisqTooltip();

                {
                    copyButton.setIcon(AwesomeIcon.COPY);
                    copyButton.setAlignment(Pos.TOP_RIGHT);
                    copyButton.setOpacity(0.8);
                    label.setAlignment(Pos.CENTER_LEFT);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                }

                @Override
                protected void updateItem(AlertListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        String data = item.getData();
                        label.setText(StringUtils.truncate(data, 25));
                        tooltip.setText(data);
                        label.setTooltip(tooltip);
                        copyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(data));
                        setGraphic(hBox);
                    } else {
                        label.setTooltip(null);
                        copyButton.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        };
    }

    private Callback<TableColumn<AlertListItem, AlertListItem>, TableCell<AlertListItem, AlertListItem>> getRemoveAlertCellFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button(Res.get("data.remove"));

            @Override
            protected void updateItem(AlertListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty && controller.isRemoveDifficultyAdjustmentButtonVisible(item.getAuthorizedAlertData())) {
                    button.setOnAction(e -> controller.onRemoveAlert(item.getAuthorizedAlertData()));
                    setGraphic(button);
                } else {
                    button.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<DifficultyAdjustmentListItem, DifficultyAdjustmentListItem>,
            TableCell<DifficultyAdjustmentListItem, DifficultyAdjustmentListItem>> getRemoveDifficultyAdjustmentCellFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button(Res.get("data.remove"));

            @Override
            protected void updateItem(DifficultyAdjustmentListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty && controller.isRemoveDifficultyAdjustmentButtonVisible(item.getData())) {
                    button.setOnAction(e -> controller.onRemoveDifficultyAdjustmentListItem(item));
                    setGraphic(button);
                } else {
                    button.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }


    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @ToString
    public static class DifficultyAdjustmentListItem implements DateTableItem {
        @EqualsAndHashCode.Include
        private final AuthorizedDifficultyAdjustmentData data;

        private final long date;
        private final String dateString, timeString, difficultyAdjustmentFactorString;
        private final double difficultyAdjustmentFactor;

        public DifficultyAdjustmentListItem(AuthorizedDifficultyAdjustmentData data) {
            this.data = data;
            date = data.getDate();
            dateString = DateFormatter.formatDate(date);
            timeString = DateFormatter.formatTime(date);
            difficultyAdjustmentFactor = data.getDifficultyAdjustmentFactor();
            difficultyAdjustmentFactorString = String.valueOf(difficultyAdjustmentFactor);
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @ToString
    public static class AlertListItem implements DateTableItem {
        @EqualsAndHashCode.Include
        private final AuthorizedAlertData authorizedAlertData;

        private final long date;
        private final String dateString, timeString, alertTypeString, haltTrading, requireVersionForTrading,
                minVersion, bondedRoleDisplayString, data;

        public AlertListItem(AuthorizedAlertData authorizedAlertData, SecurityManagerController controller) {
            this.authorizedAlertData = authorizedAlertData;
            date = authorizedAlertData.getDate();
            dateString = DateFormatter.formatDate(date);
            timeString = DateFormatter.formatTime(date);
            AlertType alertType = authorizedAlertData.getAlertType();
            alertTypeString = Res.get("authorizedRole.securityManager.alertType." + alertType.name());
            minVersion = authorizedAlertData.getMinVersion().orElse("");
            haltTrading = BooleanFormatter.toYesNo(authorizedAlertData.isHaltTrading());
            requireVersionForTrading = BooleanFormatter.toYesNo(authorizedAlertData.isRequireVersionForTrading());
            bondedRoleDisplayString = authorizedAlertData.getBannedRole().map(controller::getBannedBondedRoleDisplayString).orElse("");

            String headline = authorizedAlertData.getHeadline().orElse(Res.get("data.na"));
            String message = authorizedAlertData.getMessage().orElse(Res.get("data.na"));
            String announcement = headline + "\n" + message;
            switch (alertType) {
                case INFO, WARN, EMERGENCY -> data = announcement;
                case BAN -> data = authorizedAlertData.getBannedRole()
                        .flatMap(role -> role.getAddressByTransportTypeMap()
                                .map(AddressByTransportTypeMap::toString))
                        .orElse(Res.get("data.na"));
                case BANNED_ACCOUNT_DATA ->
                        data = authorizedAlertData.getBannedAccountData().orElse(Res.get("data.na"));
                default -> data = Res.get("data.na");
            }
        }
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class BondedRoleListItem {
        @EqualsAndHashCode.Include
        private final BondedRole bondedRole;

        private final String displayString;

        public BondedRoleListItem(BondedRole bondedRole, SecurityManagerController controller) {
            this.bondedRole = bondedRole;
            displayString = controller.getBondedRoleDisplayString(bondedRole);
        }
    }
}
