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

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.ChatMessageType;
import bisq.chat.message.Citation;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.primary.overlay.bisq_easy.create_offer.CreateOfferController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.DontShowAgainService;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.wallets.core.WalletService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ChatMessagesComponent {
    private final Controller controller;

    public ChatMessagesComponent(DefaultApplicationService applicationService,
                                 ChatChannelDomain chatChannelDomain,
                                 Consumer<UserProfile> openUserProfileSidebarHandler) {
        controller = new Controller(applicationService,
                chatChannelDomain,
                openUserProfileSidebarHandler);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    public void mentionUser(UserProfile userProfile) {
        controller.mentionUserHandler(userProfile);
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.chatMessagesListView.setSearchPredicate(predicate);
    }

    public void resetSelectedChatMessage() {
        controller.model.selectedChatMessage = null;
    }

    public void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
        controller.createAndSelectTwoPartyPrivateChatChannel(peer);
    }

    public void refreshMessages() {
        controller.chatMessagesListView.refreshMessages();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final Consumer<UserProfile> openUserProfileSidebarHandler;
        private final UserIdentityService userIdentityService;
        private final CitationBlock citationBlock;
        private final ChatMessagesListView chatMessagesListView;
        private final UserProfileService userProfileService;
        private final SettingsService settingsService;
        private final ChatService chatService;
        private final MediationService mediationService;
        private final Optional<WalletService> walletService;
        private final AccountService accountService;

        private Pin selectedChannelPin, inMediationPin, chatMessagesPin,
                selectedPaymentAccountPin, paymentAccountsPin;
        private Subscription selectedPaymentAccountSubscription;

        private Controller(DefaultApplicationService applicationService,
                           ChatChannelDomain chatChannelDomain,
                           Consumer<UserProfile> openUserProfileSidebarHandler) {
            this.openUserProfileSidebarHandler = openUserProfileSidebarHandler;

            chatService = applicationService.getChatService();
            settingsService = applicationService.getSettingsService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();
            accountService = applicationService.getAccountService();
            mediationService = applicationService.getSupportService().getMediationService();
            walletService = applicationService.getWalletService();

            citationBlock = new CitationBlock(applicationService);

            UserProfileSelection userProfileSelection = new UserProfileSelection(userIdentityService);

            chatMessagesListView = new ChatMessagesListView(applicationService,
                    this::mentionUserHandler,
                    this::showChatUserDetailsHandler,
                    this::replyHandler,
                    chatChannelDomain);

            model = new Model(chatChannelDomain, chatService);
            view = new View(model, this,
                    chatMessagesListView.getRoot(),
                    citationBlock.getRoot(),
                    userProfileSelection);
        }

        @Override
        public void onActivate() {
            model.mentionableUsers.setAll(userProfileService.getUserProfiles());
            model.getPaymentAccounts().setAll(accountService.getAccounts());
            Optional.ofNullable(model.selectedChatMessage).ifPresent(this::showChatUserDetailsHandler);

            //todo
            //model.mentionableChatChannels.setAll(publicDiscussionChannelService.getMentionableChannels());

            userIdentityService.getUserIdentityChangedFlag().addObserver(__ -> applyUserProfileOrChannelChange());
            ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain());
            selectedChannelPin = chatChannelSelectionService.getSelectedChannel()
                    .addObserver(this::selectedChannelChanged);

            paymentAccountsPin = accountService.getAccounts().addListener(this::accountsChanged);
            selectedPaymentAccountPin = FxBindings.bind(model.selectedAccountProperty())
                    .to(accountService.selectedAccountAsObservable());
            selectedPaymentAccountSubscription = EasyBind.subscribe(model.selectedAccountProperty(),
                    selectedAccount -> {
                        if (selectedAccount != null) {
                            accountService.setSelectedAccount(selectedAccount);
                        }
                    });
        }

        protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
            UIThread.run(() -> {
                model.selectedChannel.set(chatChannel);
                applyUserProfileOrChannelChange();

                boolean isBisqEasyPublicChatChannel = chatChannel instanceof BisqEasyPublicChatChannel;
                boolean isBisqEasyPrivateTradeChatChannel = chatChannel instanceof BisqEasyPrivateTradeChatChannel;
                boolean isTwoPartyPrivateChatChannel = chatChannel instanceof TwoPartyPrivateChatChannel;
                model.getLeaveChannelButtonVisible().set(false);
                model.getCreateOfferButtonVisible().set(isBisqEasyPublicChatChannel);
                model.getOpenDisputeButtonVisible().set(isBisqEasyPrivateTradeChatChannel);
                model.getSendBtcAddressButtonVisible().set(false);
                model.getSendPaymentAccountButtonVisible().set(false);

                if (chatMessagesPin != null) {
                    chatMessagesPin.unbind();
                }
                if (isBisqEasyPrivateTradeChatChannel) {
                    chatMessagesPin = chatChannel.getChatMessages().addListener(() -> privateTradeMessagesChanged((BisqEasyPrivateTradeChatChannel) chatChannel));
                    BisqEasyPrivateTradeChatChannel privateChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
                    if (inMediationPin != null) {
                        inMediationPin.unbind();
                    }
                    inMediationPin = privateChannel.isInMediationObservable().addObserver(isInMediation ->
                            model.getOpenDisputeButtonVisible().set(!isInMediation &&
                                    !privateChannel.isMediator()));
                } else if (isTwoPartyPrivateChatChannel) {
                    chatMessagesPin = chatChannel.getChatMessages().addListener(() -> updateLeaveChannelButtonState((TwoPartyPrivateChatChannel) chatChannel));
                }

                accountsChanged();
            });
        }

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
            selectedPaymentAccountPin.unbind();
            paymentAccountsPin.unbind();
            if (inMediationPin != null) {
                inMediationPin.unbind();
            }
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }

            selectedPaymentAccountSubscription.unsubscribe();
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // From method calls on component
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
            chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Handlers passed to list view component
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void replyHandler(ChatMessage chatMessage) {
            if (!chatMessage.isMyMessage(userIdentityService)) {
                citationBlock.reply(chatMessage);
            }
        }

        private void showChatUserDetailsHandler(ChatMessage chatMessage) {
            model.selectedChatMessage = chatMessage;
            userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId())
                    .ifPresent(openUserProfileSidebarHandler);
        }

        private void mentionUserHandler(UserProfile userProfile) {
            String existingText = model.getTextInput().get();
            if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
                existingText += " ";
            }
            model.getTextInput().set(existingText + "@" + userProfile.getUserName() + " ");
        }


        private void listUserNamesHandler(UserProfile user) {
            String content = model.getTextInput().get().replaceAll("@[a-zA-Z\\d]*$", "@" + user.getUserName() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        private void listChannelsHandler(ChatChannel<?> chatChannel) {
            String channelTitle = chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse("");
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z\\d]*$", "#" + channelTitle + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Change handlers from service or model
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void privateTradeMessagesChanged(BisqEasyPrivateTradeChatChannel chatChannel) {
            UIThread.run(() -> {
                BisqEasyOffer bisqEasyOffer = chatChannel.getBisqEasyOffer();
                boolean isMaker = bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
                Direction usersDirection = isMaker ?
                        bisqEasyOffer.getMakersDirection() :
                        bisqEasyOffer.getTakersDirection();

                if (usersDirection.isSell()) {
                    model.getSendPaymentAccountButtonVisible().set(true);
                    model.getSendBtcAddressButtonVisible().set(false);
                } else {
                    model.getSendPaymentAccountButtonVisible().set(false);
                    model.getSendBtcAddressButtonVisible().set(walletService.isPresent());
                }
            });
            updateLeaveChannelButtonState(chatChannel);
        }

        private void updateLeaveChannelButtonState(PrivateChatChannel<?> chatChannel) {
            UIThread.run(() -> {
                boolean peerLeft = chatChannel.getChatMessages().stream()
                        .anyMatch(message -> message.getChatMessageType() == ChatMessageType.LEAVE);
                model.getLeaveChannelButtonVisible().set(peerLeft);
            });
        }


        private void accountsChanged() {
            UIThread.run(() ->
                    model.getPaymentAccountSelectionVisible().set(
                            model.getSelectedChannel().get() instanceof BisqEasyPrivateTradeChatChannel &&
                                    accountService.getAccounts().size() > 1));
        }

        private void applyUserProfileOrChannelChange() {
            boolean multipleProfiles = userIdentityService.getUserIdentities().size() > 1;
            ChatChannel<?> selectedChatChannel = model.selectedChannel.get();
            model.userProfileSelectionVisible.set(multipleProfiles && selectedChatChannel instanceof PublicChatChannel);

            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }

            if (selectedChatChannel != null) {
                chatMessagesPin = selectedChatChannel.getChatMessages().addListener(this::maybeSwitchUserProfile);
            }
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI handlers
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        void onLeaveChannel() {
            ChatChannel<?> chatChannel = model.getSelectedChannel().get();
            if (!(chatChannel instanceof PrivateChatChannel))
                return;

            chatService.findChatChannelService(chatChannel).ifPresent(
                    service -> {
                        service.leaveChannel(chatChannel.getId());
                        chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain()).maybeSelectFirstChannel();
                    }
            );
        }

        void onCreateOffer() {
            ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel().get();
            checkArgument(chatChannel instanceof BisqEasyPublicChatChannel,
                    "channel must be instanceof BisqEasyPublicChatChannel at onCreateOfferButtonClicked");
            Navigation.navigateTo(NavigationTarget.CREATE_OFFER, new CreateOfferController.InitData(false));
        }

        void onRequestMediation() {
            ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel().get();
            checkArgument(chatChannel instanceof BisqEasyPrivateTradeChatChannel,
                    "channel must be instanceof BisqEasyPrivateTradeChatChannel at onOpenMediation");
            BisqEasyPrivateTradeChatChannel privateTradeChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
            Optional<UserProfile> mediator = privateTradeChannel.getMediator();
            if (mediator.isPresent()) {
                new Popup().headLine(Res.get("bisqEasy.requestMediation.confirm.popup.headline"))
                        .information(Res.get("bisqEasy.requestMediation.confirm.popup.msg"))
                        .actionButtonText(Res.get("bisqEasy.requestMediation.confirm.popup.openMediation"))
                        .onAction(() -> {
                            privateTradeChannel.setIsInMediation(true);
                            mediationService.requestMediation(privateTradeChannel);
                            new Popup().headLine(Res.get("bisqEasy.requestMediation.popup.headline"))
                                    .feedback(Res.get("bisqEasy.requestMediation.popup.msg")).show();
                        })
                        .closeButtonText(Res.get("cancel"))
                        .show();
            } else {
                new Popup().warning(Res.get("bisqEasy.requestMediation.popup.noMediatorAvailable")).show();
            }
        }

        void onSendBtcAddress() {
            checkArgument(walletService.isPresent());
            ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel().get();
            checkArgument(chatChannel instanceof BisqEasyPrivateTradeChatChannel);
            BisqEasyPrivateTradeChatChannel tradeChatChannel = (BisqEasyPrivateTradeChatChannel) chatChannel;
            walletService.get().getUnusedAddress().
                    thenAccept(receiveAddress -> UIThread.run(() -> {
                                if (receiveAddress == null) {
                                    log.warn("receiveAddress from the wallet is null.");
                                    return;
                                }
                                String message = Res.get("bisqEasy.sendBtcAddress.message", receiveAddress);
                                chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                                        Optional.empty(),
                                        tradeChatChannel);
                            }
                    ));
        }

        void onSendPaymentAccount() {
            if (accountService.getAccounts().size() > 1) {
                //todo
                new Popup().information("TODO").show();
            } else {
                Account<?, ? extends PaymentMethod<?>> selectedAccount = accountService.getSelectedAccount();
                if (accountService.hasAccounts() && selectedAccount instanceof UserDefinedFiatAccount) {
                    ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel().get();
                    checkArgument(chatChannel instanceof BisqEasyPrivateTradeChatChannel);
                    BisqEasyPrivateTradeChatChannel channel = (BisqEasyPrivateTradeChatChannel) chatChannel;
                    String message = Res.get("bisqEasy.sendPaymentAccount.message", ((UserDefinedFiatAccount) selectedAccount).getAccountPayload());
                    chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                            Optional.empty(),
                            channel);
                } else {
                    if (!accountService.hasAccounts()) {
                        new Popup().information(Res.get("bisqEasy.sendPaymentAccount.noAccount.popup")).show();
                    } else if (accountService.getAccountByNameMap().size() > 1) {
                        String key = "bisqEasy.sendPaymentAccount.multipleAccounts";
                        if (DontShowAgainService.showAgain(key)) {
                            new Popup().information(Res.get("bisqEasy.sendPaymentAccount.multipleAccounts.popup"))
                                    .dontShowAgainId(key)
                                    .show();
                        }
                    }
                }
            }
        }

        void onPaymentAccountSelected(@Nullable Account<?, ? extends PaymentMethod<?>> account) {
            if (account != null) {
                accountService.setSelectedAccount(account);
            }
        }

        private void onSendMessage(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }

            if (model.selectedChannel.get() instanceof PublicChatChannel) {
                List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
                if (myUserProfilesInChannel.size() > 0) {
                    UserIdentity lastUsedUserProfile = myUserProfilesInChannel.get(0);
                    if (!lastUsedUserProfile.equals(userIdentityService.getSelectedUserIdentity())) {
                        new Popup().information(Res.get("chat.sendMessage.differentUserProfile.popup"))
                                .closeButtonText(Res.get("no"))
                                .actionButtonText(Res.get("yes"))
                                .onAction(() -> doSendMessage(text))
                                .show();
                        return;
                    }
                }
            }

            doSendMessage(text);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // Private
        ///////////////////////////////////////////////////////////////////////////////////////////////////

        private void doSendMessage(String text) {
            ChatChannel<? extends ChatMessage> chatChannel = model.selectedChannel.get();
            UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            Optional<Citation> citation = citationBlock.getCitation();

            if (chatChannel instanceof BisqEasyPublicChatChannel) {
                String dontShowAgainId = "sendMsgOfferOnlyWarn";
                if (settingsService.getOffersOnly().get()) {
                    new Popup().information(Res.get("social.chat.sendMsg.offerOnly.popup"))
                            .actionButtonText(Res.get("yes"))
                            .onAction(() -> settingsService.setOffersOnly(false))
                            .closeButtonText(Res.get("no"))
                            .dontShowAgainId(dontShowAgainId)
                            .show();
                }
                chatService.getBisqEasyPublicChatChannelService().publishChatMessage(text, citation, (BisqEasyPublicChatChannel) chatChannel, userIdentity);
            } else if (chatChannel instanceof BisqEasyPrivateTradeChatChannel) {
                if (settingsService.getTradeRulesConfirmed().get() || ((BisqEasyPrivateTradeChatChannel) chatChannel).isMediator()) {
                    chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(text, citation, (BisqEasyPrivateTradeChatChannel) chatChannel);
                } else {
                    new Popup().information(Res.get("social.chat.sendMsg.tradeRulesNotConfirmed.popup"))
                            .actionButtonText(Res.get("social.chat.sendMsg.tradeRulesNotConfirmed.popup.openGuide"))
                            .onAction(() -> Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE))
                            .show();
                }
            } else if (chatChannel instanceof CommonPublicChatChannel) {
                chatService.getCommonPublicChatChannelServices().get(model.chatChannelDomain).publishChatMessage(text, citation, (CommonPublicChatChannel) chatChannel, userIdentity);
            } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                chatService.getTwoPartyPrivateChatChannelServices().get(model.chatChannelDomain).sendTextMessage(text, citation, (TwoPartyPrivateChatChannel) chatChannel);
            }

            citationBlock.close();
        }

        private boolean isOfferAuthor(String offerAuthorUserProfileId) {
            return userIdentityService.isUserIdentityPresent(offerAuthorUserProfileId);
        }

        private void maybeSwitchUserProfile() {
            if (model.userProfileSelectionVisible.get()) {
                List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
                if (myUserProfilesInChannel.size() > 0) {
                    userIdentityService.selectChatUserIdentity(myUserProfilesInChannel.get(0));
                }
            }
        }

        private List<UserIdentity> getMyUserProfilesInChannel() {
            return model.selectedChannel.get().getChatMessages().stream()
                    .sorted(Comparator.comparing(ChatMessage::getDate).reversed())
                    .map(ChatMessage::getAuthorUserProfileId)
                    .map(userIdentityService::findUserIdentity)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final BooleanProperty leaveChannelButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty createOfferButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty openDisputeButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty sendBtcAddressButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty sendPaymentAccountButtonVisible = new SimpleBooleanProperty();
        private final BooleanProperty paymentAccountSelectionVisible = new SimpleBooleanProperty();
        private final ObservableList<Account<?, ? extends PaymentMethod<?>>> paymentAccounts = FXCollections.observableArrayList();
        private final ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedAccount = new SimpleObjectProperty<>();

        private final ObjectProperty<ChatChannel<? extends ChatMessage>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final BooleanProperty userProfileSelectionVisible = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private final ObservableList<UserProfile> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<ChatChannel<?>> mentionableChatChannels = FXCollections.observableArrayList();
        private final ChatChannelDomain chatChannelDomain;
        private final ChatService chatService;
        @Nullable
        private ChatMessage selectedChatMessage;

        private Model(ChatChannelDomain chatChannelDomain, ChatService chatService) {
            this.chatChannelDomain = chatChannelDomain;
            this.chatService = chatService;
        }

        String getChannelTitle(ChatChannel<?> chatChannel) {
            return chatService.findChatChannelService(chatChannel)
                    .map(service -> service.getChannelTitle(chatChannel))
                    .orElse("");
        }

        @Nullable
        public Account<?, ? extends PaymentMethod<?>> getSelectedAccount() {
            return selectedAccount.get();
        }

        public ObjectProperty<Account<?, ? extends PaymentMethod<?>>> selectedAccountProperty() {
            return selectedAccount;
        }

        public void setSelectedAccount(Account<?, ? extends PaymentMethod<?>> selectedAccount) {
            this.selectedAccount.set(selectedAccount);
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final BisqTextArea inputField;
        private final Button sendButton, createOfferButton, sendBtcAddressButton, sendPaymentAccountButton, openDisputeButton, leaveChannelButton;
        private final AutoCompleteComboBox<Account<?, ? extends PaymentMethod<?>>> paymentAccountsComboBox;
        private final ChatMentionPopupMenu<UserProfile> userMentionPopup;
        private final ChatMentionPopupMenu<ChatChannel<?>> channelMentionPopup;
        private final Pane userProfileSelectionRoot;

        private View(Model model,
                     Controller controller,
                     Pane messagesListView,
                     Pane quotedMessageBlock,
                     UserProfileSelection userProfileSelection) {
            super(new VBox(), model, controller);

            inputField = new BisqTextArea();
            inputField.setId("chat-input-field");
            inputField.setPromptText(Res.get("social.chat.input.prompt"));

            sendButton = new Button("", ImageUtil.getImageViewById("chat-send"));
            sendButton.setId("chat-messages-send-button");
            sendButton.setPadding(new Insets(5));
            sendButton.setMinWidth(31);
            sendButton.setMaxWidth(31);

            StackPane.setAlignment(inputField, Pos.CENTER_LEFT);
            StackPane.setAlignment(sendButton, Pos.CENTER_RIGHT);
            StackPane.setMargin(sendButton, new Insets(0, 5, 0, 0));
            StackPane bottomBoxStackPane = new StackPane(inputField, sendButton);

            userProfileSelection.setMaxComboBoxWidth(165);
            userProfileSelection.setConverter(new StringConverter<>() {
                @Override
                public String toString(UserProfileSelection.ListItem item) {
                    return item != null ? StringUtils.truncate(item.getUserIdentity().getUserName(), 10) : "";
                }

                @Override
                public UserProfileSelection.ListItem fromString(String string) {
                    return null;
                }
            });
            userProfileSelectionRoot = userProfileSelection.getRoot();
            userProfileSelectionRoot.setMaxHeight(44);
            userProfileSelectionRoot.setMaxWidth(165);
            userProfileSelectionRoot.setMinWidth(165);
            userProfileSelectionRoot.setId("chat-user-profile-bg");

            HBox.setHgrow(bottomBoxStackPane, Priority.ALWAYS);
            HBox.setMargin(userProfileSelectionRoot, new Insets(0, -20, 0, -25));
            HBox bottomHBox = new HBox(10);

            int height = 32;
            createOfferButton = createAndGetChatButton(Res.get("createOffer"), 120);
            createOfferButton.setDefaultButton(true);

            sendBtcAddressButton = createAndGetChatButton(Res.get("bisqEasy.sendBtcAddress"), 160);
            sendBtcAddressButton.getStyleClass().add("outlined-button");

            sendPaymentAccountButton = createAndGetChatButton(Res.get("bisqEasy.sendPaymentAccount"), 160);
            sendPaymentAccountButton.getStyleClass().add("outlined-button");

            openDisputeButton = createAndGetChatButton(Res.get("bisqEasy.openDispute"), 110);
            openDisputeButton.getStyleClass().add("outlined-button");

            leaveChannelButton = createAndGetChatButton(Res.get("social.privateChannel.leave"), 120);
            leaveChannelButton.getStyleClass().add("outlined-button");

            //todo
            paymentAccountsComboBox = new AutoCompleteComboBox<>(model.getPaymentAccounts());
            paymentAccountsComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(Account<?, ? extends PaymentMethod<?>> object) {
                    return object != null ? object.getAccountName() : "";
                }

                @Override
                public Account<?, ? extends PaymentMethod<?>> fromString(String string) {
                    return null;
                }
            });

            bottomHBox.getChildren().addAll(userProfileSelectionRoot, bottomBoxStackPane, leaveChannelButton,
                    createOfferButton, sendBtcAddressButton, sendPaymentAccountButton, openDisputeButton);
            bottomHBox.getStyleClass().add("bg-grey-5");
            bottomHBox.setAlignment(Pos.CENTER);
            bottomHBox.setPadding(new Insets(14, 25, 14, 25));

            VBox.setVgrow(messagesListView, Priority.ALWAYS);
            root.getChildren().addAll(messagesListView, quotedMessageBlock, bottomHBox);

            userMentionPopup = new ChatMentionPopupMenu<>(inputField);
            userMentionPopup.setItemDisplayConverter(UserProfile::getUserName);
            userMentionPopup.setSelectionHandler(controller::listUserNamesHandler);

            channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
            channelMentionPopup.setItemDisplayConverter(model::getChannelTitle);
            channelMentionPopup.setSelectionHandler(controller::listChannelsHandler);
        }

        @Override
        protected void onViewAttached() {
            leaveChannelButton.visibleProperty().bind(model.getLeaveChannelButtonVisible());
            leaveChannelButton.managedProperty().bind(model.getLeaveChannelButtonVisible());
            createOfferButton.visibleProperty().bind(model.getCreateOfferButtonVisible());
            createOfferButton.managedProperty().bind(model.getCreateOfferButtonVisible());
            openDisputeButton.visibleProperty().bind(model.getOpenDisputeButtonVisible());
            openDisputeButton.managedProperty().bind(model.getOpenDisputeButtonVisible());
            sendBtcAddressButton.visibleProperty().bind(model.getSendBtcAddressButtonVisible());
            sendBtcAddressButton.managedProperty().bind(model.getSendBtcAddressButtonVisible());
            sendPaymentAccountButton.visibleProperty().bind(model.getSendPaymentAccountButtonVisible());
            sendPaymentAccountButton.managedProperty().bind(model.getSendPaymentAccountButtonVisible());
            userProfileSelectionRoot.visibleProperty().bind(model.userProfileSelectionVisible);
            userProfileSelectionRoot.managedProperty().bind(model.userProfileSelectionVisible);
            inputField.textProperty().bindBidirectional(model.getTextInput());
            userMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '@'),
                    inputField.textProperty()
            ));
            channelMentionPopup.filterProperty().bind(Bindings.createStringBinding(
                    () -> StringUtils.deriveWordStartingWith(inputField.getText(), '#'),
                    inputField.textProperty()
            ));

            inputField.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    if (event.isShiftDown()) {
                        inputField.appendText(System.getProperty("line.separator"));
                    } else if (!inputField.getText().isEmpty()) {
                        controller.onSendMessage(inputField.getText().trim());
                        inputField.clear();
                    }
                }
            });
            sendButton.setOnAction(event -> {
                controller.onSendMessage(inputField.getText().trim());
                inputField.clear();
            });
            leaveChannelButton.setOnAction(e -> controller.onLeaveChannel());
            createOfferButton.setOnAction(e -> controller.onCreateOffer());
            sendBtcAddressButton.setOnAction(e -> controller.onSendBtcAddress());
            sendPaymentAccountButton.setOnAction(e -> controller.onSendPaymentAccount());
            openDisputeButton.setOnAction(e -> controller.onRequestMediation());

            paymentAccountsComboBox.setOnChangeConfirmed(e -> {
                if (paymentAccountsComboBox.getSelectionModel().getSelectedItem() == null) {
                    paymentAccountsComboBox.getSelectionModel().select(model.getSelectedAccount());
                    return;
                }
                controller.onPaymentAccountSelected(paymentAccountsComboBox.getSelectionModel().getSelectedItem());
            });

            userMentionPopup.setItems(model.mentionableUsers);
            channelMentionPopup.setItems(model.mentionableChatChannels);
        }

        @Override
        protected void onViewDetached() {
            leaveChannelButton.visibleProperty().unbind();
            leaveChannelButton.managedProperty().unbind();
            createOfferButton.visibleProperty().unbind();
            createOfferButton.managedProperty().unbind();
            openDisputeButton.visibleProperty().unbind();
            openDisputeButton.managedProperty().unbind();
            sendBtcAddressButton.visibleProperty().unbind();
            sendBtcAddressButton.managedProperty().unbind();
            sendPaymentAccountButton.visibleProperty().unbind();
            sendPaymentAccountButton.managedProperty().unbind();
            userProfileSelectionRoot.visibleProperty().unbind();
            userProfileSelectionRoot.managedProperty().unbind();
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();

            inputField.setOnKeyPressed(null);
            sendButton.setOnAction(null);
            leaveChannelButton.setOnAction(null);
            createOfferButton.setOnAction(null);
            sendBtcAddressButton.setOnAction(null);
            sendPaymentAccountButton.setOnAction(null);
            openDisputeButton.setOnAction(null);

            paymentAccountsComboBox.setOnChangeConfirmed(null);
        }
    }

    private static Button createAndGetChatButton(String title, double width) {
        Button button = new Button(title);
        button.setStyle("-fx-label-padding: 0 -30 0 -30; -fx-background-radius: 8; -fx-border-radius: 8;");
        button.setMinHeight(34);
        button.setMinWidth(width);
        return button;
    }
}