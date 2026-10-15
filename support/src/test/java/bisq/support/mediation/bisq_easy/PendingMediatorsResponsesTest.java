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

import bisq.network.identity.NetworkId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static bisq.support.mediation.bisq_easy.BisqEasyMediationRequestServicePendingResponsesTest.networkId;
import static bisq.support.mediation.bisq_easy.PendingMediatorsResponses.MAX_PENDING_RESPONSES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pending mediator responses are bounded, the oldest is dropped first, and a response is identified by its trade ID
 * and its sender. See docs/specifications/trade/pending-trade-messages.md.
 */
class PendingMediatorsResponsesTest {
    private final PendingMediatorsResponses pending = new PendingMediatorsResponses();
    private final NetworkId sender = networkId("sender");

    @Test
    void oldestResponseIsDroppedWhenTheBoundIsReached() {
        for (int i = 0; i <= MAX_PENDING_RESPONSES; i++) {
            pending.add(new BisqEasyMediatorsResponse("trade-" + i, sender));
        }

        List<BisqEasyMediatorsResponse> responses = pending.getResponsesInArrivalOrder();
        assertEquals(MAX_PENDING_RESPONSES, responses.size());
        assertEquals("trade-1", responses.get(0).getTradeId());
        assertEquals("trade-" + MAX_PENDING_RESPONSES, responses.get(MAX_PENDING_RESPONSES - 1).getTradeId());
    }

    @Test
    void responsesOfDifferentSendersForTheSameTradeAreKeptApart() {
        BisqEasyMediatorsResponse first = new BisqEasyMediatorsResponse("trade", networkId("first-sender"));
        BisqEasyMediatorsResponse second = new BisqEasyMediatorsResponse("trade", networkId("second-sender"));
        pending.add(first);
        pending.add(second);

        // BisqEasyMediatorsResponse is equal by trade ID only, so the senders are compared
        assertEquals(List.of(first.getSenderNetworkId(), second.getSenderNetworkId()), sendersOfPendingResponses());

        assertTrue(pending.remove(first));
        assertEquals(List.of(second.getSenderNetworkId()), sendersOfPendingResponses());
    }

    @Test
    void responseWhichIsAlreadyPendingKeepsItsPlace() {
        pending.add(new BisqEasyMediatorsResponse("trade-a", sender));
        pending.add(new BisqEasyMediatorsResponse("trade-b", sender));
        pending.add(new BisqEasyMediatorsResponse("trade-a", sender));

        List<BisqEasyMediatorsResponse> responses = pending.getResponsesInArrivalOrder();
        assertEquals(2, responses.size());
        assertEquals("trade-a", responses.get(0).getTradeId());
        assertEquals("trade-b", responses.get(1).getTradeId());
    }

    @Test
    void removeOfAnotherSendersResponseKeepsThePendingResponse() {
        pending.add(new BisqEasyMediatorsResponse("trade", sender));

        assertFalse(pending.remove(new BisqEasyMediatorsResponse("trade", networkId("other-sender"))));
        assertEquals(List.of(sender), sendersOfPendingResponses());
    }

    private List<NetworkId> sendersOfPendingResponses() {
        return pending.getResponsesInArrivalOrder().stream()
                .map(BisqEasyMediatorsResponse::getSenderNetworkId)
                .toList();
    }
}
