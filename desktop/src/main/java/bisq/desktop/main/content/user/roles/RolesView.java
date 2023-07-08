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

package bisq.desktop.main.content.user.roles;

import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedBondedRoleData;
import bisq.bonded_roles.node.bisq1_bridge.data.AuthorizedOracleNode;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.OrderedList;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.role.RoleType;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class RolesView extends View<VBox, RolesModel, RolesController> {
    private final BisqTableView<ListItem> tableView;

    public RolesView(RolesModel model,
                     RolesController controller,
                     VBox tabControllerRoot) {
        super(new VBox(20), model, controller);

        Label tableHeadline = new Label(Res.get("user.roles.table.headline"));
        tableHeadline.getStyleClass().add("bisq-content-headline-label");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.setMinHeight(200);
        configTableView();

        Label verificationHeadline = new Label(Res.get("user.bondedRoles.verification.howTo"));
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

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.table.columns.userProfile"))
                .isFirst()
                .minWidth(150)
                .comparator(Comparator.comparing(ListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.roles.table.columns.role"))
                .fixWidth(150)
                .comparator(Comparator.comparing(ListItem::getRoleType))
                .valueSupplier(ListItem::getRoleType)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.table.columns.bondUserName"))
                .minWidth(200)
                .comparator(Comparator.comparing(ListItem::getBondUserName))
                .valueSupplier(ListItem::getBondUserName)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.table.columns.profileId"))
                .minWidth(150)
                .comparator(Comparator.comparing(ListItem::getUserProfileId))
                .setCellFactory(getUserProfileIdCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.table.columns.signature"))
                .minWidth(150)
                .comparator(Comparator.comparing(ListItem::getSignature))
                .setCellFactory(getSignatureCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("user.table.columns.oracleNode"))
                .minWidth(200)
                .isLast()
                .comparator(Comparator.comparing(ListItem::getOracleNodeUserName))
                .valueSupplier(ListItem::getOracleNodeUserName)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userName = new Label();
            private final UserProfileIcon userProfileIcon = new UserProfileIcon(30);
            private final HBox hBox = new HBox(10, userProfileIcon, userName);

            {
                userName.setId("chat-user-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final ListItem item, boolean empty) {
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

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getUserProfileIdCellFactory() {
        return column -> new TableCell<>() {
            private final Label userProfileId = new Label();
            private final Button icon = BisqIconButton.createIconButton(AwesomeIcon.COPY);
            private final HBox hBox = new HBox(userProfileId, icon);

            {
                icon.setMinWidth(30);
                HBox.setHgrow(icon, Priority.ALWAYS);
                HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final ListItem item, boolean empty) {
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

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getSignatureCellFactory() {
        return column -> new TableCell<>() {
            private final Label signature = new Label();
            private final Button icon = BisqIconButton.createIconButton(AwesomeIcon.COPY);
            private final HBox hBox = new HBox(signature, icon);

            {
                icon.setMinWidth(30);
                HBox.setHgrow(icon, Priority.ALWAYS);
                HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    signature.setText(item.getSignature());
                    Tooltip tooltip = new BisqTooltip(item.getSignature());
                    tooltip.getStyleClass().add("dark-tooltip");
                    signature.setTooltip(tooltip);

                    icon.setOnAction(e -> controller.onCopyPublicKeyAsHex(item.getSignature()));
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

    @EqualsAndHashCode
    @Getter
    @ToString
    static class ListItem implements TableItem {
        private final UserProfile userProfile;
        private final String roleType;
        private final String bondUserName;
        private final String signature;
        private final String userProfileId;
        private final String userName;
        private final AuthorizedOracleNode oracleNode;
        private final String oracleNodeUserName;

        public ListItem(AuthorizedBondedRoleData bondedRoleData, UserService userService) {
            oracleNode = bondedRoleData.getOracleNode();
            oracleNodeUserName = oracleNode.getUserName();
            userProfile = userService.getUserProfileService().findUserProfile(bondedRoleData.getProfileId()).orElseThrow();
            userProfileId = userProfile.getId();
            userName = userProfile.getUserName();
            bondUserName = bondedRoleData.getBondUserName();
            signature = bondedRoleData.getSignature();
            RoleType type = RoleType.valueOf(bondedRoleData.getRoleType());
            roleType = Res.get("user.roles.type." + type);
        }
    }
}
