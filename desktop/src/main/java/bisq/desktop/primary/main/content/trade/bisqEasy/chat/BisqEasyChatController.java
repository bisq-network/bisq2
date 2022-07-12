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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.Channel;
import bisq.chat.trade.TradeChannelSelectionService;
import bisq.chat.trade.priv.PrivateTradeChannel;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.chat.message.ChatMessage;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.primary.main.content.ChatController;
import bisq.desktop.primary.main.content.components.PublicTradeChannelSelection;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;

@Slf4j
public class BisqEasyChatController extends ChatController<BisqEasyChatView, BisqEasyChatModel> implements Controller {
    private final PublicTradeChannelService publicTradeChannelService;
    private final TradeChannelSelectionService tradeChannelSelectionService;
    private PublicTradeChannelSelection publicTradeChannelSelection;

    public BisqEasyChatController(DefaultApplicationService applicationService) {
        super(applicationService, false);

        publicTradeChannelService = chatService.getPublicTradeChannelService();
        tradeChannelSelectionService = chatService.getTradeChannelSelectionService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        model.getOfferOnly().set(true);
        notificationSettingSubscription = EasyBind.subscribe(notificationsSettings.getNotificationSetting(),
                value -> {
                    Channel<? extends ChatMessage> channel = tradeChannelSelectionService.getSelectedChannel().get();
                    if (channel != null) {
                        publicTradeChannelService.setNotificationSetting(channel, value);
                    }
                });

        selectedChannelPin = tradeChannelSelectionService.getSelectedChannel().addObserver(this::handleChannelChange);
    }

    @Override
    public void createComponents() {
        publicTradeChannelSelection = new PublicTradeChannelSelection(applicationService);
    }

    @Override
    public BisqEasyChatModel getChatModel(boolean isDiscussionsChat) {
        return new BisqEasyChatModel(isDiscussionsChat);
    }

    @Override
    public BisqEasyChatView getChatView() {
        return new BisqEasyChatView(model,
                this,
                publicTradeChannelSelection.getRoot(),
                privateChannelSelection.getRoot(),
                chatMessagesComponent.getRoot(),
                notificationsSettings.getRoot(),
                channelInfo.getRoot(),
                helpPane.getRoot(),
                filterBox);
    }

    @Override
    protected void handleChannelChange(Channel<? extends ChatMessage> channel) {
        super.handleChannelChange(channel);

        if (channel instanceof PrivateTradeChannel) {
            model.getPeersRoboIconImage().set(RoboHash.getImage(((PrivateTradeChannel) channel).getPeer().getPubKeyHash()));
            model.getPeersRoboIconVisible().set(true);
            model.getCreateOfferButtonVisible().set(false);
            publicTradeChannelSelection.deSelectChannel();
        } else {
            model.getPeersRoboIconVisible().set(false);
            model.getCreateOfferButtonVisible().set(true);
            privateChannelSelection.deSelectChannel();
        }
    }

    public void onToggleOffersOnly(boolean selected) {
        model.getOfferOnly().set(selected);
        //todo filter messages
    }
}
