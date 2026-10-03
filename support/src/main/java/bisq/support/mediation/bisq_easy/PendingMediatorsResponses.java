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

package bisq.support.mediation.bisq_easy;

import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mediator responses which arrived before the trade channel of their trade existed, for example because the maker was
 * offline when the offer was taken. The service processes them again when a trade channel is added.
 * <p>
 * Anybody can send a response with a made-up trade ID. Such a response never gets a channel, so at most
 * {@link #MAX_PENDING_RESPONSES} responses are kept. When the bound is reached, the oldest response is dropped, as
 * for pending trade messages (see docs/specifications/trade/pending-trade-messages.md).
 * <p>
 * A pending response is identified by its trade ID and the ID of its sender. So a response of another sender for the
 * same trade does not keep the response of the mediator out. BisqEasyMediatorsResponse itself is equal by trade ID
 * only. Its equality is not changed here, because the network layer also puts received messages into sets.
 */
@Slf4j
final class PendingMediatorsResponses {
    static final int MAX_PENDING_RESPONSES = 100;

    private record Key(String tradeId, String senderId) {
        private static Key of(BisqEasyMediatorsResponse response) {
            // The ID of the sender is the hash of its public key. The network layer verifies that the sender of the
            // response signed it with that key.
            return new Key(response.getTradeId(), response.getSenderNetworkId().getId());
        }
    }

    // Insertion order is the arrival order. All access is guarded by the monitor of this object.
    private final Map<Key, BisqEasyMediatorsResponse> responses = new LinkedHashMap<>();

    synchronized void add(BisqEasyMediatorsResponse response) {
        Key key = Key.of(response);
        if (responses.containsKey(key)) {
            return;
        }
        if (responses.size() >= MAX_PENDING_RESPONSES) {
            Iterator<Key> iterator = responses.keySet().iterator();
            Key oldest = iterator.next();
            iterator.remove();
            log.warn("We drop the oldest pending mediator response because the limit of {} pending responses is " +
                            "reached. tradeId={}",
                    MAX_PENDING_RESPONSES,
                    StringUtils.sanitizeForLog(oldest.tradeId()));
        }
        responses.put(key, response);
    }

    /**
     * Removes the pending response with the trade ID and the sender of the given response. Responses of other senders
     * for the same trade stay pending.
     */
    synchronized boolean remove(BisqEasyMediatorsResponse response) {
        return responses.remove(Key.of(response)) != null;
    }

    synchronized void clear() {
        responses.clear();
    }

    synchronized int size() {
        return responses.size();
    }

    /**
     * A copy of the pending responses in arrival order. The caller processes the responses outside the monitor of
     * this object, and processing a response removes it.
     */
    synchronized List<BisqEasyMediatorsResponse> getResponsesInArrivalOrder() {
        return new ArrayList<>(responses.values());
    }
}
