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

package bisq.desktop.main.content.components;

import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.message.BisqEasyOfferMessage;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.message.*;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.list_view.NoSelectionModel;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.FilteredListItem;
import bisq.desktop.overlay.bisq_easy.take_offer.TakeOfferController;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.text.DateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static bisq.desktop.main.content.components.ChatMessagesComponent.View.EDITED_POST_FIX;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ChatMessagesListView {
    private final Controller controller;

    public ChatMessagesListView(ServiceProvider serviceProvider,
                                Consumer<UserProfile> mentionUserHandler,
                                Consumer<ChatMessage> showChatUserDetailsHandler,
                                Consumer<ChatMessage> replyHandler,
                                ChatChannelDomain chatChannelDomain) {
        controller = new Controller(serviceProvider,
                mentionUserHandler,
                showChatUserDetailsHandler,
                replyHandler,
                chatChannelDomain);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.setSearchPredicate(predicate);
    }

    public void refreshMessages() {
        controller.refreshMessages();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final ChatService chatService;
        private final UserIdentityService userIdentityService;
        private final UserProfileService userProfileService;
        private final ReputationService reputationService;
        private final SettingsService settingsService;
        private final Consumer<UserProfile> mentionUserHandler;
        private final Consumer<ChatMessage> replyHandler;
        private final Consumer<ChatMessage> showChatUserDetailsHandler;
        private final Model model;
        @Getter
        private final View view;
        private final ChatNotificationService chatNotificationService;
        private final BisqEasyTradeService bisqEasyTradeService;
        private Pin selectedChannelPin, chatMessagesPin, offerOnlySettingsPin;
        private Subscription selectedChannelSubscription, focusSubscription;

        private Controller(ServiceProvider serviceProvider,
                           Consumer<UserProfile> mentionUserHandler,
                           Consumer<ChatMessage> showChatUserDetailsHandler,
                           Consumer<ChatMessage> replyHandler,
                           ChatChannelDomain chatChannelDomain) {
            chatService = serviceProvider.getChatService();
            chatNotificationService = chatService.getChatNotificationService();
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            userProfileService = serviceProvider.getUserService().getUserProfileService();
            reputationService = serviceProvider.getUserService().getReputationService();
            settingsService = serviceProvider.getSettingsService();
            bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
            this.mentionUserHandler = mentionUserHandler;
            this.showChatUserDetailsHandler = showChatUserDetailsHandler;
            this.replyHandler = replyHandler;

            model = new Model(userIdentityService, chatChannelDomain);
            view = new View(model, this);
        }

        public void setCreateOfferCompleteHandler(Runnable createOfferCompleteHandler) {
            model.createOfferCompleteHandler = Optional.of(createOfferCompleteHandler);
        }

        public void setTakeOfferCompleteHandler(Runnable takeOfferCompleteHandler) {
            model.takeOfferCompleteHandler = Optional.of(takeOfferCompleteHandler);
        }

        @Override
        public void onActivate() {
            model.getSortedChatMessages().setComparator(ChatMessagesListView.ChatMessageListItem::compareTo);

            offerOnlySettingsPin = FxBindings.subscribe(settingsService.getOffersOnly(), offerOnly -> UIThread.run(this::applyPredicate));

            selectedChannelPin = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain()).getSelectedChannel().addObserver(channel -> {
                UIThread.run(() -> {
                    model.selectedChannel.set(channel);
                    model.allowEditing.set(channel instanceof PublicChatChannel);

                    if (chatMessagesPin != null) {
                        chatMessagesPin.unbind();
                    }

                    if (channel instanceof BisqEasyPublicChatChannel) {
                        chatMessagesPin = bindChatMessages((BisqEasyPublicChatChannel) channel);
                    } else if (channel instanceof BisqEasyPrivateTradeChatChannel) {
                        chatMessagesPin = bindChatMessages((BisqEasyPrivateTradeChatChannel) channel);
                    } else if (channel instanceof CommonPublicChatChannel) {
                        chatMessagesPin = bindChatMessages((CommonPublicChatChannel) channel);
                    } else if (channel instanceof TwoPartyPrivateChatChannel) {
                        chatMessagesPin = bindChatMessages((TwoPartyPrivateChatChannel) channel);
                    } else if (channel == null) {
                        model.chatMessages.clear();
                    }

                    if (focusSubscription != null) {
                        focusSubscription.unsubscribe();
                    }
                    if (selectedChannelSubscription != null) {
                        selectedChannelSubscription.unsubscribe();
                    }
                    if (channel != null) {
                        // ChatChannelService<?, ?, ?> chatChannelService = chatService.findChatChannelService(channel).orElseThrow();
                        focusSubscription = EasyBind.subscribe(view.getRoot().getScene().getWindow().focusedProperty(),
                                focused -> {
                                    if (focused && model.getSelectedChannel().get() != null) {
                                        chatNotificationService.consumeNotificationId(model.getSelectedChannel().get());
                                    }
                                });

                        selectedChannelSubscription = EasyBind.subscribe(model.selectedChannel,
                                selectedChannel -> {
                                    if (selectedChannel != null) {
                                        chatNotificationService.consumeNotificationId(model.getSelectedChannel().get());
                                    }
                                });
                    }
                });
            });
        }

        @Override
        public void onDeactivate() {
            offerOnlySettingsPin.unbind();
            selectedChannelPin.unbind();
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
                chatMessagesPin = null;
            }
            focusSubscription.unsubscribe();
            selectedChannelSubscription.unsubscribe();
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // API - called from client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void refreshMessages() {
            model.chatMessages.setAll(new ArrayList<>(model.chatMessages));
        }

        void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
            model.setSearchPredicate(Objects.requireNonNullElseGet(predicate, () -> e -> true));
            applyPredicate();
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - delegate to client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void onMention(UserProfile userProfile) {
            mentionUserHandler.accept(userProfile);
        }

        private void onShowChatUserDetails(ChatMessage chatMessage) {
            showChatUserDetailsHandler.accept(chatMessage);
        }

        private void onReply(ChatMessage chatMessage) {
            replyHandler.accept(chatMessage);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI - handler
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void onTakeOffer(BisqEasyPublicChatMessage chatMessage, boolean canTakeOffer) {
            if (!canTakeOffer) {
                new Popup().information(Res.get("chat.message.offer.offerAlreadyTaken.warn")).show();
                return;
            }
            checkArgument(!model.isMyMessage(chatMessage), "tradeChatMessage must not be mine");
            checkArgument(chatMessage.getBisqEasyOffer().isPresent(), "message must contain offer");

            BisqEasyOffer bisqEasyOffer = chatMessage.getBisqEasyOffer().get();
            Navigation.navigateTo(NavigationTarget.TAKE_OFFER, new TakeOfferController.InitData(bisqEasyOffer));
        }

        private void onDeleteMessage(ChatMessage chatMessage) {
            String authorUserProfileId = chatMessage.getAuthorUserProfileId();
            userIdentityService.findUserIdentity(authorUserProfileId)
                    .ifPresent(authorUserIdentity -> {
                        if (authorUserIdentity.equals(userIdentityService.getSelectedUserIdentity())) {
                            doDeleteMessage(chatMessage, authorUserIdentity);
                        } else {
                            new Popup().information(Res.get("chat.message.delete.differentUserProfile.warn"))
                                    .closeButtonText(Res.get("confirmation.no"))
                                    .actionButtonText(Res.get("confirmation.yes"))
                                    .onAction(() -> {
                                        userIdentityService.selectChatUserIdentity(authorUserIdentity);
                                        doDeleteMessage(chatMessage, authorUserIdentity);
                                    })
                                    .show();
                        }
                    });
        }

        private void doDeleteMessage(ChatMessage chatMessage, UserIdentity userIdentity) {
            checkArgument(chatMessage instanceof PublicChatMessage);

            if (chatMessage instanceof BisqEasyPublicChatMessage) {
                BisqEasyPublicChatMessage bisqEasyPublicChatMessage = (BisqEasyPublicChatMessage) chatMessage;
                chatService.getBisqEasyPublicChatChannelService().deleteChatMessage(bisqEasyPublicChatMessage, userIdentity)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.error("We got an error at doDeleteMessage: " + throwable);
                            }
                        });
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                CommonPublicChatChannelService commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain);
                CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
                commonPublicChatChannelService.findChannel(chatMessage)
                        .ifPresent(channel -> commonPublicChatChannelService.deleteChatMessage(commonPublicChatMessage, userIdentity));
            }
        }

        private void onOpenPrivateChannel(ChatMessage chatMessage) {
            checkArgument(!model.isMyMessage(chatMessage));

            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(this::createAndSelectTwoPartyPrivateChatChannel);
        }

        private void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            checkArgument(chatMessage instanceof PublicChatMessage);
            checkArgument(model.isMyMessage(chatMessage));

            UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            if (chatMessage instanceof BisqEasyPublicChatMessage) {
                BisqEasyPublicChatMessage bisqEasyPublicChatMessage = (BisqEasyPublicChatMessage) chatMessage;
                chatService.getBisqEasyPublicChatChannelService().publishEditedChatMessage(bisqEasyPublicChatMessage, editedText, userIdentity);
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
                chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain).publishEditedChatMessage(commonPublicChatMessage, editedText, userIdentity);
            }
        }

        private void onOpenMoreOptions(Node owner, ChatMessage chatMessage, Runnable onClose) {
            if (chatMessage.equals(model.selectedChatMessageForMoreOptionsPopup.get())) {
                return;
            }
            model.selectedChatMessageForMoreOptionsPopup.set(chatMessage);

            List<BisqPopupMenuItem> items = new ArrayList<>();
            items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.copyMessage"),
                    () -> onCopyMessage(chatMessage)));
            if (!model.isMyMessage(chatMessage)) {
                if (chatMessage instanceof PublicChatMessage) {
                    items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.ignoreUser"),
                            () -> onIgnoreUser(chatMessage)));
                }
                items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.reportUser"),
                        () -> onReportUser(chatMessage)));
            }

            BisqPopupMenu menu = new BisqPopupMenu(items, onClose);
            menu.setAlignment(BisqPopup.Alignment.LEFT);
            menu.show(owner);
        }

        private void onReportUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId()).ifPresent(author ->
                    chatService.reportUserProfile(author, ""));
        }

        private void onIgnoreUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(userProfileService::ignoreUserProfile);
        }

        private void onCopyMessage(ChatMessage chatMessage) {
            ClipboardUtil.copyToClipboard(chatMessage.getText());
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Private
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
            chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer);
        }

        private void applyPredicate() {
            boolean offerOnly = settingsService.getOffersOnly().get();
            Predicate<ChatMessageListItem<? extends ChatMessage>> predicate = item -> {
                boolean offerOnlyPredicate = true;
                if (item.getChatMessage() instanceof BisqEasyPublicChatMessage) {
                    BisqEasyPublicChatMessage bisqEasyPublicChatMessage = (BisqEasyPublicChatMessage) item.getChatMessage();
                    offerOnlyPredicate = !offerOnly || bisqEasyPublicChatMessage.hasBisqEasyOffer();
                }
                // We do not display the take offer message as it has no text and is used only for sending the offer 
                // to the peer and signalling the take offer event.
                if (item.getChatMessage().getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                    return false;
                }

                return offerOnlyPredicate &&
                        item.getSenderUserProfile().isPresent() &&
                        !userProfileService.getIgnoredUserProfileIds().contains(item.getSenderUserProfile().get().getId()) &&
                        userProfileService.findUserProfile(item.getSenderUserProfile().get().getId()).isPresent();
            };
            model.filteredChatMessages.setPredicate(item -> model.getSearchPredicate().test(item) && predicate.test(item));
        }

        private <M extends ChatMessage, C extends ChatChannel<M>> Pin bindChatMessages(C channel) {
            return FxBindings.<M, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                    .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService,
                            bisqEasyTradeService, userIdentityService))
                    .to(channel.getChatMessages());
        }

        public String getUserName(String userProfileId) {
            return userProfileService.findUserProfile(userProfileId)
                    .map(UserProfile::getUserName)
                    .orElse(Res.get("data.na"));
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final UserIdentityService userIdentityService;
        private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final BooleanProperty allowEditing = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> selectedChatMessageForMoreOptionsPopup = new SimpleObjectProperty<>(null);
        private final ChatChannelDomain chatChannelDomain;
        @Setter
        private Predicate<? super ChatMessageListItem<? extends ChatMessage>> searchPredicate = e -> true;
        private Optional<Runnable> createOfferCompleteHandler = Optional.empty();
        private Optional<Runnable> takeOfferCompleteHandler = Optional.empty();

        private Model(UserIdentityService userIdentityService,
                      ChatChannelDomain chatChannelDomain) {
            this.userIdentityService = userIdentityService;
            this.chatChannelDomain = chatChannelDomain;
        }

        boolean isMyMessage(ChatMessage chatMessage) {
            return chatMessage.isMyMessage(userIdentityService);
        }

        boolean hasTradeChatOffer(ChatMessage chatMessage) {
            return chatMessage instanceof BisqEasyOfferMessage &&
                    ((BisqEasyOfferMessage) chatMessage).hasBisqEasyOffer();
        }
    }


    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final static String EDITED_POST_FIX = " " + Res.get("chat.message.wasEdited");

        private final ListView<ChatMessageListItem<? extends ChatMessage>> listView;

        private final ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messagesListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            listView = new ListView<>(model.getSortedChatMessages());
            listView.getStyleClass().add("chat-messages-list-view");

            Label placeholder = new Label(Res.get("data.noDataAvailable"));
            listView.setPlaceholder(placeholder);
            listView.setCellFactory(getCellFactory());

            // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
            listView.setSelectionModel(new NoSelectionModel<>());

            VBox.setVgrow(listView, Priority.ALWAYS);
            root.getChildren().add(listView);

            messagesListener = c -> UIThread.runOnNextRenderFrame(this::scrollDown);
        }

        @Override
        protected void onViewAttached() {
            model.getChatMessages().addListener(messagesListener);
            scrollDown();
        }

        @Override
        protected void onViewDetached() {
            model.getChatMessages().removeListener(messagesListener);
        }

        private void scrollDown() {
            listView.scrollTo(listView.getItems().size() - 1);
        }

        public Callback<ListView<ChatMessageListItem<? extends ChatMessage>>, ListCell<ChatMessageListItem<? extends ChatMessage>>> getCellFactory() {
            return new Callback<>() {
                @Override
                public ListCell<ChatMessageListItem<? extends ChatMessage>> call(ListView<ChatMessageListItem<? extends ChatMessage>> list) {
                    return new ListCell<>() {
                        private final ReputationScoreDisplay reputationScoreDisplay;
                        private final Button takeOfferButton, removeOfferButton;
                        private final Label message, userName, dateTime, replyIcon, pmIcon, editIcon, deleteIcon, copyIcon, moreOptionsIcon;
                        private final Text quotedMessageField;
                        private final BisqTextArea editInputField;
                        private final Button saveEditButton, cancelEditButton;
                        private final VBox mainVBox, quotedMessageVBox;
                        private final HBox cellHBox, messageHBox, messageBgHBox, reactionsHBox, editButtonsHBox;
                        private final UserProfileIcon userProfileIcon = new UserProfileIcon(60);

                        {
                            userName = new Label();
                            userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");

                            dateTime = new Label();
                            dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");

                            reputationScoreDisplay = new ReputationScoreDisplay();
                            takeOfferButton = new Button(Res.get("offer.takeOffer"));

                            removeOfferButton = new Button(Res.get("offer.deleteOffer"));
                            removeOfferButton.getStyleClass().addAll("red-small-button", "no-background");

                            // quoted message
                            quotedMessageField = new Text();
                            quotedMessageVBox = new VBox(5);
                            quotedMessageVBox.setVisible(false);
                            quotedMessageVBox.setManaged(false);

                            // HBox for message reputation vBox and action button
                            message = new Label();
                            message.setWrapText(true);
                            message.setPadding(new Insets(10));
                            message.getStyleClass().addAll("text-fill-white", "font-size-13", "font-default");


                            // edit
                            editInputField = new BisqTextArea();
                            //editInputField.getStyleClass().addAll("text-fill-white", "font-size-13", "font-default");
                            editInputField.setId("chat-messages-edit-text-area");
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);

                            // edit buttons
                            saveEditButton = new Button(Res.get("action.save"));
                            saveEditButton.setDefaultButton(true);
                            cancelEditButton = new Button(Res.get("action.cancel"));

                            editButtonsHBox = new HBox(15, Spacer.fillHBox(), cancelEditButton, saveEditButton);
                            editButtonsHBox.setVisible(false);
                            editButtonsHBox.setManaged(false);

                            messageBgHBox = new HBox(15);
                            messageBgHBox.setAlignment(Pos.CENTER_LEFT);

                            // Reactions box
                            replyIcon = Icons.getIcon(AwesomeIcon.REPLY);
                            replyIcon.setCursor(Cursor.HAND);
                            pmIcon = Icons.getIcon(AwesomeIcon.COMMENT_ALT);
                            pmIcon.setCursor(Cursor.HAND);
                            editIcon = Icons.getIcon(AwesomeIcon.EDIT);
                            editIcon.setCursor(Cursor.HAND);
                            copyIcon = Icons.getIcon(AwesomeIcon.COPY);
                            copyIcon.setCursor(Cursor.HAND);
                            deleteIcon = Icons.getIcon(AwesomeIcon.REMOVE_SIGN);
                            deleteIcon.setCursor(Cursor.HAND);
                            moreOptionsIcon = Icons.getIcon(AwesomeIcon.ELLIPSIS_HORIZONTAL);
                            moreOptionsIcon.setCursor(Cursor.HAND);
                            reactionsHBox = new HBox(20);

                            // reactionsHBox.setPadding(new Insets(0, 15, 0, 15));
                            reactionsHBox.setVisible(false);

                            HBox.setHgrow(messageBgHBox, Priority.SOMETIMES);
                            messageHBox = new HBox();

                            VBox.setMargin(quotedMessageVBox, new Insets(15, 0, 10, 5));
                            VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));
                            VBox.setMargin(editButtonsHBox, new Insets(10, 25, -15, 0));
                            VBox.setMargin(reactionsHBox, new Insets(4, 15, -3, 15));
                            mainVBox = new VBox();
                            mainVBox.setFillWidth(true);
                            HBox.setHgrow(mainVBox, Priority.ALWAYS);
                            cellHBox = new HBox(15);
                            cellHBox.setPadding(new Insets(0, 25, 0, 0));
                        }


                        private void hideReactionsBox() {
                            reactionsHBox.setVisible(false);
                        }

                        @Override
                        public void updateItem(final ChatMessageListItem<? extends ChatMessage> item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item != null && !empty) {

                                ChatMessage chatMessage = item.getChatMessage();

                                Node flow = this.getListView().lookup(".virtual-flow");
                                if (flow != null && !flow.isVisible())
                                    return;

                                boolean hasTradeChatOffer = model.hasTradeChatOffer(chatMessage);
                                boolean isBisqEasyPublicChatMessageWithOffer = chatMessage instanceof BisqEasyPublicChatMessage && hasTradeChatOffer;
                                boolean isMyMessage = model.isMyMessage(chatMessage);

                                dateTime.setVisible(false);

                                cellHBox.getChildren().setAll(mainVBox);

                                message.maxWidthProperty().unbind();
                                if (hasTradeChatOffer) {
                                    messageBgHBox.setPadding(new Insets(15));
                                } else {
                                    messageBgHBox.setPadding(new Insets(5, 15, 5, 15));
                                }
                                messageBgHBox.getStyleClass().remove("chat-message-bg-my-message");
                                messageBgHBox.getStyleClass().remove("chat-message-bg-peer-message");
                                VBox userProfileIconVbox = new VBox(userProfileIcon);
                                if (isMyMessage) {
                                    HBox userNameAndDateHBox = new HBox(10, dateTime, userName);
                                    userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
                                    message.setAlignment(Pos.CENTER_RIGHT);

                                    quotedMessageVBox.setId("chat-message-quote-box-my-msg");

                                    messageBgHBox.getStyleClass().add("chat-message-bg-my-message");
                                    VBox.setMargin(userNameAndDateHBox, new Insets(-5, 30, -5, 0));

                                    HBox.setMargin(copyIcon, new Insets(0, 15, 0, 0));

                                    VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);
                                    if (isBisqEasyPublicChatMessageWithOffer) {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(160));
                                        userProfileIcon.setSize(60);
                                        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
                                        HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                                        HBox.setMargin(editInputField, new Insets(-4, -10, -15, 0));
                                        HBox.setMargin(messageVBox, new Insets(0, -10, 0, 0));

                                        removeOfferButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                                        HBox.setMargin(removeOfferButton, new Insets(0, 11, 0, -15));
                                        reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, copyIcon, removeOfferButton);
                                        reactionsHBox.setAlignment(Pos.CENTER_RIGHT);
                                        // HBox.setMargin(reactionsHBox, new Insets(2.5, -10, 0, 0));
                                    } else {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(140));
                                        userProfileIcon.setSize(30);
                                        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
                                        HBox.setMargin(deleteIcon, new Insets(0, 11, 0, -15));
                                        reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, copyIcon, deleteIcon);
                                        HBox.setMargin(messageVBox, new Insets(0, -15, 0, 0));
                                        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                                        HBox.setMargin(editInputField, new Insets(6, -10, -25, 0));
                                    }
                                    mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, reactionsHBox);

                                    messageBgHBox.getChildren().setAll(messageVBox, userProfileIconVbox);

                                    messageHBox.getChildren().setAll(Spacer.fillHBox(), messageBgHBox);

                                } else {
                                    // Peer
                                    HBox userNameAndDateHBox = new HBox(10, userName, dateTime);
                                    message.setAlignment(Pos.CENTER_LEFT);
                                    userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);

                                    userProfileIcon.setSize(60);
                                    HBox.setMargin(replyIcon, new Insets(0, 0, 0, 15));
                                    reactionsHBox.getChildren().setAll(replyIcon, pmIcon, editIcon, deleteIcon, moreOptionsIcon, Spacer.fillHBox());

                                    quotedMessageVBox.setId("chat-message-quote-box-peer-msg");

                                    messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
                                    if (isBisqEasyPublicChatMessageWithOffer) {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(430));
                                        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);

                                        Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
                                        reputationLabel.getStyleClass().add("bisq-text-7");

                                        reputationScoreDisplay.applyReputationScore(item.getReputationScore());
                                        VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
                                        reputationVBox.setAlignment(Pos.CENTER_LEFT);

                                        BisqEasyPublicChatMessage bisqEasyPublicChatMessage = (BisqEasyPublicChatMessage) chatMessage;
                                        takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyPublicChatMessage, item.isCanTakeOffer()));
                                        takeOfferButton.setDefaultButton(item.isCanTakeOffer());

                                        VBox messageVBox = new VBox(quotedMessageVBox, message);
                                        HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                                        HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                                        HBox.setMargin(reputationVBox, new Insets(-5, 10, 0, 0));
                                        HBox.setMargin(takeOfferButton, new Insets(0, 10, 0, 0));
                                        messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox, Spacer.fillHBox(), reputationVBox, takeOfferButton);

                                        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 30));
                                        mainVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, reactionsHBox);
                                    } else {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(140));//165
                                        userProfileIcon.setSize(30);
                                        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);

                                        VBox messageVBox = new VBox(quotedMessageVBox, message);
                                        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                                        HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                                        messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox);
                                        messageHBox.getChildren().setAll(messageBgHBox, Spacer.fillHBox());

                                        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 30));
                                        mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, reactionsHBox);
                                    }
                                }

                                handleQuoteMessageBox(item);
                                handleReactionsBox(item);
                                handleEditBox(chatMessage);

                                message.setText(item.getMessage());
                                dateTime.setText(item.getDate());

                                item.getSenderUserProfile().ifPresent(author -> {
                                    userName.setText(author.getUserName());
                                    userName.setOnMouseClicked(e -> controller.onMention(author));

                                    userProfileIcon.setUserProfile(author);
                                    userProfileIcon.setCursor(Cursor.HAND);
                                    Tooltip.install(userProfileIcon, new BisqTooltip(author.getTooltipString()));
                                    userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(chatMessage));
                                });

                                messageBgHBox.getStyleClass().remove("chat-message-bg-my-message");
                                messageBgHBox.getStyleClass().remove("chat-message-bg-peer-message");

                                if (isMyMessage) {
                                    messageBgHBox.getStyleClass().add("chat-message-bg-my-message");
                                } else {
                                    messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
                                }


                                editInputField.maxWidthProperty().bind(message.widthProperty());
                                setGraphic(cellHBox);
                            } else {
                                message.maxWidthProperty().unbind();
                                editInputField.maxWidthProperty().unbind();

                                editInputField.maxWidthProperty().unbind();
                                removeOfferButton.setOnAction(null);
                                takeOfferButton.setOnAction(null);

                                saveEditButton.setOnAction(null);
                                cancelEditButton.setOnAction(null);

                                userName.setOnMouseClicked(null);
                                userProfileIcon.setOnMouseClicked(null);
                                replyIcon.setOnMouseClicked(null);
                                pmIcon.setOnMouseClicked(null);
                                editIcon.setOnMouseClicked(null);
                                copyIcon.setOnMouseClicked(null);
                                deleteIcon.setOnMouseClicked(null);
                                moreOptionsIcon.setOnMouseClicked(null);

                                editInputField.setOnKeyPressed(null);

                                cellHBox.setOnMouseEntered(null);
                                cellHBox.setOnMouseExited(null);

                                userProfileIcon.releaseResources();

                                setGraphic(null);
                            }
                        }

                        private void handleEditBox(ChatMessage chatMessage) {
                            saveEditButton.setOnAction(e -> {
                                controller.onSaveEditedMessage(chatMessage, editInputField.getText());
                                onCloseEditMessage();
                            });
                            cancelEditButton.setOnAction(e -> onCloseEditMessage());
                        }

                        private void handleReactionsBox(ChatMessageListItem<? extends ChatMessage> item) {
                            ChatMessage chatMessage = item.getChatMessage();
                            boolean isMyMessage = model.isMyMessage(chatMessage);
                            boolean allowEditing = model.allowEditing.get();
                            if (isMyMessage) {
                                copyIcon.setOnMouseClicked(e -> controller.onCopyMessage(chatMessage));
                                if (allowEditing) {
                                    editIcon.setOnMouseClicked(e -> onEditMessage(item));
                                    deleteIcon.setOnMouseClicked(e -> controller.onDeleteMessage(chatMessage));
                                }
                            } else {
                                moreOptionsIcon.setOnMouseClicked(e -> controller.onOpenMoreOptions(pmIcon, chatMessage, () -> {
                                    hideReactionsBox();
                                    model.selectedChatMessageForMoreOptionsPopup.set(null);
                                }));
                                replyIcon.setOnMouseClicked(e -> controller.onReply(chatMessage));
                                pmIcon.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));
                            }

                            replyIcon.setVisible(!isMyMessage);
                            replyIcon.setManaged(!isMyMessage);

                            pmIcon.setVisible(!isMyMessage && chatMessage instanceof PublicChatMessage);
                            pmIcon.setManaged(!isMyMessage && chatMessage instanceof PublicChatMessage);

                            editIcon.setVisible(isMyMessage && allowEditing);
                            editIcon.setManaged(isMyMessage && allowEditing);
                            deleteIcon.setVisible(isMyMessage && allowEditing);
                            deleteIcon.setManaged(isMyMessage && allowEditing);
                            removeOfferButton.setVisible(isMyMessage && allowEditing);
                            removeOfferButton.setManaged(isMyMessage && allowEditing);

                            setOnMouseEntered(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() != null || editInputField.isVisible()) {
                                    return;
                                }
                                dateTime.setVisible(true);
                                reactionsHBox.setVisible(true);
                            });

                            setOnMouseExited(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() == null) {
                                    hideReactionsBox();
                                    dateTime.setVisible(false);
                                    reactionsHBox.setVisible(false);
                                }
                            });
                        }

                        private void handleQuoteMessageBox(ChatMessageListItem<? extends ChatMessage> item) {
                            Optional<Citation> optionalCitation = item.getCitation();
                            if (optionalCitation.isPresent()) {
                                Citation citation = optionalCitation.get();
                                if (citation.isValid()) {
                                    quotedMessageVBox.setVisible(true);
                                    quotedMessageVBox.setManaged(true);
                                    quotedMessageField.setText(citation.getText());
                                    quotedMessageField.setStyle("-fx-fill: -fx-mid-text-color");
                                    Label userName = new Label(controller.getUserName(citation.getAuthorUserProfileId()));
                                    userName.getStyleClass().add("font-medium");
                                    userName.setStyle("-fx-text-fill: -bisq-grey-10");
                                    quotedMessageVBox.getChildren().setAll(userName, quotedMessageField);
                                }
                            } else {
                                quotedMessageVBox.getChildren().clear();
                                quotedMessageVBox.setVisible(false);
                                quotedMessageVBox.setManaged(false);
                            }
                        }

                        private void onEditMessage(ChatMessageListItem<? extends ChatMessage> item) {
                            reactionsHBox.setVisible(false);
                            editInputField.setVisible(true);
                            editInputField.setManaged(true);
                            editInputField.setInitialHeight(message.getBoundsInLocal().getHeight());
                            editInputField.setText(message.getText().replace(EDITED_POST_FIX, ""));
                            // editInputField.setScrollHideThreshold(200);
                            editInputField.requestFocus();
                            editInputField.positionCaret(message.getText().length());
                            editButtonsHBox.setVisible(true);
                            editButtonsHBox.setManaged(true);
                            removeOfferButton.setVisible(false);
                            removeOfferButton.setManaged(false);

                            message.setVisible(false);
                            message.setManaged(false);

                            editInputField.setOnKeyPressed(event -> {
                                if (event.getCode() == KeyCode.ENTER) {
                                    event.consume();
                                    if (event.isShiftDown()) {
                                        editInputField.appendText(System.getProperty("line.separator"));
                                    } else if (!editInputField.getText().isEmpty()) {
                                        controller.onSaveEditedMessage(item.getChatMessage(), editInputField.getText().trim());
                                        onCloseEditMessage();
                                    }
                                }
                            });
                        }

                        private void onCloseEditMessage() {
                            editInputField.setVisible(false);
                            editInputField.setManaged(false);
                            editButtonsHBox.setVisible(false);
                            editButtonsHBox.setManaged(false);
                            removeOfferButton.setVisible(true);
                            removeOfferButton.setManaged(true);

                            message.setVisible(true);
                            message.setManaged(true);
                            editInputField.setOnKeyPressed(null);
                        }
                    };
                }
            };
        }
    }

    @Slf4j
    @Getter
    @EqualsAndHashCode
    public static class ChatMessageListItem<T extends ChatMessage> implements Comparable<ChatMessageListItem<T>>, FilteredListItem {
        private final T chatMessage;
        private final String message;
        private final String date;
        private final Optional<Citation> citation;
        private final Optional<UserProfile> senderUserProfile;
        private final String nym;
        private final String nickName;
        @EqualsAndHashCode.Exclude
        private final ReputationScore reputationScore;
        private final boolean canTakeOffer;

        public ChatMessageListItem(T chatMessage,
                                   UserProfileService userProfileService,
                                   ReputationService reputationService,
                                   BisqEasyTradeService bisqEasyTradeService,
                                   UserIdentityService userIdentityService) {
            this.chatMessage = chatMessage;

            if (chatMessage instanceof PrivateChatMessage) {
                senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSender());
            } else {
                senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId());
            }
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            citation = chatMessage.getCitation();
            date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()), DateFormat.MEDIUM, DateFormat.SHORT, true, " " + Res.get("temporal.at") + " ");

            nym = senderUserProfile.map(UserProfile::getNym).orElse("");
            nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");

            reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore).orElse(ReputationScore.NONE);

            if (chatMessage instanceof BisqEasyPublicChatMessage) {
                BisqEasyPublicChatMessage bisqEasyPublicChatMessage = (BisqEasyPublicChatMessage) chatMessage;
                if (userIdentityService.getSelectedUserIdentity() != null && bisqEasyPublicChatMessage.getBisqEasyOffer().isPresent()) {
                    UserProfile userProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
                    NetworkId takerNetworkId = userProfile.getNetworkId();
                    BisqEasyOffer bisqEasyOffer = bisqEasyPublicChatMessage.getBisqEasyOffer().get();
                    String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
                    canTakeOffer = bisqEasyTradeService.findTrade(tradeId).isEmpty();
                } else {
                    canTakeOffer = false;
                }
            } else {
                canTakeOffer = false;
            }
        }

        @Override
        public int compareTo(ChatMessageListItem o) {
            return Comparator.comparingLong(ChatMessage::getDate).compare(this.getChatMessage(), o.getChatMessage());
        }

        @Override
        public boolean match(String filterString) {
            return filterString == null || filterString.isEmpty() || StringUtils.containsIgnoreCase(message, filterString) || StringUtils.containsIgnoreCase(nym, filterString) || StringUtils.containsIgnoreCase(nickName, filterString) || StringUtils.containsIgnoreCase(date, filterString);
        }
    }
}