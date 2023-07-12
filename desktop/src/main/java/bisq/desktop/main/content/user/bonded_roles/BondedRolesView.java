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

package bisq.desktop.main.content.user.bonded_roles;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.OrderedList;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BondedRolesView<M extends BondedRolesModel, C extends BondedRolesController> extends View<VBox, M, C> {
    protected final BisqTableView<BondedRolesListItem> tableView;

    public BondedRolesView(M model,
                           C controller,
                           VBox tabControllerRoot) {
        super(new VBox(20), model, controller);

        Label tableHeadline = new Label(getTableHeadline());
        tableHeadline.getStyleClass().add("bisq-content-headline-label");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.setMinHeight(200);
        tableView.getStyleClass().add("user-bonded-roles-table-view");
        configTableView();

        Label verificationHeadline = new Label(getVerificationHeadline());
        verificationHeadline.getStyleClass().add("bisq-text-headline-2");
        OrderedList verificationInstruction = new OrderedList(Res.get("user.bondedRoles.verification.howTo.instruction"), "bisq-text-13");

        VBox.setVgrow(tabControllerRoot, Priority.ALWAYS);
        VBox.setMargin(tabControllerRoot, new Insets(30, 0, 20, 0));
        VBox.setMargin(tableHeadline, new Insets(0, 0, -10, 10));
        VBox.setMargin(verificationHeadline, new Insets(0, 0, -10, 10));
        VBox.setMargin(verificationInstruction, new Insets(0, 0, 0, 10));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        root.getChildren().addAll(tabControllerRoot, tableHeadline, tableView, verificationHeadline, verificationInstruction);
    }

    protected String getVerificationHeadline() {
        return Res.get("user.bondedRoles.verification.howTo");
    }

    protected abstract String getTableHeadline();

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    protected abstract void configTableView();

    protected Callback<TableColumn<BondedRolesListItem, BondedRolesListItem>, TableCell<BondedRolesListItem, BondedRolesListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userName = new Label();
            private final UserProfileIcon userProfileIcon = new UserProfileIcon(30);
            private final HBox hBox = new HBox(10, userProfileIcon, userName);

            {
                userName.setId("chat-user-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final BondedRolesListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userName.setText(item.getUserName());
                    userProfileIcon.setUserProfile(item.getUserProfile());
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    protected Callback<TableColumn<BondedRolesListItem, BondedRolesListItem>, TableCell<BondedRolesListItem, BondedRolesListItem>> getUserProfileIdCellFactory() {
        return column -> new TableCell<>() {
            private final Label userProfileId = new Label();
            private final Button icon = BisqIconButton.createCopyIconButton();
            private final HBox hBox = new HBox(userProfileId, icon);

            {
                icon.setMinWidth(30);
                HBox.setHgrow(icon, Priority.ALWAYS);
                HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final BondedRolesListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileId.setText(item.getUserProfileId());
                    Tooltip tooltip = new BisqTooltip(item.getUserProfileId());
                    tooltip.getStyleClass().add("dark-tooltip");
                    userProfileId.setTooltip(tooltip);

                    icon.setOnAction(e -> controller.onCopyPublicKeyAsHex(item.getUserProfileId()));
                    Tooltip tooltip2 = new BisqTooltip(Res.get("action.copyToClipboard"));
                    tooltip2.getStyleClass().add("dark-tooltip");
                    icon.setTooltip(tooltip2);
                    setGraphic(hBox);
                } else {
                    icon.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }

    protected Callback<TableColumn<BondedRolesListItem, BondedRolesListItem>, TableCell<BondedRolesListItem, BondedRolesListItem>> getSignatureCellFactory() {
        return column -> new TableCell<>() {
            private final Label signature = new Label();
            private final Button icon = BisqIconButton.createCopyIconButton();
            private final HBox hBox = new HBox(signature, icon);

            {
                icon.setMinWidth(30);
                HBox.setHgrow(icon, Priority.ALWAYS);
                HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final BondedRolesListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    signature.setText(item.getSignature());
                    Tooltip tooltip = new BisqTooltip(item.getSignature());
                    tooltip.getStyleClass().add("dark-tooltip");
                    signature.setTooltip(tooltip);

                    icon.setOnAction(e -> controller.onCopyPublicKeyAsHex(item.getSignature()));
                    setGraphic(hBox);
                } else {
                    icon.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }
}
