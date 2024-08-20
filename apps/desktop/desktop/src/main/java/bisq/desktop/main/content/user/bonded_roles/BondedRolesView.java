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
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BondedRolesView<M extends BondedRolesModel, C extends BondedRolesController> extends View<VBox, M, C> {
    protected final RichTableView<BondedRolesListItem> richTableView;

    public BondedRolesView(M model, C controller, VBox tabControllerRoot) {
        super(new VBox(20), model, controller);

        richTableView = new RichTableView<>(model.getSortedList(), getTableHeadline(), controller::applySearchPredicate);
        configTableView();

        Label verificationHeadline = new Label(getVerificationHeadline());
        verificationHeadline.getStyleClass().add("bisq-text-headline-2");
        OrderedList verificationInstruction = new OrderedList(Res.get("user.bondedRoles.verification.howTo.instruction"), "bisq-text-13");

        VBox.setMargin(tabControllerRoot, new Insets(0, 0, 20, 0));
        VBox.setMargin(verificationHeadline, new Insets(0, 0, -10, 10));
        VBox.setMargin(verificationInstruction, new Insets(0, 0, 0, 10));
        VBox.setVgrow(richTableView, Priority.ALWAYS);
        root.setPadding(new Insets(0, 40, 40, 40));
        root.getChildren().addAll(tabControllerRoot, richTableView, verificationHeadline, verificationInstruction);
    }

    protected abstract String getVerificationHeadline();

    protected abstract String getTableHeadline();

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
    }

    protected abstract void configTableView();

    protected Callback<TableColumn<BondedRolesListItem, BondedRolesListItem>, TableCell<BondedRolesListItem, BondedRolesListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userName = new Label();
            private final UserProfileIcon userProfileIcon = new UserProfileIcon();
            private final HBox hBox = new HBox(10, userProfileIcon, userName);
            private final BisqTooltip tooltip = new BisqTooltip(Res.get("user.bondedRoles.table.columns.userProfile.defaultNode"), BisqTooltip.Style.DARK);

            {
                userName.setId("chat-user-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(BondedRolesListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userName.setText(item.getUserName());
                    if (item.isRootNode()) {
                        userName.setTooltip(tooltip);
                        userName.setStyle("-fx-text-fill: -bisq2-green;");
                    } else {
                        userName.setTooltip(null);
                        userName.setStyle("-fx-text-fill: -fx-light-text-color;");
                    }

                    item.getUserProfile().ifPresent(userProfileIcon::setUserProfile);
                    setGraphic(hBox);
                } else {
                    userProfileIcon.dispose();
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
            protected void updateItem(BondedRolesListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileId.setText(item.getUserProfileId());
                    userProfileId.setTooltip(new BisqTooltip(item.getUserProfileId(), BisqTooltip.Style.DARK));

                    icon.setOnAction(e -> controller.onCopyPublicKeyAsHex(item.getUserProfileId()));
                    icon.setTooltip(new BisqTooltip(Res.get("action.copyToClipboard"), BisqTooltip.Style.DARK));
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
            protected void updateItem(BondedRolesListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    signature.setText(item.getSignature());
                    signature.setTooltip(new BisqTooltip(item.getSignature(), BisqTooltip.Style.DARK));

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
