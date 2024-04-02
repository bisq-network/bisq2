package bisq.desktop.main.content.chat.message_container;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.*;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.desktop.main.content.components.CitationBlock;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChatMessageContainerController implements bisq.desktop.common.view.Controller {
    private final ChatMessageContainerModel model;
    @Getter
    private final ChatMessageContainerView view;
    private final Consumer<UserProfile> openUserProfileSidebarHandler;
    private final UserIdentityService userIdentityService;
    private final CitationBlock citationBlock;
    private final ChatMessagesListController chatMessagesListController;
    private final UserProfileService userProfileService;
    private final SettingsService settingsService;
    private final ChatService chatService;
    private Pin selectedChannelPin, chatMessagesPin, getUserIdentitiesPin;

    public ChatMessageContainerController(ServiceProvider serviceProvider,
                                          ChatChannelDomain chatChannelDomain,
                                          Consumer<UserProfile> openUserProfileSidebarHandler) {
        this.openUserProfileSidebarHandler = openUserProfileSidebarHandler;

        chatService = serviceProvider.getChatService();
        settingsService = serviceProvider.getSettingsService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        citationBlock = new CitationBlock(serviceProvider);

        UserProfileSelection userProfileSelection = new UserProfileSelection(serviceProvider);

        chatMessagesListController = new ChatMessagesListController(serviceProvider,
                this::mentionUserHandler,
                this::showChatUserDetailsHandler,
                this::replyHandler,
                chatChannelDomain);

        model = new ChatMessageContainerModel(chatChannelDomain);
        view = new ChatMessageContainerView(model, this,
                chatMessagesListController.getView().getRoot(),
                citationBlock.getRoot(),
                userProfileSelection);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void resetSelectedChatMessage() {
        model.setSelectedChatMessage(null);
    }

    public void refreshMessages() {
        chatMessagesListController.refreshMessages();
    }

    public void mentionUser(UserProfile userProfile) {
        mentionUserHandler(userProfile);
    }

    public void createAndSelectTwoPartyPrivateChatChannel(UserProfile peer) {
        chatService.createAndSelectTwoPartyPrivateChatChannel(model.getChatChannelDomain(), peer)
                .ifPresent(channel -> {
                    if (model.getChatChannelDomain() == ChatChannelDomain.BISQ_EASY_OFFERBOOK) {
                        Navigation.navigateTo(NavigationTarget.BISQ_EASY_PRIVATE_CHAT);
                    }
                });
    }

    public void enableChatDialog(boolean isEnabled) {
        model.getChatDialogEnabled().set(isEnabled);
    }

    public void setSearchPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
        chatMessagesListController.setSearchPredicate(predicate);
    }

    public void setBisqEasyOfferDirectionOrOwnerFilterPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
        chatMessagesListController.setBisqEasyOfferDirectionOrOwnerFilterPredicate(predicate);
    }

    public void setBisqEasyPeerReputationFilterPredicate(Predicate<? super ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> predicate) {
        chatMessagesListController.setBisqEasyPeerReputationFilterPredicate(predicate);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Controller
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onActivate() {
        model.getMentionableUsers().setAll(userProfileService.getUserProfiles());
        Optional.ofNullable(model.getSelectedChatMessage()).ifPresent(this::showChatUserDetailsHandler);

        getUserIdentitiesPin = userIdentityService.getUserIdentities().addObserver(() -> UIThread.run(this::applyUserProfileOrChannelChange));

        if (selectedChannelPin != null) {
            selectedChannelPin.unbind();
        }
        ChatChannelSelectionService selectionService = chatService.getChatChannelSelectionServices().get(model.getChatChannelDomain());
        selectedChannelPin = selectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);
    }

    @Override
    public void onDeactivate() {
        if (selectedChannelPin != null) {
            selectedChannelPin.unbind();
            selectedChannelPin = null;
        }
        getUserIdentitiesPin.unbind();
        if (chatMessagesPin != null) {
            chatMessagesPin.unbind();
        }

        model.getSelectedChannel().set(null);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Handlers passed to list view component
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void replyHandler(ChatMessage chatMessage) {
        if (!chatMessage.isMyMessage(userIdentityService)) {
            citationBlock.reply(chatMessage);
            // To ensure that we trigger an update we set it to null first (and don't use a
            // BooleanProperty but ObjectProperty<Boolean>
            model.getFocusInputTextField().set(null);
            model.getFocusInputTextField().set(true);
        }
    }

    private void showChatUserDetailsHandler(ChatMessage chatMessage) {
        model.setSelectedChatMessage(chatMessage);
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


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // UI handlers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    void onSendMessage(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (model.getSelectedChannel().get() instanceof PublicChatChannel) {
            List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
            if (!myUserProfilesInChannel.isEmpty()) {
                UserIdentity lastUsedUserProfile = myUserProfilesInChannel.get(0);
                if (!lastUsedUserProfile.equals(userIdentityService.getSelectedUserIdentity())) {
                    new Popup().warning(Res.get("chat.message.send.differentUserProfile.warn"))
                            .closeButtonText(Res.get("confirmation.no"))
                            .actionButtonText(Res.get("confirmation.yes"))
                            .onAction(() -> doSendMessage(text))
                            .show();
                    return;
                }
            }
        }

        doSendMessage(text);
    }

    void onListUserNames(UserProfile user) {
        String content = model.getTextInput().get().replaceAll("@[a-zA-Z\\d]*$", "@" + user.getUserName() + " ");
        model.getTextInput().set(content);
        model.getCaretPosition().set(content.length());
    }

    void onListChannels(ChatChannel<?> chatChannel) {
        String channelTitle = chatService.findChatChannelService(chatChannel)
                .map(service -> service.getChannelTitle(chatChannel))
                .orElse("");
        String content = model.getTextInput().get().replaceAll("#[a-zA-Z\\d]*$", "#" + channelTitle + " ");
        model.getTextInput().set(content);
        model.getCaretPosition().set(content.length());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void applyUserProfileOrChannelChange() {
        boolean multipleProfiles = userIdentityService.getUserIdentities().size() > 1;
        ChatChannel<?> selectedChatChannel = model.getSelectedChannel().get();
        model.getUserProfileSelectionVisible().set(multipleProfiles && selectedChatChannel instanceof PublicChatChannel);

        if (chatMessagesPin != null) {
            chatMessagesPin.unbind();
        }

        if (selectedChatChannel != null) {
            chatMessagesPin = selectedChatChannel.getChatMessages().addObserver(this::maybeSwitchUserProfile);
        }
    }


    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        UIThread.run(() -> {
            model.getSelectedChannel().set(chatChannel);
            applyUserProfileOrChannelChange();
        });
    }

    private void doSendMessage(String text) {
        if (text.length() > ChatMessage.MAX_TEXT_LENGTH) {
            new Popup().warning(Res.get("validation.tooLong", ChatMessage.MAX_TEXT_LENGTH)).show();
            return;
        }

        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel().get();
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity(), "user identity must not be null");
        Optional<Citation> citation = citationBlock.getCitation();

        if (citation.isPresent() && citation.get().getText().length() > Citation.MAX_TEXT_LENGTH) {
            new Popup().warning(Res.get("validation.tooLong", Citation.MAX_TEXT_LENGTH)).show();
            return;
        }

        if (chatChannel instanceof BisqEasyOfferbookChannel) {
            String dontShowAgainId = "sendMsgOfferOnlyWarn";
            if (settingsService.getOffersOnly().get()) {
                new Popup().information(Res.get("chat.message.send.offerOnly.warn"))
                        .actionButtonText(Res.get("confirmation.yes"))
                        .onAction(() -> settingsService.getOffersOnly().set(false))
                        .closeButtonText(Res.get("confirmation.no"))
                        .dontShowAgainId(dontShowAgainId)
                        .show();
            }
            chatService.getBisqEasyOfferbookChannelService().publishChatMessage(text, citation, (BisqEasyOfferbookChannel) chatChannel, userIdentity);
        } else if (chatChannel instanceof BisqEasyOpenTradeChannel) {
            if (settingsService.getTradeRulesConfirmed().get() || ((BisqEasyOpenTradeChannel) chatChannel).isMediator()) {
                chatService.getBisqEasyOpenTradeChannelService().sendTextMessage(text, citation, (BisqEasyOpenTradeChannel) chatChannel);
            } else {
                new Popup().information(Res.get("bisqEasy.tradeGuide.notConfirmed.warn"))
                        .actionButtonText(Res.get("bisqEasy.tradeGuide.open"))
                        .onAction(() -> Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE))
                        .show();
            }
        } else if (chatChannel instanceof CommonPublicChatChannel) {
            chatService.getCommonPublicChatChannelServices().get(model.getChatChannelDomain()).publishChatMessage(text, citation, (CommonPublicChatChannel) chatChannel, userIdentity);
        } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
            chatService.getTwoPartyPrivateChatChannelServices().get(model.getChatChannelDomain()).sendTextMessage(text, citation, (TwoPartyPrivateChatChannel) chatChannel);
        }

        citationBlock.close();
    }

    private void maybeSwitchUserProfile() {
        if (model.getUserProfileSelectionVisible().get()) {
            List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
            if (!myUserProfilesInChannel.isEmpty()) {
                userIdentityService.selectChatUserIdentity(myUserProfilesInChannel.get(0));
            }
        }
    }

    private List<UserIdentity> getMyUserProfilesInChannel() {
        return model.getSelectedChannel().get().getChatMessages().stream()
                .sorted(Comparator.comparing(ChatMessage::getDate).reversed())
                .map(ChatMessage::getAuthorUserProfileId)
                .map(userIdentityService::findUserIdentity)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());
    }
}
