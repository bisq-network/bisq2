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

import bisq.chat.*;
import bisq.chat.bisqeasy.BisqEasyOfferMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.PrivateChatMessage;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Icons;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.list_view.NoSelectionModel;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.FilteredListItem;
import bisq.desktop.main.content.bisq_easy.take_offer.TakeOfferController;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.settings.SettingsService;
import bisq.trade.Trade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.*;
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
import javafx.stage.Window;
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        private final BannedUserService bannedUserService;
        private final NetworkService networkService;
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
            bannedUserService = serviceProvider.getUserService().getBannedUserService();
            networkService = serviceProvider.getNetworkService();
            this.mentionUserHandler = mentionUserHandler;
            this.showChatUserDetailsHandler = showChatUserDetailsHandler;
            this.replyHandler = replyHandler;

            model = new Model(userIdentityService, chatChannelDomain);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            Window window = view.getRoot().getScene().getWindow();

            model.getSortedChatMessages().setComparator(ChatMessagesListView.ChatMessageListItem::compareTo);

            offerOnlySettingsPin = FxBindings.subscribe(settingsService.getOffersOnly(), offerOnly -> UIThread.run(this::applyPredicate));

            ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain());
            selectedChannelPin = chatChannelSelectionService.getSelectedChannel().addObserver(channel -> {
                UIThread.run(() -> {
                    model.selectedChannel.set(channel);
                    model.isPublicChannel.set(channel instanceof PublicChatChannel);

                    if (chatMessagesPin != null) {
                        chatMessagesPin.unbind();
                    }

                    // Clear and call dispose on the current messages when we change the channel.
                    model.chatMessages.forEach(ChatMessageListItem::dispose);
                    model.chatMessages.clear();

                    if (channel instanceof BisqEasyOfferbookChannel) {
                        chatMessagesPin = bindChatMessages((BisqEasyOfferbookChannel) channel);
                    } else if (channel instanceof BisqEasyOpenTradeChannel) {
                        chatMessagesPin = bindChatMessages((BisqEasyOpenTradeChannel) channel);
                    } else if (channel instanceof CommonPublicChatChannel) {
                        chatMessagesPin = bindChatMessages((CommonPublicChatChannel) channel);
                    } else if (channel instanceof TwoPartyPrivateChatChannel) {
                        chatMessagesPin = bindChatMessages((TwoPartyPrivateChatChannel) channel);
                    }

                    if (focusSubscription != null) {
                        focusSubscription.unsubscribe();
                    }
                    if (selectedChannelSubscription != null) {
                        selectedChannelSubscription.unsubscribe();
                    }
                    if (channel != null) {
                        focusSubscription = EasyBind.subscribe(window.focusedProperty(),
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
            if (focusSubscription != null) {
                focusSubscription.unsubscribe();
            }
            if (selectedChannelSubscription != null) {
                selectedChannelSubscription.unsubscribe();
            }
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

        private void onTakeOffer(BisqEasyOfferbookMessage chatMessage, boolean canTakeOffer) {
            if (userIdentityService.getSelectedUserIdentity() == null ||
                    bannedUserService.isUserProfileBanned(chatMessage.getAuthorUserProfileId()) ||
                    bannedUserService.isUserProfileBanned(userIdentityService.getSelectedUserIdentity().getUserProfile())) {
                return;
            }

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
                            new Popup().warning(Res.get("chat.message.delete.differentUserProfile.warn"))
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

            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                chatService.getBisqEasyOfferbookChannelService().deleteChatMessage(bisqEasyOfferbookMessage, userIdentity.getNodeIdAndKeyPair())
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.error("We got an error at doDeleteMessage: " + throwable);
                            }
                        });
            } else if (chatMessage instanceof CommonPublicChatMessage) {
                CommonPublicChatChannelService commonPublicChatChannelService = chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain);
                CommonPublicChatMessage commonPublicChatMessage = (CommonPublicChatMessage) chatMessage;
                commonPublicChatChannelService.findChannel(chatMessage)
                        .ifPresent(channel -> commonPublicChatChannelService.deleteChatMessage(commonPublicChatMessage, userIdentity.getNodeIdAndKeyPair()));
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

            if (editedText.length() > ChatMessage.MAX_TEXT_LENGTH) {
                new Popup().warning(Res.get("validation.tooLong", ChatMessage.MAX_TEXT_LENGTH)).show();
                return;
            }

            UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                chatService.getBisqEasyOfferbookChannelService().publishEditedChatMessage(bisqEasyOfferbookMessage, editedText, userIdentity);
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
            ChatChannelDomain chatChannelDomain = model.getSelectedChannel().get().getChatChannelDomain();
            if (chatMessage instanceof PrivateChatMessage) {
                PrivateChatMessage privateChatMessage = (PrivateChatMessage) chatMessage;
                Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                        new ReportToModeratorWindow.InitData(privateChatMessage.getSenderUserProfile(), chatChannelDomain));
            } else {
                userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                        .ifPresent(accusedUserProfile -> Navigation.navigateTo(NavigationTarget.REPORT_TO_MODERATOR,
                                new ReportToModeratorWindow.InitData(accusedUserProfile, chatChannelDomain)));
            }
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
            chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer)
                    .ifPresent(channel -> {
                        if (model.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OFFERBOOK) {
                            Navigation.navigateTo(NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
                        }
                    });
        }

        private void applyPredicate() {
            boolean offerOnly = settingsService.getOffersOnly().get();
            Predicate<ChatMessageListItem<? extends ChatMessage>> predicate = item -> {
                Optional<UserProfile> senderUserProfile = item.getSenderUserProfile();
                if (senderUserProfile.isEmpty()) {
                    return false;
                }
                if (bannedUserService.isUserProfileBanned(item.getChatMessage().getAuthorUserProfileId()) ||
                        bannedUserService.isUserProfileBanned(senderUserProfile.get())) {
                    return false;
                }

                boolean offerOnlyPredicate = true;
                if (item.getChatMessage() instanceof BisqEasyOfferbookMessage) {
                    BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
                    offerOnlyPredicate = !offerOnly || bisqEasyOfferbookMessage.hasBisqEasyOffer();
                }
                // We do not display the take offer message as it has no text and is used only for sending the offer 
                // to the peer and signalling the take offer event.
                if (item.getChatMessage().getChatMessageType() == ChatMessageType.TAKE_BISQ_EASY_OFFER) {
                    return false;
                }

                return offerOnlyPredicate &&
                        !userProfileService.getIgnoredUserProfileIds().contains(senderUserProfile.get().getId()) &&
                        userProfileService.findUserProfile(senderUserProfile.get().getId()).isPresent();
            };
            model.filteredChatMessages.setPredicate(item -> model.getSearchPredicate().test(item) && predicate.test(item));
        }

        private <M extends ChatMessage, C extends ChatChannel<M>> Pin bindChatMessages(C channel) {
            return channel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(M chatMessage) {
                    UIThread.run(() -> {
                        model.chatMessages.add(new ChatMessageListItem<>(chatMessage, userProfileService, reputationService,
                                bisqEasyTradeService, userIdentityService, networkService));
                    });
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof ChatMessage) {
                        UIThread.run(() -> {
                            ChatMessage chatMessage = (ChatMessage) element;
                            Optional<ChatMessageListItem<? extends ChatMessage>> toRemove = model.chatMessages.stream()
                                    .filter(item -> item.getChatMessage().getId().equals(chatMessage.getId()))
                                    .findAny();
                            toRemove.ifPresent(item -> {
                                item.dispose();
                                model.chatMessages.remove(item);
                            });
                        });
                    }
                }

                @Override
                public void clear() {
                    UIThread.run(() -> {
                        model.chatMessages.forEach(ChatMessageListItem::dispose);
                        model.chatMessages.clear();
                    });
                }
            });
        }

        private String getUserName(String userProfileId) {
            return userProfileService.findUserProfile(userProfileId)
                    .map(UserProfile::getUserName)
                    .orElse(Res.get("data.na"));
        }

        private String getSupportedLanguageCodes(BisqEasyOfferbookMessage chatMessage) {
            String result = getSupportedLanguageCodes(chatMessage, ", ", LanguageRepository::getDisplayLanguage);
            return result.isEmpty() ? "" : Res.get("chat.message.supportedLanguages") + " " + StringUtils.truncate(result, 100);
        }

        private String getSupportedLanguageCodesForTooltip(BisqEasyOfferbookMessage chatMessage) {
            String result = getSupportedLanguageCodes(chatMessage, "\n", LanguageRepository::getDisplayString);
            return result.isEmpty() ? "" : Res.get("chat.message.supportedLanguages") + "\n" + result;
        }

        private String getSupportedLanguageCodes(BisqEasyOfferbookMessage chatMessage, String separator, Function<String, String> toStringFunction) {
            return chatMessage.getBisqEasyOffer()
                    .map(BisqEasyOffer::getSupportedLanguageCodes)
                    .map(supportedLanguageCodes -> Joiner.on(separator)
                            .join(supportedLanguageCodes.stream()
                                    .map(toStringFunction)
                                    .collect(Collectors.toList())))
                    .orElse("");
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final UserIdentityService userIdentityService;
        private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final ObservableList<ChatMessageListItem<? extends ChatMessage>> chatMessages = FXCollections.observableArrayList();
        private final FilteredList<ChatMessageListItem<? extends ChatMessage>> filteredChatMessages = new FilteredList<>(chatMessages);
        private final SortedList<ChatMessageListItem<? extends ChatMessage>> sortedChatMessages = new SortedList<>(filteredChatMessages);
        private final BooleanProperty isPublicChannel = new SimpleBooleanProperty();
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
        private UIScheduler scrollDelay;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            listView = new ListView<>(model.getSortedChatMessages());
            listView.getStyleClass().add("chat-messages-list-view");

            //Label placeholder = new Label(Res.get("data.noDataAvailable"));
            //listView.setPlaceholder(placeholder);
            listView.setCellFactory(getCellFactory());

            // https://stackoverflow.com/questions/20621752/javafx-make-listview-not-selectable-via-mouse
            listView.setSelectionModel(new NoSelectionModel<>());

            VBox.setVgrow(listView, Priority.ALWAYS);
            root.getChildren().add(listView);

            messagesListener = c -> {
                UIThread.runOnNextRenderFrame(this::scrollDown);
                if (scrollDelay != null) {
                    scrollDelay.stop();
                }
                scrollDelay = UIScheduler.run(this::scrollDown).after(200);
            };
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
                        private final Label message, userName, dateTime, replyIcon, pmIcon, editIcon, deleteIcon, copyIcon,
                                moreOptionsIcon, supportedLanguages;
                        private final Label deliveryState;
                        private final Text quotedMessageField;
                        private final BisqTextArea editInputField;
                        private final Button saveEditButton, cancelEditButton;
                        private final VBox mainVBox, quotedMessageVBox;
                        private final HBox cellHBox, messageHBox, messageBgHBox, reactionsHBox, editButtonsHBox;
                        private final UserProfileIcon userProfileIcon = new UserProfileIcon(60);
                        private final Set<Subscription> subscriptions = new HashSet<>();

                        {
                            userName = new Label();
                            userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");

                            deliveryState = new Label();
                            deliveryState.setCursor(Cursor.HAND);
                            BisqTooltip tooltip = new BisqTooltip();
                            tooltip.getStyleClass().add("dark-tooltip");
                            deliveryState.setTooltip(tooltip);

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
                            editInputField.setMinWidth(150);
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
                            replyIcon = getIconWithToolTip(AwesomeIcon.REPLY, Res.get("chat.message.reply"));
                            pmIcon = getIconWithToolTip(AwesomeIcon.COMMENT_ALT, Res.get("chat.message.privateMessage"));
                            editIcon = getIconWithToolTip(AwesomeIcon.EDIT, Res.get("action.edit"));
                            HBox.setMargin(editIcon, new Insets(1, 0, 0, 0));
                            copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));
                            deleteIcon = getIconWithToolTip(AwesomeIcon.REMOVE_SIGN, Res.get("action.delete"));
                            moreOptionsIcon = getIconWithToolTip(AwesomeIcon.ELLIPSIS_HORIZONTAL, Res.get("chat.message.moreOptions"));
                            supportedLanguages = new Label();

                            reactionsHBox = new HBox(20);

                            reactionsHBox.setVisible(false);

                            HBox.setHgrow(messageBgHBox, Priority.SOMETIMES);
                            messageHBox = new HBox();

                            VBox.setMargin(quotedMessageVBox, new Insets(15, 0, 10, 5));
                            VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));
                            VBox.setMargin(editButtonsHBox, new Insets(10, 25, -15, 0));
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

                                subscriptions.clear();
                                ChatMessage chatMessage = item.getChatMessage();

                                Node flow = this.getListView().lookup(".virtual-flow");
                                if (flow != null && !flow.isVisible())
                                    return;

                                boolean hasTradeChatOffer = model.hasTradeChatOffer(chatMessage);
                                boolean isBisqEasyPublicChatMessageWithOffer = chatMessage instanceof BisqEasyOfferbookMessage && hasTradeChatOffer;
                                boolean isMyMessage = model.isMyMessage(chatMessage);

                                if (isBisqEasyPublicChatMessageWithOffer) {
                                    supportedLanguages.setText(controller.getSupportedLanguageCodes(((BisqEasyOfferbookMessage) chatMessage)));
                                    supportedLanguages.setTooltip(new BisqTooltip(controller.getSupportedLanguageCodesForTooltip(((BisqEasyOfferbookMessage) chatMessage))));
                                }

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
                                    VBox.setMargin(userNameAndDateHBox, new Insets(-5, 10, -5, 0));

                                    VBox messageVBox = new VBox(quotedMessageVBox, message, editInputField);
                                    if (isBisqEasyPublicChatMessageWithOffer) {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(160));
                                        userProfileIcon.setSize(60);
                                        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
                                        HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                                        HBox.setMargin(editInputField, new Insets(-4, -10, -15, 0));
                                        HBox.setMargin(messageVBox, new Insets(0, -10, 0, 0));

                                        removeOfferButton.setOnAction(e -> controller.onDeleteMessage(chatMessage));
                                        reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, supportedLanguages, copyIcon);
                                        reactionsHBox.setAlignment(Pos.CENTER_RIGHT);

                                        HBox.setMargin(userProfileIconVbox, new Insets(0, 0, 10, 0));
                                        HBox hBox = new HBox(15, messageVBox, userProfileIconVbox);
                                        HBox removeOfferButtonHBox = new HBox(Spacer.fillHBox(), removeOfferButton);
                                        VBox vBox = new VBox(hBox, removeOfferButtonHBox);
                                        messageBgHBox.getChildren().setAll(vBox);
                                    } else {
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(140));
                                        userProfileIcon.setSize(30);
                                        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
                                        HBox.setMargin(deleteIcon, new Insets(0, 10, 0, 0));
                                        reactionsHBox.getChildren().setAll(Spacer.fillHBox(), replyIcon, pmIcon, editIcon, copyIcon, deleteIcon);
                                        HBox.setMargin(messageVBox, new Insets(0, -15, 0, 0));
                                        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                                        HBox.setMargin(editInputField, new Insets(6, -10, -25, 0));
                                        messageBgHBox.getChildren().setAll(messageVBox, userProfileIconVbox);
                                    }

                                    HBox.setMargin(deliveryState, new Insets(0, 10, 0, 0));
                                    HBox deliveryStateHBox = new HBox(Spacer.fillHBox(), reactionsHBox);

                                    subscriptions.add(EasyBind.subscribe(reactionsHBox.visibleProperty(), v -> {
                                        if (v) {
                                            deliveryStateHBox.getChildren().remove(deliveryState);
                                            if (!reactionsHBox.getChildren().contains(deliveryState)) {
                                                reactionsHBox.getChildren().add(deliveryState);
                                            }
                                        } else {
                                            reactionsHBox.getChildren().remove(deliveryState);
                                            if (!deliveryStateHBox.getChildren().contains(deliveryState)) {
                                                deliveryStateHBox.getChildren().add(deliveryState);
                                            }
                                        }
                                    }));

                                    VBox.setMargin(deliveryStateHBox, new Insets(4, 0, -3, 0));
                                    mainVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, editButtonsHBox, deliveryStateHBox);

                                    messageHBox.getChildren().setAll(Spacer.fillHBox(), messageBgHBox);

                                } else {
                                    // Peer
                                    HBox userNameAndDateHBox = new HBox(10, userName, dateTime);
                                    message.setAlignment(Pos.CENTER_LEFT);
                                    userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);

                                    userProfileIcon.setSize(60);
                                    HBox.setMargin(replyIcon, new Insets(4, 0, -4, 10));
                                    HBox.setMargin(pmIcon, new Insets(4, 0, -4, 0));
                                    HBox.setMargin(moreOptionsIcon, new Insets(6, 0, -6, 0));


                                    quotedMessageVBox.setId("chat-message-quote-box-peer-msg");

                                    messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
                                    if (isBisqEasyPublicChatMessageWithOffer) {
                                        reactionsHBox.getChildren().setAll(replyIcon, pmIcon, editIcon, deleteIcon, moreOptionsIcon, supportedLanguages, Spacer.fillHBox());
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(430));
                                        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);

                                        Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
                                        reputationLabel.getStyleClass().add("bisq-text-7");

                                        reputationScoreDisplay.setReputationScore(item.getReputationScore());
                                        VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
                                        reputationVBox.setAlignment(Pos.CENTER_LEFT);

                                        BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                                        takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage, item.isCanTakeOffer()));
                                        takeOfferButton.setDefaultButton(item.isCanTakeOffer());

                                        VBox messageVBox = new VBox(quotedMessageVBox, message);
                                        HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
                                        HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                                        HBox.setMargin(reputationVBox, new Insets(-5, 10, 0, 0));
                                        HBox.setMargin(takeOfferButton, new Insets(0, 10, 0, 0));
                                        messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox, Spacer.fillHBox(), reputationVBox, takeOfferButton);

                                        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 10));
                                        mainVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, reactionsHBox);
                                    } else {
                                        reactionsHBox.getChildren().setAll(replyIcon, pmIcon, editIcon, deleteIcon, moreOptionsIcon, Spacer.fillHBox());
                                        message.maxWidthProperty().bind(root.widthProperty().subtract(140));//165
                                        userProfileIcon.setSize(30);
                                        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);

                                        VBox messageVBox = new VBox(quotedMessageVBox, message);
                                        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
                                        HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
                                        messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox);
                                        messageHBox.getChildren().setAll(messageBgHBox, Spacer.fillHBox());

                                        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 10));
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

                                subscriptions.add(EasyBind.subscribe(item.getMessageDeliveryStatusIcon(), icon -> {
                                            deliveryState.setManaged(icon != null);
                                            deliveryState.setVisible(icon != null);
                                            if (icon != null) {
                                                AwesomeDude.setIcon(deliveryState, icon, AwesomeDude.DEFAULT_ICON_SIZE);
                                            }
                                        }
                                ));
                                deliveryState.getTooltip().textProperty().bind(item.messageDeliveryStatusTooltip);

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

                                subscriptions.forEach(Subscription::unsubscribe);
                                subscriptions.clear();

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

                            boolean isPublicChannel = model.isPublicChannel.get();
                            boolean allowEditing = isPublicChannel;
                            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                                allowEditing = allowEditing && bisqEasyOfferbookMessage.getBisqEasyOffer().isEmpty();
                            }
                            if (isMyMessage) {
                                copyIcon.setOnMouseClicked(e -> controller.onCopyMessage(chatMessage));
                                if (allowEditing) {
                                    editIcon.setOnMouseClicked(e -> onEditMessage(item));
                                }
                                if (isPublicChannel) {
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
                            deleteIcon.setVisible(isMyMessage && isPublicChannel);
                            deleteIcon.setManaged(isMyMessage && isPublicChannel);
                            removeOfferButton.setVisible(isMyMessage && isPublicChannel);
                            removeOfferButton.setManaged(isMyMessage && isPublicChannel);

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

                private Label getIconWithToolTip(AwesomeIcon icon, String tooltipString) {
                    Label iconLabel = Icons.getIcon(icon);
                    iconLabel.setCursor(Cursor.HAND);
                    Tooltip tooltip = new BisqTooltip(tooltipString);
                    tooltip.getStyleClass().add("dark-tooltip");
                    iconLabel.setTooltip(tooltip);
                    return iconLabel;
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
        @EqualsAndHashCode.Exclude
        private final StringProperty messageDeliveryStatusTooltip = new SimpleStringProperty();
        @EqualsAndHashCode.Exclude
        private final ObjectProperty<AwesomeIcon> messageDeliveryStatusIcon = new SimpleObjectProperty<>();
        @EqualsAndHashCode.Exclude
        private final Set<Pin> pins = new HashSet<>();

        public ChatMessageListItem(T chatMessage,
                                   UserProfileService userProfileService,
                                   ReputationService reputationService,
                                   BisqEasyTradeService bisqEasyTradeService,
                                   UserIdentityService userIdentityService,
                                   NetworkService networkService) {
            this.chatMessage = chatMessage;

            if (chatMessage instanceof PrivateChatMessage) {
                senderUserProfile = Optional.of(((PrivateChatMessage) chatMessage).getSenderUserProfile());
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

            if (chatMessage instanceof BisqEasyOfferbookMessage) {
                BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) chatMessage;
                if (userIdentityService.getSelectedUserIdentity() != null && bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent()) {
                    UserProfile userProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
                    NetworkId takerNetworkId = userProfile.getNetworkId();
                    BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().get();
                    String tradeId = Trade.createId(bisqEasyOffer.getId(), takerNetworkId.getId());
                    canTakeOffer = !bisqEasyTradeService.hadTrade(tradeId);
                } else {
                    canTakeOffer = false;
                }
            } else {
                canTakeOffer = false;
            }

            pins.add(networkService.getMessageDeliveryStatusByMessageId().addObserver(new HashMapObserver<>() {
                @Override
                public void put(String key, Observable<MessageDeliveryStatus> value) {
                    if (key.equals(chatMessage.getId())) {
                        pins.add(value.addObserver(status -> {
                            UIThread.run(() -> {
                                if (status != null) {
                                    UIThread.run(() -> {
                                        messageDeliveryStatusTooltip.set(Res.get("chat.message.deliveryState." + status.name()));
                                        switch (status) {
                                            case SENT:
                                                messageDeliveryStatusIcon.set(AwesomeIcon.SPINNER);
                                                break;
                                            case ARRIVED:
                                                messageDeliveryStatusIcon.set(AwesomeIcon.OK_SIGN);
                                                break;
                                            case ADDED_TO_MAILBOX:
                                                messageDeliveryStatusIcon.set(AwesomeIcon.ENVELOPE);
                                                break;
                                            case MAILBOX_MSG_RECEIVED:
                                                messageDeliveryStatusIcon.set(AwesomeIcon.CIRCLE_ARROW_DOWN);
                                                break;
                                            case FAILED:
                                                messageDeliveryStatusIcon.set(AwesomeIcon.EXCLAMATION_SIGN);
                                                break;
                                        }
                                    });
                                }
                            });
                        }));
                    }
                }

                @Override
                public void remove(Object key) {
                }

                @Override
                public void clear() {
                }
            }));
        }

        @Override
        public int compareTo(ChatMessageListItem o) {
            return Comparator.comparingLong(ChatMessage::getDate).compare(this.getChatMessage(), o.getChatMessage());
        }

        @Override
        public boolean match(String filterString) {
            return filterString == null || filterString.isEmpty() || StringUtils.containsIgnoreCase(message, filterString) || StringUtils.containsIgnoreCase(nym, filterString) || StringUtils.containsIgnoreCase(nickName, filterString) || StringUtils.containsIgnoreCase(date, filterString);
        }

        public void dispose() {
            pins.forEach(Pin::unbind);
        }
    }
}