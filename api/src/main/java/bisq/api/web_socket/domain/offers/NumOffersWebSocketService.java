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

import bisq.api.offerbook.RecordedOfferItems;
import bisq.api.web_socket.domain.SimpleObservableWebSocketService;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.Topic;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;

import java.util.HashMap;

// The count of the offers which the OFFERS topic sends, per market. OffersWebSocketService records those offers and
// publishes their count, so that the two topics describe the same set.
public class NumOffersWebSocketService extends SimpleObservableWebSocketService<ObservableHashMap<String, Integer>, HashMap<String, Integer>> {
    private final RecordedOfferItems recordedOffers;

    public NumOffersWebSocketService(SubscriberRepository subscriberRepository, RecordedOfferItems recordedOffers) {
        super(subscriberRepository, Topic.NUM_OFFERS);
        this.recordedOffers = recordedOffers;
    }

    @Override
    protected ObservableHashMap<String, Integer> getObservable() {
        return recordedOffers.getNumOffersByQuoteCurrencyCode();
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
