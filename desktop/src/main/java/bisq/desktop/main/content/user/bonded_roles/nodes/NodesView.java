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

package bisq.desktop.main.content.user.bonded_roles.nodes;

import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.main.content.user.bonded_roles.BondedRolesListItem;
import bisq.desktop.main.content.user.bonded_roles.BondedRolesView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class NodesView extends BondedRolesView<NodesModel, NodesController> {
    public NodesView(NodesModel model,
                     NodesController controller,
                     VBox tabControllerRoot) {
        super(model, controller, tabControllerRoot);
    }

    @Override
    protected String getTableHeadline() {
        return Res.get("user.bondedRoles.table.headline.nodes");
    }

    @Override
    protected String getVerificationHeadline() {
        return Res.get("user.bondedRoles.verification.howTo.nodes");
    }

    @Override
    protected void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.userProfile"))
                .isFirst()
                .minWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.node"))
                .fixWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getRoleTypeString))
                .valueSupplier(BondedRolesListItem::getRoleTypeString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.node.address"))
                .minWidth(200)
                .setCellFactory(getAddressCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.bondUserName"))
                .minWidth(200)
                .comparator(Comparator.comparing(BondedRolesListItem::getBondUserName))
                .valueSupplier(BondedRolesListItem::getBondUserName)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.profileId"))
                .minWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getUserProfileId))
                .setCellFactory(getUserProfileIdCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.signature"))
                .minWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getSignature))
                .setCellFactory(getSignatureCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.oracleNode"))
                .minWidth(200)
                .isLast()
                .comparator(Comparator.comparing(BondedRolesListItem::getOracleNodeUserName))
                .valueSupplier(BondedRolesListItem::getOracleNodeUserName)
                .build());
    }

    private Callback<TableColumn<BondedRolesListItem, BondedRolesListItem>, TableCell<BondedRolesListItem, BondedRolesListItem>> getAddressCellFactory() {
        return column -> new TableCell<>() {
            private final Label address = new Label();
            private final Button icon = BisqIconButton.createInfoIconButton(Res.get("user.bondedRoles.table.columns.node.address.openPopup"));
            private final HBox hBox = new HBox(address, icon);

            {
                icon.setMinWidth(30);
                HBox.setHgrow(icon, Priority.ALWAYS);
                HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                hBox.setAlignment(Pos.CENTER_RIGHT);
            }

            @Override
            public void updateItem(final BondedRolesListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    String addressString = item.getAddress();
                    address.setText(addressString);
                    Tooltip tooltip = new BisqTooltip(addressString);
                    tooltip.getStyleClass().add("dark-tooltip");
                    address.setTooltip(tooltip);
                    icon.setOnAction(e -> new Popup()
                            .headLine(Res.get("user.bondedRoles.table.columns.node.address.popup.headline"))
                            .message(item.getAddressInfoJson())
                            .show());
                    setGraphic(hBox);
                } else {
                    icon.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }
}
