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

package bisq.desktop.primary.main.content.components;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChannelKind;
import bisq.chat.ChatService;
import bisq.chat.channel.Channel;
import bisq.chat.discuss.DiscussionChannelSelectionService;
import bisq.chat.discuss.priv.PrivateDiscussionChannel;
import bisq.chat.discuss.priv.PrivateDiscussionChannelService;
import bisq.chat.discuss.priv.PrivateDiscussionChatMessage;
import bisq.chat.discuss.pub.PublicDiscussionChannel;
import bisq.chat.discuss.pub.PublicDiscussionChannelService;
import bisq.chat.discuss.pub.PublicDiscussionChatMessage;
import bisq.chat.events.EventsChannelSelectionService;
import bisq.chat.events.priv.PrivateEventsChannel;
import bisq.chat.events.priv.PrivateEventsChannelService;
import bisq.chat.events.priv.PrivateEventsChatMessage;
import bisq.chat.events.pub.PublicEventsChannel;
import bisq.chat.events.pub.PublicEventsChannelService;
import bisq.chat.events.pub.PublicEventsChatMessage;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.Quotation;
import bisq.chat.support.SupportChannelSelectionService;
import bisq.chat.support.priv.PrivateSupportChannel;
import bisq.chat.support.priv.PrivateSupportChannelService;
import bisq.chat.support.priv.PrivateSupportChatMessage;
import bisq.chat.support.pub.PublicSupportChannel;
import bisq.chat.support.pub.PublicSupportChannelService;
import bisq.chat.support.pub.PublicSupportChatMessage;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.chat.trade.priv.PrivateTradeChatMessage;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.chat.trade.pub.PublicTradeChatMessage;
import bisq.chat.trade.pub.TradeChatOffer;
import bisq.common.monetary.Coin;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.*;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.table.FilteredListItem;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
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

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static bisq.desktop.primary.main.content.components.ChatMessagesComponent.View.EDITED_POST_FIX;

@Slf4j
public class ChatMessagesListView {
    private final Controller controller;

    public ChatMessagesListView(DefaultApplicationService applicationService,
                                Consumer<UserProfile> mentionUserHandler,
                                Consumer<ChatMessage> showChatUserDetailsHandler,
                                Consumer<ChatMessage> replyHandler,
                                ChannelKind channelKind) {
        controller = new Controller(applicationService,
                mentionUserHandler,
                showChatUserDetailsHandler,
                replyHandler,
                channelKind);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ObservableList<ChatMessageListItem<? extends ChatMessage>> getChatMessages() {
        return controller.model.getChatMessages();
    }

    public FilteredList<ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.model.getFilteredChatMessages();
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.setSearchPredicate(predicate);
    }

    public SortedList<ChatMessageListItem<? extends ChatMessage>> getSortedChatMessages() {
        return controller.model.getSortedChatMessages();
    }

    public void refreshMessages() {
        controller.refreshMessages();
    }

    public void setCreateOfferCompleteHandler(Runnable createOfferCompleteHandler) {
        controller.setCreateOfferCompleteHandler(createOfferCompleteHandler);
    }

    public void setTakeOfferCompleteHandler(Runnable takeOfferCompleteHandler) {
        controller.setTakeOfferCompleteHandler(takeOfferCompleteHandler);
    }

