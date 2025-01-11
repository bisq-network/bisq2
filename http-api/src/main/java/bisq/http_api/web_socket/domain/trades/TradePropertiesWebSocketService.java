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

package bisq.http_api.web_socket.domain.trades;

import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.dto.DtoMappings;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static bisq.http_api.web_socket.subscription.Topic.TRADE_PROPERTIES;

@Slf4j
public class TradePropertiesWebSocketService extends BaseWebSocketService {
    private final BisqEasyTradeService bisqEasyTradeService;

    private Pin tradesPin;
    private final Map<String, Set<Pin>> pinsByTradeId = new HashMap<>();

    public TradePropertiesWebSocketService(ObjectMapper objectMapper,
                                           SubscriberRepository subscriberRepository,
                                           TradeService tradeService) {
        super(objectMapper, subscriberRepository, TRADE_PROPERTIES);
        bisqEasyTradeService = tradeService.getBisqEasyTradeService();
    }


    @Override
    public CompletableFuture<Boolean> initialize() {
        tradesPin = bisqEasyTradeService.getTrades().addObserver(new CollectionObserver<>() {
            @Override
            public void add(BisqEasyTrade bisqEasyTrade) {
                String tradeId = bisqEasyTrade.getId();
                pinsByTradeId.computeIfAbsent(tradeId, k -> new HashSet<>());
                Set<Pin> pins = pinsByTradeId.get(tradeId);
                pins.add(observeTradeState(bisqEasyTrade, tradeId));
                pins.add(observeInterruptTradeInitiator(bisqEasyTrade, tradeId));
                pins.add(observePaymentAccountData(bisqEasyTrade, tradeId));
                pins.add(observeBitcoinPaymentData(bisqEasyTrade, tradeId));
                pins.add(observePaymentProof(bisqEasyTrade, tradeId));
                pins.add(observeErrorMessage(bisqEasyTrade, tradeId));
                pins.add(observeErrorStackTrace(bisqEasyTrade, tradeId));
                pins.add(observePeersErrorMessage(bisqEasyTrade, tradeId));
                pins.add(observePeersErrorStackTrace(bisqEasyTrade, tradeId));
            }

            @Override
            public void remove(Object element) {
                if (element instanceof BisqEasyTrade bisqEasyTrade) {
                    String tradeId = bisqEasyTrade.getId();
                    Optional.ofNullable(pinsByTradeId.remove(tradeId))
                            .ifPresent(set -> set.forEach(Pin::unbind));
                }
            }

            @Override
            public void clear() {
                pinsByTradeId.values().forEach(set -> set.forEach(Pin::unbind));
                pinsByTradeId.clear();
            }
        });
        return CompletableFuture.completedFuture(true);
    }


    private Pin observeTradeState(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.tradeStateObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.tradeState = Optional.of(DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(value));
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observeInterruptTradeInitiator(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.getInterruptTradeInitiator().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.interruptTradeInitiator = Optional.of(DtoMappings.RoleMapping.fromBisq2Model(value));
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observePaymentAccountData(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.getPaymentAccountData().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.paymentAccountData = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observeBitcoinPaymentData(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.getBitcoinPaymentData().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.bitcoinPaymentData = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observePaymentProof(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.getPaymentProof().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.paymentProof = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observeErrorMessage(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.errorMessageObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.errorMessage = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observeErrorStackTrace(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.errorStackTraceObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.errorStackTrace = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observePeersErrorMessage(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.peersErrorMessageObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.peersErrorMessage = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    private Pin observePeersErrorStackTrace(BisqEasyTrade bisqEasyTrade, String tradeId) {
        return bisqEasyTrade.peersErrorStackTraceObservable().addObserver(value -> {
            if (value != null) {
                var data = new TradePropertiesDto();
                data.peersErrorStackTrace = Optional.of(value);
                send(Map.of(tradeId, data));
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        tradesPin.unbind();
        pinsByTradeId.values().forEach(set -> set.forEach(Pin::unbind));
        pinsByTradeId.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        List<Map<String, TradePropertiesDto>> maps = bisqEasyTradeService.getTrades().stream()
                .map(bisqEasyTrade -> {
                    var data = new TradePropertiesDto();
                    data.tradeState = Optional.ofNullable(DtoMappings.BisqEasyTradeStateMapping.fromBisq2Model(bisqEasyTrade.getTradeState()));
                    data.interruptTradeInitiator = Optional.ofNullable(DtoMappings.RoleMapping.fromBisq2Model(bisqEasyTrade.getInterruptTradeInitiator().get()));
                    data.paymentAccountData = Optional.ofNullable(bisqEasyTrade.getPaymentAccountData().get());
                    data.bitcoinPaymentData = Optional.ofNullable(bisqEasyTrade.getBitcoinPaymentData().get());
                    data.paymentProof = Optional.ofNullable(bisqEasyTrade.getPaymentProof().get());
                    data.errorMessage = Optional.ofNullable(bisqEasyTrade.getErrorMessage());
                    data.errorStackTrace = Optional.ofNullable(bisqEasyTrade.getErrorStackTrace());
                    data.peersErrorMessage = Optional.ofNullable(bisqEasyTrade.getPeersErrorMessage());
                    data.peersErrorStackTrace = Optional.ofNullable(bisqEasyTrade.getPeersErrorStackTrace());
                    return Map.of(bisqEasyTrade.getId(), data);
                })
                .collect(Collectors.toList());
        return toJson(maps);
    }

    private void send(Map<String, TradePropertiesDto> map) {
        send(Collections.singletonList(map));
    }

    private void send(List<Map<String, TradePropertiesDto>> maps) {
        // The payload is defined as a list to support batch data delivery at subscribe.
        toJson(maps).ifPresent(json -> {
            subscriberRepository.findSubscribers(topic)
                    .ifPresent(subscribers -> subscribers
                            .forEach(subscriber -> send(json, subscriber, ModificationType.REPLACE)));
        });
    }
}
