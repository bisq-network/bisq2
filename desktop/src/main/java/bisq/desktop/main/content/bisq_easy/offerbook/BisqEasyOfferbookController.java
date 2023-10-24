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

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
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
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.SettingsService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BisqEasyOfferbookController extends ChatController<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private final BisqEasyOfferbookSelectionService bisqEasyOfferbookSelectionService;
    private final SettingsService settingsService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private Pin selectedChannelPin, offerOnlySettingsPin, bisqEasyPrivateTradeChatChannelsPin;
    private Subscription searchTextPin;

    public BisqEasyOfferbookController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_OFFERBOOK, NavigationTarget.BISQ_EASY_OFFERBOOK);

        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookSelectionService = chatService.getBisqEasyOfferbookChannelSelectionService();
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
        model.getSearchText().set("");
        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                chatMessagesComponent.setSearchPredicate(item -> true);
            } else {
                chatMessagesComponent.setSearchPredicate(item -> item.match(searchText));
            }
        });
        selectedChannelPin = bisqEasyOfferbookSelectionService.getSelectedChannel().addObserver(this::chatChannelChanged);
        offerOnlySettingsPin = FxBindings.bindBiDir(model.getOfferOnly()).to(settingsService.getOffersOnly());

        ObservableArray<BisqEasyOpenTradeChannel> bisqEasyOpenTradeChannels = chatService.getBisqEasyOpenTradeChannelService().getChannels();
        bisqEasyPrivateTradeChatChannelsPin = bisqEasyOpenTradeChannels.addObserver(() ->
                model.getIsTradeChannelVisible().set(!bisqEasyOpenTradeChannels.isEmpty()));

        List<MarketChannelItem> marketChannelItems = bisqEasyOfferbookChannelService.getChannels().stream()
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

            Comparator<MarketChannelItem> byName = Comparator.comparing(MarketChannelItem::getMarketString);
            return byNumMessages
                    .thenComparing(byMajorMarkets)
                    .thenComparing(byName)
                    .compare(o1, o2);
        });
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
        offerOnlySettingsPin.unbind();
        bisqEasyPrivateTradeChatChannelsPin.unbind();

        resetSelectedChildTarget();
    }

    @Override
    protected void chatChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.chatChannelChanged(chatChannel);

        if (chatChannel instanceof BisqEasyOfferbookChannel) {
            UIThread.run(() -> {
                updateMarketItemsPredicate();

                model.getSearchText().set("");
                resetSelectedChildTarget();

                Market market = ((BisqEasyOfferbookChannel) chatChannel).getMarket();
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
        checkArgument(chatChannel instanceof BisqEasyOfferbookChannel,
                "channel must be instanceof BisqEasyPublicChatChannel at onCreateOfferButtonClicked");
        Navigation.navigateTo(NavigationTarget.TRADE_WIZARD, new TradeWizardController.InitData(true));
    }

    void onToggleFilter() {
        bisqEasyOfferbookModel.getShowFilterOverlay().set(!bisqEasyOfferbookModel.getShowFilterOverlay().get());
    }

    void onCloseFilter() {
        bisqEasyOfferbookModel.getShowFilterOverlay().set(false);
    }

    void onSwitchMarketChannel(MarketChannelItem marketChannelItem) {
        if (marketChannelItem != null) {
            bisqEasyOfferbookChannelService.findChannel(marketChannelItem.getMarket())
                    .ifPresent(channel -> {
                        if (bisqEasyOfferbookSelectionService.getSelectedChannel().get() != null) {
                            bisqEasyOfferbookChannelService.leaveChannel(bisqEasyOfferbookSelectionService.getSelectedChannel().get().getId());
                        }
                        bisqEasyOfferbookChannelService.joinChannel(channel);
                        bisqEasyOfferbookSelectionService.selectChannel(channel);
                    });
            updateMarketItemsPredicate();
        }
    }


    int getNumMessages(Market market) {
        return bisqEasyOfferbookChannelService.findChannel(market)
                .map(channel -> (int) channel.getChatMessages().stream()
                        .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                        .count())
                .orElse(0);
    }

    private void updateMarketItemsPredicate() {
        model.getFilteredMarketChannelItems().setPredicate(item -> !bisqEasyOfferbookChannelService.isVisible(item.getChannel()));
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }
}
