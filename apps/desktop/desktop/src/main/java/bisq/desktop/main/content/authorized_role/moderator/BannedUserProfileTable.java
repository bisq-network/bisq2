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

package bisq.desktop.main.content.authorized_role.moderator;

import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.chat.ChatChannelDomain;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.network.SendMessageResult;
import bisq.support.moderator.ModeratorService;
import bisq.user.banned.BannedUserProfileData;
import bisq.user.banned.BannedUserService;
import bisq.user.profile.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;

public class BannedUserProfileTable {
    @Getter
    private final Controller controller;

    public BannedUserProfileTable(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    public Pane getRoot() {
        return controller.getView().getRoot();
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final BannedUserService bannedUserService;
        private final ModeratorService moderatorService;
        private Pin bannedUserListItemsPin;

        private Controller(ServiceProvider serviceProvider) {
            bannedUserService = serviceProvider.getUserService().getBannedUserService();
            moderatorService = serviceProvider.getSupportService().getModeratorService();

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            bannedUserListItemsPin = FxBindings.<BannedUserProfileData, View.ListItem>bind(model.getListItems())
                    .map(View.ListItem::new)
                    .to(bannedUserService.getBannedUserProfileDataSet());
        }

        @Override
        public void onDeactivate() {
            bannedUserListItemsPin.unbind();
        }

        void onContactUser(BannedUserProfileData data) {
            ChatChannelDomain chatChannelDomain = ChatChannelDomain.SUPPORT;
            moderatorService.contactUser(chatChannelDomain, data.getUserProfile(), Optional.empty(), false)
                    .whenComplete((result, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable == null) {
                                SendMessageResult.findAnyErrorMsg(result)
                                        .ifPresent(errorMsg -> new Popup().error(errorMsg).show());
                                navigateToChannel(chatChannelDomain);
                                UIThread.runOnNextRenderFrame(() -> navigateToChannel(chatChannelDomain));
                            } else {
                                new Popup().error(throwable).show();
                            }
                        });
                    });
        }

        void onRemoveBan(BannedUserProfileData data) {
            moderatorService.unBanReportedUser(data);
        }

        private void navigateToChannel(ChatChannelDomain chatChannelDomain) {
            switch (chatChannelDomain) {
                case BISQ_EASY_OFFERBOOK:
                case BISQ_EASY_OPEN_TRADES:
                case BISQ_EASY_PRIVATE_CHAT:
                    Navigation.navigateTo(NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
                    break;
                case DISCUSSION:
                    Navigation.navigateTo(NavigationTarget.DISCUSSION);
                    break;
                case EVENTS:
                    Navigation.navigateTo(NavigationTarget.EVENTS);
                    break;
                case SUPPORT:
                    Navigation.navigateTo(NavigationTarget.SUPPORT);
                    break;
            }
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        private BondedRole bondedRole;
        private final ObservableList<View.ListItem> listItems = FXCollections.observableArrayList();
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final RichTableView<ListItem> richTableView;

        private View(Model model, Controller controller) {
            super(new VBox(5), model, controller);

            root.setAlignment(Pos.TOP_LEFT);

            richTableView = new RichTableView<>(model.getListItems(), Res.get("authorizedRole.moderator.bannedUserProfile.table.headline"));
            configTableView();

            root.getChildren().addAll(richTableView);
        }

        @Override
        protected void onViewAttached() {
            richTableView.initialize();
        }

        @Override
        protected void onViewDetached() {
            richTableView.dispose();
        }

        private void configTableView() {
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("authorizedRole.moderator.bannedUserProfile.table.userProfile"))
                    .minWidth(150)
                    .left()
                    .comparator(Comparator.comparing(ListItem::getUserName))
                    .valueSupplier(ListItem::getUserName)
                    .setCellFactory(getUserProfileCellFactory())
                    .build());
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .isSortable(false)
                    .fixWidth(200)
                    .setCellFactory(getContactCellFactory())
                    .includeForCsv(false)
                    .build());
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .isSortable(false)
                    .fixWidth(250)
                    .setCellFactory(getRemoveBanCellFactory())
                    .includeForCsv(false)
                    .build());
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getUserProfileCellFactory() {
            return column -> new TableCell<>() {
                private final Label userName = new Label();
                private final UserProfileIcon userProfileIcon = new UserProfileIcon();
                private final HBox hBox = new HBox(10, userProfileIcon, userName);

                {
                    userName.setId("chat-user-name");
                    hBox.setAlignment(Pos.CENTER_LEFT);
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        userName.setText(item.getUserName());
                        userProfileIcon.setUserProfile(item.getUserProfile());
                        setGraphic(hBox);
                    } else {
                        userProfileIcon.dispose();
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getContactCellFactory() {
            return column -> new TableCell<>() {
                private final Button button = new Button(Res.get("authorizedRole.moderator.bannedUserProfile.table.contact"));

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        button.setOnAction(e -> controller.onContactUser(item.getBannedUserProfileData()));
                        setGraphic(button);
                    } else {
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getRemoveBanCellFactory() {
            return column -> new TableCell<>() {
                private final Button button = new Button(Res.get("authorizedRole.moderator.table.removeBan"));

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        button.setOnAction(e -> controller.onRemoveBan(item.getBannedUserProfileData()));
                        setGraphic(button);
                    } else {
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        @Getter
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        private static class ListItem {
            @EqualsAndHashCode.Include
            private final BannedUserProfileData bannedUserProfileData;

            private final UserProfile userProfile;
            private final String userName;

            private ListItem(BannedUserProfileData bannedUserProfileData) {
                this.bannedUserProfileData = bannedUserProfileData;

                userProfile = bannedUserProfileData.getUserProfile();
                userName = userProfile.getUserName();
            }
        }
    }
}