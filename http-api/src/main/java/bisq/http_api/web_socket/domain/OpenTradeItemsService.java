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

package bisq.http_api.web_socket.domain;

import bisq.chat.ChatService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.dto.presentation.open_trades.TradeItemPresentationDto;
import bisq.dto.presentation.open_trades.TradeItemPresentationDtoFactory;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.UserService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OpenTradeItemsService implements Service {
    private final BisqEasyOpenTradeChannelService channelService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;

    @Getter
    private final ObservableArray<TradeItemPresentationDto> items = new ObservableArray<>();
    @Getter
    private final Observable<Boolean> isAnyTradeInMediation = new Observable<>();

    private Pin channelsPin, tradesPin;
    private final Map<String, Pin> isInMediationPinMap = new HashMap<>();

    public OpenTradeItemsService(ChatService chatService,
                                 TradeService tradeService,
                                 UserService userService) {

        channelService = chatService.getBisqEasyOpenTradeChannelService();
        bisqEasyTradeService = tradeService.getBisqEasyTradeService();

        userProfileService = userService.getUserProfileService();
        reputationService = userService.getReputationService();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        tradesPin = bisqEasyTradeService.getTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyTrade trade) {
                handleTradeAdded(trade);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyTrade) {
                    handleTradeRemoved((BisqEasyTrade) element);
                }
            }

            @Override
            public void clear() {
                handleTradesCleared();
            }
        });

        channelsPin = channelService.getChannels().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyOpenTradeChannel channel) {
                handleChannelAdded(channel);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyOpenTradeChannel) {
                    handleChannelRemoved((BisqEasyOpenTradeChannel) element);
                }
            }

            @Override
            public void clear() {
                handleChannelsCleared();
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (channelsPin != null) {
            channelsPin.unbind();
        }
        if (tradesPin != null) {
            tradesPin.unbind();
        }
        isInMediationPinMap.values().forEach(Pin::unbind);
        isInMediationPinMap.clear();
        return CompletableFuture.completedFuture(true);
    }

    // Trade
    private void handleTradeAdded(BisqEasyTrade trade) {
        channelService.findChannelByTradeId(trade.getId())
                .ifPresentOrElse(channel -> handleTradeAndChannelAdded(trade, channel),
                        () -> log.warn("Trade with id {} was added but associated channel is not found.", trade.getId()));
    }

    private void handleTradeRemoved(BisqEasyTrade trade) {
        String tradeId = trade.getId();
        channelService.findChannelByTradeId(tradeId)
                .ifPresentOrElse(channel -> handleTradeAndChannelRemoved(trade),
                        () -> {
                            if (findListItem(trade).isEmpty()) {
                                log.warn("Trade with id {} was removed but associated channel and listItem is not found. " +
                                        "We ignore that call.", tradeId);
                            } else {
                                log.warn("Trade with id {} was removed but associated channel is not found but a listItem with that trade is still present." +
                                        "We call handleTradeAndChannelRemoved.", tradeId);
                                handleTradeAndChannelRemoved(trade);
                            }
                        });
    }

    private void handleTradesCleared() {
        handleClearTradesAndChannels();
    }

    // Channel
    private void handleChannelAdded(BisqEasyOpenTradeChannel channel) {
        bisqEasyTradeService.findTrade(channel.getTradeId())
                .ifPresentOrElse(trade -> handleTradeAndChannelAdded(trade, channel),
                        () -> log.warn("Channel with tradeId {} was added but associated trade is not found.", channel.getTradeId()));
    }

    private void handleChannelRemoved(BisqEasyOpenTradeChannel channel) {
        String tradeId = channel.getTradeId();
        bisqEasyTradeService.findTrade(tradeId)
                .ifPresentOrElse(this::handleTradeAndChannelRemoved,
                        () -> {
                            Optional<TradeItemPresentationDto> listItem = findListItem(tradeId);
                            if (listItem.isEmpty()) {
                                log.debug("Channel with tradeId {} was removed but associated trade and the listItem is not found. " +
                                        "This is expected as we first remove the trade and then the channel.", tradeId);
                            } else {
                                log.warn("Channel with tradeId {} was removed but associated trade is not found but we still have the listItem with that trade. " +
                                        "We call handleTradeAndChannelRemoved.", tradeId);

                                items.remove(listItem.get());
                                if (isInMediationPinMap.containsKey(tradeId)) {
                                    isInMediationPinMap.get(tradeId).unbind();
                                    isInMediationPinMap.remove(tradeId);
                                    updateIsAnyTradeInMediation();
                                }
                            }
                        });
    }

    private void handleChannelsCleared() {
        handleClearTradesAndChannels();
    }

    // TradeAndChannel
    private void handleTradeAndChannelAdded(BisqEasyTrade trade, BisqEasyOpenTradeChannel channel) {
        if (findListItem(trade).isPresent()) {
            log.debug("We got called handleTradeAndChannelAdded but we have that trade list item already. " +
                    "This is expected as we get called both when a trade is added and the associated channel.");
            return;
        }
        if (trade.getContract() == null) {
            // TODO should we throw an exception?
            log.error("Contract is null for trade {}", trade);
            return;
        }

        TradeItemPresentationDto item = TradeItemPresentationDtoFactory.create(trade, channel, userProfileService, reputationService);
        items.add(item);

        String tradeId = trade.getId();
        if (isInMediationPinMap.containsKey(tradeId)) {
            isInMediationPinMap.get(tradeId).unbind();
        }
        Pin pin = channel.isInMediationObservable().addObserver(isInMediation -> {
            if (isInMediation != null) {
                updateIsAnyTradeInMediation();
            }
        });
        isInMediationPinMap.put(tradeId, pin);
    }


    private void handleTradeAndChannelRemoved(BisqEasyTrade trade) {
        String tradeId = trade.getId();
        if (findListItem(trade).isEmpty()) {
            log.warn("We got called handleTradeAndChannelRemoved but we have not found any trade list item with tradeId {}", tradeId);
            return;
        }

        TradeItemPresentationDto item = findListItem(trade).get();
        items.remove(item);

        if (isInMediationPinMap.containsKey(tradeId)) {
            isInMediationPinMap.get(tradeId).unbind();
            isInMediationPinMap.remove(trade.getId());
            updateIsAnyTradeInMediation();
        }
    }

    private void handleClearTradesAndChannels() {
        items.clear();

        isInMediationPinMap.values().forEach(Pin::unbind);
        isInMediationPinMap.clear();
        updateIsAnyTradeInMediation();
    }

    private Optional<TradeItemPresentationDto> findListItem(BisqEasyTrade trade) {
        return findListItem(trade.getId());
    }

    private Optional<TradeItemPresentationDto> findListItem(String tradeId) {
        return items.stream()
                .filter(item -> item.trade().id().equals(tradeId))
                .findAny();
    }


    private void updateIsAnyTradeInMediation() {
        boolean value = channelService.getChannels().stream()
                .anyMatch(BisqEasyOpenTradeChannel::isInMediation);
        isAnyTradeInMediation.set(value);
    }
}
