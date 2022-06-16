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

package bisq.desktop.primary.overlay.createOffer.market;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.common.view.Controller;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.PublicTradeChannel;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.PublicTradeChatMessage;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class MarketController implements Controller {
    private final MarketModel model;
    @Getter
    private final MarketView view;
    private final ChatService chatService;
    private Subscription searchTextPin;

    public MarketController(DefaultApplicationService applicationService) {
        chatService = applicationService.getChatService();
        model = new MarketModel();
        view = new MarketView(model, this);
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }

    @Override
    public void onActivate() {
        model.getListItems().setAll(MarketRepository.getAllFiatMarkets().stream()
                .map(market -> {
                    Set<PublicTradeChatMessage> offerMessages = chatService.getPublicTradeChannels().stream()
                            .filter(channel -> channel.getMarket().isPresent())
                            .filter(channel -> channel.getMarket().get().equals(market))
                            .flatMap(channel -> channel.getChatMessages().stream())
                            .filter(message -> message.getTradeChatOffer().isPresent())
                            .collect(Collectors.toSet());
                    int numOffersInChannel = offerMessages.size();
                    int numUsersInChannel = (int) offerMessages.stream()
                            .map(ChatMessage::getAuthorId)
                            .count();
                    MarketView.MarketListItem marketListItem = new MarketView.MarketListItem(market, numOffersInChannel, numUsersInChannel);
                    return marketListItem;
                })
                .collect(Collectors.toList()));

        // We pre-select the market from the selected channel, or if not available we use the default market.
        Optional.ofNullable(chatService.getSelectedTradeChannel().get())
                .filter(channel -> channel instanceof PublicTradeChannel)
                .map(channel -> (PublicTradeChannel) channel)
                .map(PublicTradeChannel::getMarket)
                .orElse(Optional.of(MarketRepository.getDefault()))
                .flatMap(this::findMarketListItem)
                .ifPresent(marketListItem -> {
                    marketListItem.getSelected().set(true);
                    model.getSelectedMarket().set(marketListItem.getMarket());
                });

        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                model.getFilteredList().setPredicate(item -> true);
            } else {
                String search = searchText.toLowerCase();
                model.getFilteredList().setPredicate(item ->
                        item != null &&
                                (item.getMarketCodes().toLowerCase().contains(search) ||
                                        item.getMarketName().toLowerCase().contains(search) ||
                                        item.getNumOffers().contains(search) ||
                                        item.getNumUsers().contains(search))
                );
            }
        });
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
    }

    public void onSelect(MarketView.MarketListItem item) {
        //unselect previous
        Optional.ofNullable(model.getSelectedMarketListItem().get()).ifPresent(selItem -> selItem.getSelected().set(false));

        item.getSelected().set(true);
        model.getSelectedMarketListItem().set(item);
        model.getSelectedMarket().set(item.getMarket());
    }

    private Optional<MarketView.MarketListItem> findMarketListItem(Market market) {
        return model.getListItems().stream()
                .filter(marketListItem -> marketListItem.getMarket().equals(market))
                .findAny();
    }
}
