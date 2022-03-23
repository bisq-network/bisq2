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

package bisq.desktop.primary.main.content.social.chat;

import bisq.common.data.ByteArray;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.social.chat.components.UserProfileComboBox;
import bisq.i18n.Res;
import bisq.social.chat.ChatMessage;
import bisq.social.chat.QuotedMessage;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class ChatView extends View<SplitPane, ChatModel, ChatController> {

    private final ListView<ChatMessageListItem> messagesListView;
    private final BisqTextArea inputField;
    private final BisqLabel selectedChannelLabel;
    private final Button searchButton, notificationsButton, infoButton, closeButton;
    private final ComboBox<UserProfileComboBox.ListItem> userProfileComboBox;
    private final VBox left, sideBar;
    private final FilterBox filterBox;
    private final BisqInputTextField filterBoxRoot;
    private final Pane notificationsSettings;
    private final Pane channelInfo;
    private final ListChangeListener<ChatMessageListItem> messagesListener;
    private final HBox messagesListAndSideBar;
    private Subscription chatUserOverviewRootSubscription;
    private Pane chatUserOverviewRoot;

    public ChatView(ChatModel model, ChatController controller,
                    ComboBox<UserProfileComboBox.ListItem> userProfileComboBox,
                    Pane publicChannelSelection,
                    Pane privateChannelSelection,
                    Pane notificationsSettings,
                    Pane channelInfo,
                    Pane reply) {
        super(new SplitPane(), model, controller);

        this.notificationsSettings = notificationsSettings;
        this.channelInfo = channelInfo;
        this.userProfileComboBox = userProfileComboBox;

        this.root.getStyleClass().add("hide-focus");

        userProfileComboBox.setPadding(new Insets(10, 10, 10, 10));

        left = Layout.vBoxWith(userProfileComboBox, publicChannelSelection, privateChannelSelection);
        left.setMinWidth(150);

        selectedChannelLabel = new BisqLabel();
        selectedChannelLabel.getStyleClass().add("headline-label");
        filterBox = new FilterBox(model.getFilteredChatMessages());
        filterBoxRoot = filterBox.getRoot();
        HBox.setHgrow(filterBoxRoot, Priority.ALWAYS);
        HBox.setMargin(filterBoxRoot, new Insets(0, 0, 0, 10));
        searchButton = BisqIconButton.createIconButton(AwesomeIcon.SEARCH);
        notificationsButton = BisqIconButton.createIconButton(AwesomeIcon.BELL);
        infoButton = BisqIconButton.createIconButton(AwesomeIcon.INFO_SIGN);
        HBox centerToolbar = Layout.hBoxWith(selectedChannelLabel, filterBoxRoot, searchButton, notificationsButton, infoButton);

        messagesListView = new ListView<>();
        messagesListView.setCellFactory(getCellFactory());
        messagesListView.setFocusTraversable(false);
        VBox.setVgrow(messagesListView, Priority.ALWAYS);

        inputField = new BisqTextArea();
        inputField.setPromptText(Res.get("social.chat.input.prompt"));

        VBox messagesAndInput = Layout.vBoxWith(messagesListView, reply, inputField);
        channelInfo.setMinWidth(200);

        closeButton = BisqIconButton.createIconButton(AwesomeIcon.REMOVE_SIGN);

        sideBar = Layout.vBoxWith(closeButton, notificationsSettings, channelInfo);
        sideBar.setAlignment(Pos.TOP_RIGHT);
        messagesListAndSideBar = Layout.hBoxWith(messagesAndInput, sideBar);
        HBox.setHgrow(messagesAndInput, Priority.ALWAYS);
        VBox.setVgrow(messagesListAndSideBar, Priority.ALWAYS);
        VBox center = Layout.vBoxWith(centerToolbar, messagesListAndSideBar);
        center.setPadding(new Insets(10, 10, 10, 10));
        root.setDividerPosition(0, model.getDefaultLeftDividerPosition());
        root.getItems().addAll(left, center);

        messagesListener = c -> messagesListView.scrollTo(model.getFilteredChatMessages().size() - 1);
    }

    @Override
    public void onViewAttached() {
        userProfileComboBox.prefWidthProperty().bind(left.widthProperty());
        selectedChannelLabel.textProperty().bind(model.getSelectedChannelAsString());
        filterBoxRoot.visibleProperty().bind(model.getFilterBoxVisible());
        notificationsSettings.visibleProperty().bind(model.getNotificationsVisible());
        notificationsSettings.managedProperty().bind(model.getNotificationsVisible());
        channelInfo.visibleProperty().bind(model.getChannelInfoVisible());
        channelInfo.managedProperty().bind(model.getChannelInfoVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());

        inputField.textProperty().bindBidirectional(model.getTextInput());

        searchButton.setOnAction(e -> controller.onToggleFilterBox());
        notificationsButton.setOnAction(e -> controller.onToggleNotifications());
        infoButton.setOnAction(e -> controller.onToggleChannelInfo());
        closeButton.setOnAction(e -> controller.onCloseSideBar());

        inputField.autoAdjustHeight(19);
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                if (event.isShiftDown()) {
                    inputField.appendText(System.getProperty("line.separator"));
                } else if (!inputField.getText().isEmpty()) {
                    controller.onSendMessage( StringUtils.trimTrailingLinebreak(inputField.getText()));
                    inputField.clear();
                    inputField.resetAutoAdjustedHeight();
                }
            }
        });

        model.getFilteredChatMessages().addListener(messagesListener);

        messagesListView.setItems(model.getFilteredChatMessages());

        chatUserOverviewRootSubscription = EasyBind.subscribe(model.getChatUserDetailsRoot(),
                pane -> {
                    if (chatUserOverviewRoot != null) {
                        sideBar.getChildren().remove(chatUserOverviewRoot);
                        chatUserOverviewRoot = null;
                    }

                    if (pane != null) {
                        sideBar.getChildren().add(pane);
                        chatUserOverviewRoot = pane;
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        userProfileComboBox.prefWidthProperty().unbind();
        selectedChannelLabel.textProperty().unbind();
        filterBoxRoot.visibleProperty().unbind();
        notificationsSettings.visibleProperty().unbind();
        notificationsSettings.managedProperty().unbind();
        channelInfo.visibleProperty().unbind();
        channelInfo.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();

        inputField.textProperty().unbindBidirectional(model.getTextInput());
        inputField.releaseResources();

        searchButton.setOnAction(null);
        notificationsButton.setOnAction(null);
        infoButton.setOnAction(null);
        inputField.setOnKeyPressed(null);
        closeButton.setOnAction(null);
        model.getFilteredChatMessages().removeListener(messagesListener);
        chatUserOverviewRootSubscription.unsubscribe();
    }

    private Callback<ListView<ChatMessageListItem>, ListCell<ChatMessageListItem>> getCellFactory() {
        return new Callback<>() {

            @Override
            public ListCell<ChatMessageListItem> call(ListView<ChatMessageListItem> list) {
                return new ListCell<>() {
                    private final BisqButton saveEditButton, cancelEditButton;
                    private final BisqTextArea editedMessageField;
                    private final Button emojiButton1, emojiButton2, openEmojiSelectorButton, replyButton,
                            pmButton, editButton, deleteButton, moreOptionsButton;
                    private final BisqLabel userName = new BisqLabel();
                    private final BisqLabel time = new BisqLabel();
                    private final Text message = new Text();
                    private final Text quotedMessageField = new Text();
                    private final ImageView icon = new ImageView();
                    private final HBox hBox, reactionsBox, editControlsBox, quotedMessageBox;
                    private final VBox vBox, messageBox;
                    Tooltip dateTooltip;
                    Subscription widthSubscription;

                    {
                        userName.setId("chat-user-name");
                        userName.setPadding(new Insets(2, 0, -8, 0));
                        time.getStyleClass().add("message-header");
                        time.setPadding(new Insets(-6, 0, 0, 0));
                        time.setVisible(false);
                        icon.setFitWidth(30);
                        icon.setFitHeight(30);
                        message.setId("chat-message-text");
                        VBox.setMargin(message, new Insets(5, 0, 0, 5));
                        //todo emojiButton1, emojiButton2, emojiButton3 will be filled with emoji icons
                        emojiButton1 = BisqIconButton.createIconButton(AwesomeIcon.THUMBS_UP_ALT);
                        emojiButton1.setUserData(":+1:");
                        emojiButton2 = BisqIconButton.createIconButton(AwesomeIcon.THUMBS_DOWN_ALT);
                        emojiButton1.setUserData(":-1:");
                        openEmojiSelectorButton = BisqIconButton.createIconButton(AwesomeIcon.DOUBLE_ANGLE_UP);
                        replyButton = BisqIconButton.createIconButton(AwesomeIcon.REPLY);
                        pmButton = BisqIconButton.createIconButton(AwesomeIcon.COMMENT_ALT);
                        editButton = BisqIconButton.createIconButton(AwesomeIcon.EDIT);
                        deleteButton = BisqIconButton.createIconButton(AwesomeIcon.REMOVE_SIGN);
                        moreOptionsButton = BisqIconButton.createIconButton(AwesomeIcon.ELLIPSIS_HORIZONTAL);
                        Label verticalLine = new Label("|");
                        HBox.setMargin(verticalLine, new Insets(4, 0, 0, 0));
                        verticalLine.setId("chat-message-reactions-separator");
                        reactionsBox = Layout.hBoxWith(
                                Spacer.fillHBox(),
                                emojiButton1,
                                emojiButton2,
                                verticalLine,
                                openEmojiSelectorButton,
                                replyButton,
                                pmButton,
                                editButton,
                                deleteButton,
                                moreOptionsButton);
                        reactionsBox.setSpacing(5);
                        reactionsBox.setVisible(false);
                        reactionsBox.setId("chat-message-reactions");

                        editedMessageField = new BisqTextArea();
                        editedMessageField.setVisible(false);
                        editedMessageField.setManaged(false);
                        editedMessageField.setId("chat-message-edit-text-area");

                        saveEditButton = new BisqButton(Res.get("shared.save"));
                        cancelEditButton = new BisqButton(Res.get("shared.cancel"));

                        editControlsBox = Layout.hBoxWith(Spacer.fillHBox(), cancelEditButton, saveEditButton);
                        editControlsBox.setVisible(false);
                        editControlsBox.setManaged(false);
                        quotedMessageBox = new HBox();
                        quotedMessageBox.setSpacing(10);
                        messageBox = Layout.vBoxWith(quotedMessageBox, message, editedMessageField, editControlsBox, reactionsBox);
                        VBox.setVgrow(messageBox, Priority.ALWAYS);
                        vBox = Layout.vBoxWith(userName, messageBox);
                        HBox.setHgrow(vBox, Priority.ALWAYS);
                        hBox = Layout.hBoxWith(Layout.vBoxWith(icon, time), vBox);
                    }

                    @Override
                    public void updateItem(final ChatMessageListItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            if (item.getQuotedMessage().isPresent()) {
                                QuotedMessage quotedMessage = item.getQuotedMessage().get();
                                if (quotedMessage.userName() != null &&
                                        quotedMessage.pubKeyHash() != null &&
                                        quotedMessage.message() != null) {
                                    Region verticalLine = new Region();
                                    verticalLine.setId("chat-quoted-message-vertical-line");
                                    verticalLine.setMinWidth(3);
                                    verticalLine.setMinHeight(20);
                                    HBox.setMargin(verticalLine, new Insets(5, 0, 0, 5));
                                    quotedMessageField.setText(quotedMessage.message());
                                    quotedMessageField.setId("chat-quoted-message");
                                    BisqLabel userName = new BisqLabel(quotedMessage.userName());
                                    userName.setPadding(new Insets(4, 0, 0, 0));
                                    userName.setId("chat-quoted-message-user-name");
                                    ImageView roboIconImageView = new ImageView();
                                    roboIconImageView.setFitWidth(20);
                                    roboIconImageView.setFitHeight(20);
                                    Image image = RoboHash.getImage(new ByteArray(quotedMessage.pubKeyHash()), false);
                                    roboIconImageView.setImage(image);
                                    HBox.setMargin(roboIconImageView, new Insets(0, 0, 0, -5));
                                    HBox iconAndUserName = Layout.hBoxWith(roboIconImageView, userName);
                                    iconAndUserName.setSpacing(3);
                                    VBox contentBox = Layout.vBoxWith(iconAndUserName, quotedMessageField);
                                    contentBox.setSpacing(0);
                                    quotedMessageBox.getChildren().setAll(verticalLine, contentBox);
                                    UIThread.runLater(() -> verticalLine.setMinHeight(contentBox.getHeight() - 10));
                                }
                            } else {
                                quotedMessageBox.getChildren().clear();
                            }
                            message.setText(item.getMessage());
                            time.setText(item.getTime());

                            saveEditButton.setOnAction(e -> {
                                controller.onSaveEditedMessage(item.getChatMessage(), editedMessageField.getText());
                                onCloseEditMessage();
                            });
                            cancelEditButton.setOnAction(e -> onCloseEditMessage());

                            dateTooltip = new Tooltip(item.getDate());
                            dateTooltip.setShowDelay(Duration.millis(100));
                            Tooltip.install(time, dateTooltip);

                            userName.setText(item.getSenderUserName());
                            userName.setOnMouseClicked(e -> controller.onUserNameClicked(item.getSenderUserName()));

                            icon.setImage(item.getIconImage());
                            icon.setCursor(Cursor.HAND);
                            icon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));

                            hBox.setOnMouseEntered(e -> {
                                time.setVisible(true);
                                reactionsBox.setVisible(true);
                                messageBox.getStyleClass().add("chat-message-box-active");
                            });
                            hBox.setOnMouseExited(e -> {
                                time.setVisible(false);
                                reactionsBox.setVisible(false);
                                messageBox.getStyleClass().remove("chat-message-box-active");
                            });

                            ChatMessage chatMessage = item.getChatMessage();
                            emojiButton1.setOnAction(e -> controller.onAddEmoji((String) emojiButton1.getUserData()));
                            emojiButton2.setOnAction(e -> controller.onAddEmoji((String) emojiButton2.getUserData()));
                            openEmojiSelectorButton.setOnAction(e -> controller.onOpenEmojiSelector(chatMessage));
                            replyButton.setOnAction(e -> controller.onReply(chatMessage));
                            pmButton.setOnAction(e -> controller.onSendPrivateMessage(chatMessage));
                            editButton.setOnAction(e -> onEditMessage(item));
                            deleteButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                            moreOptionsButton.setOnAction(e -> controller.onOpenMoreOptions(chatMessage));

                            boolean isMyMessage = model.isMyMessage(chatMessage);
                            replyButton.setVisible(!isMyMessage);
                            replyButton.setManaged(!isMyMessage);
                            pmButton.setVisible(!isMyMessage);
                            pmButton.setManaged(!isMyMessage);
                            editButton.setVisible(isMyMessage);
                            editButton.setManaged(isMyMessage);
                            deleteButton.setVisible(isMyMessage);
                            deleteButton.setManaged(isMyMessage);

                            widthSubscription = EasyBind.subscribe(messagesListView.widthProperty(),
                                    width -> {
                                        double wrappingWidth = width.doubleValue() - 95;
                                        message.setWrappingWidth(wrappingWidth);
                                        quotedMessageField.setWrappingWidth(wrappingWidth - 20);
                                    });

                            setGraphic(hBox);
                        } else {
                            if (widthSubscription != null) {
                                widthSubscription.unsubscribe();
                            }
                            userName.setOnMouseClicked(null);
                            icon.setOnMouseClicked(null);
                            hBox.setOnMouseEntered(null);
                            hBox.setOnMouseExited(null);
                            icon.setImage(null);

                            emojiButton1.setOnAction(null);
                            emojiButton2.setOnAction(null);
                            openEmojiSelectorButton.setOnAction(null);
                            replyButton.setOnAction(null);
                            pmButton.setOnAction(null);
                            editButton.setOnAction(null);
                            deleteButton.setOnAction(null);
                            moreOptionsButton.setOnAction(null);
                            saveEditButton.setOnAction(null);
                            cancelEditButton.setOnAction(null);

                            editedMessageField.releaseResources();
                            editedMessageField.setOnKeyPressed(null);
                            
                            setGraphic(null);
                        }
                    }

                    private void onEditMessage(ChatMessageListItem item) {
                        editedMessageField.setPrefWidth(message.getWrappingWidth());
                        editedMessageField.setPrefHeight(message.getLayoutBounds().getHeight());
                        editedMessageField.setText(message.getText());
                        editedMessageField.setVisible(true);
                        editedMessageField.setManaged(true);
                        editControlsBox.setVisible(true);
                        editControlsBox.setManaged(true);
                        message.setVisible(false);
                        message.setManaged(false);
                        editedMessageField.autoAdjustHeight(19);
                        editedMessageField.setOnKeyPressed(event -> {
                            if (event.getCode() == KeyCode.ENTER) {
                                event.consume();
                                if (event.isShiftDown()) {
                                    editedMessageField.appendText(System.getProperty("line.separator"));
                                } else if (!editedMessageField.getText().isEmpty()) {
                                    controller.onSaveEditedMessage(item.getChatMessage(), StringUtils.trimTrailingLinebreak(editedMessageField.getText()));
                                    onCloseEditMessage();
                                }
                            }
                        });
                    }

                    private void onCloseEditMessage() {
                        editedMessageField.setVisible(false);
                        editedMessageField.setManaged(false);
                        editControlsBox.setVisible(false);
                        editControlsBox.setManaged(false);
                        message.setVisible(true);
                        message.setManaged(true);
                        editedMessageField.resetAutoAdjustedHeight();
                        editedMessageField.setOnKeyPressed(null);
                    }
                };
            }
        };
    }
}
