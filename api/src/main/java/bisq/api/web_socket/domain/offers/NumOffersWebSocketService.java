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

package bisq.api.web_socket.domain.offers;

import bisq.api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.Topic;
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bisq_easy.BisqEasyService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.user.UserService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class NumOffersWebSocketService extends SimpleObservableWebSocketService<ObservableHashMap<String, Integer>, HashMap<String, Integer>> {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;

    private final ObservableHashMap<String, Integer> numOffersByCurrencyCode = new ObservableHashMap<>();
    private final BisqEasyOfferbookMessageService bisqEasyOfferbookMessageService;
    private final Set<Pin> pins = new CopyOnWriteArraySet<>();

    public NumOffersWebSocketService(SubscriberRepository subscriberRepository,
                                     ChatService chatService,
                                     UserService userService,
                                     BisqEasyService bisqEasyService) {
        super(subscriberRepository, Topic.NUM_OFFERS);
        this.bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOfferbookMessageService = bisqEasyService.getBisqEasyOfferbookMessageService();

        bisqEasyOfferbookChannelService.getChannels().addObserver(() -> {
            pins.forEach(Pin::unbind);
            pins.clear();
            bisqEasyOfferbookChannelService.getChannels().forEach(
                    channel -> {
                        var code = channel.getMarket().getQuoteCurrencyCode();
                        ObservableSet<BisqEasyOfferbookMessage> chatMessages = channel.getChatMessages();
                        pins.add(chatMessages.addObserver(() -> {
                            var numOffers = (int) bisqEasyOfferbookMessageService.getOffers(channel).count();
                            numOffersByCurrencyCode.put(code, numOffers);
                        }));
                    }
            );
        });
    }

    @Override
    protected ObservableHashMap<String, Integer> getObservable() {
        return numOffersByCurrencyCode;
    }

    @Override
    protected HashMap<String, Integer> toPayload(ObservableHashMap<String, Integer> observable) {
        return new HashMap<>(observable);
    }

    @Override
    protected Pin setupObserver() {
        return getObservable().addObserver(this::onChange);
    }
}
