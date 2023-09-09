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

package bisq.desktop.main.content.bisq_easy.create_offer.market;

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
public class CreateOfferMarketController implements Controller {
    private final CreateOfferMarketModel model;
    @Getter
    private final CreateOfferMarketView view;
    private final ChatService chatService;
    private final Runnable onNextHandler;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyOfferbookSelectionService bisqEasyOfferbookSelectionService;
    private Subscription searchTextPin;

    public CreateOfferMarketController(ServiceProvider serviceProvider, Runnable onNextHandler) {
        this.onNextHandler = onNextHandler;
        chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookSelectionService = chatService.getBisqEasyOfferbookChannelSelectionService();
        model = new CreateOfferMarketModel();
        view = new CreateOfferMarketView(model, this);
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }

    public void setDirection(Direction direction) {
        model.setDirection(direction);
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.setHeadline(model.getDirection().isBuy() ? Res.get("bisqEasy.createOffer.market.headline.buyer") : Res.get("bisqEasy.createOffer.market.headline.seller"));
        model.getSearchText().set("");
        if (model.getSelectedMarket().get() == null) {
            // Use selected public channel or if private channel is selected we use any of the public channels for 
            // setting the default market 
            Optional.ofNullable(bisqEasyOfferbookSelectionService.getSelectedChannel().get())
                    .filter(channel -> channel instanceof BisqEasyOfferbookChannel)
                    .map(channel -> (BisqEasyOfferbookChannel) channel)
                    .or(() -> bisqEasyOfferbookChannelService.getVisibleChannels().stream().findFirst())
                    .map(BisqEasyOfferbookChannel::getMarket)
                    .ifPresent(market -> model.getSelectedMarket().set(market));
        }

        model.getListItems().setAll(MarketRepository.getAllFiatMarkets().stream()
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
                    CreateOfferMarketView.MarketListItem item = new CreateOfferMarketView.MarketListItem(market, numOffersInChannel, numUsersInChannel);
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
                                (item.getQuoteCurrencyName().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyName().toLowerCase().contains(search))
                );
            }
        });
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
    }

    void onMarketListItemClicked(CreateOfferMarketView.MarketListItem item) {
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

    private Optional<CreateOfferMarketView.MarketListItem> findMarketListItem(Market market) {
        return model.getListItems().stream()
                .filter(marketListItem -> marketListItem.getMarket().equals(market))
                .findAny();
    }
}
