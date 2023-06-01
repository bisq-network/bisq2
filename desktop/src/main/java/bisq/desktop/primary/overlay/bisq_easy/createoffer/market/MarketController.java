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

package bisq.desktop.primary.overlay.bisq_easy.createoffer.market;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.BisqEasyChatChannelSelectionService;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannel;
import bisq.chat.bisqeasy.channel.pub.BisqEasyPublicChatChannelService;
import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.chat.message.ChatMessage;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.common.view.Controller;
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
    private final Runnable onNextHandler;
    private final BisqEasyPublicChatChannelService bisqEasyPublicChatChannelService;
    private final BisqEasyChatChannelSelectionService bisqEasyChatChannelSelectionService;
    private Subscription searchTextPin;

    public MarketController(DefaultApplicationService applicationService, Runnable onNextHandler) {
        this.onNextHandler = onNextHandler;
        chatService = applicationService.getChatService();
        bisqEasyPublicChatChannelService = chatService.getBisqEasyPublicChatChannelService();
        bisqEasyChatChannelSelectionService = chatService.getBisqEasyChatChannelSelectionService();
        model = new MarketModel();
        view = new MarketView(model, this);
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        model.getSearchText().set("");
        if (model.getSelectedMarket().get() == null) {
            // Use selected public channel or if private channel is selected we use any of the public channels for 
            // setting the default market 
            Optional.ofNullable(bisqEasyChatChannelSelectionService.getSelectedChannel().get())
                    .filter(channel -> channel instanceof BisqEasyPublicChatChannel)
                    .map(channel -> (BisqEasyPublicChatChannel) channel)
                    .or(() -> bisqEasyPublicChatChannelService.getVisibleChannels().stream().findFirst())
                    .map(BisqEasyPublicChatChannel::getMarket)
                    .ifPresent(market -> model.getSelectedMarket().set(market));
        }

        model.getListItems().setAll(MarketRepository.getAllFiatMarkets().stream()
                .map(market -> {
                    Set<BisqEasyPublicChatMessage> allMessages = bisqEasyPublicChatChannelService.getChannels().stream()
                            .filter(channel -> channel.getMarket().equals(market))
                            .flatMap(channel -> channel.getChatMessages().stream())
                            .collect(Collectors.toSet());
                    int numOffersInChannel = (int) allMessages.stream()
                            .filter(message -> message.getBisqEasyOffer().isPresent())
                            .distinct()
                            .count();
                    int numUsersInChannel = (int) allMessages.stream()
                            .map(ChatMessage::getAuthorUserProfileId)
                            .distinct()
                            .count();
                    MarketView.MarketListItem item = new MarketView.MarketListItem(market, numOffersInChannel, numUsersInChannel);
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
                                (item.getMarketCodes().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyName().toLowerCase().contains(search))
                );
            }
        });
    }

    @Override
    public void onDeactivate() {
        searchTextPin.unsubscribe();
    }

    void onMarketListItemClicked(MarketView.MarketListItem item) {
        if (item == null) {
            return;
        }
        if (item.equals(model.getSelectedMarketListItem().get())) {
            onNextHandler.run();
        }
        model.getSelectedMarketListItem().set(item);
        model.getSelectedMarket().set(item.getMarket());
    }

    private Optional<MarketView.MarketListItem> findMarketListItem(Market market) {
        return model.getListItems().stream()
                .filter(marketListItem -> marketListItem.getMarket().equals(market))
                .findAny();
    }
}
