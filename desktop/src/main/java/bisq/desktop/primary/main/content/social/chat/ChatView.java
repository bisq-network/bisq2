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

import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.KeyWordDetection;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.social.chat.components.ChatUserIcon;
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
    public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

    private final ListView<ChatMessageListItem<? extends ChatMessage>> messagesListView;
    private final BisqTextArea inputField;
    private final BisqLabel selectedChannelLabel;
    private final Button searchButton, notificationsButton, infoButton, closeButton;
    private final Pane userProfileComboBox;
    private final VBox left, sideBar;
    private final FilterBox filterBox;
    private final BisqInputTextField filterBoxRoot;
    private final Pane notificationsSettings;
    private final Pane channelInfo;
    private final ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messagesListener;
    private final HBox messagesListAndSideBar;
    private final BisqButton createOfferButton;
    private Subscription chatUserOverviewRootSubscription;
    private Pane chatUserOverviewRoot;

    public ChatView(ChatModel model,
                    ChatController controller,
                    Pane userProfileComboBox,
                    Pane publicChannelSelection,
                    Pane privateChannelSelection,
                    Pane notificationsSettings,
                    Pane channelInfo,
                    Pane reply) {
        super(new SplitPane(), model, controller);


        this.notificationsSettings = notificationsSettings;
        this.channelInfo = channelInfo;
        this.userProfileComboBox = userProfileComboBox;

        root.setPadding(new Insets(20, 0, 0, 0));
        root.setStyle("-fx-background-color: -fx-base");
        root.getStyleClass().add("hide-focus");

        // Left 
        userProfileComboBox.setPadding(new Insets(10, 10, 10, 10));

        createOfferButton = new BisqButton(Res.get("satoshisquareapp.chat.createOffer.button"));
        createOfferButton.setActionButton(true);
        VBox.setMargin(createOfferButton, new Insets(0, 20, 20, 20));
        createOfferButton.setPrefWidth(1000);
        left = Layout.vBoxWith(userProfileComboBox,
                publicChannelSelection,
                privateChannelSelection,
                Spacer.fillVBox(),
                createOfferButton);
        left.setMinWidth(250);

        // Center toolbar
        selectedChannelLabel = new BisqLabel();
        selectedChannelLabel.getStyleClass().add("headline-label");

        filterBox = new FilterBox(model.getFilteredChatMessages());
        filterBoxRoot = filterBox.getRoot();
        filterBoxRoot.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");
        HBox.setHgrow(filterBoxRoot, Priority.ALWAYS);
        HBox.setMargin(filterBoxRoot, new Insets(0, 0, 0, 10));

        searchButton = BisqIconButton.createIconButton(AwesomeIcon.SEARCH);
        notificationsButton = BisqIconButton.createIconButton(AwesomeIcon.BELL);
        infoButton = BisqIconButton.createIconButton(AwesomeIcon.INFO_SIGN);
        HBox centerToolbar = Layout.hBoxWith(selectedChannelLabel, filterBoxRoot, searchButton, notificationsButton, infoButton);
        centerToolbar.setStyle("-fx-background-color: -fx-base");
        centerToolbar.setPadding(new Insets(10, 10, 10, 10));

        // messagesAndInput
        messagesListView = new ListView<>();
        messagesListView.setCellFactory(getCellFactory());
        messagesListView.setFocusTraversable(false);
        messagesListView.setStyle("-fx-border-width: 0; -fx-background-color: -fx-base");
        VBox.setVgrow(messagesListView, Priority.ALWAYS);

        inputField = new BisqTextArea();
        inputField.setLabelFloat(true);
        inputField.setPromptText(Res.get("social.chat.input.prompt"));
        inputField.setStyle("-fx-background-color: -fx-base");

        VBox messagesAndInput = Layout.vBoxWith(messagesListView, reply, inputField);
        channelInfo.setMinWidth(200);
        messagesAndInput.setStyle("-fx-background-color: -fx-base");

        // sideBar
        closeButton = BisqIconButton.createIconButton(AwesomeIcon.REMOVE_SIGN);
        VBox.setMargin(closeButton, new Insets(-10, -20, 0, 0));
        sideBar = Layout.vBoxWith(closeButton, notificationsSettings, channelInfo);
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setPadding(new Insets(10, 20, 20, 20));
        sideBar.setFillWidth(true);
        sideBar.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");

        // messagesListAndSideBar
        messagesListAndSideBar = Layout.hBoxWith(messagesAndInput, sideBar);
        HBox.setHgrow(messagesAndInput, Priority.ALWAYS);
        VBox.setVgrow(messagesListAndSideBar, Priority.ALWAYS);
        VBox center = Layout.vBoxWith(centerToolbar, messagesListAndSideBar);
        center.setStyle("-fx-background-color: -fx-base");
        // center.setSpacing(0);
        messagesListAndSideBar.setPadding(new Insets(10, 10, 10, 10));
        messagesListAndSideBar.setStyle("-fx-background-color: -fx-base");

        root.setDividerPosition(0, model.getDefaultLeftDividerPosition());
        root.getItems().addAll(left, center);

        messagesListener = c -> messagesListView.scrollTo(model.getFilteredChatMessages().size() - 1);
    }

    @Override
    protected void onViewAttached() {
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
        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                if (event.isShiftDown()) {
                    inputField.appendText(System.getProperty("line.separator"));
                } else if (!inputField.getText().isEmpty()) {
                    controller.onSendMessage(StringUtils.trimTrailingLinebreak(inputField.getText()));
                    inputField.clear();
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

        searchButton.setOnAction(null);
        notificationsButton.setOnAction(null);
        infoButton.setOnAction(null);
        inputField.setOnKeyPressed(null);
        closeButton.setOnAction(null);
        createOfferButton.setOnAction(null);
        model.getFilteredChatMessages().removeListener(messagesListener);
        chatUserOverviewRootSubscription.unsubscribe();
    }

    private Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> getCellFactory() {
        return new Callback<>() {

            @Override
            public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
                return new ListCell<>() {
                    private final BisqButton saveEditButton, cancelEditButton;
                    private final BisqTextArea editedMessageField;
                    private final Button emojiButton1, emojiButton2, openEmojiSelectorButton, replyButton,
                            pmButton, editButton, deleteButton, moreOptionsButton;
                    private final BisqLabel userName = new BisqLabel();
                    private final BisqLabel time = new BisqLabel();
                    private final BisqTaggableTextArea message = new BisqTaggableTextArea();
                    private final Text quotedMessageField = new Text();
                    private final HBox hBox, reactionsBox, editControlsBox, quotedMessageBox;
                    private final VBox vBox, messageBox;
                    private final ChatUserIcon chatUserIcon = new ChatUserIcon(50);
                    Tooltip dateTooltip;
                    Subscription widthSubscription;

                    {
                        userName.setId("chat-user-name");
                        userName.setPadding(new Insets(2, 0, -8, 0));
                        time.getStyleClass().add("message-header");
                        time.setPadding(new Insets(-6, 0, 0, 0));
                        time.setVisible(false);

                        message.setAutoHeight(true);
                        VBox.setMargin(message, new Insets(0, 0, 0, 5));

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
                        reactionsBox.setStyle("-fx-background-color: -bisq-grey-left-nav-selected-bg; -fx-background-radius: 3px");
                        // reactionsBox.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");

                        editedMessageField = new BisqTextArea();
                        editedMessageField.setVisible(false);
                        editedMessageField.setManaged(false);

                        saveEditButton = new BisqButton(Res.get("shared.save"));
                        cancelEditButton = new BisqButton(Res.get("shared.cancel"));

                        editControlsBox = Layout.hBoxWith(Spacer.fillHBox(), cancelEditButton, saveEditButton);
                        editControlsBox.setVisible(false);
                        editControlsBox.setManaged(false);
                        quotedMessageBox = new HBox();
                        quotedMessageBox.setSpacing(10);
                        VBox.setMargin(quotedMessageBox, new Insets(10,0,5,0));
                        messageBox = Layout.vBoxWith(quotedMessageBox,
                                message, 
                                editedMessageField,
                                editControlsBox, 
                                Layout.hBoxWith(Spacer.fillHBox(),reactionsBox));
                        VBox.setVgrow(messageBox, Priority.ALWAYS);
                        vBox = Layout.vBoxWith(userName, messageBox);
                        HBox.setHgrow(vBox, Priority.ALWAYS);
                        hBox = Layout.hBoxWith(Layout.vBoxWith(chatUserIcon, time), vBox);
                        setStyle("-fx-background-color: -fx-base");
                    }

                    @Override
                    public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            if (item.getQuotedMessage().isPresent()) {
                                QuotedMessage quotedMessage = item.getQuotedMessage().get();
                                if (quotedMessage.userName() != null &&
                                        quotedMessage.pubKeyHash() != null &&
                                        quotedMessage.message() != null) {
                                    Region verticalLine = new Region();
                                    verticalLine.setStyle("-fx-background-color: -bisq-grey-9");
                                    verticalLine.setMinWidth(3);
                                    verticalLine.setMinHeight(25);
                                    HBox.setMargin(verticalLine, new Insets(0, 0, 0, 5));
                                   
                                    quotedMessageField.setText(quotedMessage.message());
                                    quotedMessageField.setStyle("-fx-fill: -bisq-grey-9");
                                   
                                    BisqLabel userName = new BisqLabel(quotedMessage.userName());
                                    userName.setPadding(new Insets(4, 0, 0, 0));
                                    userName.setStyle("-fx-text-fill: -bisq-grey-9");
                                   
                                    ImageView roboIconImageView = new ImageView();
                                    roboIconImageView.setFitWidth(25);
                                    roboIconImageView.setFitHeight(25);
                                    Image image = RoboHash.getImage(quotedMessage.pubKeyHash());
                                    roboIconImageView.setImage(image);
                                   
                                    HBox.setMargin(roboIconImageView, new Insets(0, 0, 0, -5));
                                    HBox iconAndUserName = Layout.hBoxWith(roboIconImageView, userName);
                                    iconAndUserName.setSpacing(5);
                                   
                                    VBox contentBox = Layout.vBoxWith(iconAndUserName, quotedMessageField);
                                    contentBox.setSpacing(5);
                                    quotedMessageBox.getChildren().setAll(verticalLine, contentBox);
                                    UIThread.runOnNextRenderFrame(() -> verticalLine.setMinHeight(contentBox.getHeight() - 10));
                                }
                            } else {
                                quotedMessageBox.getChildren().clear();
                            }

                            message.setText(item.getMessage());
                            message.setStyleSpans(0, KeyWordDetection.getStyleSpans(item.getMessage(),
                                    model.getTradeTags(),
                                    model.getCurrencyTags(),
                                    model.getPaymentMethodsTags(),
                                    model.getCustomTags()));

                            time.setText(item.getTime());

                            saveEditButton.setOnAction(e -> {
                                controller.onSaveEditedMessage(item.getChatMessage(), editedMessageField.getText());
                                onCloseEditMessage();
                            });
                            cancelEditButton.setOnAction(e -> onCloseEditMessage());

                            dateTooltip = new Tooltip(item.getDate());
                            dateTooltip.setShowDelay(Duration.millis(100));
                            Tooltip.install(time, dateTooltip);

                            userName.setText(item.getAuthorUserName());
                            userName.setOnMouseClicked(e -> controller.onUserNameClicked(item.getAuthorUserName()));

                            chatUserIcon.setChatUser(item.getAuthor(), model.getUserProfileService());
                            chatUserIcon.setCursor(Cursor.HAND);
                            chatUserIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
                            setOnMouseEntered(e -> {
                                time.setVisible(true);
                                reactionsBox.setVisible(true);
                                messageBox.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");
                                setStyle("-fx-background-color: -bisq-grey-left-nav-bg;");
                            });
                            setOnMouseExited(e -> {
                                time.setVisible(false);
                                reactionsBox.setVisible(false);
                                messageBox.setStyle("-fx-background-color: -fx-base");
                                setStyle("-fx-background-color: -fx-base");
                            });

                            ChatMessage chatMessage = item.getChatMessage();
                            emojiButton1.setOnAction(e -> controller.onAddEmoji((String) emojiButton1.getUserData()));
                            emojiButton2.setOnAction(e -> controller.onAddEmoji((String) emojiButton2.getUserData()));
                            openEmojiSelectorButton.setOnAction(e -> controller.onOpenEmojiSelector(chatMessage));
                            replyButton.setOnAction(e -> controller.onReply(chatMessage));
                            pmButton.setOnAction(e -> controller.onOpenPrivateChannel(chatMessage));
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
                                        quotedMessageField.setWrappingWidth(wrappingWidth - 20);
                                    });

                            setGraphic(hBox);
                        } else {
                            if (widthSubscription != null) {
                                widthSubscription.unsubscribe();
                            }
                            userName.setOnMouseClicked(null);
                            chatUserIcon.setOnMouseClicked(null);
                            hBox.setOnMouseEntered(null);
                            hBox.setOnMouseExited(null);
                            chatUserIcon.releaseResources();

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
                            editedMessageField.setOnKeyPressed(null);

                            setGraphic(null);
                        }
                    }

                    private void onEditMessage(ChatMessageListItem<? extends ChatMessage> item) {
                        editedMessageField.setVisible(true);
                        editedMessageField.setManaged(true);
                        editedMessageField.setText(message.getText().replace(EDITED_POST_FIX, ""));
                        editedMessageField.setInitialHeight(message.getHeight());
                        editedMessageField.setScrollHideThreshold(200);

                        editControlsBox.setVisible(true);
                        editControlsBox.setManaged(true);
                        message.setVisible(false);
                        message.setManaged(false);
                        editedMessageField.setOnKeyPressed(event -> {
                            if (event.getCode() == KeyCode.ENTER) {
                                event.consume();
                                if (event.isShiftDown()) {
                                    editedMessageField.appendText(System.getProperty("line.separator"));
                                } else if (!editedMessageField.getText().isEmpty()) {
                                    controller.onSaveEditedMessage(item.getChatMessage(),
                                            StringUtils.trimTrailingLinebreak(editedMessageField.getText()));
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
                        editedMessageField.setOnKeyPressed(null);
                    }
                };
            }
        };
    }
}
