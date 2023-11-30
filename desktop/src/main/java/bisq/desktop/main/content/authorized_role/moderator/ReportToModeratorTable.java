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

import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.chat.ChatChannelDomain;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.network.SendMessageResult;
import bisq.presentation.formatters.DateFormatter;
import bisq.support.moderator.ModeratorService;
import bisq.support.moderator.ReportToModeratorMessage;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;

public class ReportToModeratorTable {
    private final Controller controller;

    public Pane getRoot() {
        return controller.getView().getRoot();
    }

    public ReportToModeratorTable(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    @Slf4j
    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final ServiceProvider serviceProvider;
        private final ModeratorService moderatorService;
        private Pin reportListItemsPin;

        private Controller(ServiceProvider serviceProvider) {
            this.serviceProvider = serviceProvider;
            moderatorService = serviceProvider.getSupportService().getModeratorService();

            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            reportListItemsPin = FxBindings.<ReportToModeratorMessage, View.ReportListItem>bind(model.getListItems())
                    .map(message -> new View.ReportListItem(message, serviceProvider))
                    .to(moderatorService.getReportToModeratorMessages());
        }

        @Override
        public void onDeactivate() {
            reportListItemsPin.unbind();
        }

        void onContactUser(ReportToModeratorMessage message, UserProfile userProfile) {
            ChatChannelDomain chatChannelDomain = message.getChatChannelDomain();
            navigateToChannel(chatChannelDomain);
            moderatorService.contactUser(chatChannelDomain, userProfile, Optional.of(message.getMessage()))
                    .whenComplete((result, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable == null) {
                                SendMessageResult.findAnyErrorMsg(result)
                                        .ifPresent(errorMsg -> new Popup().error(errorMsg).show());
                            } else {
                                new Popup().error(throwable).show();
                            }
                        });
                    });
        }

        void onBan(ReportToModeratorMessage message) {
            moderatorService.banReportedUser(message);
        }

        void onDeleteMessage(ReportToModeratorMessage message) {
            moderatorService.deleteReportToModeratorMessage(message);
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
        private final ObservableList<View.ReportListItem> listItems = FXCollections.observableArrayList();

    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqTableView<ReportListItem> tableView;

        private View(Model model, Controller controller) {
            super(new VBox(5), model, controller);

            root.setAlignment(Pos.TOP_LEFT);

            Label headline = new Label(Res.get("authorizedRole.moderator.reportToModerator.table.headline"));
            headline.getStyleClass().add("large-thin-headline");

            tableView = new BisqTableView<>(model.getListItems());
            tableView.setMinHeight(200);
            tableView.getStyleClass().add("user-bonded-roles-table-view");
            configTableView();

            root.getChildren().addAll(headline, tableView);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // ReportTable
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void configTableView() {
            BisqTableColumn<ReportListItem> date = new BisqTableColumn.Builder<ReportListItem>()
                    .title(Res.get("authorizedRole.moderator.table.date"))
                    .fixWidth(180)
                    .left()
                    .comparator(Comparator.comparing(ReportListItem::getDate).reversed())
                    .valueSupplier(ReportListItem::getDateString)
                    .build();
            tableView.getColumns().add(date);
            tableView.getSortOrder().add(date);
            tableView.getColumns().add(new BisqTableColumn.Builder<ReportListItem>()
                    .title(Res.get("authorizedRole.moderator.table.reporter"))
                    .minWidth(250)
                    .left()
                    .comparator(Comparator.comparing(ReportListItem::getReporterUserName))
                    .setCellFactory(getReporterUserProfileCellFactory())
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<ReportListItem>()
                    .title(Res.get("authorizedRole.moderator.table.accused"))
                    .minWidth(250)
                    .left()
                    .comparator(Comparator.comparing(ReportListItem::getAccusedUserName))
                    .setCellFactory(getAccusedUserProfileCellFactory())
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<ReportListItem>()
                    .title(Res.get("authorizedRole.moderator.table.message"))
                    .minWidth(150)
                    .left()
                    .comparator(Comparator.comparing(ReportListItem::getMessage))
                    .setCellFactory(getMessageCellFactory())
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<ReportListItem>()
                    .title(Res.get("authorizedRole.moderator.table.chatChannelDomain"))
                    .fixWidth(150)
                    .left()
                    .comparator(Comparator.comparing(ReportListItem::getChatChannelDomain))
                    .valueSupplier(ReportListItem::getChatChannelDomain)
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<ReportListItem>()
                    .isSortable(false)
                    .fixWidth(130)
                    .setCellFactory(getBanCellFactory())
                    .build());
            tableView.getColumns().add(new BisqTableColumn.Builder<ReportListItem>()
                    .isSortable(false)
                    .fixWidth(130)
                    .right()
                    .setCellFactory(getDeleteMessageCellFactory())
                    .build());
        }

        private Callback<TableColumn<ReportListItem, ReportListItem>, TableCell<ReportListItem, ReportListItem>> getReporterUserProfileCellFactory() {
            return column -> new TableCell<>() {
                private final Label userNameLabel = new Label();
                private final UserProfileIcon userProfileIcon = new UserProfileIcon(30);
                private final Button button = new Button();
                private final HBox hBox = new HBox(10, userProfileIcon, userNameLabel, Spacer.fillHBox(), button);

                {
                    userNameLabel.setId("chat-user-name");
                    userNameLabel.setMinWidth(50);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    button.setMinWidth(100);
                    button.setStyle("-fx-padding: 5 10 5 10;");
                }

                @Override
                public void updateItem(final ReportListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty && item.getReporterUserProfile().isPresent()) {
                        String userName = item.getReporterUserName();
                        UserProfile userProfile = item.getReporterUserProfile().get();
                        userNameLabel.setText(userName);
                        userProfileIcon.setUserProfile(userProfile);
                        button.setText(Res.get("authorizedRole.moderator.table.contact") + " " + StringUtils.truncate(userName, 8));
                        button.setOnAction(e -> controller.onContactUser(item.getReportToModeratorMessage(), userProfile));
                        setGraphic(hBox);
                    } else {
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ReportListItem, ReportListItem>, TableCell<ReportListItem, ReportListItem>> getAccusedUserProfileCellFactory() {
            return column -> new TableCell<>() {
                private final Label userNameLabel = new Label();
                private final UserProfileIcon userProfileIcon = new UserProfileIcon(30);
                private final Button button = new Button();
                private final HBox hBox = new HBox(10, userProfileIcon, userNameLabel, Spacer.fillHBox(), button);

                {
                    userNameLabel.setId("chat-user-name");
                    userNameLabel.setMinWidth(50);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    button.setMinWidth(100);
                    button.setStyle("-fx-padding: 5 10 5 10;");
                }

                @Override
                public void updateItem(final ReportListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        String userName = item.getAccusedUserName();
                        UserProfile userProfile = item.getAccusedUserProfile();
                        userNameLabel.setText(userName);
                        userProfileIcon.setUserProfile(userProfile);
                        button.setText(Res.get("authorizedRole.moderator.table.contact") + " " + StringUtils.truncate(userName, 8));
                        button.setOnAction(e -> controller.onContactUser(item.getReportToModeratorMessage(), userProfile));
                        setGraphic(hBox);
                    } else {
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ReportListItem, ReportListItem>, TableCell<ReportListItem, ReportListItem>> getMessageCellFactory() {
            return column -> new TableCell<>() {
                private final Label message = new Label();
                private final Button icon = BisqIconButton.createCopyIconButton();
                private final HBox hBox = new HBox(message, icon);

                {
                    icon.setMinWidth(30);
                    HBox.setHgrow(icon, Priority.ALWAYS);
                    HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                    hBox.setAlignment(Pos.CENTER_LEFT);
                }

                @Override
                public void updateItem(final ReportListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        message.setText(item.getMessage());
                        message.setMaxHeight(30);
                        message.setTooltip(new BisqTooltip(item.getMessage(), true));

                        icon.setOnAction(e -> ClipboardUtil.copyToClipboard(item.getMessage()));
                        setGraphic(hBox);
                    } else {
                        icon.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ReportListItem, ReportListItem>, TableCell<ReportListItem, ReportListItem>> getBanCellFactory() {
            return column -> new TableCell<>() {
                private final Button button = new Button(Res.get("authorizedRole.moderator.table.ban"));

                @Override
                public void updateItem(final ReportListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        button.setOnAction(e -> controller.onBan(item.getReportToModeratorMessage()));
                        setGraphic(button);
                    } else {
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ReportListItem, ReportListItem>, TableCell<ReportListItem, ReportListItem>> getDeleteMessageCellFactory() {
            return column -> new TableCell<>() {
                private final Button button = new Button(Res.get("authorizedRole.moderator.table.delete"));

                @Override
                public void updateItem(final ReportListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        button.setOnAction(e -> controller.onDeleteMessage(item.getReportToModeratorMessage()));
                        setGraphic(button);
                    } else {
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // TableItems
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        @Getter
        @EqualsAndHashCode
        private static class ReportListItem implements TableItem {
            private final ReportToModeratorMessage reportToModeratorMessage;
            private final long date;
            private final Optional<UserProfile> reporterUserProfile;
            private final String dateString, message, reporterUserName, accusedUserName, chatChannelDomain;
            private final UserProfile accusedUserProfile;

            private ReportListItem(ReportToModeratorMessage reportToModeratorMessage, ServiceProvider serviceProvider) {
                UserProfileService userProfileService = serviceProvider.getUserService().getUserProfileService();
                this.reportToModeratorMessage = reportToModeratorMessage;

                String reporterUserProfileId = reportToModeratorMessage.getReporterUserProfileId();
                reporterUserProfile = userProfileService.findUserProfile(reporterUserProfileId);
                reporterUserName = reporterUserProfile.map(UserProfile::getUserName).orElse(Res.get("data.na"));

                accusedUserProfile = reportToModeratorMessage.getAccusedUserProfile();
                accusedUserName = accusedUserProfile.getUserName();

                chatChannelDomain = Res.get("chat.channelDomain." + reportToModeratorMessage.getChatChannelDomain().name());
                date = reportToModeratorMessage.getDate();
                dateString = DateFormatter.formatDateTime(date);
                message = reportToModeratorMessage.getMessage();
            }
        }
    }
}