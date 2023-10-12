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

package bisq.desktop.main.content.bisq_easy.private_chats;

import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.data.Triple;
import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
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
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class BisqEasyPrivateChatsView extends ChatView {
    private final static double HEADER_HEIGHT = 61;

    private BisqTableView<ListItem> tableView;
    private VBox chatVBox;
    private Subscription noOpenChatsPin, tableViewSelectionPin, selectedModelItemPin, peersUserProfilePin;
    private UserProfileDisplay chatPeerUserProfileDisplay;
    private Button leaveChatButton;

    public BisqEasyPrivateChatsView(BisqEasyPrivateChatsModel model,
                                    BisqEasyPrivateChatsController controller,
                                    VBox chatMessagesComponent,
                                    Pane channelSidebar) {
        super(model, controller, chatMessagesComponent, channelSidebar);
    }

    @Override
    protected void configTitleHBox() {
    }

    @Override
    protected void configCenterVBox() {
        addTableBox();
        addChatBox();
    }

    private void addTableBox() {
        tableView = new BisqTableView<>(getModel().getSortedList());
        tableView.getStyleClass().addAll("bisq-easy-open-trades", "hide-horizontal-scrollbar");
        configTableView();

        VBox.setMargin(tableView, new Insets(10, 0, 0, 0));
        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer(Res.get("bisqEasy.privateChats.table.headline"), tableView);
        VBox container = triple.getThird();

        VBox.setMargin(container, new Insets(0, 0, 10, 0));
        centerVBox.getChildren().add(container);
    }

    private void addChatBox() {
        chatMessagesComponent.setMinHeight(200);
        chatMessagesComponent.setPadding(new Insets(0, -30, -15, -30));

        Label peerDescription = new Label(Res.get("bisqEasy.openTrades.chat.peer.description").toUpperCase());
        peerDescription.getStyleClass().add("bisq-easy-open-trades-header-description");
        chatPeerUserProfileDisplay = new UserProfileDisplay(25);
        VBox.setMargin(peerDescription, new Insets(2, 0, 3, 0));
        VBox peerVBox = new VBox(0, peerDescription, chatPeerUserProfileDisplay);
        peerVBox.setAlignment(Pos.CENTER_LEFT);

        leaveChatButton = new Button(Res.get("bisqEasy.privateChats.leave"));
        leaveChatButton.getStyleClass().add("outlined-button");

        HBox chatHeaderHBox = new HBox(10, peerVBox, Spacer.fillHBox(), leaveChatButton);
        chatHeaderHBox.setMinHeight(HEADER_HEIGHT);
        chatHeaderHBox.setMaxHeight(HEADER_HEIGHT);
        chatHeaderHBox.setAlignment(Pos.CENTER_LEFT);
        chatHeaderHBox.setPadding(new Insets(15, 30, 15, 30));
        chatHeaderHBox.getStyleClass().add("bisq-easy-container-header");

        VBox.setMargin(chatMessagesComponent, new Insets(0, 30, 15, 30));
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatVBox = new VBox(chatHeaderHBox, Layout.hLine(), chatMessagesComponent);
        chatVBox.getStyleClass().add("bisq-easy-container");

        VBox.setVgrow(chatVBox, Priority.ALWAYS);
        centerVBox.getChildren().add(chatVBox);
    }

    @Override
    protected void configSideBarVBox() {
        sideBar.getChildren().add(channelSidebar);
        sideBar.getStyleClass().add("bisq-easy-chat-sidebar-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);
    }

    @Override
    protected void configContainerHBox() {
        containerHBox.setSpacing(10);
        containerHBox.setFillHeight(true);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);
        containerHBox.setPadding(new Insets(20, 40, 40, 40));

        Layout.pinToAnchorPane(containerHBox, 30, 0, 0, 0);
        VBox.setVgrow(containerHBox, Priority.ALWAYS);
        root.setContent(containerHBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        BisqEasyPrivateChatsModel model = getModel();

        selectedModelItemPin = EasyBind.subscribe(model.getSelectedItem(), selected ->
                tableView.getSelectionModel().select(selected));

        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                item -> {
                    if (item != null) {
                        getController().onSelectItem(item);
                    }
                });

        noOpenChatsPin = EasyBind.subscribe(model.getNoOpenChats(),
                noOpenTrades -> {
                    if (noOpenTrades) {
                        tableView.removeListeners();
                        tableView.setPlaceholderText(Res.get("bisqEasy.privateChats.noChats"));
                        tableView.allowVerticalScrollbar();
                        tableView.setFixHeight(150);
                        tableView.getStyleClass().add("empty-table");
                    } else {
                        tableView.setPlaceholder(null);
                        tableView.adjustHeightToNumRows();
                        tableView.hideVerticalScrollbar();
                        tableView.getStyleClass().remove("empty-table");
                    }
                    chatVBox.setVisible(!noOpenTrades);
                    chatVBox.setManaged(!noOpenTrades);
                });

        peersUserProfilePin = EasyBind.subscribe(model.getPeersUserProfile(), userProfile -> {
            if (userProfile != null) {
                chatPeerUserProfileDisplay.setUserProfile(userProfile);
                chatPeerUserProfileDisplay.setReputationScore(model.getPeersReputationScore());
            }
        });

        leaveChatButton.setOnAction(e -> getController().onLeaveChat());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        tableView.removeListeners();

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        noOpenChatsPin.unsubscribe();
        peersUserProfilePin.unsubscribe();
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.myUser"))
                .minWidth(100)
                .left()
                .comparator(Comparator.comparing(ListItem::getMyUserName))
                .setCellFactory(getMyUserCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.peer"))
                .minWidth(100)
                .left()
                .comparator(Comparator.comparing(ListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.totalReputationScore"))
                .minWidth(100)
                .comparator(Comparator.comparing(ListItem::getTotalReputationScore))
                .valueSupplier(ListItem::getTotalReputationScoreString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.privateChats.table.age"))
                .minWidth(100)
                .right()
                .comparator(Comparator.comparing(ListItem::getProfileAge))
                .valueSupplier(ListItem::getProfileAgeString)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getTradePeerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    UserProfileDisplay userProfileDisplay = new UserProfileDisplay(item.getChannel().getPeer());
                    userProfileDisplay.setReputationScore(item.getReputationScore());
                    setGraphic(userProfileDisplay);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getMyUserCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    UserProfileDisplay userProfileDisplay = new UserProfileDisplay(item.getChannel().getMyUserIdentity().getUserProfile());
                    setGraphic(userProfileDisplay);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private BisqEasyPrivateChatsModel getModel() {
        return (BisqEasyPrivateChatsModel) model;
    }

    private BisqEasyPrivateChatsController getController() {
        return (BisqEasyPrivateChatsController) controller;
    }

    @Getter
    @EqualsAndHashCode
    static class ListItem implements TableItem {
        private final TwoPartyPrivateChatChannel channel;
        private final String peersUserName, myUserName;
        private final long totalReputationScore, profileAge;
        private final String totalReputationScoreString, profileAgeString;
        private final ReputationScore reputationScore;

        public ListItem(TwoPartyPrivateChatChannel channel, ReputationService reputationService) {
            this.channel = channel;

            UserProfile userProfile = channel.getPeer();
            peersUserName = userProfile.getUserName();
            myUserName = channel.getMyUserIdentity().getUserName();

            reputationScore = reputationService.getReputationScore(userProfile);
            totalReputationScore = reputationScore.getTotalScore();
            totalReputationScoreString = String.valueOf(totalReputationScore);

            Optional<Long> optionalProfileAge = reputationService.getProfileAgeService().getProfileAge(userProfile);
            profileAge = optionalProfileAge.orElse(0L);
            profileAgeString = optionalProfileAge
                    .map(TimeFormatter::formatAgeInDays)
                    .orElse(Res.get("data.na"));
        }
    }
}
