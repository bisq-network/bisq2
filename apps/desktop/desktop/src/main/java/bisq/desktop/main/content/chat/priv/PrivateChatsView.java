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

package bisq.desktop.main.content.chat.priv;

import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
import java.util.Optional;

@Slf4j
public abstract class PrivateChatsView extends ChatView<PrivateChatsView, PrivateChatsModel> {
    private BisqTableView<ListItem> tableView;
    private VBox openChatsSelectionList, chatHeaderVBox;
    private Subscription noOpenChatsPin, tableViewSelectionPin, selectedModelItemPin, peersUserProfilePin,
            myUserProfilePin;
    private UserProfileDisplay chatPeerUserProfileDisplay, chatMyUserProfileDisplay;
    private DropdownMenuItem leaveChatButton;

    public PrivateChatsView(PrivateChatsModel model,
                            PrivateChatsController controller,
                            Pane chatMessagesComponent,
                            Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);
    }

    @Override
    protected void configCenterVBox() {
        addOpenChatsSelectionList();
        addChatBox();
    }

    @Override
    protected void configContainerHBox() {
        super.configContainerHBox();

        containerHBox.getChildren().setAll(openChatsSelectionList, centerVBox, sideBar);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        tableView.initialize();

        PrivateChatsModel model = getModel();

        selectedModelItemPin = EasyBind.subscribe(model.getSelectedItem(), selected -> {
            tableView.getSelectionModel().select(selected);
        });

        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(), item -> {
            if (item != null) {
                getController().onSelectItem(item);
            }
        });

        noOpenChatsPin = EasyBind.subscribe(model.getNoOpenChats(), noOpenChats -> {
            createHeaderVBox(!noOpenChats);
            if (noOpenChats) {
                tableView.removeListeners();
            }
        });

        peersUserProfilePin = EasyBind.subscribe(model.getPeersUserProfile(), userProfile -> {
            if (userProfile != null) {
                chatPeerUserProfileDisplay.applyData(userProfile, model.getPeerLastSeenAsString(), model.getPeerLastSeen());
                chatPeerUserProfileDisplay.setReputationScore(model.getPeersReputationScore());
            }
        });

        myUserProfilePin = EasyBind.subscribe(model.getMyUserProfile(), userProfile -> {
            if (userProfile != null) {
                chatMyUserProfileDisplay.applyData(userProfile, model.getMyselfLastSeenAsString(), model.getMyselfLastSeen());
                chatMyUserProfileDisplay.setReputationScore(model.getMyUserReputationScore());
            }
        });

        // Using leaveChatButton.setOnAction leads to duplicate execution of the handler on MacOS 14.5 / M3.
        // Might be a bug in the MacOS specific JavaFX libraries as other devs could not reproduce the issue.
        // Using the BisqMenuItem for the event handler works.
        leaveChatButton.getBisqMenuItem().setOnAction(e -> getController().onLeaveChat());

        headerDropdownMenu.visibleProperty().bind(model.getNoOpenChats().not());
        headerDropdownMenu.managedProperty().bind(model.getNoOpenChats().not());

        tableView.visibleProperty().bind(model.getNoOpenChats().not());
        tableView.managedProperty().bind(model.getNoOpenChats().not());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        tableView.dispose();

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        noOpenChatsPin.unsubscribe();
        peersUserProfilePin.unsubscribe();
        myUserProfilePin.unsubscribe();

        leaveChatButton.setOnAction(null);
        headerDropdownMenu.visibleProperty().unbind();
        headerDropdownMenu.managedProperty().unbind();

        tableView.visibleProperty().unbind();
        tableView.managedProperty().unbind();
    }

    @Override
    protected void configTitleHBox() {
        super.configTitleHBox();

        chatHeaderVBox = new VBox(0);
        HBox.setHgrow(chatHeaderVBox, Priority.ALWAYS);

        leaveChatButton = new DropdownMenuItem("leave-chat-red-lit-10", "leave-chat-red",
                Res.get("bisqEasy.privateChats.leave"));
        leaveChatButton.getStyleClass().add("red-menu-item");

        headerDropdownMenu.clearMenuItems();
        headerDropdownMenu.addMenuItems(helpButton, leaveChatButton);

        titleHBox.getChildren().setAll(chatHeaderVBox, searchBox, headerDropdownMenu);
    }

    private void addOpenChatsSelectionList() {
        Label openChatsHeader = new Label(Res.get("chat.private.openChatsList.headline"));
        openChatsHeader.setMinHeight(HEADER_HEIGHT);
        openChatsHeader.setMaxHeight(HEADER_HEIGHT);
        openChatsHeader.setAlignment(Pos.CENTER_LEFT);
        openChatsHeader.setPadding(new Insets(15, 30, 15, 30));
        openChatsHeader.getStyleClass().add("chat-header-title");

        tableView = new BisqTableView<>(getModel().getSortedList());
        tableView.getStyleClass().add("private-chats-selection-list");
        tableView.allowVerticalScrollbar();
        tableView.hideHorizontalScrollbar();
        configTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        openChatsSelectionList = new VBox(openChatsHeader, Layout.hLine(), tableView);
        openChatsSelectionList.setPrefWidth(210);
        openChatsSelectionList.setMinWidth(210);
        openChatsSelectionList.setFillWidth(true);
        openChatsSelectionList.getStyleClass().add("chat-container");
    }

    private void addChatBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);
        chatMessagesComponent.setMinWidth(200);
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), chatMessagesComponent);
        centerVBox.getStyleClass().add("bisq-easy-container");
        centerVBox.setAlignment(Pos.CENTER);
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .minWidth(100)
                .left()
                .comparator(Comparator.comparing(ListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getTradePeerCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hBox = new HBox(5);

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    UserProfileDisplay userProfileDisplay = new UserProfileDisplay(item.getChannel().getPeer());
                    userProfileDisplay.setReputationScore(item.getReputationScore());
                    getStyleClass().add("user-profile-table-cell");
                    hBox.getChildren().setAll(userProfileDisplay, Spacer.fillHBox(), item.getNumMessagesBadge());

                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    protected PrivateChatsModel getModel() {
        return (PrivateChatsModel) model;
    }

    protected PrivateChatsController getController() {
        return (PrivateChatsController) controller;
    }

    private void createHeaderVBox(boolean hasPeerToDisplay) {
        chatHeaderVBox.getChildren().clear();
        if (hasPeerToDisplay) {
            chatMyUserProfileDisplay = new UserProfileDisplay(25);
            chatPeerUserProfileDisplay = new UserProfileDisplay(25);
            HBox hBox = new HBox(30,
                    createUserProfileVBox(chatMyUserProfileDisplay, "bisqEasy.privateChats.table.myUser"),
                    createUserProfileVBox(chatPeerUserProfileDisplay, "bisqEasy.openTrades.chat.peer.description")
            );
            chatHeaderVBox.getChildren().add(hBox);
            chatHeaderVBox.setAlignment(Pos.CENTER_LEFT);
        } else {
            Label emptyChatBoxHeader = new Label(
                    Res.get("chat.private.messagebox.noChats.title", model.getChatChannelDomain().getDisplayString()));
            ImageView image = ImageUtil.getImageViewById("channels-private-chats");
            image.setScaleX(1.25);
            image.setScaleY(1.25);
            emptyChatBoxHeader.setGraphicTextGap(13);
            emptyChatBoxHeader.setGraphic(image);
            emptyChatBoxHeader.getStyleClass().add("chat-header-title");
            chatHeaderVBox.setPadding(new Insets(15, 0, 15, 0));
            chatHeaderVBox.getChildren().add(emptyChatBoxHeader);
        }
    }

    private VBox createUserProfileVBox(UserProfileDisplay userProfileDisplay, String descriptionKey) {
        Label peerDescription = new Label(Res.get(descriptionKey).toUpperCase());
        peerDescription.getStyleClass().add("bisq-easy-open-trades-header-description");
        VBox.setMargin(peerDescription, new Insets(2, 0, 3, 0));
        VBox vbox = new VBox(0, peerDescription, userProfileDisplay);
        vbox.setAlignment(Pos.CENTER_LEFT);
        return vbox;
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    static class ListItem {
        private final TwoPartyPrivateChatChannel channel;
        private final String peersUserName, myUserName;
        private final long totalReputationScore, profileAge;
        private final String totalReputationScoreString, profileAgeString;
        private final ReputationScore reputationScore;
        @EqualsAndHashCode.Exclude
        private final Badge numMessagesBadge;

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

            numMessagesBadge = new Badge(null, Pos.CENTER_RIGHT);
        }

        public void setNumNotifications(long numNotifications) {
            numMessagesBadge.setText(numNotifications == 0 ? "" : String.valueOf(numNotifications));
        }
    }
}
