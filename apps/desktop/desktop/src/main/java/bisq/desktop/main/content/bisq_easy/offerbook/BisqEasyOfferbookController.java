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

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableArray;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.SettingsService;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public final class BisqEasyOfferbookController extends ChatController<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private final SettingsService settingsService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private Pin offerOnlySettingsPin, bisqEasyPrivateTradeChatChannelsPin, selectedChannelPin;
    private Subscription marketSelectorSearchPin, selectedMarketFilterPin, selectedOffersFilterPin;

    public BisqEasyOfferbookController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_OFFERBOOK, NavigationTarget.BISQ_EASY_OFFERBOOK);

        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        settingsService = serviceProvider.getSettingsService();
        bisqEasyOfferbookModel = getModel();

        createMarketChannels();
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

        model.getMarketSelectorSearchText().set("");

        offerOnlySettingsPin = FxBindings.bindBiDir(model.getOfferOnly()).to(settingsService.getOffersOnly());

        ObservableArray<BisqEasyOpenTradeChannel> bisqEasyOpenTradeChannels = chatService.getBisqEasyOpenTradeChannelService().getChannels();
        bisqEasyPrivateTradeChatChannelsPin = bisqEasyOpenTradeChannels.addObserver(() ->
                model.getIsTradeChannelVisible().set(!bisqEasyOpenTradeChannels.isEmpty()));

        selectedChannelPin = FxBindings.subscribe(selectionService.getSelectedChannel(), this::selectedChannelChanged);

        marketSelectorSearchPin = EasyBind.subscribe(model.getMarketSelectorSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                model.getFilteredMarketChannelItems().setPredicate(item -> true);
            } else {
                String search = searchText.toLowerCase();
                model.getFilteredMarketChannelItems().setPredicate(item ->
                        item != null &&
                                (item.getMarket().getQuoteCurrencyCode().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyName().toLowerCase().contains(search))
                );
            }
        });

        selectedMarketFilterPin = EasyBind.subscribe(model.getSelectedMarketsFilter(), filter -> {
            if (filter == null) {
                // By default, show only markets with offers
                model.getSelectedMarketsFilter().set(Filters.Markets.WITH_OFFERS);
                model.getFilteredMarketChannelItems().setPredicate(model.getSelectedMarketsFilter().get().getPredicate());
            } else {
                model.getFilteredMarketChannelItems().setPredicate(filter.getPredicate());
            }
        });

        selectedOffersFilterPin = EasyBind.subscribe(model.getSelectedOffersFilter(), filter -> {
            if (filter == null) {
                // By default, show all offers
                model.getSelectedOffersFilter().set(Filters.Offers.ALL);
                chatMessagesComponent.setBisqEasyOffersFilerPredicate(model.getSelectedOffersFilter().get().getPredicate());
            } else {
                chatMessagesComponent.setBisqEasyOffersFilerPredicate(filter.getPredicate());
            }
        });

        updateMarketItemsPredicate();
        maybeSelectFirst();
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        offerOnlySettingsPin.unbind();
        bisqEasyPrivateTradeChatChannelsPin.unbind();
        selectedChannelPin.unbind();
        marketSelectorSearchPin.unsubscribe();
        selectedMarketFilterPin.unsubscribe();
        selectedOffersFilterPin.unsubscribe();

        resetSelectedChildTarget();
    }

    @Override
    protected void selectedChannelChanged(ChatChannel<? extends ChatMessage> chatChannel) {
        super.selectedChannelChanged(chatChannel);

        UIThread.run(() -> {
            if (chatChannel == null) {
                model.getSelectedMarketChannelItem().set(null);
                maybeSelectFirst();
            }

            if (chatChannel instanceof BisqEasyOfferbookChannel) {
                BisqEasyOfferbookChannel channel = (BisqEasyOfferbookChannel) chatChannel;

                // FIXME (low prio): marketChannelItems needs to be a hashmap
                model.getMarketChannelItems().stream()
                        .filter(item -> item.getChannel().equals(channel))
                        .findAny()
                        .ifPresent(item -> model.getSelectedMarketChannelItem().set(item));

                updateMarketItemsPredicate();

                model.getSearchText().set("");
                resetSelectedChildTarget();

                String description = ((BisqEasyOfferbookChannel) chatChannel).getDescription();
                String oneLineDescription = description.replace("\n", " ");
                model.getChannelDescription().set(oneLineDescription);

                Market market = ((BisqEasyOfferbookChannel) chatChannel).getMarket();
                StackPane marketsImage = MarketImageComposition.imageBoxForMarkets(
                        market.getBaseCurrencyCode().toLowerCase(),
                        market.getQuoteCurrencyCode().toLowerCase());
                model.getChannelIconNode().set(marketsImage);
            }
        });
    }

    private void createMarketChannels() {
        List<MarketChannelItem> marketChannelItems = bisqEasyOfferbookChannelService.getChannels().stream()
                .map(MarketChannelItem::new)
                .collect(Collectors.toList());
        model.getMarketChannelItems().setAll(marketChannelItems);
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

    private void updateMarketItemsPredicate() {
        //model.getFilteredMarketChannelItems().setPredicate(item -> !bisqEasyOfferbookChannelService.isVisible(item.getChannel()));
    }

    private boolean isMaker(BisqEasyOffer bisqEasyOffer) {
        return bisqEasyOffer.isMyOffer(userIdentityService.getMyUserProfileIds());
    }

    void onSelectMarketChannelItem(MarketChannelItem item) {
        if (item == null) {
            selectionService.selectChannel(null);
        } else if (!item.getChannel().equals(selectionService.getSelectedChannel().get())) {
            selectionService.selectChannel(item.getChannel());
        }
    }

    private void maybeSelectFirst() {
        if (selectionService.getSelectedChannel().get() == null &&
                !bisqEasyOfferbookChannelService.getChannels().isEmpty() &&
                !model.getSortedMarketChannelItems().isEmpty()) {
            selectionService.selectChannel(model.getSortedMarketChannelItems().get(0).getChannel());
        }
    }
}
