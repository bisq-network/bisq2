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

package bisq.desktop.main.content.bisq_easy.trade_wizard.market;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TradeWizardMarketController implements Controller {
    private final TradeWizardMarketModel model;
    @Getter
    private final TradeWizardMarketView view;
    private final ChatService chatService;
    private final Runnable onNextHandler;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyOfferbookSelectionService bisqEasyOfferbookSelectionService;
    private final MarketPriceService marketPriceService;
    private Subscription searchTextPin;

    public TradeWizardMarketController(ServiceProvider serviceProvider, Runnable onNextHandler) {
        this.onNextHandler = onNextHandler;
        chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookSelectionService = chatService.getBisqEasyOfferbookChannelSelectionService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        model = new TradeWizardMarketModel();
        view = new TradeWizardMarketView(model, this);
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            model.setDirection(direction);
        }
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.setHeadline(model.getDirection().isBuy() ? Res.get("bisqEasy.tradeWizard.market.headline.buyer") : Res.get("bisqEasy.tradeWizard.market.headline.seller"));
        model.getSearchText().set("");
        if (model.getSelectedMarket().get() == null) {
            // Use selected public channel or if private channel is selected we use any of the public channels for 
            // setting the default market 
            Optional.ofNullable(bisqEasyOfferbookSelectionService.getSelectedChannel().get())
                    .filter(channel -> channel instanceof BisqEasyOfferbookChannel)
                    .map(channel -> (BisqEasyOfferbookChannel) channel)
                    .map(BisqEasyOfferbookChannel::getMarket)
                    .ifPresent(market -> model.getSelectedMarket().set(market));
        }

        model.getListItems().setAll(MarketRepository.getAllFiatMarkets().stream()
                .filter(market -> marketPriceService.getMarketPriceByCurrencyMap().containsKey(market))
                .map(market -> {
                    Set<BisqEasyOfferbookMessage> allMessages = bisqEasyOfferbookChannelService.getChannels().stream()
                            .filter(channel -> channel.getMarket().equals(market))
                            .flatMap(channel -> channel.getChatMessages().stream())
                            .collect(Collectors.toSet());
                    int numOffersInChannel = (int) allMessages.stream()
                            .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                            .distinct()
                            .count();
                    int numUsersInChannel = (int) allMessages.stream()
                            .map(ChatMessage::getAuthorUserProfileId)
                            .distinct()
                            .count();
                    TradeWizardMarketView.ListItem item = new TradeWizardMarketView.ListItem(market, numOffersInChannel, numUsersInChannel);
                    if (market.equals(model.getSelectedMarket().get())) {
                        model.getSelectedMarketListItem().set(item);
                    }
                    return item;
                })
                .collect(Collectors.toList()));

        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                model.getFilteredList().setPredicate(item -> true);
            } else {
                String search = searchText.toLowerCase();
                model.getFilteredList().setPredicate(item ->
                        item != null &&
                                (item.getQuoteCurrencyDisplayName().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyDisplayName().toLowerCase().contains(search))
                );
            }
        });
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
    }

    void onMarketListItemClicked(TradeWizardMarketView.ListItem item) {
        if (item == null) {
            return;
        }
        if (item.equals(model.getSelectedMarketListItem().get())) {
            onNextHandler.run();
        }
        model.getSelectedMarketListItem().set(item);
        model.getSelectedMarket().set(item.getMarket());
        bisqEasyOfferbookChannelService.findChannel(item.getMarket())
                .ifPresent(bisqEasyOfferbookSelectionService::selectChannel);
    }
}