    public void setComparator(Comparator<ReputationScore> comparing) {

    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final ChatService chatService;
        private final PrivateTradeChannelService privateTradeChannelService;
        private final PrivateDiscussionChannelService privateDiscussionChannelService;
        private final PublicDiscussionChannelService publicDiscussionChannelService;
        private final PublicTradeChannelService publicTradeChannelService;
        private final UserIdentityService userIdentityService;
        private final Consumer<UserProfile> mentionUserHandler;
        private final Consumer<ChatMessage> replyHandler;
        private final Consumer<ChatMessage> showChatUserDetailsHandler;
        private final ReputationService reputationService;
        private final UserProfileService userProfileService;
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final DiscussionChannelSelectionService discussionChannelSelectionService;
        private final SettingsService settingsService;
        private final PrivateEventsChannelService privateEventsChannelService;
        private final PublicEventsChannelService publicEventsChannelService;
        private final EventsChannelSelectionService eventsChannelSelectionService;
        private final PrivateSupportChannelService privateSupportChannelService;
        private final PublicSupportChannelService publicSupportChannelService;
        private final SupportChannelSelectionService supportChannelSelectionService;
        private Pin selectedChannelPin, chatMessagesPin;
        private Pin offerOnlySettingsPin;

        private Controller(DefaultApplicationService applicationService,
                           Consumer<UserProfile> mentionUserHandler,
                           Consumer<ChatMessage> showChatUserDetailsHandler,
                           Consumer<ChatMessage> replyHandler,
                           ChannelKind channelKind) {
            chatService = applicationService.getChatService();

            privateTradeChannelService = chatService.getPrivateTradeChannelService();
            publicTradeChannelService = chatService.getPublicTradeChannelService();
            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();

            privateDiscussionChannelService = chatService.getPrivateDiscussionChannelService();
            publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();

            privateEventsChannelService = chatService.getPrivateEventsChannelService();
            publicEventsChannelService = chatService.getPublicEventsChannelService();
            eventsChannelSelectionService = chatService.getEventsChannelSelectionService();

            privateSupportChannelService = chatService.getPrivateSupportChannelService();
            publicSupportChannelService = chatService.getPublicSupportChannelService();
            supportChannelSelectionService = chatService.getSupportChannelSelectionService();

            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();
            reputationService = applicationService.getUserService().getReputationService();
            settingsService = applicationService.getSettingsService();
            this.mentionUserHandler = mentionUserHandler;
            this.showChatUserDetailsHandler = showChatUserDetailsHandler;
            this.replyHandler = replyHandler;

            model = new Model(chatService,
                    userIdentityService,
                    channelKind);
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
            offerOnlySettingsPin = FxBindings.subscribe(settingsService.getOffersOnly(), offerOnly -> applyPredicate());

            model.getSortedChatMessages().setComparator(ChatMessagesListView.ChatMessageListItem::compareTo);

            if (model.getChannelKind() == ChannelKind.TRADE) {
                selectedChannelPin = tradeChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (chatMessagesPin != null) {
                        chatMessagesPin.unbind();
                    }
                    if (channel instanceof PublicTradeChannel) {
                        chatMessagesPin = FxBindings.<PublicTradeChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicTradeChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateTradeChannel) {
                        chatMessagesPin = FxBindings.<PrivateTradeChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateTradeChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    } else if (channel == null) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        model.chatMessages.clear();
                    }
                });
            } else if (model.getChannelKind() == ChannelKind.DISCUSSION) {
                selectedChannelPin = discussionChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicDiscussionChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PublicDiscussionChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicDiscussionChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateDiscussionChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PrivateDiscussionChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateDiscussionChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            } else if (model.getChannelKind() == ChannelKind.EVENTS) {
                selectedChannelPin = eventsChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicEventsChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PublicEventsChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicEventsChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateEventsChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PrivateEventsChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateEventsChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            } else if (model.getChannelKind() == ChannelKind.SUPPORT) {
                selectedChannelPin = supportChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof PublicSupportChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PublicSupportChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PublicSupportChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                    } else if (channel instanceof PrivateSupportChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<PrivateSupportChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateSupportChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                    }
                });
            }
        }

        @Override
        public void onDeactivate() {
            offerOnlySettingsPin.unbind();
            selectedChannelPin.unbind();
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
                chatMessagesPin = null;
            }
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // API - called from client
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void refreshMessages() {
            model.chatMessages.setAll(new ArrayList<>(model.chatMessages));
        }

        public void setOfferOnly(boolean offerOnly) {
            model.offerOnly.set(offerOnly);
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
        // UI - handle internally
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void onTakeOffer(PublicTradeChatMessage chatMessage) {
            if (model.isMyMessage(chatMessage) || chatMessage.getTradeChatOffer().isEmpty()) {
                return;
            }
            userProfileService.findUserProfile(chatMessage.getAuthorId())
                    .ifPresent(userProfile -> {
                        Optional<PrivateTradeChannel> channel = privateTradeChannelService.createAndAddChannel(userProfile);
                        channel.ifPresent(privateTradeChannel -> {
                            tradeChannelSelectionService.selectChannel(privateTradeChannel);
                            TradeChatOffer tradeChatOffer = chatMessage.getTradeChatOffer().get();
                            String text = chatMessage.getText();
                            Optional<Quotation> quotation = Optional.of(new Quotation(userProfile.getNym(),
                                    userProfile.getNickName(),
                                    userProfile.getPubKeyHash(),
                                    text));
                            String direction = Res.get(tradeChatOffer.getDirection().name().toLowerCase()).toUpperCase();
                            String amount = AmountFormatter.formatAmountWithCode(Coin.of(tradeChatOffer.getQuoteSideAmount(),
                                    tradeChatOffer.getMarket().getQuoteCurrencyCode()), true);
                            String methods = Joiner.on(", ").join(tradeChatOffer.getPaymentMethods());
                            String replyText = Res.get("satoshisquareapp.chat.takeOffer.takerRequest",
                                    direction, amount, methods);
                            privateTradeChannelService.sendPrivateChatMessage(replyText,
                                            quotation,
                                            privateTradeChannel)
                                    .thenAccept(result -> UIThread.run(() -> model.takeOfferCompleteHandler.ifPresent(Runnable::run)));
                        });
                    });
        }

        private void onDeleteMessage(ChatMessage chatMessage) {
            if (isMyMessage(chatMessage)) {
                UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
                if (chatMessage instanceof PublicTradeChatMessage) {
                    publicTradeChannelService.deleteChatMessage((PublicTradeChatMessage) chatMessage, userIdentity);
                } else if (chatMessage instanceof PublicDiscussionChatMessage) {
                    publicDiscussionChannelService.deleteChatMessage((PublicDiscussionChatMessage) chatMessage, userIdentity);
                } else if (chatMessage instanceof PublicEventsChatMessage) {
                    publicEventsChannelService.deleteChatMessage((PublicEventsChatMessage) chatMessage, userIdentity);
                } else if (chatMessage instanceof PublicSupportChatMessage) {
                    publicSupportChannelService.deleteChatMessage((PublicSupportChatMessage) chatMessage, userIdentity);
                }
                //todo delete private message
            }
        }

        private void onCreateOffer(PublicTradeChatMessage chatMessage) {
            UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
            publicTradeChannelService.publishChatMessage(chatMessage, userIdentity)
                    .thenAccept(result -> UIThread.run(() -> model.createOfferCompleteHandler.ifPresent(Runnable::run)));
        }

        private void onOpenPrivateChannel(ChatMessage chatMessage) {
            if (isMyMessage(chatMessage)) {
                return;
            }
            userProfileService.findUserProfile(chatMessage.getAuthorId()).ifPresent(this::createAndSelectPrivateChannel);
        }

        private void onSaveEditedMessage(ChatMessage chatMessage, String editedText) {
            if (!isMyMessage(chatMessage)) {
                return;
            }
            if (chatMessage instanceof PublicTradeChatMessage) {
                UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
                publicTradeChannelService.publishEditedChatMessage((PublicTradeChatMessage) chatMessage, editedText, userIdentity);
            } else if (chatMessage instanceof PublicDiscussionChatMessage) {
                UserIdentity userIdentity = userIdentityService.getSelectedUserProfile().get();
                publicDiscussionChannelService.publishEditedChatMessage((PublicDiscussionChatMessage) chatMessage, editedText, userIdentity);
            }
            //todo editing private message not supported yet
        }

        private void onOpenMoreOptions(Node owner, ChatMessage chatMessage, Runnable onClose) {
            if (chatMessage.equals(model.selectedChatMessageForMoreOptionsPopup.get())) {
                return;
            }
            model.selectedChatMessageForMoreOptionsPopup.set(chatMessage);

            List<BisqPopupMenuItem> items = new ArrayList<>();
            items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.copyMessage"),
                    () -> onCopyMessage(chatMessage)));
            if (!isMyMessage(chatMessage)) {
                items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.ignoreUser"),
                        () -> onIgnoreUser(chatMessage)));
                items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.reportUser"),
                        () -> onReportUser(chatMessage)));
            }

            BisqPopupMenu menu = new BisqPopupMenu(items, onClose);
            menu.setAlignment(BisqPopup.Alignment.LEFT);
            menu.show(owner);
        }

        private void onReportUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorId()).ifPresent(author ->
                    chatService.reportUserProfile(author, ""));
        }

        private void onIgnoreUser(ChatMessage chatMessage) {
            userProfileService.findUserProfile(chatMessage.getAuthorId())
                    .ifPresent(userProfileService::ignoreUserProfile);
        }

        private boolean isMyMessage(ChatMessage chatMessage) {
            return userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
        }

        private void createAndSelectPrivateChannel(UserProfile peer) {
            if (model.getChannelKind() == ChannelKind.TRADE) {
                privateTradeChannelService.createAndAddChannel(peer).ifPresent(tradeChannelSelectionService::selectChannel);
            } else if (model.getChannelKind() == ChannelKind.DISCUSSION) {
                privateDiscussionChannelService.createAndAddChannel(peer).ifPresent(discussionChannelSelectionService::selectChannel);
            } else if (model.getChannelKind() == ChannelKind.EVENTS) {
                privateEventsChannelService.createAndAddChannel(peer).ifPresent(eventsChannelSelectionService::selectChannel);
            } else if (model.getChannelKind() == ChannelKind.SUPPORT) {
                privateSupportChannelService.createAndAddChannel(peer).ifPresent(supportChannelSelectionService::selectChannel);
            }
        }

        private void applyPredicate() {
            boolean offerOnly = settingsService.getOffersOnly().get();
            Predicate<ChatMessageListItem<? extends ChatMessage>> predicate = item -> {
                boolean offerOnlyPredicate = true;
                if (item.getChatMessage() instanceof PublicTradeChatMessage) {
                    PublicTradeChatMessage publicTradeChatMessage = (PublicTradeChatMessage) item.getChatMessage();
                    offerOnlyPredicate = !offerOnly || publicTradeChatMessage.isOfferMessage();
                }
                return offerOnlyPredicate &&
                        item.getSenderUserProfile().isPresent() &&
                        !userProfileService.getIgnoredUserProfileIds().contains(item.getSenderUserProfile().get().getId()) &&
                        userProfileService.findUserProfile(item.getSenderUserProfile().get().getId()).isPresent();
            };
            model.filteredChatMessages.setPredicate(item -> model.getSearchPredicate().test(item) && predicate.test(item));
        }

        private void onCopyMessage(ChatMessage chatMessage) {
            ClipboardUtil.copyToClipboard(chatMessage.getText());
        }
    }


    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ChatService chatService;
        private final UserIdentityService userIdentityService;
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final BooleanProperty allowEditing = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> selectedChatMessageForMoreOptionsPopup = new SimpleObjectProperty<>(null);
        private final ChannelKind channelKind;
        @Setter
        private Predicate<? super ChatMessageListItem<? extends ChatMessage>> searchPredicate = e -> true;
        private Optional<Runnable> createOfferCompleteHandler = Optional.empty();
        private Optional<Runnable> takeOfferCompleteHandler = Optional.empty();
        private final BooleanProperty offerOnly = new SimpleBooleanProperty();

        private Model(ChatService chatService,
                      UserIdentityService userIdentityService,
                      ChannelKind channelKind) {
            this.chatService = chatService;
            this.userIdentityService = userIdentityService;
            this.channelKind = channelKind;
        }

        boolean isMyMessage(ChatMessage chatMessage) {
            return userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
        }

        boolean isOfferMessage(ChatMessage chatMessage) {
            return chatMessage instanceof PublicTradeChatMessage &&
                    ((PublicTradeChatMessage) chatMessage).isOfferMessage();
        }
    }


    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final ListView<ChatMessageListItem<? extends ChatMessage>> listView;

        private final ListChangeListener<ChatMessageListItem<? extends ChatMessage>> messagesListener;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            listView = new ListView<>(model.getSortedChatMessages());
            listView.getStyleClass().add("chat-messages-list-view");

            Label placeholder = new Label(Res.get("noData"));
            listView.setPlaceholder(placeholder);
            listView.setCellFactory(getCellFactory());

            // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
            listView.setSelectionModel(new NoSelectionModel<>());

            VBox.setVgrow(listView, Priority.ALWAYS);
            root.getChildren().addAll(listView);

            messagesListener = c -> UIThread.runOnNextRenderFrame(this::scrollDown);
        }

        @Override
        protected void onViewAttached() {
            model.getSortedChatMessages().addListener(messagesListener);
            UIThread.runOnNextRenderFrame(this::scrollDown);
        }

        @Override
        protected void onViewDetached() {
            model.getSortedChatMessages().removeListener(messagesListener);
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
                        private HBox removeOfferButtonHBox;
                        private final Label message, userName, dateTime, replyButton, pmButton, editButton, deleteButton, copyButton, moreOptionsButton;
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
                            takeOfferButton = new Button(Res.get("takeOffer"));
                            takeOfferButton.getStyleClass().add("default-button");

                            removeOfferButton = new Button(Res.get("deleteOffer"));
                            removeOfferButton.getStyleClass().addAll("red-button");

                            // quoted message
                            quotedMessageField = new Text();
                            quotedMessageVBox = new VBox(5);
                            quotedMessageVBox.setId("chat-message-quote-box");
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
                            saveEditButton = new Button(Res.get("save"));
                            saveEditButton.setDefaultButton(true);
                            cancelEditButton = new Button(Res.get("cancel"));

                            editButtonsHBox = Layout.hBoxWith(Spacer.fillHBox(), cancelEditButton, saveEditButton);
                            editButtonsHBox.setVisible(false);
                            editButtonsHBox.setManaged(false);

                            messageBgHBox = new HBox(15);
                            messageBgHBox.setAlignment(Pos.CENTER_LEFT);

                            // Reactions box
                            replyButton = Icons.getIcon(AwesomeIcon.REPLY);
                            replyButton.setCursor(Cursor.HAND);
                            pmButton = Icons.getIcon(AwesomeIcon.COMMENT_ALT);
                            pmButton.setCursor(Cursor.HAND);
                            editButton = Icons.getIcon(AwesomeIcon.EDIT);
                            editButton.setCursor(Cursor.HAND);
                            copyButton = Icons.getIcon(AwesomeIcon.COPY);
                            copyButton.setCursor(Cursor.HAND);
                            deleteButton = Icons.getIcon(AwesomeIcon.REMOVE_SIGN);
                            deleteButton.setCursor(Cursor.HAND);
                            moreOptionsButton = Icons.getIcon(AwesomeIcon.ELLIPSIS_HORIZONTAL);
                            moreOptionsButton.setCursor(Cursor.HAND);
                            reactionsHBox = new HBox(20);

                            reactionsHBox.setPadding(new Insets(0, 15, 5, 15));
                            reactionsHBox.setVisible(false);

                            HBox.setHgrow(messageBgHBox, Priority.SOMETIMES);
                            messageHBox = new HBox();

                            VBox.setMargin(quotedMessageVBox, new Insets(15, 0, 10, 5));
                            VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));
                            VBox.setMargin(editButtonsHBox, new Insets(10, 25, -15, 0));
                            VBox.setMargin(reactionsHBox, new Insets(4, 0, -10, 0));
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


                                boolean isOfferMessage = model.isOfferMessage(chatMessage);
                                boolean myMessage = model.isMyMessage(chatMessage);


                                dateTime.setVisible(false);

                                cellHBox.getChildren().setAll(mainVBox);

                                message.maxWidthProperty().unbind();
                                if (isOfferMessage) {
                                    messageBgHBox.setPadding(new Insets(15));
                                } else {
                                    messageBgHBox.setPadding(new Insets(5, 15, 5, 15));
                                }
                                messageBgHBox.getStyleClass().remove("chat-message-bg-my-message");
                                messageBgHBox.getStyleClass().remove("chat-message-bg-peer-message");
                                VBox userProfileIconVbox = new VBox(userProfileIcon);
                                removeOfferButtonHBox = null;
                                if (myMessage) {
                                    HBox userNameAndDateHBox = new HBox(10, dateTime, userName);
                                    userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
                                    message.setAlignment(Pos.CENTER_RIGHT);

                                    messageBgHBox.getStyleClass().add("chat-message-bg-my-message");
                                    VBox.setMargin(userNameAndDateHBox, new Insets(-5, 30, -5, 0));

                                    HBox.setMargin(copyButton, new Insets(0, 15, 0, 0));
                                    reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyButton, pmButton, editButton, deleteButton, copyButton);

                                    VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);
                                    if (isOfferMessage) {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(160));
                                        userProfileIcon.setSize(60);
                                        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
                                        HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                                        HBox.setMargin(editInputField, new Insets(-4, -10, -15, 0));
                                        HBox.setMargin(messageVBox, new Insets(0, -10, 0, 0));

                                        removeOfferButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                                        HBox.setMargin(reactionsHBox, new Insets(2.5, -10, 0, 0));
                                        removeOfferButtonHBox = new HBox(0, reactionsHBox, removeOfferButton);
                                        reactionsHBox.setAlignment(Pos.CENTER_RIGHT);
                                        removeOfferButtonHBox.setAlignment(Pos.CENTER_RIGHT);
                                        removeOfferButtonHBox.setManaged(false);
                                        removeOfferButtonHBox.setVisible(false);
                                        VBox.setMargin(removeOfferButtonHBox, new Insets(7.5, 25, 0, 0));
                                        mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, removeOfferButtonHBox);
                                    } else {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(140));
                                        userProfileIcon.setSize(30);
                                        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
                                        HBox.setMargin(messageVBox, new Insets(0, -15, 0, 0));
                                        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                                        HBox.setMargin(editInputField, new Insets(6, -10, -25, 0));
                                        mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, reactionsHBox);
                                    }

                                    messageBgHBox.getChildren().setAll(messageVBox, userProfileIconVbox);
                                    messageHBox.getChildren().setAll(Spacer.fillHBox(), messageBgHBox);

                                } else {
                                    // Peer
                                    HBox userNameAndDateHBox = new HBox(10, userName, dateTime);
                                    message.setAlignment(Pos.CENTER_LEFT);
                                    userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);

                                    userProfileIcon.setSize(60);
                                    HBox.setMargin(replyButton, new Insets(0, 0, 0, 15));
                                    reactionsHBox.getChildren().setAll(replyButton, pmButton, editButton, deleteButton, moreOptionsButton, Spacer.fillHBox());

                                    messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
                                    if (isOfferMessage) {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(430));
                                        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);

                                        Label reputationLabel = new Label(Res.get("reputation").toUpperCase());
                                        reputationLabel.getStyleClass().add("bisq-text-7");

                                        reputationScoreDisplay.applyReputationScore(item.getReputationScore());
                                        VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
                                        reputationVBox.setAlignment(Pos.CENTER_LEFT);

                                        takeOfferButton.setOnAction(e -> controller.onTakeOffer((PublicTradeChatMessage) chatMessage));

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
                                    userName.setText(author.getNickName());
                                    userName.setOnMouseClicked(e -> controller.onMention(author));

                                    userProfileIcon.setUserProfile(author);
                                    userProfileIcon.setCursor(Cursor.HAND);
                                    Tooltip.install(userProfileIcon, new BisqTooltip(author.getTooltipString()));
                                    userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(chatMessage));
                                });

                                messageBgHBox.getStyleClass().remove("chat-message-bg-my-message");
                                messageBgHBox.getStyleClass().remove("chat-message-bg-peer-message");

                                if (myMessage) {
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
                                replyButton.setOnMouseClicked(null);
                                pmButton.setOnMouseClicked(null);
                                editButton.setOnMouseClicked(null);
                                copyButton.setOnMouseClicked(null);
                                deleteButton.setOnMouseClicked(null);
                                moreOptionsButton.setOnMouseClicked(null);

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
                                copyButton.setOnMouseClicked(e -> controller.onCopyMessage(chatMessage));
                                if (allowEditing) {
                                    editButton.setOnMouseClicked(e -> onEditMessage(item));
                                    deleteButton.setOnMouseClicked(e -> controller.onDeleteMessage(chatMessage));
                                }
                            } else {
                                moreOptionsButton.setOnMouseClicked(e -> controller.onOpenMoreOptions(pmButton, chatMessage, () -> {
                                    hideReactionsBox();
                                    model.selectedChatMessageForMoreOptionsPopup.set(null);
                                }));
                                replyButton.setOnMouseClicked(e -> controller.onReply(chatMessage));
                                pmButton.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));
                            }
                            replyButton.setVisible(!isMyMessage);
                            replyButton.setManaged(!isMyMessage);
                            pmButton.setVisible(!isMyMessage);
                            pmButton.setManaged(!isMyMessage);
                            editButton.setVisible(isMyMessage && allowEditing);
                            editButton.setManaged(isMyMessage && allowEditing);
                            deleteButton.setVisible(isMyMessage && allowEditing);
                            deleteButton.setManaged(isMyMessage && allowEditing);


                            setupOnMouseEntered();
                            setOnMouseExited(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() == null) {
                                    hideReactionsBox();
                                }
                                dateTime.setVisible(false);
                                reactionsHBox.setVisible(false);
                                if (removeOfferButtonHBox != null) {
                                    // We add a delay calling setupOnMouseEntered to avoid flicker due alternate 
                                    // enter/exit when cell height changes
                                    Transitions.fadeOut(removeOfferButtonHBox, 100, () -> {
                                        removeOfferButtonHBox.setVisible(false);
                                        removeOfferButtonHBox.setManaged(false);
                                        removeOfferButtonHBox.setOpacity(1);
                                        setupOnMouseEntered();
                                    });
                                } else {
                                    setupOnMouseEntered();
                                }
                            });
                        }

                        private void setupOnMouseEntered() {
                            setOnMouseEntered(e -> {
                                if (model.selectedChatMessageForMoreOptionsPopup.get() != null || editInputField.isVisible()) {
                                    return;
                                }
                                reactionsHBox.setVisible(true);
                                dateTime.setVisible(true);

                                if (removeOfferButtonHBox != null) {
                                    removeOfferButtonHBox.setManaged(true);
                                    removeOfferButtonHBox.setVisible(true);
                                }
                                setOnMouseEntered(null);
                            });
                        }

                        private void handleQuoteMessageBox(ChatMessageListItem<? extends ChatMessage> item) {
                            Optional<Quotation> optionalQuotation = item.getQuotedMessage();
                            if (optionalQuotation.isPresent()) {
                                Quotation quotation = optionalQuotation.get();
                                if (quotation.isValid()) {
                                    quotedMessageVBox.setVisible(true);
                                    quotedMessageVBox.setManaged(true);
                                    quotedMessageField.setText(quotation.getMessage());
                                    quotedMessageField.setStyle("-fx-fill: -bisq-grey-dimmed");
                                    Label userName = new Label(quotation.getUserName());
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
        private final Optional<Quotation> quotedMessage;
        private final Optional<UserProfile> senderUserProfile;
        private final String nym;
        private final String nickName;
        @EqualsAndHashCode.Exclude
        private final ReputationScore reputationScore;

        public ChatMessageListItem(T chatMessage, UserProfileService userProfileService, ReputationService reputationService) {
            this.chatMessage = chatMessage;

            if (chatMessage instanceof PrivateTradeChatMessage) {
                senderUserProfile = Optional.of(((PrivateTradeChatMessage) chatMessage).getSender());
            } else if (chatMessage instanceof PrivateDiscussionChatMessage) {
                senderUserProfile = Optional.of(((PrivateDiscussionChatMessage) chatMessage).getSender());
            } else {
                senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorId());
            }
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            quotedMessage = chatMessage.getQuotation();
            date = DateFormatter.formatDateTimeV2(new Date(chatMessage.getDate()));

            nym = senderUserProfile.map(UserProfile::getNym).orElse("");
            nickName = senderUserProfile.map(UserProfile::getNickName).orElse("");

            reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore).orElse(ReputationScore.NONE);
        }

        @Override
        public int compareTo(ChatMessageListItem o) {
            return new Date(chatMessage.getDate()).compareTo(new Date(o.getChatMessage().getDate()));
        }

        @Override
        public boolean match(String filterString) {
            return filterString == null || filterString.isEmpty() || StringUtils.containsIgnoreCase(message, filterString) || StringUtils.containsIgnoreCase(nym, filterString) || StringUtils.containsIgnoreCase(nickName, filterString) || StringUtils.containsIgnoreCase(date, filterString);
        }
    }
}