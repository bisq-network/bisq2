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

package bisq.api.web_socket.domain;

import bisq.api.dto.presentation.open_trades.TradeItemPresentationDto;
import bisq.api.dto.presentation.open_trades.TradeItemPresentationDtoFactory;
import bisq.api.dto.trade.bisq_easy.BisqEasyTradeDto;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Verifies the items list and the tradeId index stay in sync, so the O(1) findListItem lookups
 * (dedup on add, retrieval on remove/clear) keep behaving like the previous linear scan.
 */
class OpenTradeItemsServiceTest {
    private static final String TRADE_ID = "trade-1";

    @Test
    void addsItemOnceAndKeepsLookupInSyncOnRemove() {
        ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();
        ObservableSet<BisqEasyOpenTradeChannel> channels = new ObservableSet<>();
        BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
        BisqEasyOpenTradeChannelService channelService = mock(BisqEasyOpenTradeChannelService.class);
        BisqEasyTrade trade = trade();
        BisqEasyOpenTradeChannel channel = channel();
        when(bisqEasyTradeService.findTrade(TRADE_ID)).thenReturn(Optional.of(trade));
        when(channelService.findChannelByTradeId(TRADE_ID)).thenReturn(Optional.of(channel));

        TradeItemPresentationDto itemDto = itemDto();
        try (MockedStatic<TradeItemPresentationDtoFactory> factory = mockStatic(TradeItemPresentationDtoFactory.class)) {
            factory.when(() -> TradeItemPresentationDtoFactory.create(any(), any(), any(), any()))
                    .thenReturn(itemDto);
            OpenTradeItemsService service = newService(trades, channels, bisqEasyTradeService, channelService);

            // The same trade arrives via both the trade and the channel observer; the id index dedups it.
            trades.add(trade);
            channels.add(channel);
            assertThat(service.getItems().size()).isEqualTo(1);

            // Removing the trade clears the item, which only works if the id was still in the index.
            trades.remove(trade);
            assertThat(service.getItems().isEmpty()).isTrue();
        }
    }

    @Test
    void clearAlsoClearsTheLookupIndex() {
        ObservableSet<BisqEasyTrade> trades = new ObservableSet<>();
        ObservableSet<BisqEasyOpenTradeChannel> channels = new ObservableSet<>();
        BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
        BisqEasyOpenTradeChannelService channelService = mock(BisqEasyOpenTradeChannelService.class);
        BisqEasyTrade trade = trade();
        BisqEasyOpenTradeChannel channel = channel();
        when(channelService.findChannelByTradeId(TRADE_ID)).thenReturn(Optional.of(channel));

        TradeItemPresentationDto itemDto = itemDto();
        try (MockedStatic<TradeItemPresentationDtoFactory> factory = mockStatic(TradeItemPresentationDtoFactory.class)) {
            factory.when(() -> TradeItemPresentationDtoFactory.create(any(), any(), any(), any()))
                    .thenReturn(itemDto);
            OpenTradeItemsService service = newService(trades, channels, bisqEasyTradeService, channelService);

            trades.add(trade);
            assertThat(service.getItems().size()).isEqualTo(1);

            trades.clear();
            assertThat(service.getItems().isEmpty()).isTrue();

            // Re-adding the same trade recreates the item; if the index had not been cleared it would be deduped away.
            trades.add(trade);
            assertThat(service.getItems().size()).isEqualTo(1);
        }
    }

    private OpenTradeItemsService newService(ObservableSet<BisqEasyTrade> trades,
                                             ObservableSet<BisqEasyOpenTradeChannel> channels,
                                             BisqEasyTradeService bisqEasyTradeService,
                                             BisqEasyOpenTradeChannelService channelService) {
        when(bisqEasyTradeService.getTrades()).thenReturn(trades);
        when(channelService.getChannels()).thenReturn(channels);

        ChatService chatService = mock(ChatService.class);
        when(chatService.getBisqEasyOpenTradeChannelService()).thenReturn(channelService);
        TradeService tradeService = mock(TradeService.class);
        when(tradeService.getBisqEasyTradeService()).thenReturn(bisqEasyTradeService);
        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);

        OpenTradeItemsService service = new OpenTradeItemsService(chatService, tradeService, userService);
        service.initialize().join();
        return service;
    }

    private BisqEasyTrade trade() {
        BisqEasyTrade trade = mock(BisqEasyTrade.class);
        when(trade.getId()).thenReturn(TRADE_ID);
        return trade;
    }

    private BisqEasyOpenTradeChannel channel() {
        BisqEasyOpenTradeChannel channel = mock(BisqEasyOpenTradeChannel.class);
        when(channel.getTradeId()).thenReturn(TRADE_ID);
        when(channel.isInMediation()).thenReturn(false);
        when(channel.isInMediationObservable()).thenReturn(new Observable<>(false));
        return channel;
    }

    private TradeItemPresentationDto itemDto() {
        BisqEasyTradeDto tradeDto = mock(BisqEasyTradeDto.class);
        when(tradeDto.id()).thenReturn(TRADE_ID);
        TradeItemPresentationDto item = mock(TradeItemPresentationDto.class);
        when(item.trade()).thenReturn(tradeDto);
        return item;
    }
}
