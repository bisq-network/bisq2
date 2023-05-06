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
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.PrivateBisqEasyTradeChatChannel;
import bisq.chat.bisqeasy.channel.priv.PrivateBisqEasyTradeChatChannelService;
import bisq.chat.bisqeasy.channel.pub.PublicBisqEasyOfferChatChannel;
import bisq.chat.bisqeasy.channel.pub.PublicBisqEasyOfferChatChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannel;
import bisq.chat.channel.priv.TwoPartyPrivateChatChannelService;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.chat.channel.pub.PublicChatChannel;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.Citation;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.SettingsService;
import bisq.support.MediationService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ChatMessagesComponent {
    private final Controller controller;

    public ChatMessagesComponent(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
        controller = new Controller(applicationService, chatChannelDomain);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void mentionUser(UserProfile userProfile) {
        controller.mentionUser(userProfile);
    }

    public FilteredList<ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> getFilteredChatMessages() {
        return controller.chatMessagesListView.getFilteredChatMessages();
    }

    public void setSearchPredicate(Predicate<? super ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>> predicate) {
        controller.chatMessagesListView.setSearchPredicate(predicate);
    }

    public void setOnShowChatUserDetails(Consumer<UserProfile> handler) {
        controller.model.showChatUserDetailsHandler = Optional.of(handler);
    }

    public void resetSelectedChatMessage() {
        controller.model.selectedChatMessage = null;
    }

    public void openPrivateChannel(UserProfile peer) {
        controller.createAndSelectPrivateChannel(peer);
    }

    public void refreshMessages() {
        controller.chatMessagesListView.refreshMessages();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserIdentityService userIdentityService;
        private final QuotedMessageBlock citationBlock;
        private final ChatMessagesListView chatMessagesListView;
        private final UserProfileService userProfileService;
        private final PrivateBisqEasyTradeChatChannelService privateBisqEasyTradeChatChannelService;
        private final TwoPartyPrivateChatChannelService privateDiscussionChannelService;
        private final CommonPublicChatChannelService publicDiscussionChannelService;
        private final PublicBisqEasyOfferChatChannelService publicBisqEasyOfferChatChannelService;
        private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
        private final ChatChannelSelectionService discussionChatChannelSelectionService;
        private final SettingsService settingsService;
        private final CommonPublicChatChannelService publicEventsChannelService;
        private final TwoPartyPrivateChatChannelService privateEventsChannelService;
        private final ChatChannelSelectionService eventsChatChannelSelectionService;
        private final CommonPublicChatChannelService publicSupportChannelService;
        private final TwoPartyPrivateChatChannelService privateSupportChannelService;
        private final ChatChannelSelectionService supportChatChannelSelectionService;
        private final UserProfileSelection userProfileSelection;
        private final MediationService mediationService;
        private Pin selectedChannelPin;
        private Pin chatMessagesPin;

        private Controller(DefaultApplicationService applicationService,
                           ChatChannelDomain chatChannelDomain) {
            ChatService chatService = applicationService.getChatService();
            publicBisqEasyOfferChatChannelService = chatService.getPublicBisqEasyOfferChatChannelService();
            privateBisqEasyTradeChatChannelService = chatService.getPrivateBisqEasyTradeChatChannelService();
            bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();

            publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
            privateDiscussionChannelService = chatService.getPrivateDiscussionChannelService();
            discussionChatChannelSelectionService = chatService.getDiscussionChatChannelSelectionService();

            publicEventsChannelService = chatService.getPublicEventsChannelService();
            privateEventsChannelService = chatService.getPrivateEventsChannelService();
            eventsChatChannelSelectionService = chatService.getEventsChatChannelSelectionService();

            publicSupportChannelService = chatService.getPublicSupportChannelService();
            privateSupportChannelService = chatService.getPrivateSupportChannelService();
            supportChatChannelSelectionService = chatService.getSupportChatChannelSelectionService();

            settingsService = applicationService.getSettingsService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();
            citationBlock = new QuotedMessageBlock(applicationService);

            userProfileSelection = new UserProfileSelection(userIdentityService);
            mediationService = applicationService.getSupportService().getMediationService();

            chatMessagesListView = new ChatMessagesListView(applicationService,
                    this::mentionUser,
                    this::showChatUserDetails,
                    this::onReply,
                    chatChannelDomain);

            model = new Model(chatChannelDomain);
            view = new View(model, this,
                    chatMessagesListView.getRoot(),
                    citationBlock.getRoot(),
                    userProfileSelection);
        }

        @Override
        public void onActivate() {
            model.mentionableUsers.setAll(userProfileService.getUserProfiles());
            model.mentionableChatChannels.setAll(publicDiscussionChannelService.getMentionableChannels());

            if (model.getChatChannelDomain() == ChatChannelDomain.TRADE) {
                selectedChannelPin = bisqEasyChatChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.DISCUSSION) {
                selectedChannelPin = discussionChatChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.EVENTS) {
                selectedChannelPin = eventsChatChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.SUPPORT) {
                selectedChannelPin = supportChatChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
            }

            Optional.ofNullable(model.selectedChatMessage).ifPresent(this::showChatUserDetails);

            userIdentityService.getUserIdentityChangedFlag().addObserver(__ -> applyUserProfileOrChannelChange());
        }

        @Override
        public void onDeactivate() {
            selectedChannelPin.unbind();
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }
        }


        ///////////////////////////////////////////////////////////////////////////////////////////////////
        // UI
        ///////////////////////////////////////////////////////////////////////////////////////////////////


        private void onSendMessage(String text) {
            if (text == null || text.isEmpty()) {
                return;
            }

            if (model.selectedChannel.get() instanceof PublicChatChannel) {
                List<UserIdentity> myUserProfilesInChannel = getMyUserProfilesInChannel();
                if (myUserProfilesInChannel.size() > 0) {
                    UserIdentity lastUsedUserProfile = myUserProfilesInChannel.get(0);
                    if (!lastUsedUserProfile.equals(userIdentityService.getSelectedUserIdentity().get())) {
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

        private void doSendMessage(String text) {
            ChatChannel<? extends ChatMessage> chatChannel = model.selectedChannel.get();
            UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity().get();
            checkNotNull(userIdentity, "chatUserIdentity must not be null at onSendMessage");
            Optional<Citation> citation = citationBlock.getCitation();
            if (chatChannel instanceof PublicBisqEasyOfferChatChannel) {
                String dontShowAgainId = "sendMsgOfferOnlyWarn";
                if (settingsService.getOffersOnly().get()) {
                    new Popup().information(Res.get("social.chat.sendMsg.offerOnly.popup"))
                            .actionButtonText(Res.get("yes"))
                            .onAction(() -> settingsService.setOffersOnly(false))
                            .closeButtonText(Res.get("no"))
                            .dontShowAgainId(dontShowAgainId)
                            .show();
                }
                publicBisqEasyOfferChatChannelService.publishChatMessage(text, citation, (PublicBisqEasyOfferChatChannel) chatChannel, userIdentity);
            } else if (chatChannel instanceof PrivateBisqEasyTradeChatChannel) {
                if (settingsService.getTradeRulesConfirmed().get() || ((PrivateBisqEasyTradeChatChannel) chatChannel).isMediator()) {
                    privateBisqEasyTradeChatChannelService.sendTextMessage(text, citation, (PrivateBisqEasyTradeChatChannel) chatChannel);
                } else {
                    new Popup().information(Res.get("social.chat.sendMsg.tradeRulesNotConfirmed.popup")).show();
                }
            } else if (chatChannel instanceof CommonPublicChatChannel) {
                switch (chatChannel.getChatChannelDomain()) {
                    case TRADE:
                        break;
                    case DISCUSSION:
                        publicDiscussionChannelService.publishChatMessage(text, citation, (CommonPublicChatChannel) chatChannel, userIdentity);
                        break;
                    case EVENTS:
                        publicEventsChannelService.publishChatMessage(text, citation, (CommonPublicChatChannel) chatChannel, userIdentity);
                        break;
                    case SUPPORT:
                        publicSupportChannelService.publishChatMessage(text, citation, (CommonPublicChatChannel) chatChannel, userIdentity);
                        break;
                }

            } else if (chatChannel instanceof TwoPartyPrivateChatChannel) {
                switch (chatChannel.getChatChannelDomain()) {
                    case TRADE:
                        break;
                    case DISCUSSION:
                        privateDiscussionChannelService.sendTextMessage(text, citation, (TwoPartyPrivateChatChannel) chatChannel);
                        break;
                    case EVENTS:
                        privateEventsChannelService.sendTextMessage(text, citation, (TwoPartyPrivateChatChannel) chatChannel);
                        break;
                    case SUPPORT:
                        privateSupportChannelService.sendTextMessage(text, citation, (TwoPartyPrivateChatChannel) chatChannel);
                        break;
                }
            }
            citationBlock.close();
        }

        private void onReply(ChatMessage chatMessage) {
            if (!userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId())) {
                citationBlock.reply(chatMessage);
            }
        }

        private void fillUserMention(UserProfile user) {
            String content = model.getTextInput().get().replaceAll("@[a-zA-Z\\d]*$", "@" + user.getUserName() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        private void fillChannelMention(ChatChannel<?> chatChannel) {
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z\\d]*$", "#" + chatChannel.getDisplayString() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        private void createAndSelectPrivateChannel(UserProfile peer) {
            if (model.getChatChannelDomain() == ChatChannelDomain.TRADE) {
                // todo use new 2 party channelservice
                // PrivateTradeChannel privateTradeChannel = getPrivateTradeChannel(peer);
                // tradeChannelSelectionService.selectChannel(privateTradeChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.DISCUSSION) {
                privateDiscussionChannelService.maybeCreateAndAddChannel(peer)
                        .ifPresent(discussionChatChannelSelectionService::selectChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.EVENTS) {
                privateEventsChannelService.maybeCreateAndAddChannel(peer)
                        .ifPresent(eventsChatChannelSelectionService::selectChannel);
            } else if (model.getChatChannelDomain() == ChatChannelDomain.SUPPORT) {
                privateSupportChannelService.maybeCreateAndAddChannel(peer)
                        .ifPresent(supportChatChannelSelectionService::selectChannel);
            }
        }

        private void showChatUserDetails(ChatMessage chatMessage) {
            model.selectedChatMessage = chatMessage;
            userProfileService.findUserProfile(chatMessage.getAuthorId()).ifPresent(author ->
                    model.showChatUserDetailsHandler.ifPresent(handler -> handler.accept(author)));
        }

        private void mentionUser(UserProfile userProfile) {
            String existingText = model.getTextInput().get();
            if (!existingText.isEmpty() && !existingText.endsWith(" ")) {
                existingText += " ";
            }
            model.getTextInput().set(existingText + "@" + userProfile.getUserName() + " ");
        }

        private void applySelectedChannel(ChatChannel<? extends ChatMessage> chatChannel) {
            model.selectedChannel.set(chatChannel);
            applyUserProfileOrChannelChange();
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
                    .map(ChatMessage::getAuthorId)
                    .map(userIdentityService::findUserIdentity)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<ChatChannel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final BooleanProperty userProfileSelectionVisible = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private final ObservableList<UserProfile> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<ChatChannel<?>> mentionableChatChannels = FXCollections.observableArrayList();
        private final ChatChannelDomain chatChannelDomain;
        @Nullable
        private ChatMessage selectedChatMessage;
        private Optional<Consumer<UserProfile>> showChatUserDetailsHandler = Optional.empty();

        private Model(ChatChannelDomain chatChannelDomain) {
            this.chatChannelDomain = chatChannelDomain;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final BisqTextArea inputField;
        private final Button sendButton;
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
            StackPane.setMargin(sendButton, new Insets(0, 10, 0, 0));
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
            HBox bottomBox = new HBox(10, userProfileSelectionRoot, bottomBoxStackPane);
            bottomBox.getStyleClass().add("bg-grey-5");
            bottomBox.setAlignment(Pos.CENTER);
            bottomBox.setPadding(new Insets(14, 25, 14, 25));

            VBox.setVgrow(messagesListView, Priority.ALWAYS);
            root.getChildren().addAll(messagesListView, quotedMessageBlock, bottomBox);

            userMentionPopup = new ChatMentionPopupMenu<>(inputField);
            userMentionPopup.setItemDisplayConverter(UserProfile::getUserName);
            userMentionPopup.setSelectionHandler(controller::fillUserMention);

            channelMentionPopup = new ChatMentionPopupMenu<>(inputField);
            channelMentionPopup.setItemDisplayConverter(ChatChannel::getDisplayString);
            channelMentionPopup.setSelectionHandler(controller::fillChannelMention);
        }

        @Override
        protected void onViewAttached() {
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

            userMentionPopup.setItems(model.mentionableUsers);
            channelMentionPopup.setItems(model.mentionableChatChannels);
        }

        @Override
        protected void onViewDetached() {
            userProfileSelectionRoot.visibleProperty().unbind();
            userProfileSelectionRoot.managedProperty().unbind();
            inputField.textProperty().unbindBidirectional(model.getTextInput());
            userMentionPopup.filterProperty().unbind();
            channelMentionPopup.filterProperty().unbind();

            inputField.setOnKeyPressed(null);
            sendButton.setOnAction(null);
        }
    }
}