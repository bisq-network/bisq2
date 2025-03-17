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

package bisq.http_api.web_socket.domain.offers;

import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;
import bisq.http_api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.http_api.web_socket.subscription.Topic;
import bisq.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
public class NumOffersWebSocketService extends SimpleObservableWebSocketService<ObservableHashMap<String, Integer>, HashMap<String, Integer>> {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;

    public NumOffersWebSocketService(ObjectMapper objectMapper,
                                        SubscriberRepository subscriberRepository,
                                        ChatService chatService,
                                        UserService userService) {
        super(objectMapper, subscriberRepository, Topic.NUM_OFFERS);
        this.bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
    }

    @Override
    protected ObservableHashMap<String, Integer> getObservable() {
        return bisqEasyOfferbookChannelService.getNumOffersByCurrencyCode();
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
