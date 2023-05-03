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
import bisq.chat.channel.*;
import bisq.chat.message.ChatMessage;
import bisq.chat.message.Quotation;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.priv.PrivateTradeChannelService;
import bisq.chat.trade.pub.PublicTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
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

    public ChatMessagesComponent(DefaultApplicationService applicationService, ChannelDomain channelDomain) {
        controller = new Controller(applicationService, channelDomain);
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
        private final QuotedMessageBlock quotedMessageBlock;
        private final ChatMessagesListView chatMessagesListView;
        private final UserProfileService userProfileService;
        private final PrivateTradeChannelService privateTradeChannelService;
        private final PrivateTwoPartyChannelService privateDiscussionChannelService;
        private final PublicChatChannelService publicDiscussionChannelService;
        private final PublicTradeChannelService publicTradeChannelService;
        private final TradeChannelSelectionService tradeChannelSelectionService;
        private final ChannelSelectionService discussionChannelSelectionService;
        private final SettingsService settingsService;
        private final PublicChatChannelService publicEventsChannelService;
        private final PrivateTwoPartyChannelService privateEventsChannelService;
        private final ChannelSelectionService eventsChannelSelectionService;
        private final PublicChatChannelService publicSupportChannelService;
        private final PrivateTwoPartyChannelService privateSupportChannelService;
        private final ChannelSelectionService supportChannelSelectionService;
        private final UserProfileSelection userProfileSelection;
        private final MediationService mediationService;
        private Pin selectedChannelPin;
        private Pin chatMessagesPin;

        private Controller(DefaultApplicationService applicationService,
                           ChannelDomain channelDomain) {
            ChatService chatService = applicationService.getChatService();
            publicTradeChannelService = chatService.getPublicTradeChannelService();
            privateTradeChannelService = chatService.getPrivateTradeChannelService();
            tradeChannelSelectionService = chatService.getTradeChannelSelectionService();

            publicDiscussionChannelService = chatService.getPublicDiscussionChannelService();
            privateDiscussionChannelService = chatService.getPrivateDiscussionChannelService();
            discussionChannelSelectionService = chatService.getDiscussionChannelSelectionService();

            publicEventsChannelService = chatService.getPublicEventsChannelService();
            privateEventsChannelService = chatService.getPrivateEventsChannelService();
            eventsChannelSelectionService = chatService.getEventsChannelSelectionService();

            publicSupportChannelService = chatService.getPublicSupportChannelService();
            privateSupportChannelService = chatService.getPrivateSupportChannelService();
            supportChannelSelectionService = chatService.getSupportChannelSelectionService();

            settingsService = applicationService.getSettingsService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            userProfileService = applicationService.getUserService().getUserProfileService();
            quotedMessageBlock = new QuotedMessageBlock(applicationService);

            userProfileSelection = new UserProfileSelection(userIdentityService);
            mediationService = applicationService.getSupportService().getMediationService();

            chatMessagesListView = new ChatMessagesListView(applicationService,
                    this::mentionUser,
                    this::showChatUserDetails,
                    this::onReply,
                    channelDomain);

            model = new Model(channelDomain);
            view = new View(model, this,
                    chatMessagesListView.getRoot(),
                    quotedMessageBlock.getRoot(),
                    userProfileSelection);
        }

        @Override
        public void onActivate() {
            model.mentionableUsers.setAll(userProfileService.getUserProfiles());
            model.mentionableChannels.setAll(publicDiscussionChannelService.getMentionableChannels());

            if (model.getChannelDomain() == ChannelDomain.TRADE) {
                selectedChannelPin = tradeChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
            } else if (model.getChannelDomain() == ChannelDomain.DISCUSSION) {
                selectedChannelPin = discussionChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
            } else if (model.getChannelDomain() == ChannelDomain.EVENTS) {
                selectedChannelPin = eventsChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
            } else if (model.getChannelDomain() == ChannelDomain.SUPPORT) {
                selectedChannelPin = supportChannelSelectionService.getSelectedChannel().addObserver(this::applySelectedChannel);
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

            if (model.selectedChannel.get() instanceof BasePublicChannel) {
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
            Channel<? extends ChatMessage> channel = model.selectedChannel.get();
            UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity().get();
            checkNotNull(userIdentity, "chatUserIdentity must not be null at onSendMessage");
            Optional<Quotation> quotation = quotedMessageBlock.getQuotation();
            if (channel instanceof PublicTradeChannel) {
                String dontShowAgainId = "sendMsgOfferOnlyWarn";
                if (settingsService.getOffersOnly().get()) {
                    new Popup().information(Res.get("social.chat.sendMsg.offerOnly.popup"))
                            .actionButtonText(Res.get("yes"))
                            .onAction(() -> settingsService.setOffersOnly(false))
                            .closeButtonText(Res.get("no"))
                            .dontShowAgainId(dontShowAgainId)
                            .show();
                }
                publicTradeChannelService.publishChatMessage(text, quotation, (PublicTradeChannel) channel, userIdentity);
            } else if (channel instanceof PrivateTradeChannel) {
                if (settingsService.getTradeRulesConfirmed().get() || ((PrivateTradeChannel) channel).isMediator()) {
                    privateTradeChannelService.sendPrivateChatMessage(text, quotation, (PrivateTradeChannel) channel);
                } else {
                    new Popup().information(Res.get("social.chat.sendMsg.tradeRulesNotConfirmed.popup")).show();
                }
            } else if (channel instanceof PublicChatChannel) {
                switch (channel.getChannelDomain()) {
                    case TRADE:
                        break;
                    case DISCUSSION:
                        publicDiscussionChannelService.publishChatMessage(text, quotation, (PublicChatChannel) channel, userIdentity);
                        break;
                    case EVENTS:
                        publicEventsChannelService.publishChatMessage(text, quotation, (PublicChatChannel) channel, userIdentity);
                        break;
                    case SUPPORT:
                        publicSupportChannelService.publishChatMessage(text, quotation, (PublicChatChannel) channel, userIdentity);
                        break;
                }

            } else if (channel instanceof PrivateTwoPartyChannel) {
                switch (channel.getChannelDomain()) {
                    case TRADE:
                        break;
                    case DISCUSSION:
                        privateDiscussionChannelService.sendPrivateChatMessage(text, quotation, (PrivateTwoPartyChannel) channel);
                        break;
                    case EVENTS:
                        privateEventsChannelService.sendPrivateChatMessage(text, quotation, (PrivateTwoPartyChannel) channel);
                        break;
                    case SUPPORT:
                        privateSupportChannelService.sendPrivateChatMessage(text, quotation, (PrivateTwoPartyChannel) channel);
                        break;
                }
            }
            quotedMessageBlock.close();
        }

        private void onReply(ChatMessage chatMessage) {
            if (!userIdentityService.isUserIdentityPresent(chatMessage.getAuthorId())) {
                quotedMessageBlock.reply(chatMessage);
            }
        }

        private void fillUserMention(UserProfile user) {
            String content = model.getTextInput().get().replaceAll("@[a-zA-Z\\d]*$", "@" + user.getUserName() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        private void fillChannelMention(Channel<?> channel) {
            String content = model.getTextInput().get().replaceAll("#[a-zA-Z\\d]*$", "#" + channel.getDisplayString() + " ");
            model.getTextInput().set(content);
            //todo
            view.inputField.positionCaret(content.length());
        }

        private void createAndSelectPrivateChannel(UserProfile peer) {
            if (model.getChannelDomain() == ChannelDomain.TRADE) {
                PrivateTradeChannel privateTradeChannel = getPrivateTradeChannel(peer);
                tradeChannelSelectionService.selectChannel(privateTradeChannel);
            } else if (model.getChannelDomain() == ChannelDomain.DISCUSSION) {
                privateDiscussionChannelService.maybeCreateAndAddChannel(peer)
                        .ifPresent(discussionChannelSelectionService::selectChannel);
            } else if (model.getChannelDomain() == ChannelDomain.EVENTS) {
                privateEventsChannelService.maybeCreateAndAddChannel(peer)
                        .ifPresent(eventsChannelSelectionService::selectChannel);
            } else if (model.getChannelDomain() == ChannelDomain.SUPPORT) {
                privateSupportChannelService.maybeCreateAndAddChannel(peer)
                        .ifPresent(supportChannelSelectionService::selectChannel);
            }
        }

        private PrivateTradeChannel getPrivateTradeChannel(UserProfile peersUserProfile) {
            UserIdentity myUserIdentity = userIdentityService.getSelectedUserIdentity().get();
            Optional<UserProfile> mediator = mediationService.selectMediator(myUserIdentity.getUserProfile().getId(), peersUserProfile.getId());
            return privateTradeChannelService.traderCreatesNewChannel(myUserIdentity, peersUserProfile, mediator);
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

        private void applySelectedChannel(Channel<? extends ChatMessage> channel) {
            model.selectedChannel.set(channel);
            applyUserProfileOrChannelChange();
        }

        private void applyUserProfileOrChannelChange() {
            boolean multipleProfiles = userIdentityService.getUserIdentities().size() > 1;
            Channel<?> selectedChannel = model.selectedChannel.get();
            model.userProfileSelectionVisible.set(multipleProfiles && selectedChannel instanceof BasePublicChannel);

            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }

            if (selectedChannel != null) {
                chatMessagesPin = selectedChannel.getChatMessages().addListener(this::maybeSwitchUserProfile);
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
        private final ObjectProperty<Channel<?>> selectedChannel = new SimpleObjectProperty<>();
        private final StringProperty textInput = new SimpleStringProperty("");
        private final BooleanProperty userProfileSelectionVisible = new SimpleBooleanProperty();
        private final ObjectProperty<ChatMessage> moreOptionsVisibleMessage = new SimpleObjectProperty<>(null);
        private final ObservableList<UserProfile> mentionableUsers = FXCollections.observableArrayList();
        private final ObservableList<Channel<?>> mentionableChannels = FXCollections.observableArrayList();
        private final ChannelDomain channelDomain;
        @Nullable
        private ChatMessage selectedChatMessage;
        private Optional<Consumer<UserProfile>> showChatUserDetailsHandler = Optional.empty();

        private Model(ChannelDomain channelDomain) {
            this.channelDomain = channelDomain;
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public final static String EDITED_POST_FIX = " " + Res.get("social.message.wasEdited");

        private final BisqTextArea inputField;
        private final Button sendButton;
        private final ChatMentionPopupMenu<UserProfile> userMentionPopup;
        private final ChatMentionPopupMenu<Channel<?>> channelMentionPopup;
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
            channelMentionPopup.setItemDisplayConverter(Channel::getDisplayString);
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
            channelMentionPopup.setItems(model.mentionableChannels);
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