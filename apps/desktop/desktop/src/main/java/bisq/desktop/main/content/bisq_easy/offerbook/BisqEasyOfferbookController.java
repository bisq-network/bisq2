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
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.util.ProtobufUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardController;
import bisq.desktop.main.content.chat.ChatController;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public final class BisqEasyOfferbookController extends ChatController<BisqEasyOfferbookView, BisqEasyOfferbookModel> {
    private final SettingsService settingsService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyOfferbookModel bisqEasyOfferbookModel;
    private Pin offerOnlySettingsPin, bisqEasyPrivateTradeChatChannelsPin, selectedChannelPin, marketPriceByCurrencyMapPin;
    private Subscription marketSelectorSearchPin, selectedMarketFilterPin, selectedOfferDirectionOrOwnerFilterPin,
            selectedPeerReputationFilterPin, selectedMarketSortTypePin;

    public BisqEasyOfferbookController(ServiceProvider serviceProvider) {
        super(serviceProvider, ChatChannelDomain.BISQ_EASY_OFFERBOOK, NavigationTarget.BISQ_EASY_OFFERBOOK);

        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        settingsService = serviceProvider.getSettingsService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
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
            if (searchText == null || searchText.trim().isEmpty()) {
                model.setMarketSearchTextPredicate(item -> true);
            } else {
                String search = searchText.trim().toLowerCase();
                model.setMarketSearchTextPredicate(item ->
                        item != null &&
                                (item.getMarket().getQuoteCurrencyCode().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyName().toLowerCase().contains(search))
                );
            }
            updateFilteredMarketChannelItems();
        });

        Filters.Markets persistedMarketsFilter = settingsService.getCookie().asString(CookieKey.MARKETS_FILTER).map(name ->
                        ProtobufUtils.enumFromProto(Filters.Markets.class, name, Filters.Markets.WITH_OFFERS))
                .orElse(Filters.Markets.WITH_OFFERS);
        model.getSelectedMarketsFilter().set(persistedMarketsFilter);

        selectedMarketFilterPin = EasyBind.subscribe(model.getSelectedMarketsFilter(), filter -> {
            if (filter != null) {
                model.setMarketFilterPredicate(filter.getPredicate());
                settingsService.setCookie(CookieKey.MARKETS_FILTER, model.getSelectedMarketsFilter().get().name());
                updateFilteredMarketChannelItems();
            }
        });

        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() -> {
            UIThread.run(() -> {
                model.setMarketPricePredicate(item -> marketPriceService.getMarketPriceByCurrencyMap().isEmpty() ||
                        marketPriceService.getMarketPriceByCurrencyMap().containsKey(item.getMarket()));
                updateFilteredMarketChannelItems();
            });
        });

        model.getMarketChannelItems().addListener(new WeakReference<>(
                (ListChangeListener<? super MarketChannelItem>) c -> updateFilteredMarketChannelItems()
        ).get());

        selectedOfferDirectionOrOwnerFilterPin = EasyBind.subscribe(model.getSelectedOfferDirectionOrOwnerFilter(), filter -> {
            if (filter == null) {
                // By default, show all offers (any direction or owner)
                model.getSelectedOfferDirectionOrOwnerFilter().set(Filters.OfferDirectionOrOwner.ALL);
                chatMessagesComponent.setBisqEasyOfferDirectionOrOwnerFilterPredicate(model.getSelectedOfferDirectionOrOwnerFilter().get().getPredicate());
            } else {
                chatMessagesComponent.setBisqEasyOfferDirectionOrOwnerFilterPredicate(filter.getPredicate());
            }
        });

        selectedPeerReputationFilterPin = EasyBind.subscribe(model.getSelectedPeerReputationFilter(), filter -> {
            if (filter == null) {
                // By default, show all offers (with any reputation)
                model.getSelectedPeerReputationFilter().set(Filters.PeerReputation.ALL);
                chatMessagesComponent.setBisqEasyPeerReputationFilterPredicate(model.getSelectedPeerReputationFilter().get().getPredicate());
            } else {
                chatMessagesComponent.setBisqEasyPeerReputationFilterPredicate(filter.getPredicate());
            }
        });

        MarketSortType persistedMarketSortType = settingsService.getCookie().asString(CookieKey.MARKET_SORT_TYPE).map(name ->
                        ProtobufUtils.enumFromProto(MarketSortType.class, name, MarketSortType.NUM_OFFERS))
                .orElse(MarketSortType.NUM_OFFERS);
        model.getSelectedMarketSortType().set(persistedMarketSortType);
        selectedMarketSortTypePin = EasyBind.subscribe(model.getSelectedMarketSortType(), marketSortType -> {
            if (marketSortType != null) {
                settingsService.setCookie(CookieKey.MARKET_SORT_TYPE, marketSortType.name());
            }
        });

        model.getSortedMarketChannelItems().setComparator(model.getSelectedMarketSortType().get().getComparator());

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
        selectedOfferDirectionOrOwnerFilterPin.unsubscribe();
        selectedPeerReputationFilterPin.unsubscribe();
        marketPriceByCurrencyMapPin.unbind();
        selectedMarketSortTypePin.unsubscribe();

        resetSelectedChildTarget();

        model.getMarketChannelItems().forEach(MarketChannelItem::cleanUp);
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
                        .ifPresent(item -> {
                            model.getSelectedMarketChannelItem().set(item);
                            updateSelectedMarketChannelItem(item);
                        });

                model.getSearchText().set("");
                resetSelectedChildTarget();

                String description = channel.getDescription();
                String channelTitle = description.replace("\n", " ").replaceAll("\\s*\\([^)]*\\)", "");
                model.getChannelTitle().set(channelTitle);

                String marketSpecs = channel.getDisplayString();
                model.getChannelDescription().set(marketSpecs);

                Market market = channel.getMarket();
                StackPane marketsImage = MarketImageComposition.imageBoxForMarkets(
                        market.getBaseCurrencyCode().toLowerCase(),
                        market.getQuoteCurrencyCode().toLowerCase());
                model.getChannelIconNode().set(marketsImage);

                updateMarketPrice();
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

    void onSortMarkets(MarketSortType marketSortType) {
        model.getSelectedMarketSortType().set(marketSortType);
        model.getSortedMarketChannelItems().setComparator(marketSortType.getComparator());
    }

    private void updateMarketPrice() {
        Market selectedMarket = getModel().getSelectedMarketChannelItem().get().getMarket();
        if (selectedMarket != null) {
            marketPriceService
                    .findMarketPrice(selectedMarket)
                    .ifPresent(marketPrice ->
                            model.getMarketPrice().set(PriceFormatter.format(marketPrice.getPriceQuote(), true)));
        }
    }

    private void updateFilteredMarketChannelItems() {
        model.getFilteredMarketChannelItems().setPredicate(item ->
                model.getMarketFilterPredicate().test(item) &&
                        model.getMarketSearchTextPredicate().test(item) &&
                        model.getMarketPricePredicate().test(item));
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

    private void updateSelectedMarketChannelItem(MarketChannelItem selectedItem) {
        model.getMarketChannelItems().forEach(item -> item.getSelected().set(item == selectedItem));
    }
}
