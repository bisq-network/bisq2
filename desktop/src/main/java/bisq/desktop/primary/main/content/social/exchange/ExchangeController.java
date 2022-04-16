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

package bisq.desktop.primary.main.content.social.exchange;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.primary.main.content.social.components.*;
import bisq.social.chat.ChatService;
import bisq.social.chat.PublicChannel;
import bisq.social.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class ExchangeController implements Controller {
    private final ChatService chatService;
    private final ExchangeModel model;
    @Getter
    private final ExchangeView view;
    private final UserProfileService userProfileService;
    private final PrivateChannelSelection privateChannelSelection;
    private final ChannelInfo channelInfo;
    private final NotificationsSettings notificationsSettings;
    private final QuotedMessageBlock quotedMessageBlock;
    private final MarketChannelSelection marketChannelSelection;
    private final UserProfileSelection userProfileDisplay;
    private final ChatMessagesComponent chatMessagesComponent;
    private Pin selectedChannelPin, tradeTagsPin, currencyTagsPin, paymentMethodTagsPin, customTagsPin;

    private Subscription notificationSettingSubscription;

    public ExchangeController(DefaultApplicationService applicationService) {
        chatService = applicationService.getChatService();
        userProfileService = applicationService.getUserProfileService();

        userProfileDisplay = new UserProfileSelection(userProfileService);
        marketChannelSelection = new MarketChannelSelection(chatService, applicationService.getSettingsService());
        privateChannelSelection = new PrivateChannelSelection(chatService);
        chatMessagesComponent = new ChatMessagesComponent(chatService, userProfileService);
        channelInfo = new ChannelInfo(chatService);
        notificationsSettings = new NotificationsSettings();
        quotedMessageBlock = new QuotedMessageBlock();
        FilterBox filterBox = new FilterBox(chatMessagesComponent.getFilteredChatMessages());
        model = new ExchangeModel(chatService, userProfileService);
        view = new ExchangeView(model,
                this,
                userProfileDisplay.getRoot(),
                marketChannelSelection,
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                notificationsSettings.getRoot(),
                channelInfo.getRoot(),
                filterBox);
    }

    @Override
    public void onActivate() {
        //  chatMessagesComponent.selectedChannelListItemProperty().bind(marketChannelSelection.selectedMarketProperty());

        EasyBind.subscribe(marketChannelSelection.selectedMarketProperty(), item -> {
            if (item != null) {
                model.getSelectedChannelAsString().set(item.getChannel().getId());
                chatMessagesComponent.setSelectedChannelListItem(item);
            }
        });
        // marketSelection.setSelectedMarket(data.market());

        notificationSettingSubscription = EasyBind.subscribe(notificationsSettings.getNotificationSetting(),
                value -> chatService.setNotificationSetting(chatService.getPersistableStore().getSelectedChannel().get(), value));

        selectedChannelPin = chatService.getPersistableStore().getSelectedChannel().addObserver(channel -> {
            if (channel instanceof PublicChannel publicChannel) {
                tradeTagsPin = FxBindings.<String, String>bind(model.getTradeTags()).map(String::toUpperCase).to(publicChannel.getTradeTags());
                currencyTagsPin = FxBindings.<String, String>bind(model.getCurrencyTags()).map(String::toUpperCase).to(publicChannel.getCurrencyTags());
                paymentMethodTagsPin = FxBindings.<String, String>bind(model.getPaymentMethodsTags()).map(String::toUpperCase).to(publicChannel.getPaymentMethodTags());
                customTagsPin = FxBindings.<String, String>bind(model.getCustomTags()).map(String::toUpperCase).to(publicChannel.getCustomTags());
            }

         /*   if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
            }*/
            
          /*  chatMessagesPin = FxBindings.<ChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.getChatMessages())
                    .map(ChatMessageListItem::new) // only Public or PrivatChatmessage possible here
                    .to((ObservableSet<ChatMessage>) channel.getChatMessages());

            model.getSelectedChannelAsString().set(channel.getMarket());*/

            model.getSelectedChannel().set(channel);
            if (model.getChannelInfoVisible().get()) {
                channelInfo.setChannel(channel);
                channelInfo.setOnUndoIgnoreChatUser(() -> {
                    refreshMessages();
                    channelInfo.setChannel(channel);
                });
            }
            if (model.getNotificationsVisible().get()) {
                notificationsSettings.setChannel(channel);
            }


        });
    }

    @Override
    public void onDeactivate() {
        notificationSettingSubscription.unsubscribe();
        selectedChannelPin.unbind();
       // chatMessagesPin.unbind();

        if (tradeTagsPin != null) {
            tradeTagsPin.unbind();
            currencyTagsPin.unbind();
            paymentMethodTagsPin.unbind();
            customTagsPin.unbind();
        }
    }


    public void onToggleFilterBox() {
        boolean visible = !model.getFilterBoxVisible().get();
        model.getFilterBoxVisible().set(visible);
    }

    public void onToggleNotifications() {
        boolean visible = !model.getNotificationsVisible().get();
        model.getNotificationsVisible().set(visible);
        model.getSideBarVisible().set(visible);
        model.getChannelInfoVisible().set(false);
        removeChatUserDetails();
        if (visible) {
            notificationsSettings.setChannel(model.getSelectedChannel().get());
        }
    }

    public void onToggleChannelInfo() {
        boolean visible = !model.getChannelInfoVisible().get();
        model.getChannelInfoVisible().set(visible);
        model.getSideBarVisible().set(visible);
        model.getNotificationsVisible().set(false);
        removeChatUserDetails();
        if (visible) {
            channelInfo.setChannel(model.getSelectedChannel().get());
            channelInfo.setOnUndoIgnoreChatUser(() -> {
                refreshMessages();
                channelInfo.setChannel(model.getSelectedChannel().get());
            });
        }
    }


    private void refreshMessages() {
        //chatMessagesPin.unbind();
      /*  chatMessagesPin = FxBindings.<ChatMessage, ChatMessageListItem<? extends ChatMessage>>bind(model.getChatMessages())
                .map(ChatMessageListItem::new)
                .to((ObservableSet<ChatMessage>) model.getSelectedChannel().get().getChatMessages()); */
        //todo expected type <? extends ChatMessage> does not work ;-(
    }

    public void onCloseSideBar() {
        model.getSideBarVisible().set(false);
        model.getChannelInfoVisible().set(false);
        model.getNotificationsVisible().set(false);
        removeChatUserDetails();
    }

    private void removeChatUserDetails() {
        model.getChatUserDetails().ifPresent(e -> e.setOnMentionUser(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnSendPrivateMessage(null));
        model.getChatUserDetails().ifPresent(e -> e.setOnIgnoreChatUser(null));
        model.setChatUserDetails(Optional.empty());
        model.getChatUserDetailsRoot().set(null);
    }


}
