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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

// Keeps the unverified chat message keys of each trade together with the retained requests they were computed
// from, and computes them again when the trade's retained requests are not the same instances. A case gains a
// request in place, so a change of the case set alone does not show every change.
final class UnverifiedChatMessageKeysByTrade {
    private final Collection<BisqEasyMediationCase> mediationCases;
    private final Map<String, Entry> entryByTradeId = new ConcurrentHashMap<>();

    UnverifiedChatMessageKeysByTrade(Collection<BisqEasyMediationCase> mediationCases) {
        this.mediationCases = mediationCases;
    }

    Set<String> get(String tradeId) {
        List<BisqEasyMediationRequest> requestsForTrade = BisqEasyMediatorService.getRequestsForTrade(tradeId, mediationCases);
        if (requestsForTrade.isEmpty()) {
            entryByTradeId.remove(tradeId);
            return Set.of();
        }
        Entry entry = entryByTradeId.get(tradeId);
        if (entry != null && entry.isComputedFrom(requestsForTrade)) {
            return entry.unverifiedChatMessageKeys();
        }
        Set<String> unverifiedChatMessageKeys = BisqEasyMediatorService.getUnverifiedChatMessageKeys(requestsForTrade);
        entryByTradeId.put(tradeId, new Entry(requestsForTrade, unverifiedChatMessageKeys));
        return unverifiedChatMessageKeys;
    }

    private record Entry(List<BisqEasyMediationRequest> requests, Set<String> unverifiedChatMessageKeys) {
        // By reference, as request equality ignores the chat messages.
        private boolean isComputedFrom(List<BisqEasyMediationRequest> requestsForTrade) {
            return requests.size() == requestsForTrade.size()
                    && IntStream.range(0, requests.size()).allMatch(index -> requests.get(index) == requestsForTrade.get(index));
        }
    }
}
