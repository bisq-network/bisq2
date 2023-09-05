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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.message.ChatMessage;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableArray;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.overlay.bisq_easy.create_offer.CreateOfferController;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.SettingsService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyOfferbookController extends ChatController<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private final SettingsService settingsService;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private Pin offerOnlySettingsPin, bisqEasyPrivateTradeChatChannelsPin;

    public BisqEasyOfferbookController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY, NavigationTarget.BISQ_EASY_OFFERBOOK);

        bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        settingsService = serviceProvider.getSettingsService();
        bisqEasyOfferbookModel = getModel();
    }

    @Override
    public void createDependencies(ChatChannelDomain chatChannelDomain) {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            default: {
                return Optional.empty();
            }
        }
    }

    @Override
    public BisqEasyOfferbookModel createAndGetModel(ChatChannelDomain chatChannelDomain) {
        return new BisqEasyOfferbookModel(chatChannelDomain);
    }

    @Override
    public BisqEasyOfferbookView createAndGetView() {
        return new BisqEasyOfferbookView(model,
                this,
                chatMessagesComponent.getRoot(),
                channelSidebar.getRoot());
    }

    @Override
    public void onActivate() {
        super.onActivate();

        selectedChannelPin = bisqEasyChatChannelSelectionService.getSelectedChannel().addObserver(this::chatChannelChanged);
        offerOnlySettingsPin = FxBindings.bindBiDir(model.getOfferOnly()).to(settingsService.getOffersOnly());

        ObservableArray<BisqEasyPrivateTradeChatChannel> bisqEasyPrivateTradeChatChannels = chatService.getBisqEasyPrivateTradeChatChannelService().getChannels();
        bisqEasyPrivateTradeChatChannelsPin = bisqEasyPrivateTradeChatChannels.addListener(() ->
                model.getIsTradeChannelVisible().set(!bisqEasyPrivateTradeChatChannels.isEmpty()));

        List<MarketChannelItem> marketChannelItems = bisqEasyPublicChatChannelService.getChannels().stream()
                .map(MarketChannelItem::new)
                .collect(Collectors.toList());
        model.getMarketChannelItems().setAll(marketChannelItems);

        updateMarketItemsPredicate();

        model.getSortedMarketChannelItems().setComparator((o1, o2) -> {
            Comparator<MarketChannelItem> byNumMessages = (left, right) -> Integer.compare(
                    getNumMessages(right.getMarket()),
                    getNumMessages(left.getMarket()));

            List<Market> majorMarkets = MarketRepository.getMajorMarkets();
            Comparator<MarketChannelItem> byMajorMarkets = (left, right) -> {
                int indexOfLeftMarket = majorMarkets.indexOf(left.getMarket());
                int indexOfRightMarket = majorMarkets.indexOf(right.getMarket());
                if (indexOfLeftMarket > -1 && indexOfRightMarket > -1) {
                    return Integer.compare(indexOfLeftMarket, indexOfRightMarket);
                } else {
                    return -1;
                }
            };

            Comparator<MarketChannelItem> byName = (left, right) -> left.getMarket().toString().compareTo(right.toString());
            return byNumMessages
                    .thenComparing(byMajorMarkets)
                    .thenComparing(byName)
                    .compare(o1, o2);
        });
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        offerOnlySettingsPin.unbind();
        bisqEasyPrivateTradeChatChannelsPin.unbind();

        resetSelectedChildTarget();
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.chatChannelChanged(chatChannel);

        if (chatChannel instanceof BisqEasyPublicChatChannel) {
            UIThread.run(() -> {
                resetSelectedChildTarget();

                Market market = ((BisqEasyPublicChatChannel) chatChannel).getMarket();
                StackPane marketsImage = MarketImageComposition.imageBoxForMarket(
                        market.getBaseCurrencyCode().toLowerCase(),
                        market.getQuoteCurrencyCode().toLowerCase()).getFirst();

                //todo get larger icons and dont use scaling
                marketsImage.setScaleX(1.25);
                marketsImage.setScaleY(1.25);
                model.getChannelIconNode().set(marketsImage);
            });
        }
    }

    void onCreateOffer() {
        ChatChannel<? extends ChatMessage> chatChannel = model.getSelectedChannel();
        checkArgument(chatChannel instanceof BisqEasyPublicChatChannel,
                "channel must be instanceof BisqEasyPublicChatChannel at onCreateOfferButtonClicked");
        Navigation.navigateTo(NavigationTarget.CREATE_OFFER, new CreateOfferController.InitData(false));
    }

    void onToggleFilter() {
        bisqEasyOfferbookModel.getShowFilterOverlay().set(!bisqEasyOfferbookModel.getShowFilterOverlay().get());
    }

    void onCloseFilter() {
        bisqEasyOfferbookModel.getShowFilterOverlay().set(false);
    }

    void onSwitchMarketChannel(MarketChannelItem marketChannelItem) {
        if (marketChannelItem != null) {
            bisqEasyPublicChatChannelService.findChannel(marketChannelItem.getMarket())
                    .ifPresent(channel -> {
                        if (bisqEasyChatChannelSelectionService.getSelectedChannel() != null) {
                            bisqEasyPublicChatChannelService.leaveChannel(bisqEasyChatChannelSelectionService.getSelectedChannel().get().getId());
                        }
                        bisqEasyPublicChatChannelService.joinChannel(channel);
                        bisqEasyChatChannelSelectionService.selectChannel(channel);
                    });
            updateMarketItemsPredicate();
        }
    }


    int getNumMessages(Market market) {
        return bisqEasyPublicChatChannelService.findChannel(market)
                .map(channel -> channel.getChatMessages().size())
                .orElse(0);
    }

    private void updateMarketItemsPredicate() {
        model.getFilteredMarketChannelItems().setPredicate(item -> !bisqEasyPublicChatChannelService.isVisible(item.getChannel()));
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }
}
