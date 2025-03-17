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
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.DateTableItem;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.network.SendMessageResult;
import bisq.presentation.formatters.DateFormatter;
import bisq.support.moderator.ModeratorService;
import bisq.support.moderator.ReportToModeratorMessage;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import de.jensd.fx.fontawesome.AwesomeIcon;
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
            reportListItemsPin = FxBindings.<ReportToModeratorMessage, View.ListItem>bind(model.getListItems())
                    .map(message -> new View.ListItem(message, serviceProvider))
                    .to(moderatorService.getReportToModeratorMessages());
        }

        @Override
        public void onDeactivate() {
            reportListItemsPin.unbind();
        }

        void onContactUser(ReportToModeratorMessage message, UserProfile userProfile, boolean isReportingUser) {
            ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
            Optional<String> citation = isReportingUser ? Optional.of(message.getMessage()) : Optional.empty();
            moderatorService.contactUser(userProfile, citation, isReportingUser)
                    .whenComplete((result, throwable) -> UIThread.run(() -> {
                        if (throwable == null) {
                            SendMessageResult.findAnyErrorMsg(result)
                                    .ifPresent(errorMsg -> new Popup().error(errorMsg).show());
                            navigateToChannel(chatChannelDomain);
                            UIThread.runOnNextRenderFrame(() -> navigateToChannel(chatChannelDomain));
                        } else {
                            new Popup().error(throwable).show();
                        }
                    }));
        }

        void onBan(ReportToModeratorMessage message) {
            moderatorService.banReportedUser(message);
        }

        void onDeleteMessage(ReportToModeratorMessage message) {
            moderatorService.deleteReportToModeratorMessage(message);
        }

        private void navigateToChannel(ChatChannelDomain chatChannelDomain) {
            Navigation.navigateTo(NavigationTarget.CHAT_PRIVATE);
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

            richTableView = new RichTableView<>(model.getListItems(),
                    Res.get("authorizedRole.moderator.reportToModerator.table.headline"));
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


        /* --------------------------------------------------------------------- */
        // ReportTable

        /* --------------------------------------------------------------------- */

        private void configTableView() {
            richTableView.getColumns().add(DateColumnUtil.getDateColumn(richTableView.getSortOrder()));

            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("authorizedRole.moderator.table.reporter"))
                    .minWidth(250)
                    .left()
                    .comparator(Comparator.comparing(ListItem::getReporterUserName))
                    .valueSupplier(ListItem::getReporterUserName)
                    .setCellFactory(getReporterUserProfileCellFactory())
                    .build());
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("authorizedRole.moderator.table.accused"))
                    .minWidth(250)
                    .left()
                    .comparator(Comparator.comparing(ListItem::getAccusedUserName))
                    .valueSupplier(ListItem::getAccusedUserName)
                    .setCellFactory(getAccusedUserProfileCellFactory())
                    .build());
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("authorizedRole.moderator.table.message"))
                    .minWidth(150)
                    .left()
                    .comparator(Comparator.comparing(ListItem::getMessage))
                    .valueSupplier(ListItem::getMessage)
                    .setCellFactory(getMessageCellFactory())
                    .build());
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("authorizedRole.moderator.table.chatChannelDomain"))
                    .fixWidth(150)
                    .left()
                    .comparator(Comparator.comparing(ListItem::getChatChannelDomain))
                    .valueSupplier(ListItem::getChatChannelDomain)
                    .build());
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .isSortable(false)
                    .fixWidth(130)
                    .setCellFactory(getBanCellFactory())
                    .includeForCsv(false)
                    .build());
            richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .isSortable(false)
                    .fixWidth(130)
                    .right()
                    .setCellFactory(getDeleteMessageCellFactory())
                    .includeForCsv(false)
                    .build());
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getReporterUserProfileCellFactory() {
            return column -> new TableCell<>() {
                private final Label userNameLabel = new Label();
                private final UserProfileIcon userProfileIcon = new UserProfileIcon();
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
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty && item.getReporterUserProfile().isPresent()) {
                        String reporterUserName = item.getReporterUserName();
                        userNameLabel.setText(reporterUserName);
                        UserProfile reporterUserProfile = item.getReporterUserProfile().get();
                        userProfileIcon.setUserProfile(reporterUserProfile);

                        button.setText(Res.get("authorizedRole.moderator.table.contact") + " " + StringUtils.truncate(reporterUserName, 8));
                        button.setOnAction(e -> controller.onContactUser(item.getReportToModeratorMessage(), reporterUserProfile, true));
                        setGraphic(hBox);
                    } else {
                        userProfileIcon.dispose();
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getAccusedUserProfileCellFactory() {
            return column -> new TableCell<>() {
                private final Label userNameLabel = new Label();
                private final UserProfileIcon userProfileIcon = new UserProfileIcon();
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
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        String accusedUserName = item.getAccusedUserName();
                        userNameLabel.setText(accusedUserName);
                        UserProfile accusedUserProfile = item.getAccusedUserProfile();
                        userProfileIcon.setUserProfile(accusedUserProfile);

                        button.setText(Res.get("authorizedRole.moderator.table.contact") + " " + StringUtils.truncate(accusedUserName, 8));
                        button.setOnAction(e -> controller.onContactUser(item.getReportToModeratorMessage(), accusedUserProfile, false));

                        setGraphic(hBox);
                    } else {
                        userProfileIcon.dispose();
                        button.setOnAction(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getMessageCellFactory() {
            return column -> new TableCell<>() {
                private final Label message = new Label();
                private final Button icon = BisqIconButton.createIconButton(AwesomeIcon.EXTERNAL_LINK);
                private final HBox hBox = new HBox(message, icon);
                private final BisqTooltip tooltip = new BisqTooltip(BisqTooltip.Style.DARK);

                {
                    icon.setMinWidth(30);
                    HBox.setHgrow(icon, Priority.ALWAYS);
                    HBox.setMargin(icon, new Insets(0, 10, 0, 10));
                    hBox.setAlignment(Pos.CENTER_LEFT);
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item != null && !empty) {
                        message.setText(item.getMessage());
                        message.setMaxHeight(30);
                        tooltip.setText(item.getMessage());
                        message.setTooltip(tooltip);

                       // icon.setOnAction(e -> ClipboardUtil.copyToClipboard(item.getMessage()));
                        icon.setOnAction(e -> new Popup()
                                .headline(Res.get("authorizedRole.moderator.table.message.popup.headline"))
                                .information(item.getMessage())
                                .actionButtonText(Res.get("action.copyToClipboard"))
                                .onAction(()-> ClipboardUtil.copyToClipboard(item.getMessage()))
                                .show());
                        setGraphic(hBox);
                    } else {
                        icon.setOnAction(null);
                        message.setTooltip(null);
                        setGraphic(null);
                    }
                }
            };
        }

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getBanCellFactory() {
            return column -> new TableCell<>() {
                private final Button button = new Button(Res.get("authorizedRole.moderator.table.ban"));

                {
                    button.setDefaultButton(true);
                }

                @Override
                protected void updateItem(ListItem item, boolean empty) {
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

        private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getDeleteMessageCellFactory() {
            return column -> new TableCell<>() {
                private final Button button = new Button(Res.get("authorizedRole.moderator.table.delete"));

                @Override
                protected void updateItem(ListItem item, boolean empty) {
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


        /* --------------------------------------------------------------------- */
        // TableItems

        /* --------------------------------------------------------------------- */

        @Getter
        @EqualsAndHashCode(onlyExplicitlyIncluded = true)
        private static class ListItem implements DateTableItem {
            @EqualsAndHashCode.Include
            private final ReportToModeratorMessage reportToModeratorMessage;

            private final long date;
            private final String dateString, timeString, message, reporterUserName, accusedUserName, chatChannelDomain;
            private final Optional<UserProfile> reporterUserProfile;
            private final UserProfile accusedUserProfile;

            private ListItem(ReportToModeratorMessage reportToModeratorMessage,
                             ServiceProvider serviceProvider) {
                this.reportToModeratorMessage = reportToModeratorMessage;

                UserProfileService userProfileService = serviceProvider.getUserService().getUserProfileService();
                String reporterUserProfileId = reportToModeratorMessage.getReporterUserProfileId();
                reporterUserProfile = userProfileService.findUserProfile(reporterUserProfileId);
                reporterUserName = reporterUserProfile.map(UserProfile::getUserName).orElse(Res.get("data.na"));

                accusedUserProfile = reportToModeratorMessage.getAccusedUserProfile();
                accusedUserName = accusedUserProfile.getUserName();

                chatChannelDomain = Res.get("chat.channelDomain." + reportToModeratorMessage.getChatChannelDomain().name());
                date = reportToModeratorMessage.getDate();
                dateString = DateFormatter.formatDate(date);
                timeString = DateFormatter.formatTime(date);
                message = reportToModeratorMessage.getMessage();
            }
        }
    }
}