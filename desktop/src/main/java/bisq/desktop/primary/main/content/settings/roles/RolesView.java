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

package bisq.desktop.primary.main.content.settings.roles;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.primary.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ProfileAgeService;
import bisq.user.role.AuthorizedRoleRegistrationData;
import de.jensd.fx.fontawesome.AwesomeIcon;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class RolesView extends View<VBox, RolesModel, RolesController> {
    private final BisqTableView<ListItem> tableView;
    private Subscription userProfileIdOfScoreUpdatePin;

    public RolesView(RolesModel model,
                     RolesController controller,
                     VBox rolesTabControllerRoot) {
        super(new VBox(20), model, controller);

        Label tableHeadline = new Label(Res.get("roles.table.headline"));
        tableHeadline.getStyleClass().add("bisq-content-headline-label");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.setMinHeight(200);
        configTableView();

        VBox.setVgrow(rolesTabControllerRoot, Priority.ALWAYS);
        VBox.setMargin(rolesTabControllerRoot, new Insets(30, 0, 20, 0));
        VBox.setMargin(tableHeadline, new Insets(0, 0, -10, 10));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        root.getChildren().addAll(rolesTabControllerRoot, tableHeadline, tableView);
    }

    @Override
    protected void onViewAttached() {
        userProfileIdOfScoreUpdatePin = EasyBind.subscribe(model.getUserProfileIdOfScoreUpdate(), profileId -> {
            if (profileId != null) {
                tableView.refresh();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        userProfileIdOfScoreUpdatePin.unsubscribe();
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("roles.table.columns.userProfile"))
                .isFirst()
                .comparator(Comparator.comparing(ListItem::getUserName))
                .setCellFactory(getUserProfileCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("roles.table.columns.profileAge"))
                .fixWidth(100)
                .comparator(Comparator.comparing(ListItem::getProfileAge))
                .valueSupplier(ListItem::getProfileAgeString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("roles.table.columns.role"))
                .fixWidth(100)
                .comparator(Comparator.comparing(ListItem::getRoleType))
                .valueSupplier(ListItem::getRoleType)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("roles.table.columns.pubKey"))
                .minWidth(200)
                .comparator(Comparator.comparing(ListItem::getPublicKeyAsHex))
                .setCellFactory(getPubKeyCellFactory())
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

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getPubKeyCellFactory() {
        return column -> new TableCell<>() {
            private final Label pubKey = new Label();
            private final Button icon = BisqIconButton.createIconButton(AwesomeIcon.COPY);
            private final HBox hBox = new HBox(pubKey, icon);

            {
                icon.setMinWidth(30);
                // HBox.setHgrow(pubKey, Priority.NEVER);
                HBox.setHgrow(icon, Priority.ALWAYS);
                HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    pubKey.setText(item.getPublicKeyAsHex());
                    icon.setOnAction(e -> controller.onCopyPublicKeyAsHex(item.getPublicKeyAsHex()));
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
        private final String publicKeyAsHex;
        private final AuthorizedRoleRegistrationData data;
        private final String userName;
        private final Long profileAge;
        private final String profileAgeString;

        public ListItem(AuthorizedData authorizedData, ProfileAgeService profileAgeService) {
            data = (AuthorizedRoleRegistrationData) authorizedData.getAuthorizedDistributedData();
            this.userProfile = data.getUserProfile();
            this.roleType = Res.get("roles.type." + data.getRoleType());
            this.publicKeyAsHex = data.getPublicKeyAsHex();

            profileAge = profileAgeService.getProfileAge(userProfile)
                    .orElse(0L);
            profileAgeString = profileAgeService.getProfileAge(userProfile)
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("na"));

            userName = userProfile.getUserName();
        }
    }
}
