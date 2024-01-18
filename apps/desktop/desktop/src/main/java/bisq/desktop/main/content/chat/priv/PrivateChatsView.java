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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
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
    private VBox openChatsList, chatHeaderVBox;
    private Subscription noOpenChatsPin, tableViewSelectionPin, selectedModelItemPin, peersUserProfilePin,
            myUserProfilePin;
    private UserProfileDisplay chatPeerUserProfileDisplay, chatMyUserProfileDisplay;
    private Button leaveChatButton;

    public PrivateChatsView(PrivateChatsModel model,
                            PrivateChatsController controller,
                            Pane chatMessagesComponent,
                            Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);
    }

    @Override
    protected void configCenterVBox() {
        addOpenChatsList();
        addChatBox();
    }

    private void addOpenChatsList() {
        Label openChatsHeader = new Label(Res.get("chat.private.openChatsList.headline"));
        openChatsHeader.setMinHeight(HEADER_HEIGHT);
        openChatsHeader.setMaxHeight(HEADER_HEIGHT);
        openChatsHeader.setAlignment(Pos.CENTER_LEFT);
        openChatsHeader.setPadding(new Insets(15, 30, 15, 30));
        openChatsHeader.getStyleClass().add("chat-header-title");

        tableView = new BisqTableView<>(getModel().getSortedList());
        tableView.allowVerticalScrollbar();
        configTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        openChatsList = new VBox(openChatsHeader, Layout.hLine(), tableView);
        openChatsList.setPrefWidth(210);
        openChatsList.setMinWidth(210);
        openChatsList.setFillWidth(true);
        openChatsList.getStyleClass().add("chat-container");
    }

    private void addChatBox() {
        chatMessagesComponent.setMinHeight(200);
        chatMessagesComponent.setPadding(new Insets(0, -30, -15, -30));

        chatHeaderVBox = new VBox(0);

        leaveChatButton = new Button(Res.get("bisqEasy.privateChats.leave"));
        leaveChatButton.getStyleClass().add("outlined-button");

        HBox chatHeaderHBox = new HBox(10, chatHeaderVBox, Spacer.fillHBox(), leaveChatButton);
        chatHeaderHBox.setMinHeight(HEADER_HEIGHT);
        chatHeaderHBox.setMaxHeight(HEADER_HEIGHT);
        chatHeaderHBox.setAlignment(Pos.CENTER_LEFT);
        chatHeaderHBox.setPadding(new Insets(15, 30, 15, 30));
        chatHeaderHBox.getStyleClass().add("bisq-easy-container-header");
        chatHeaderHBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);

        VBox.setMargin(chatMessagesComponent, new Insets(0, 30, 15, 30));
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        VBox chatVBox = new VBox(chatHeaderHBox, Layout.hLine(), chatMessagesComponent);
        chatVBox.getStyleClass().add("bisq-easy-container");
        chatVBox.setAlignment(Pos.CENTER);

        VBox.setVgrow(chatVBox, Priority.ALWAYS);
        centerVBox.setAlignment(Pos.CENTER);
        centerVBox.setFillWidth(true);
        centerVBox.getChildren().add(chatVBox);
    }

    @Override
    protected void configContainerHBox() {
        containerHBox.setSpacing(10);
        containerHBox.setFillHeight(true);
        Layout.pinToAnchorPane(containerHBox, 0, 0, 0, 0);

        AnchorPane wrapper = new AnchorPane();
        wrapper.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        wrapper.getChildren().add(containerHBox);

        root.setContent(wrapper);

        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(openChatsList, centerVBox, sideBar);
        containerHBox.setAlignment(Pos.CENTER);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

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
                chatPeerUserProfileDisplay.setUserProfile(userProfile);
                chatPeerUserProfileDisplay.setReputationScore(model.getPeersReputationScore());
            }
        });

        myUserProfilePin = EasyBind.subscribe(model.getMyUserProfile(), userProfile -> {
            if (userProfile != null) {
                chatMyUserProfileDisplay.setUserProfile(userProfile);
                chatMyUserProfileDisplay.setReputationScore(model.getMyUserReputationScore());
            }
        });

        leaveChatButton.setOnAction(e -> getController().onLeaveChat());
        leaveChatButton.visibleProperty().bind(model.getNoOpenChats().not());
        leaveChatButton.managedProperty().bind(model.getNoOpenChats().not());

        tableView.visibleProperty().bind(model.getNoOpenChats().not());
        tableView.managedProperty().bind(model.getNoOpenChats().not());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        tableView.removeListeners();

        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        noOpenChatsPin.unsubscribe();
        peersUserProfilePin.unsubscribe();
        myUserProfilePin.unsubscribe();

        leaveChatButton.setOnAction(null);
        leaveChatButton.visibleProperty().unbind();
        leaveChatButton.managedProperty().unbind();

        tableView.visibleProperty().unbind();
        tableView.managedProperty().unbind();
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
