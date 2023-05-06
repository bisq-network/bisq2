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
import bisq.chat.ChatService;
import bisq.chat.channel.ChannelSelectionService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelService;
import bisq.chat.channel.priv.PrivateTwoPartyChatChannel;
import bisq.chat.channel.priv.PrivateTwoPartyChatChannelService;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.chat.message.*;
import bisq.chat.trade.channel.TradeChannelSelectionService;
import bisq.chat.trade.channel.priv.PrivateTradeChannelService;
import bisq.chat.trade.channel.priv.PrivateTradeChatChannel;
import bisq.chat.trade.channel.pub.PublicTradeChannel;
import bisq.chat.trade.channel.pub.PublicTradeChannelService;
import bisq.chat.trade.message.PrivateTradeChatMessage;
import bisq.chat.trade.message.PublicTradeChatMessage;
import bisq.chat.trade.message.TradeChatOfferMessage;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.utils.NoSelectionModel;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.FilteredListItem;
import bisq.desktop.helpers.TakeOfferHelper;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
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

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static bisq.desktop.primary.main.content.components.ChatMessagesComponent.View.EDITED_POST_FIX;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class ChatMessagesListView {
    private final Controller controller;

    public ChatMessagesListView(DefaultApplicationService applicationService,
                                Consumer<UserProfile> mentionUserHandler,
                                Consumer<ChatMessage> showChatUserDetailsHandler,
                                Consumer<ChatMessage> replyHandler,
                                ChatChannelDomain chatChannelDomain) {
        controller = new Controller(applicationService,
                mentionUserHandler,
                showChatUserDetailsHandler,
                replyHandler,
                chatChannelDomain);
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
        private final PrivateTwoPartyChatChannelService privateDiscussionChannelService;
        private final CommonPublicChatChannelService publicDiscussionChannelService;
        private final PublicTradeChannelService publicTradeChannelService;
        private final UserIdentityService userIdentityService;
        private final Consumer<UserProfile> mentionUserHandler;
        private final Consumer<ChatMessage> replyHandler;
        private final Consumer<ChatMessage> showChatUserDetailsHandler;
        private final ReputationService reputationService;
        private final UserProfileService userProfileService;
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final ChannelSelectionService discussionChannelSelectionService;
        private final SettingsService settingsService;
        private final PrivateTwoPartyChatChannelService privateEventsChannelService;
        private final CommonPublicChatChannelService publicEventsChannelService;
        private final ChannelSelectionService eventsChannelSelectionService;
        private final PrivateTwoPartyChatChannelService privateSupportChannelService;
        private final CommonPublicChatChannelService publicSupportChannelService;
        private final ChannelSelectionService supportChannelSelectionService;
        private final MediationService mediationService;
        private Pin selectedChannelPin;
        private Pin chatMessagesPin;
        private Pin offerOnlySettingsPin;
        @Nullable
        private ChatChannelService<?, ?, ?> currentChatChannelService;
        private Subscription selectedChannelSubscription;
        private Subscription focusSubscription;

        private Controller(DefaultApplicationService applicationService,
                           Consumer<UserProfile> mentionUserHandler,
                           Consumer<ChatMessage> showChatUserDetailsHandler,
                           Consumer<ChatMessage> replyHandler,
                           ChatChannelDomain chatChannelDomain) {
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
            mediationService = applicationService.getSupportService().getMediationService();
            settingsService = applicationService.getSettingsService();
            this.mentionUserHandler = mentionUserHandler;
            this.showChatUserDetailsHandler = showChatUserDetailsHandler;
            this.replyHandler = replyHandler;

            model = new Model(chatService,
                    userIdentityService,
                    chatChannelDomain);
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

            if (model.getChatChannelDomain() == ChatChannelDomain.TRADE) {
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
                        currentChatChannelService = publicTradeChannelService;
                    } else if (channel instanceof PrivateTradeChatChannel) {
                        chatMessagesPin = FxBindings.<PrivateTradeChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateTradeChatChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                        currentChatChannelService = privateTradeChannelService;
                    } else if (channel == null) {
                        model.chatMessages.clear();
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        currentChatChannelService = null;
                    }
                });
            } else if (model.getChatChannelDomain() == ChatChannelDomain.DISCUSSION) {
                selectedChannelPin = discussionChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof CommonPublicChatChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<CommonPublicChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((CommonPublicChatChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                        currentChatChannelService = publicDiscussionChannelService;
                    } else if (channel instanceof PrivateTwoPartyChatChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<TwoPartyPrivateChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateTwoPartyChatChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                        currentChatChannelService = privateDiscussionChannelService;
                    }
                });
            } else if (model.getChatChannelDomain() == ChatChannelDomain.EVENTS) {
                selectedChannelPin = eventsChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof CommonPublicChatChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<CommonPublicChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((CommonPublicChatChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                        currentChatChannelService = publicEventsChannelService;
                    } else if (channel instanceof PrivateTwoPartyChatChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<TwoPartyPrivateChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateTwoPartyChatChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                        currentChatChannelService = privateEventsChannelService;
                    }
                });
            } else if (model.getChatChannelDomain() == ChatChannelDomain.SUPPORT) {
                selectedChannelPin = supportChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                    model.selectedChannel.set(channel);
                    if (channel instanceof CommonPublicChatChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<CommonPublicChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((CommonPublicChatChannel) channel).getChatMessages());
                        model.allowEditing.set(true);
                        currentChatChannelService = publicSupportChannelService;
                    } else if (channel instanceof PrivateTwoPartyChatChannel) {
                        if (chatMessagesPin != null) {
                            chatMessagesPin.unbind();
                        }
                        chatMessagesPin = FxBindings.<TwoPartyPrivateChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.chatMessages)
                                .map(chatMessage -> new ChatMessageListItem<>(chatMessage, userProfileService, reputationService))
                                .to(((PrivateTwoPartyChatChannel) channel).getChatMessages());
                        model.allowEditing.set(false);
                        currentChatChannelService = privateSupportChannelService;
                    }
                });
            }
            focusSubscription = EasyBind.subscribe(view.getRoot().getScene().getWindow().focusedProperty(), focused -> {
                if (focused && currentChatChannelService != null && model.getSelectedChannel().get() != null) {
                    currentChatChannelService.updateSeenChatMessageIds(model.getSelectedChannel().get());
                }
            });
            selectedChannelSubscription = EasyBind.subscribe(model.selectedChannel, selectedChannel -> {
                if (currentChatChannelService != null && selectedChannel != null) {
                    currentChatChannelService.updateSeenChatMessageIds(selectedChannel);
                }
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
            checkArgument(!model.isMyMessage(chatMessage), "tradeChatMessage must not be mine");

            TakeOfferHelper.sendTakeOfferMessage(userProfileService, userIdentityService, mediationService, privateTradeChannelService, chatMessage)
                    .thenAccept(result -> UIThread.run(() -> {
                        privateTradeChannelService.findChannel(chatMessage.getTradeChatOffer().orElseThrow().getId())
                                .ifPresent(tradeChannelSelectionService::selectChannel);
                        Optional<Runnable> takeOfferCompleteHandler = model.takeOfferCompleteHandler;
                        takeOfferCompleteHandler.ifPresent(Runnable::run);
                    }));
        }

        private void onDeleteMessage(ChatMessage chatMessage) {
            if (userIdentityService.findUserIdentity(chatMessage.getAuthorId()).isPresent()) {
                UserIdentity messageAuthor = userIdentityService.findUserIdentity(chatMessage.getAuthorId()).get();
                if (userIdentityService.getSelectedUserIdentity().get().equals(messageAuthor)) {
                    doDeleteMessage(chatMessage, messageAuthor);
                } else {
                    new Popup().information(Res.get("chat.deleteMessage.wrongUserProfile.popup"))
                            .closeButtonText(Res.get("no"))
                            .actionButtonText(Res.get("yes"))
                            .onAction(() -> {
                                userIdentityService.selectChatUserIdentity(messageAuthor);
                                doDeleteMessage(chatMessage, messageAuthor);
                            })
                            .show();
                }
                //todo delete private message
            }
        }

        private void doDeleteMessage(ChatMessage chatMessage, UserIdentity messageAuthor) {
            if (chatMessage instanceof PublicTradeChatMessage) {
                publicTradeChannelService.deleteChatMessage((PublicTradeChatMessage) chatMessage, messageAuthor);
            } else if (chatMessage instanceof CommonPublicChatMessage) {

                //todo services dont do any domain specific here
                // -> networkService.removeAuthenticatedData(chatMessage, userIdentity.getNodeIdAndKeyPair());
                publicDiscussionChannelService.findChannelForMessage(chatMessage)
                        .ifPresent(c -> publicDiscussionChannelService.deleteChatMessage((CommonPublicChatMessage) chatMessage, messageAuthor));
                publicEventsChannelService.findChannelForMessage(chatMessage)
                        .ifPresent(c -> publicEventsChannelService.deleteChatMessage((CommonPublicChatMessage) chatMessage, messageAuthor));
                publicSupportChannelService.findChannelForMessage(chatMessage)
                        .ifPresent(c -> publicSupportChannelService.deleteChatMessage((CommonPublicChatMessage) chatMessage, messageAuthor));
            }
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
                UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity().get();
                publicTradeChannelService.publishEditedChatMessage((PublicTradeChatMessage) chatMessage, editedText, userIdentity);
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity().get();
                publicDiscussionChannelService.publishEditedChatMessage((CommonPublicChatMessage) chatMessage, editedText, userIdentity);
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
                if (chatMessage instanceof PublicChatMessage) {
                    items.add(new BisqPopupMenuItem(Res.get("satoshisquareapp.chat.messageMenu.ignoreUser"),
                            () -> onIgnoreUser(chatMessage)));
                }
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
            if (model.getChatChannelDomain() == ChatChannelDomain.TRADE) {
                // todo use new 2 party channelservice
                //PrivateTradeChannel privateTradeChannel = getPrivateTradeChannel(peer);
                //tradeChannelSelectionService.selectChannel(privateTradeChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.DISCUSSION) {
                privateDiscussionChannelService.maybeCreateAndAddChannel(peer).ifPresent(discussionChannelSelectionService::selectChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.EVENTS) {
                privateEventsChannelService.maybeCreateAndAddChannel(peer).ifPresent(eventsChannelSelectionService::selectChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.SUPPORT) {
                privateSupportChannelService.maybeCreateAndAddChannel(peer).ifPresent(supportChannelSelectionService::selectChannel);
            }
        }

        private void applyPredicate() {
            boolean offerOnly = settingsService.getOffersOnly().get();
            Predicate<ChatMessageListItem<? extends ChatMessage>> predicate = item -> {
                boolean offerOnlyPredicate = true;
                if (item.getChatMessage() instanceof PublicTradeChatMessage) {
                    PublicTradeChatMessage publicTradeChatMessage = (PublicTradeChatMessage) item.getChatMessage();
                    offerOnlyPredicate = !offerOnly || publicTradeChatMessage.hasTradeChatOffer();
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
        private final BooleanProperty offerOnly = new SimpleBooleanProperty();

        private Model(ChatService chatService,
                      UserIdentityService userIdentityService,
                      ChatChannelDomain chatChannelDomain) {
            this.chatService = chatService;
            this.userIdentityService = userIdentityService;
            this.chatChannelDomain = chatChannelDomain;
        }

        boolean isMyMessage(ChatMessage chatMessage) {
            return userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId());
        }

        boolean isOfferMessage(ChatMessage chatMessage) {
            return chatMessage instanceof TradeChatOfferMessage &&
                    ((TradeChatOfferMessage) chatMessage).hasTradeChatOffer();
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
            root.getChildren().add(listView);

            messagesListener = c -> UIThread.runOnNextRenderFrame(this::scrollDown);
        }

        @Override
        protected void onViewAttached() {
            model.getSortedChatMessages().addListener(messagesListener);
            scrollDown();
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
                            takeOfferButton = new Button(Res.get("takeOffer"));
                            takeOfferButton.getStyleClass().add("default-button");

                            removeOfferButton = new Button(Res.get("deleteOffer"));
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
                            saveEditButton = new Button(Res.get("save"));
                            saveEditButton.setDefaultButton(true);
                            cancelEditButton = new Button(Res.get("cancel"));

                            editButtonsHBox = Layout.hBoxWith(Spacer.fillHBox(), cancelEditButton, saveEditButton);
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


                                boolean isOfferMessage = model.isOfferMessage(chatMessage);
                                boolean isPublicOfferMessage = chatMessage instanceof PublicTradeChatMessage && isOfferMessage;
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
                                if (myMessage) {
                                    HBox userNameAndDateHBox = new HBox(10, dateTime, userName);
                                    userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
                                    message.setAlignment(Pos.CENTER_RIGHT);

                                    quotedMessageVBox.setId("chat-message-quote-box-my-msg");

                                    messageBgHBox.getStyleClass().add("chat-message-bg-my-message");
                                    VBox.setMargin(userNameAndDateHBox, new Insets(-5, 30, -5, 0));

                                    HBox.setMargin(copyIcon, new Insets(0, 15, 0, 0));

                                    VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);
                                    if (isPublicOfferMessage) {
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
                                    if (isPublicOfferMessage) {
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
                                    userName.setText(author.getUserName());
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

            if (chatMessage instanceof PrivateChatMessage) {
                senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSender());
            } else {
                senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorId());
            }
            String editPostFix = chatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = chatMessage.getText() + editPostFix;
            quotedMessage = chatMessage.getQuotation();
            date = DateFormatter.formatDateTime(new Date(chatMessage.getDate()), DateFormat.MEDIUM, DateFormat.SHORT, true, " " + Res.get("at") + " ");

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